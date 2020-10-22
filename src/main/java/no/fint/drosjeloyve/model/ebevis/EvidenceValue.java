package no.fint.drosjeloyve.model.ebevis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.drosjeloyve.model.ebevis.vocab.ValueType;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class EvidenceValue {
    @JsonProperty("evidenceValueName")
    private String evidenceValueName;

    @JsonProperty("valueType")
    private ValueType valueType;

    @JsonProperty("mimeType")
    private MediaType mimeType;

    @JsonProperty("source")
    private String source;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @JsonProperty("jsonSchemaDefintion")
    private String jsonSchemaDefinition;

    public void setMimeType(String mimeType) {
        this.mimeType = MediaType.parseMediaType(mimeType);
    }
}
