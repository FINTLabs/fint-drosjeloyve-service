package no.fint.drosjeloyve.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("drosjeloyve")
public class OrganisationProperties {
    private Map<String, Organisation> organisations = new HashMap<>();

    @Data
    public static class Organisation {
        private String name;
        private boolean enabled;
        private String registration;
        private String username;
        private String password;
        private boolean deviationPolicy;
        private String skjermingshjemmel;
        private String tilgangsrestriksjon;
        private String variantformat;
        private LegalBasis politiattest;
        private LegalBasis skatteattest;
        private LegalBasis konkursattest;
        private LegalBasis fagkompetanse;
        private LegalBasis domForelegg;
        private LegalBasis soknadsskjema;
        private int limit;
    }

    @Data
    public static class LegalBasis {
        private String skjermingshjemmel;
        private String tilgangsrestriksjon;
    }
}
