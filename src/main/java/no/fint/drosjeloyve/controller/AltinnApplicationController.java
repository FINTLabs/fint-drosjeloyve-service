package no.fint.drosjeloyve.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.altinn.model.AltinnApplication;
import no.fint.altinn.model.AltinnApplicationStatus;
import no.fint.drosjeloyve.configuration.OrganisationProperties;
import no.fint.drosjeloyve.repository.AltinnApplicationRepository;
import no.fint.drosjeloyve.service.CaseHandlerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api")
public class AltinnApplicationController {
    private final OrganisationProperties organisationProperties;
    private final AltinnApplicationRepository repository;
    private final CaseHandlerService caseHandlerService;

    public AltinnApplicationController(AltinnApplicationRepository repository, OrganisationProperties organisationProperties, CaseHandlerService caseHandlerService) {
        this.organisationProperties = organisationProperties;
        this.repository = repository;
        this.caseHandlerService = caseHandlerService;
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

    @GetMapping("{application:AR[\\d]{9,}}")
    public AltinnApplication getApplication(@PathVariable String application) {
        return repository.findById(application).orElseThrow(IllegalArgumentException::new);
    }

    @DeleteMapping("{application:AR[\\d]{9,}}")
    public AltinnApplication resetApplicationDocuments(@PathVariable String application) {
        final AltinnApplication altinnApplication = repository.findById(application).orElseThrow(IllegalArgumentException::new);
        altinnApplication.getForm().setDocumentId(null);
        altinnApplication.getAttachments().values().forEach(it -> it.setDocumentId(null));
        altinnApplication.getConsents().values().forEach(it -> it.setDocumentId(null));
        return repository.save(altinnApplication);
    }

    @PostMapping("{application:AR[\\d]{9,}}")
    public AltinnApplication postApplication(@PathVariable String application) {
        final AltinnApplication altinnApplication = repository.findById(application).orElseThrow(IllegalArgumentException::new);
        final OrganisationProperties.Organisation organisation = Optional.ofNullable(organisationProperties.getOrganisations().get(altinnApplication.getRequestor())).filter(OrganisationProperties.Organisation::isEnabled).orElseThrow(IllegalStateException::new);
        caseHandlerService.create(organisation, altinnApplication);
        return altinnApplication;
    }

    @PutMapping("{application:AR[\\d]{9,}}")
    public AltinnApplication putApplication(@PathVariable String application) {
        final AltinnApplication altinnApplication = repository.findById(application).orElseThrow(IllegalArgumentException::new);
        final OrganisationProperties.Organisation organisation = Optional.ofNullable(organisationProperties.getOrganisations().get(altinnApplication.getRequestor())).filter(OrganisationProperties.Organisation::isEnabled).orElseThrow(IllegalStateException::new);
        caseHandlerService.submit(organisation, altinnApplication);
        return altinnApplication;
    }

    @PatchMapping("{application:AR[\\d]{9,}}")
    public AltinnApplication patchApplication(@PathVariable String application) {
        final AltinnApplication altinnApplication = repository.findById(application).orElseThrow(IllegalArgumentException::new);
        final OrganisationProperties.Organisation organisation = Optional.ofNullable(organisationProperties.getOrganisations().get(altinnApplication.getRequestor())).filter(OrganisationProperties.Organisation::isEnabled).orElseThrow(IllegalStateException::new);
        caseHandlerService.update(organisation, altinnApplication);
        return altinnApplication;
    }
}
