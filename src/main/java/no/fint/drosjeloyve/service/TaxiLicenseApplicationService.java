package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

@Slf4j
@Service
public class TaxiLicenseApplicationService {
    private final OrganisationProperties organisationProperties;
    private final AltinnApplicationRepository repository;
    private final CaseHandlerService caseHandlerService;

    public TaxiLicenseApplicationService(OrganisationProperties organisationProperties, AltinnApplicationRepository repository, CaseHandlerService caseHandlerService, FintClient fintClient) {
        this.organisationProperties = organisationProperties;
        this.repository = repository;
        this.caseHandlerService = caseHandlerService;
    }

    @Scheduled(cron = "${scheduling.cron}")
    public void run() {
        List<AltinnApplication> applications = repository.findAllByStatus(AltinnApplicationStatus.CONSENTS_ACCEPTED);

        log.info("Found {} application(s) ready for submission", applications.size());

        ConcurrentMap<String, Integer> limits = new ConcurrentSkipListMap<>();

        Flux.fromIterable(applications)
                .sort(Comparator.comparing(AltinnApplication::getArchivedDate))
                .delayElements(Duration.ofSeconds(10))
                .subscribe(application -> {
                    OrganisationProperties.Organisation organisation = organisationProperties.getOrganisations().get(application.getRequestor());

                    if (organisation == null) {
                        log.warn("No organisation found for {}", application.getRequestor());
                        return;
                    }

                    if (organisation.getLimit() > 0 && limits.merge(application.getRequestor(), 1, Integer::sum) > organisation.getLimit()) {
                        log.info("Organization {} is above its limit of {}", organisation.getName(), organisation.getLimit());
                        return;
                    }

                    if (isComplete.test(application)) {
                        log.info("Attempting final submit for application {}", application.getArchiveReference());

                        caseHandlerService.submit(organisation, application);
                    } else {
                        log.info("Attempting initial submit for application {}", application.getArchiveReference());

                        if (organisation.isDeviationPolicy()) {
                            caseHandlerService.update(organisation, application);
                        } else {
                            caseHandlerService.create(organisation, application);
                        }
                    }
                });
    }

    private final Predicate<AltinnApplication> isComplete = application ->
            Objects.nonNull(application.getCaseId()) && Objects.nonNull(application.getForm().getDocumentId()) &&
                    application.getConsents().values().stream().map(AltinnApplication.Consent::getDocumentId).allMatch(Objects::nonNull) &&
                    application.getAttachments().values().stream().map(AltinnApplication.Attachment::getDocumentId).allMatch(Objects::nonNull);
}