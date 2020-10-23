package no.fint.drosjeloyve.client;

public enum FintEndpoints {
    DROSJELOYVE("drosjeloyve"),
    DROSJELOYVE_MAPPE_ID("drosjeloyve-mappe-id"),
    DROSJELOYVE_SYSTEM_ID("drosjeloyve-system-id"),
    DOKUMENTFIL("dokumentfil"),
    DOKUMENTFIL_SYSTEM_ID("dokumentfil-system-id");

    private final String key;

    FintEndpoints(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
