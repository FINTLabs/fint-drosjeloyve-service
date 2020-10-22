package no.fint.drosjeloyve.model.ebevis.vocab;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ValueType {
    BOOLEAN("boolean"),
    NUMBER("number"),
    STRING("string"),
    ATTACHMENT("attachment"),
    DATETIME("dateTime"),
    URI("uri"),
    AMOUNT("amount"),
    JSON_SCHEMA("jsonSchema");

    private final String value;

    ValueType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
