package com.example.sana.AI.Enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StoryEventProposalField {

    // Canvas position fields
    POSITION_X("positionX"),
    POSITION_Y("positionY"),

    // Main story event information
    TITLE("title"),
    DESCRIPTION("description"),
    EVENT_TYPE("eventType"),

    // The order in which events are shown to the reader
    PRESENTATION_ORDER("presentationOrder"),

    // The actual chronological order inside the story
    CHRONOLOGICAL_ORDER("chronologicalOrder"),

    // Additional story event information
    IN_WORLD_TIME("inWorldTime"),
    NOTES("notes");

    private final String fieldName;

    StoryEventProposalField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    // Create a lookup map once when the enum is initialized.
    // This keeps supported JSON field names in one place.
    private static final Map<String, StoryEventProposalField> BY_NAME =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(StoryEventProposalField::getFieldName, Function.identity()));

    // Return true only for fields explicitly supported by SANA.
    public static boolean isAllowed(String name) {
        return BY_NAME.containsKey(name);
    }

    // Convert a JSON field name to its enum value.
    // Unsupported fields must never be accepted silently.
    public static StoryEventProposalField fromFieldName(String name) {

        StoryEventProposalField field = BY_NAME.get(name);

        if (field == null) {
            throw new IllegalArgumentException("Unsupported story event field: " + name);
        }

        return field;
    }
}
