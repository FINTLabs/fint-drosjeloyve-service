package no.fint.drosjeloyve.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api")
public class AltinnApplicationController {
    private final AltinnApplicationRepository repository;

    public AltinnApplicationController(AltinnApplicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("applications")
    public List<AltinnApplication> getAltinnApplications() {
        return repository.findAllMinified();
    }

    @GetMapping("status")
    public List<AltinnApplicationStatus> getStatuses() {
        return Arrays.asList(AltinnApplicationStatus.values());
    }

    @GetMapping("organisations")
    public Map<String, String> getOrganisations() {
        return repository.findAll()
                .stream()
                .filter(altinnApplication -> StringUtils.isNotBlank(altinnApplication.getRequestor()))
                .collect(Collectors.toMap(AltinnApplication::getRequestor, AltinnApplication::getRequestorName,
                        (k, v) -> k, HashMap::new));
    }
}
