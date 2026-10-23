package com.example.sana.AI.Enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum CharacterProposalField {

    POSITION_X("positionX"),
    POSITION_Y("positionY"),
    NAME("name"),
    SHORT_DESCRIPTION("shortDescription"),
    ROLE("role"),
    ALIASES("aliases"),
    AGE("age"),
    APPEARANCE("appearance"),
    TRAITS("traits"),
    STRENGTHS("strengths"),
    WEAKNESSES("weaknesses"),
    SKILLS("skills"),
    HISTORY("history"),
    GOALS("goals"),
    FEARS("fears"),
    INNER_CONFLICT("innerConflict"),
    SECRETS("secrets"),
    CHARACTER_VALUES("characterValues"),
    CHARACTER_ARC("characterArc"),
    WRITER_NOTES("writerNotes");

    private final String fieldName;

    CharacterProposalField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    // Get the allowed JSON field names
    private static final Set<String> ALLOWED_NAMES = Arrays.stream(values()).map(CharacterProposalField::getFieldName).collect(Collectors.toUnmodifiableSet());
    // Check whether a field is allowed in a character proposal
    public static boolean isAllowed(String fieldName) {
        return ALLOWED_NAMES.contains(fieldName);
    }

    // Convert an external JSON field name into its enum value.
    // Using this method keeps field-name parsing in one place.
    public static CharacterProposalField fromFieldName(String fieldName) {
        for (CharacterProposalField field : values()) {
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unsupported character field: " + fieldName);
    }

}
