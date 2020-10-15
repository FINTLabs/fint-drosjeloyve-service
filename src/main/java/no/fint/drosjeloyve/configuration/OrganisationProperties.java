package no.fint.drosjeloyve.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("organisation")
public class OrganisationProperties {
    private Map<String, Organisation> organisations = new HashMap<>();

    @Data
    public static class Organisation {
        private String name;
        private String registration;
        private String username;
        private String password;
    }
}
