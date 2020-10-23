package no.fint.drosjeloyve.client;

public enum Endpoints {
    DROSJELOYVE("drosjeloyve"),
    DROSJELOYVE_MAPPE_ID("drosjeloyve-mappe-id"),
    DROSJELOYVE_SYSTEM_ID("drosjeloyve-system-id"),
    DOKUMENTFIL("dokumentfil"),
    DOKUMENTFIL_SYSTEM_ID("dokumentfil-system-id"),
    ATTACHMENT("attachment"),
    APPLICATION("application"),
    EVIDENCE("evidence");

    private final String key;

    Endpoints(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
