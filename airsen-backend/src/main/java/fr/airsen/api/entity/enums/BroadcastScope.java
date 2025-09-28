package fr.airsen.api.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Geographic scope for admin notification broadcasts")
public enum BroadcastScope {
    
    @Schema(description = "All users in France")
    FRANCE("All users in France"),
    
    @Schema(description = "Users in a specific region")
    REGION("Users in specific region"),
    
    @Schema(description = "Users in a specific department")
    DEPARTMENT("Users in specific department"),
    
    @Schema(description = "Users in a specific commune")
    COMMUNE("Users in specific commune");

    private final String description;

    BroadcastScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresGeographicCode() {
        return this != FRANCE;
    }

    public String getExpectedParameterName() {
        return switch (this) {
            case FRANCE -> null;
            case REGION -> "regionCode";
            case DEPARTMENT -> "departmentCode";
            case COMMUNE -> "communeCode";
        };
    }

    public boolean isValidWithCodes(String regionCode, String departmentCode, String communeCode) {
        return switch (this) {
            case FRANCE -> true;
            case REGION -> regionCode != null && !regionCode.trim().isEmpty();
            case DEPARTMENT -> departmentCode != null && !departmentCode.trim().isEmpty();
            case COMMUNE -> communeCode != null && !communeCode.trim().isEmpty();
        };
    }
}