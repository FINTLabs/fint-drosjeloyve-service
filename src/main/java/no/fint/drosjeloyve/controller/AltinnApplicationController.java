package no.fint.drosjeloyve.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    public List<AltinnApplication> getAltinnApplications(@RequestHeader(required = false, name = "x-requestor") String requestor) {
        if (StringUtils.isEmpty(requestor)) {
            return repository.findAllMinified();
        } else {
            return repository.findAllByRequestor(requestor);
        }
    }

    @GetMapping("status")
    public List<AltinnApplicationStatus> getStatuses() {
        return Arrays.asList(AltinnApplicationStatus.values());
    }

    @GetMapping("organisations")
    public Map<String, String> getOrganisations(@RequestHeader(required = false, name = "x-requestor") String requestor) {
        return repository.findAll()
                .stream()
                .filter(altinnApplication -> StringUtils.isNotBlank(altinnApplication.getRequestor()))
                .filter(altinnApplication -> !StringUtils.isNotEmpty(requestor) || altinnApplication.getRequestor().equals(requestor))
                .collect(Collectors.toMap(AltinnApplication::getRequestor, AltinnApplication::getRequestorName,
                        (k, v) -> k, HashMap::new));
    }
}
