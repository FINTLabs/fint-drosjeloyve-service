package no.fint.drosjeloyve.model.ebevis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class EvidenceStatus {
    @JsonProperty("evidenceCodeName")
    private String evidenceCodeName;

    @JsonProperty("status")
    private EvidenceStatusCode status;

    @JsonProperty("validFrom")
    private OffsetDateTime validFrom;

    @JsonProperty("validTo")
    private OffsetDateTime validTo;

    @JsonProperty("didSupplyLegalBasis")
    private Boolean didSupplyLegalBasis;
}
