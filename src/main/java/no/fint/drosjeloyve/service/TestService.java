package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.client.FintClient;
import no.fint.drosjeloyve.client.FintEndpoints;
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
    private final FintProperties fintProperties;
    private final AltinnApplicationRepository altinnApplicationRepository;

    public TestService(FintClient fintClient, FintProperties fintProperties, AltinnApplicationRepository altinnApplicationRepository) {
        this.fintClient = fintClient;
        this.fintProperties = fintProperties;
        this.altinnApplicationRepository = altinnApplicationRepository;
    }


    @Scheduled(initialDelay = 2000, fixedDelay = 1000000)
    public void run() {
        List<AltinnApplication> altinnApplications = altinnApplicationRepository.findAllByStatus(AltinnApplicationStatus.EVIDENCE_FETCHED);

        String uri = fintProperties.getEndpoints().get(FintEndpoints.DROSJELOYVE.getKey());
    }
}
