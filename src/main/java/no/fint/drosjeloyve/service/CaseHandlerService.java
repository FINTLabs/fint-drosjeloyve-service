package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.FinalStatusPendingException;
import no.fint.drosjeloyve.client.AltinnClient;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.factory.DrosjeloyveResourceFactory;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    public final Retry<?> finalStatusPending;

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
        updateApplication()
                .andThen(updateForm())
                .andThen(updateAttachments())
                .andThen(updateEvidence())
                .accept(organisation, application);
    }

    public void update(OrganisationProperties.Organisation organisation, AltinnApplication application) {

    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> updateApplication() {
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

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> updateForm() {
        return (organisation, application) -> {
            if (application.getForm().getDocumentId() == null) {
                altinnClient.getApplication(applicationEndpoint, application.getArchiveReference(), application.getLanguageCode())
                        .doOnSuccess(bytes -> fintClient.postFile(organisation, bytes, MediaType.APPLICATION_PDF, "søknadsskjema.pdf", dokumentfilEndpoint)
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
                                .subscribe())
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe();
            }
        };
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> updateAttachments() {
        return (organisation, application) -> application.getAttachments().values().stream()
                .filter(attachment -> attachment.getDocumentId() == null)
                .forEach(attachment -> altinnClient.getAttachment(attachmentEndpoint, attachment.getAttachmentId())
                        .doOnSuccess(bytes -> fintClient.postFile(organisation, bytes, MediaType.parseMediaType(attachment.getAttachmentType()), attachment.getFileName(), dokumentfilEndpoint)
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
                                .subscribe())
                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getMessage()))
                        .subscribe());
    }

    private BiConsumer<OrganisationProperties.Organisation, AltinnApplication> updateEvidence() {
        return (organisation, application) -> application.getConsents().values().stream()
                .filter(consent -> consent.getDocumentId() == null)
                .forEach(consent -> altinnClient.getEvidence(evidenceEndpoint, application.getAccreditationId(), consent.getEvidenceCodeName())
                        .doOnSuccess(bytes -> fintClient.postFile(organisation, bytes.getEvidenceStatus().getEvidenceCodeName().getBytes(), MediaType.APPLICATION_PDF, consent.getEvidenceCodeName().concat(".pdf"), dokumentfilEndpoint)
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
                                .subscribe())
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
