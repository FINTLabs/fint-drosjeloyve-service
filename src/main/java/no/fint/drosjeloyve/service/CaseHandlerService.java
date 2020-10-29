package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.exception.FinalStatusPendingException;
import no.fint.drosjeloyve.client.AltinnClient;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.factory.DrosjeloyveResourceFactory;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import no.fint.drosjeloyve.util.CertificateConverter;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import static reactor.util.retry.Retry.withThrowable;

@Slf4j
@Service
public class CaseHandlerService {
    private final FintClient fintClient;
    private final AltinnClient altinnClient;
    private final AltinnApplicationRepository repository;

    private final CertificateConverter certificateConverter = new CertificateConverter();

    public final Retry<?> finalStatusPending;

    private static final String BANKRUPTCY = "KonkursDrosje";
    private static final String ARREARS = "RestanserDrosje";

    @Value("${fint.endpoints.drosjeloyve}")
    private String drosjeloyveEndpoint;

    @Value("${fint.endpoints.drosjeloyve-mappe-id}")
    private String drosjeloyveMappeIdEndpoint;

    @Value("${fint.endpoints.dokumentfil}")
    private String dokumentfilEndpoint;

    @Value("${fint.endpoints.application}")
    private String applicationEndpoint;

    @Value("${fint.endpoints.attachment}")
    private String attachmentEndpoint;

    @Value("${fint.endpoints.evidence}")
    private String evidenceEndpoint;

