package no.novari.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.AltinnApplicationStatus;
import no.novari.drosjeloyve.client.AltinnClient;
import no.novari.drosjeloyve.client.FintClient;
import no.novari.drosjeloyve.configuration.OrganisationProperties;
import no.novari.drosjeloyve.exception.FinalStatusPendingException;
import no.novari.drosjeloyve.factory.DrosjeloyveResourceFactory;
import no.novari.drosjeloyve.repository.AltinnApplicationRepository;
import no.novari.drosjeloyve.util.CertificateConverter;
import no.fint.model.resource.arkiv.samferdsel.SoknadDrosjeloyveResource;
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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static reactor.util.retry.Retry.withThrowable;

@Slf4j
@Service
public class CaseHandlerService {
    private final FintClient fintClient;
    private final AltinnClient altinnClient;
    private final AltinnApplicationRepository repository;

    private final CertificateConverter certificateConverter;

    public final Retry<?> finalStatusPending;

    private static final String BANKRUPTCY = "KonkursDrosje";
    private static final Set<String> ARREARS = new HashSet<>(Arrays.asList("RestanserDrosje", "RestanserV2"));

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

    public CaseHandlerService(FintClient fintClient, AltinnClient altinnClient, AltinnApplicationRepository repository, OrganisationProperties organisationProperties, CertificateConverter certificateConverter) {
        this.fintClient = fintClient;
        this.altinnClient = altinnClient;
        this.repository = repository;
        this.certificateConverter = certificateConverter;

        finalStatusPending = Retry.anyOf(FinalStatusPendingException.class)
                .exponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5))
                .timeout(Duration.ofMinutes(30))
                .doOnRetry(exception -> log.debug("{}", exception));
    }

    public void create(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        createApplication()
                .andThen(createForm())
                .andThen(createAttachments())
                .andThen(createEvidence())
                .accept(organisation, application);
    }

    public void update(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        if (application.getCaseId() == null) {
            List<AltinnApplication> existingCase = repository.findAllByRequestorAndSubject(application.getRequestor(), application.getSubject());

            List<String> caseIds = existingCase.stream()
                    .map(AltinnApplication::getCaseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (caseIds.size() == 1) {
                application.setCaseId(caseIds.get(0));
                repository.save(application);

                createForm()
                        .andThen(createAttachments())
                        .andThen(createEvidence())
                        .accept(organisation, application);
            } else if (caseIds.size() == 0) {
                create(organisation, application);
            } else {
                log.error("Found more than 1 caseId for requestor {} and subject {}", application.getRequestor(), application.getSubject());
            }
        } else {
            createForm()
                    .andThen(createAttachments())
                    .andThen(createEvidence())
                    .accept(organisation, application);
        }
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> createApplication() {
        return (organisation, application) -> {
            if (application.getCaseId() == null) {
                fintClient.postResource(organisation, DrosjeloyveResourceFactory.ofBasic(application), drosjeloyveEndpoint)
                        .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, Object.class, responseEntity.getHeaders().getLocation())
                                .doOnSuccess(statusEntity -> {
                                    if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                        throw new FinalStatusPendingException();
                                    }

                                    getId(statusEntity, "mappeid/").ifPresent(id -> {
                                        application.setCaseId(id);
                                        repository.save(application);
                                        log.info("Application (post) of archive reference: {}", application.getArchiveReference());
                                    });
                                })
                                .doOnError(WebClientResponseException.class, throwable -> log.error("Application (status) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                                .retryWhen(withThrowable(finalStatusPending))
                                .subscribe())
                        .doOnError(WebClientResponseException.class, throwable -> log.error("Application (post) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
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
                                                    log.info("Form (post) of archive reference: {}", application.getArchiveReference());
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, throwable -> log.error("Form (status) of archive reference {} for organisation {} ({}) - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, throwable -> log.error("Form (post) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, throwable -> log.error("Form (get) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
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
                                                    application.getAttachments().computeIfPresent(attachment.getAttachmentId(), (key, value) -> {
                                                        value.setDocumentId(id);
                                                        return value;
                                                    });
                                                    repository.save(application);
                                                    log.info("Attachment (post) of archive reference: {}", application.getArchiveReference());
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, throwable -> log.error("Attachment (status) of archive reference {} for organisation {} ({}) - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, throwable -> log.error("Attachment (post) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, throwable -> log.error("Attachment (get) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
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
                            } else if (ARREARS.contains(consent.getEvidenceCodeName())) {
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
                                                    application.getConsents().computeIfPresent(consent.getEvidenceCodeName(), (key, value) -> {
                                                        value.setDocumentId(id);
                                                        return value;
                                                    });
                                                    repository.save(application);
                                                    log.info("Evidence (post) of archive reference: {}", application.getArchiveReference());
                                                });
                                            })
                                            .doOnError(WebClientResponseException.class, throwable -> log.error("Evidence (status) of archive reference {} for organisation {} ({}) - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage()))
                                            .retryWhen(withThrowable(finalStatusPending))
                                            .subscribe())
                                    .doOnError(WebClientResponseException.class, throwable -> log.error("Evidence (post) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                                    .subscribe();
                        })
                        .doOnError(WebClientResponseException.class, throwable -> log.error("Evidence (get) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                        .subscribe());
    }

    public void submit(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        fintClient.getResource(organisation, SoknadDrosjeloyveResource.class, drosjeloyveMappeIdEndpoint, application.getCaseId())
                .doOnSuccess(resource -> {
                    SoknadDrosjeloyveResource drosjeloyveResource = DrosjeloyveResourceFactory.ofComplete(resource, application, organisation);

                    fintClient.putResource(organisation, drosjeloyveResource, drosjeloyveMappeIdEndpoint, application.getCaseId())
                            .doOnSuccess(responseEntity -> fintClient.getStatus(organisation, Object.class, responseEntity.getHeaders().getLocation())
                                    .doOnSuccess(statusEntity -> {
                                        if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                            throw new FinalStatusPendingException();
                                        }

                                        application.setStatus(AltinnApplicationStatus.ARCHIVED);
                                        repository.save(application);
                                        log.info("Application (put) of archive reference: {}", application.getArchiveReference());
                                    })
                                    .doOnError(WebClientResponseException.class, throwable -> log.error("Application (status) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                                    .retryWhen(withThrowable(finalStatusPending))
                                    .subscribe())
                            .doOnError(WebClientResponseException.class, throwable -> log.error("Application (put) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                            .subscribe();
                })
                .doOnError(WebClientResponseException.class, throwable -> log.error("Application (get) of archive reference {} for organisation {} ({}) - {} - {}", application.getArchiveReference(), application.getRequestorName(), application.getRequestor(), throwable.getMessage(), throwable.getResponseBodyAsString()))
                .subscribe();
    }

    private Optional<String> getId(ResponseEntity<?> responseEntity, String separator) {
        return Optional.of(responseEntity.getHeaders())
                .map(HttpHeaders::getLocation)
                .map(URI::toString)
                .map(href -> StringUtils.substringAfterLast(href, separator));
    }
}
