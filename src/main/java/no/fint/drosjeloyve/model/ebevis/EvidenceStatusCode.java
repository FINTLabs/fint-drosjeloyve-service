package no.fint.drosjeloyve.model.ebevis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class EvidenceStatusCode {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("description")
    private String description;

    @JsonProperty("retryAt")
    private OffsetDateTime retryAt;
}