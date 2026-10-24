package com.example.sana.AI.Enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WorldRuleProposalField {

    // Canvas position
    POSITION_X("positionX"),
    POSITION_Y("positionY"),

    // Required rule information
    NAME("name"),
    DESCRIPTION("description"),

    // Optional rule information
    SCOPE("scope"),
    CONDITIONS("conditions"),
    EXCEPTIONS("exceptions"),
    CONSEQUENCES("consequences"),
    NOTES("notes"),

    // Optional World relationship during creation only
    WORLD_ID("worldId");

    private final String fieldName;

    WorldRuleProposalField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    // Build the field lookup once when the enum is initialized.
    private static final Map<String, WorldRuleProposalField> BY_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            WorldRuleProposalField::getFieldName,
                            Function.identity()
                    ));

    // Check whether an AI-proposed field is supported.
    public static boolean isAllowed(String name) {
        return BY_NAME.containsKey(name);
    }

    // Convert a JSON field name into its enum constant.
    // Unknown field names must be rejected.
    public static WorldRuleProposalField fromFieldName(String name) {

        WorldRuleProposalField field = BY_NAME.get(name);

        if (field == null) {
            throw new IllegalArgumentException(
                    "Unsupported WorldRule field: " + name);
        }

        return field;
    }
}
