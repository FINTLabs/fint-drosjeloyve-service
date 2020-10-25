package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TestService {
    private final OrganisationProperties organisationProperties;
    private final AltinnApplicationRepository repository;
    private final CaseHandlerService caseHandlerService;

    public TestService(OrganisationProperties organisationProperties, AltinnApplicationRepository repository, CaseHandlerService caseHandlerService) {
        this.organisationProperties = organisationProperties;
        this.repository = repository;
        this.caseHandlerService = caseHandlerService;
    }

    @Scheduled(initialDelayString = "${scheduling.initial-delay}", fixedDelayString = "${scheduling.fixed-delay}")
    public void run() {
        List<AltinnApplication> applications = repository.findAllByStatus(AltinnApplicationStatus.NEW);

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
}
