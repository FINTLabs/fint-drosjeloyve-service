package no.novari.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.AltinnApplicationStatus;
import no.novari.drosjeloyve.configuration.OrganisationProperties;
import no.novari.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaxiLicenseApplicationService {
    private final OrganisationProperties organisationProperties;
    private final AltinnApplicationRepository repository;
    private final CaseHandlerService caseHandlerService;
    private final int interval;

    @Value("${fint.drosjeloyve.scheduled:false}")
    private Boolean isScheduled;

    public TaxiLicenseApplicationService(
            OrganisationProperties organisationProperties,
            AltinnApplicationRepository repository,
            CaseHandlerService caseHandlerService,
            @Value("${scheduling.interval:30}") int interval) {
        this.organisationProperties = organisationProperties;
        this.repository = repository;
        this.caseHandlerService = caseHandlerService;
        this.interval = interval;
    }

    @Scheduled(cron = "${scheduling.cron}")
    public void run() {
        if (isScheduled) {
            final Set<String> enabledOrganisations = organisationProperties
                    .getOrganisations()
                    .entrySet()
                    .stream()
                    .filter(it -> it.getValue().isEnabled())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            final Map<String, AtomicInteger> limits = organisationProperties
                    .getOrganisations()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            it -> new AtomicInteger(limit(it.getValue().getLimit()))
                    ));

            List<AltinnApplication> applications =
                    repository.findAllByStatus(AltinnApplicationStatus.CONSENTS_ACCEPTED)
                            .stream()
                            .filter(it -> StringUtils.startsWithIgnoreCase(it.getArchiveReference(),"AR"))
                            .filter(it -> enabledOrganisations.contains(it.getRequestor()))
                            .filter(it -> limits.get(it.getRequestor()).decrementAndGet() > 0)
                            .sorted(Comparator.comparing(AltinnApplication::getArchivedDate))
                            .collect(Collectors.toList());

            log.info("Found {} application(s)", applications.size());

            Flux.fromIterable(applications)
                    .delayElements(Duration.ofSeconds(interval))
                    .subscribe(application -> {
                        OrganisationProperties.Organisation organisation = organisationProperties.getOrganisations().get(application.getRequestor());

                        if (isComplete.test(application)) {
                            log.info("{}: Attempting final put for application {}", organisation.getName(), application.getArchiveReference());

                            caseHandlerService.submit(organisation, application);
                        } else {
                            log.info("{}: Attempting initial post for application {}", organisation.getName(), application.getArchiveReference());

                            if (organisation.isDeviationPolicy()) {
                                caseHandlerService.update(organisation, application);
                            } else {
                                caseHandlerService.create(organisation, application);
                            }
                        }
                    });
        }
    }

    private int limit(int limit) {
        return limit > 0 ? limit : Integer.MAX_VALUE;
    }

    public final Predicate<AltinnApplication> isComplete = application ->
            Objects.nonNull(application.getCaseId()) && Objects.nonNull(application.getForm().getDocumentId()) &&
                    application.getConsents().values().stream().map(AltinnApplication.Consent::getDocumentId).allMatch(Objects::nonNull) &&
                    application.getAttachments().values().stream().map(AltinnApplication.Attachment::getDocumentId).allMatch(Objects::nonNull);
}