    public CaseHandlerService(FintClient fintClient, AltinnClient altinnClient, AltinnApplicationRepository repository, OrganisationProperties organisationProperties) {
        this.fintClient = fintClient;
        this.altinnClient = altinnClient;
        this.repository = repository;

        finalStatusPending = Retry.anyOf(FinalStatusPendingException.class)
                .exponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5))
                .timeout(Duration.ofMinutes(30))
                .doOnRetry(exception -> log.info("{}", exception));
    }

    public void create(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        createApplication()
                .andThen(createForm())
                .andThen(createAttachments())
                .andThen(createEvidence())
                .accept(organisation, application);
    }

    public void update(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        Optional<AltinnApplication> existingCase = repository.findByRequestorAndSubject(application.getRequestor(), application.getSubject());

        Optional<String> caseId = existingCase.map(AltinnApplication::getCaseId);

        if (caseId.isPresent()) {
            application.setCaseId(caseId.get());
            repository.save(application);

            createForm()
                    .andThen(createAttachments())
                    .andThen(createEvidence())
                    .accept(organisation, application);
        } else {
            create(organisation, application);
        }
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> createApplication() {
        return (organisation, application) -> {
            if (application.getCaseId() == null) {
                fintClient.postResource(organisation, DrosjeloyveResourceFactory.ofBasic(application), drosjeloyveEndpoint)
                        .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, responseEntity.getHeaders().getLocation())
                                .doOnSuccess(statusEntity -> {
                                    if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                        throw new FinalStatusPendingException();
                                    }

                                    getId(statusEntity, "mappeid/").ifPresent(id -> {
                                        application.setCaseId(id);
                                        repository.save(application);
                                    });
                                })
                                .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                .retryWhen(withThrowable(finalStatusPending))
                                .subscribe())
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe();
            }
        };
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> createForm() {
        return (organisation, application) -> {
            if (application.getForm().getDocumentId() == null) {
                altinnClient.getApplication(applicationEndpoint, application.getArchiveReference(), application.getLanguageCode())
                        .doOnSuccess(bytes -> {
                            if (bytes.length == 0) {
                                log.warn("Received empty PDF of form for archive reference {}.", application.getArchiveReference());
                                return;
                            }

                            fintClient.postFile(organisation, bytes, MediaType.APPLICATION_PDF, "søknadsskjema.pdf", dokumentfilEndpoint)
                                    .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, responseEntity.getHeaders().getLocation())
                                            .doOnSuccess(statusEntity -> {
                                                if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                                    throw new FinalStatusPendingException();
                                                }

                                                getId(statusEntity, "/").ifPresent(id -> {
                                                    application.getForm().setDocumentId(id);
                                                    repository.save(application);
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe();
            }
        };
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> createAttachments() {
        return (organisation, application) -> Flux.fromIterable(application.getAttachments().values())
                .filter(attachment -> attachment.getDocumentId() == null)
                .delayElements(Duration.ofSeconds(1))
                .subscribe(attachment -> altinnClient.getAttachment(attachmentEndpoint, attachment.getAttachmentId())
                        .doOnSuccess(bytes -> {
                            if (bytes.length == 0) {
                                log.warn("Received empty PDF of attachment {} for archive reference {}.", attachment.getAttachmentId(), application.getArchiveReference());
                                return;
                            }

                            fintClient.postFile(organisation, bytes, MediaType.parseMediaType(attachment.getAttachmentType()), attachment.getFileName(), dokumentfilEndpoint)
                                    .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, responseEntity.getHeaders().getLocation())
                                            .doOnSuccess(statusEntity -> {
                                                if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                                    throw new FinalStatusPendingException();
                                                }

                                                getId(statusEntity, "/").ifPresent(id -> {
                                                    application.getAttachments().get(attachment.getAttachmentId()).setDocumentId(id);
                                                    repository.save(application);
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe());
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> createEvidence() {
        return (organisation, application) -> Flux.fromIterable(application.getConsents().values())
                .filter(consent -> consent.getDocumentId() == null)
                .delayElements(Duration.ofSeconds(1))
                .subscribe(consent -> altinnClient.getEvidence(evidenceEndpoint, application.getAccreditationId(), consent.getEvidenceCodeName())
                        .doOnSuccess(evidence -> {
                            byte[] bytes = null;

                            if (consent.getEvidenceCodeName().equals(BANKRUPTCY)) {
                                bytes = certificateConverter.convertBankruptCertificate(evidence, application);
                            } else if (consent.getEvidenceCodeName().equals(ARREARS)) {
                                bytes = certificateConverter.convertTaxCertificate(evidence, application);
                            }

                            if (bytes == null) {
                                log.warn("Failed to create PDF of evidence {} for archive reference {}.", consent.getEvidenceCodeName(), application.getArchiveReference());
                                return;
                            }

                            String filename = consent.getEvidenceCodeName().concat(".pdf");

                            fintClient.postFile(organisation, bytes, MediaType.APPLICATION_PDF, filename, dokumentfilEndpoint)
                                    .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, responseEntity.getHeaders().getLocation())
                                            .doOnSuccess(statusEntity -> {
                                                if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                                    throw new FinalStatusPendingException();
                                                }

                                                getId(statusEntity, "/").ifPresent(id -> {
                                                    application.getConsents().get(consent.getEvidenceCodeName()).setDocumentId(id);
                                                    repository.save(application);
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe());
    }

    public void submit(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        fintClient.getResource(organisation, DrosjeloyveResource.class, drosjeloyveMappeIdEndpoint, application.getCaseId())
                .doOnSuccess(resource -> {
                    DrosjeloyveResource drosjeloyveResource = DrosjeloyveResourceFactory.ofComplete(resource, application, organisation);

                    fintClient.putResource(organisation, drosjeloyveResource, drosjeloyveMappeIdEndpoint, application.getCaseId())
                            .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, responseEntity.getHeaders().getLocation())
                                    .doOnSuccess(statusEntity -> {
                                        if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                            throw new FinalStatusPendingException();
                                        }

                                        application.setStatus(AltinnApplicationStatus.ARCHIVED);
                                        repository.save(application);
                                    })
                                    .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                                    .retryWhen(withThrowable(finalStatusPending))
                                    .subscribe())
                            .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                            .subscribe();
                })
                .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                .subscribe();
    }

    private Optional<String> getId(ResponseEntity<Void> responseEntity, String separator) {
        return Optional.of(responseEntity.getHeaders())
                .map(HttpHeaders::getLocation)
                .map(URI::toString)
                .map(href -> StringUtils.substringAfterLast(href, separator));
    }
}
