package no.fint.drosjeloyve.model.ebevis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class Evidence {
    @JsonProperty("name")
    private String name;

    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @JsonProperty("evidenceStatus")
    private EvidenceStatus evidenceStatus;

    @JsonProperty("evidenceValues")
    private List<EvidenceValue> evidenceValues;

    private String documentId;
}
