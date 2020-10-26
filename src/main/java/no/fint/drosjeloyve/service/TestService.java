package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.FinalStatusPendingException;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.factory.DrosjeloyveResourceFactory;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.model.ebevis.Evidence;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import no.fint.model.resource.arkiv.samferdsel.DrosjeloyveResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;

import static no.fint.drosjeloyve.service.CaseHandlerService.finalStatusPending;
import static reactor.util.retry.Retry.withThrowable;

@Slf4j
@Service
public class TestService {
    private final OrganisationProperties organisationProperties;
    private final AltinnApplicationRepository repository;
    private final CaseHandlerService caseHandlerService;
    private final FintClient fintClient;

    @Value("${fint.endpoints.drosjeloyve-mappe-id}")
    private String drosjeloyveMappeIdEndpoint;

    public TestService(OrganisationProperties organisationProperties, AltinnApplicationRepository repository, CaseHandlerService caseHandlerService, FintClient fintClient) {
        this.organisationProperties = organisationProperties;
        this.repository = repository;
        this.caseHandlerService = caseHandlerService;
        this.fintClient = fintClient;
    }

    @Scheduled(initialDelayString = "${scheduling.initial-delay}", fixedDelayString = "${scheduling.fixed-delay}")
    public void run() {
        List<AltinnApplication> applications = repository.findAllByStatus(AltinnApplicationStatus.CONSENTS_ACCEPTED);

        applications.forEach(application -> {
            OrganisationProperties.Organisation organisation = organisationProperties.getOrganisations().get(application.getRequestor());

            if (organisation == null) {
                log.warn("No organisation found for requestor {}", application.getRequestor());
                return;
            }

            if (organisation.isDeviationPolicy()) {
                caseHandlerService.updateCase(organisation, application);
            } else {
                caseHandlerService.newCase(organisation, application);
            }
        });
    }

    public void run2() {
        List<AltinnApplication> applications = repository.findAllByStatus(AltinnApplicationStatus.CONSENTS_ACCEPTED);

        applications.stream()
                .filter(application -> Objects.nonNull(application.getCaseId()) && Objects.nonNull(application.getForm().getDocumentId()) &&
                        application.getConsents().values().stream().map(AltinnApplication.Consent::getDocumentId).allMatch(Objects::nonNull) &&
                        application.getAttachments().values().stream().map(AltinnApplication.Attachment::getDocumentId).allMatch(Objects::nonNull))
                .forEach(application -> {
                    OrganisationProperties.Organisation organisation = organisationProperties.getOrganisations().get(application.getRequestor());

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
                                                .doOnError(WebClientResponseException.class, ex -> log.error(ex.getResponseBodyAsString()))
                                                .retryWhen(withThrowable(finalStatusPending))
                                                .subscribe())
                                        .doOnError(WebClientResponseException.class, ex -> log.error(ex.getResponseBodyAsString()))
                                        .subscribe();
                            })
                            .doOnError(WebClientResponseException.class, ex -> log.error(ex.getResponseBodyAsString()))
                            .subscribe();
                });
    }
}
