package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.client.FintAltinnClient;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.client.Endpoints;
import no.fint.drosjeloyve.configuration.FintProperties;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TestService {
    private final FintClient fintClient;
    private final FintAltinnClient fintAltinnClient;
    private final FintProperties fintProperties;
    private final AltinnApplicationRepository altinnApplicationRepository;

    public TestService(FintClient fintClient, FintAltinnClient fintAltinnClient, FintProperties fintProperties, AltinnApplicationRepository altinnApplicationRepository) {
        this.fintClient = fintClient;
        this.fintAltinnClient = fintAltinnClient;
        this.fintProperties = fintProperties;
        this.altinnApplicationRepository = altinnApplicationRepository;
    }

    @Scheduled(initialDelayString = "${scheduling.initial-delay}", fixedDelayString = "${scheduling.fixed-delay}")
    public void run() {
        List<AltinnApplication> applications = altinnApplicationRepository.findAllByStatus(AltinnApplicationStatus.EVIDENCE_FETCHED);

        String uri = fintProperties.getEndpoints().get(Endpoints.APPLICATION.getKey());
    }
}
