package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.FinalStatusPendingException;
import no.fint.drosjeloyve.client.Endpoints;
import no.fint.drosjeloyve.client.FintAltinnClient;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.configuration.FintProperties;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;
import reactor.util.retry.RetrySpec;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseHandlerService {
    private final FintClient fintClient;
    private final FintAltinnClient fintAltinnClient;
    private final FintProperties fintProperties;
    private final AltinnApplicationRepository repository;

    public reactor.retry.Retry<?> finalStatusPending;

    @PostConstruct
    public void init() {
        finalStatusPending = Retry.anyOf(FinalStatusPendingException.class)
                .exponentialBackoff(Duration.ofSeconds(1), Duration.ofMinutes(5))
                .timeout(Duration.ofMinutes(30))
                .doOnRetry(exception -> log.info("{}", exception));
    }

    public CaseHandlerService(FintClient fintClient, FintAltinnClient fintAltinnClient, FintProperties fintProperties, AltinnApplicationRepository repository) {
        this.fintClient = fintClient;
        this.fintAltinnClient = fintAltinnClient;
        this.fintProperties = fintProperties;
        this.repository = repository;
    }

    public void newCase(OrganisationProperties.Organisation organisation, AltinnApplication application) {
        if (application.getCaseId() == null) {
            DrosjeloyveResource resource = newDrosjeloyveResource(application);

            fintClient.postResource(organisation, resource, fintProperties.getEndpoints().get(Endpoints.DROSJELOYVE.getKey()))
                    .doOnSuccess(resourceEntity -> fintClient.getStatus(organisation, DrosjeloyveResource.class, resourceEntity.getHeaders().getLocation())
                            .doOnSuccess(statusEntity -> {
                                if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                    throw new FinalStatusPendingException();
                                }

                                Optional.ofNullable(statusEntity.getBody())
                                        .map(DrosjeloyveResource::getMappeId)
                                        .map(Identifikator::getIdentifikatorverdi)
                                        .ifPresent(id -> {
                                            application.setCaseId(id);
                                            repository.save(application);
                                        });
                            })
                            .doOnError(WebClientResponseException.class, ex -> log.info(ex.getResponseBodyAsString()))
                            .retryWhen(reactor.util.retry.Retry.withThrowable(finalStatusPending))
                            .subscribe())
                    .doOnError(WebClientResponseException.class, ex -> log.info(ex.getResponseBodyAsString()))
                    .subscribe();
        }

        application.getAttachments().values().stream()
                .filter(attachment -> attachment.getDocumentId() == null)
                .forEach(attachment -> fintAltinnClient.getAttachment(fintProperties.getEndpoints().get(Endpoints.ATTACHMENT.getKey()), attachment.getAttachmentId())
                        .doOnSuccess(bytes -> fintClient.postFile(organisation, bytes, attachment, fintProperties.getEndpoints().get(Endpoints.DOKUMENTFIL.getKey()))
                                .doOnSuccess(resourceEntity -> {
                                    log.info("{}", resourceEntity.getHeaders().getLocation().toString());
                                    fintClient.getStatus(organisation, DokumentfilResource.class, resourceEntity.getHeaders().getLocation())
                                            .doOnSuccess(statusEntity -> {
                                                log.info(statusEntity.getHeaders().getContentType().toString());

                                                if (statusEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                                                    throw new FinalStatusPendingException();
                                                }

                                                Optional.ofNullable(statusEntity.getBody())
                                                        .map(DokumentfilResource::getSystemId)
                                                        .map(Identifikator::getIdentifikatorverdi)
                                                        .ifPresent(id -> {
                                                            attachment.setDocumentId(id);
                                                            application.getAttachments().put(attachment.getAttachmentId(), attachment);
                                                            repository.save(application);
                                                        });
                                            })
                                            .doOnError(ex -> log.error("dsadsad {}", ex.getMessage()))
                                            .retryWhen(reactor.util.retry.Retry.withThrowable(finalStatusPending))
                                            .subscribe();
                                }

                                )
                                .doOnError(ex -> log.error("{}", ex.getMessage()))
                                .subscribe())
                        .doOnError(ex -> log.error("{}", ex.getMessage()))
                        .subscribe());
    }

    public void updateCase(OrganisationProperties.Organisation organisation, AltinnApplication application) {

    }

    private DrosjeloyveResource newDrosjeloyveResource(AltinnApplication application) {
        DrosjeloyveResource resource = new DrosjeloyveResource();
        resource.setOrganisasjonsnummer(application.getSubject());
        resource.setTittel(application.getSubjectName());

        return resource;
    }
}
