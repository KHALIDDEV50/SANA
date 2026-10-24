package com.example.sana.AI.Enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum LocationProposalField {

    // Canvas coordinates
    POSITION_X("positionX"),
    POSITION_Y("positionY"),

    // Location information
    NAME("name"),
    LOCATION_TYPE("locationType"),
    NARRATIVE_SIGNIFICANCE("narrativeSignificance"),
    NOTES("notes"),

    // Relationships allowed when creating a new location
    WORLD_ID("worldId"),
    PARENT_LOCATION_ID("parentLocationId");

    private final String fieldName;

    LocationProposalField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    // Cache field-name mappings to avoid repeated enum searches.
    private static final Map<String, LocationProposalField> BY_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            LocationProposalField::getFieldName,
                            Function.identity()
                    ));

    // Reject any JSON field that is not explicitly supported.
    public static boolean isAllowed(String name) {
        return BY_NAME.containsKey(name);
    }

    // Convert a JSON field name to its matching enum constant.
    public static LocationProposalField fromFieldName(String name) {

        LocationProposalField field = BY_NAME.get(name);

        if (field == null) {
            throw new IllegalArgumentException(
                    "Unsupported location field: " + name);
        }

        return field;
    }
}
