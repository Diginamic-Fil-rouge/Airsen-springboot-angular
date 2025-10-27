package fr.airsen.api.entity.enums;


public enum GeographicScopeType {

    FRANCE("France"),

    REGION("Region"),

    DEPARTMENT("Department"),

    COMMUNE("Commune");

    private final String displayName;

    GeographicScopeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
