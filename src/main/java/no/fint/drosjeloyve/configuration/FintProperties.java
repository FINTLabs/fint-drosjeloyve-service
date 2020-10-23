package no.fint.drosjeloyve.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("fint")
public class FintProperties {
    private Map<String, String> endpoints = new HashMap<>();
}
