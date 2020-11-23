package no.fint.drosjeloyve.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/applications")
public class AltinnApplicationController {
    private final AltinnApplicationRepository repository;

    public AltinnApplicationController(AltinnApplicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AltinnApplication> getAltinnApplications() {
        return repository.findAllMinified();
    }
}
