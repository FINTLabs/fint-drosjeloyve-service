package no.fint.drosjeloyve.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TestService {
    private final AltinnApplicationRepository altinnApplicationRepository;

    public TestService(AltinnApplicationRepository altinnApplicationRepository) {
        this.altinnApplicationRepository = altinnApplicationRepository;
    }

    public void run() {
        List<AltinnApplication> altinnApplications = altinnApplicationRepository.findByStatus(AltinnApplicationStatus.EVIDENCE_FETCHED);
    }
}
