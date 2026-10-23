package com.example.sana.AI.Enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WorldProposalField {

    // Canvas position fields
    POSITION_X("positionX"),
    POSITION_Y("positionY"),

    // Required world fields
    NAME("name"),
    DESCRIPTION("description"),

    // Optional world-building details
    HISTORY("history"),
    LORE("lore"),
    ATMOSPHERE("atmosphere"),
    NOTES("notes");

    // The actual field name used in the JSON proposal
    private final String fieldName;

    WorldProposalField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    // Build the lookup map only once when the enum is loaded.
    // This avoids searching through all enum values repeatedly.
    private static final Map<String, WorldProposalField> BY_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            WorldProposalField::getFieldName,
                            Function.identity()
                    ));

    // Check whether the AI is allowed to propose this field.
    public static boolean isAllowed(String fieldName) {
        return BY_NAME.containsKey(fieldName);
    }

    // Convert a JSON field name into its corresponding enum value.
    // Unknown fields must never be silently accepted.
    public static WorldProposalField fromFieldName(String fieldName) {

        WorldProposalField field = BY_NAME.get(fieldName);

        if (field == null) {
            throw new IllegalArgumentException(
                    "Unsupported world field: " + fieldName
            );
        }

        return field;
    }
}
