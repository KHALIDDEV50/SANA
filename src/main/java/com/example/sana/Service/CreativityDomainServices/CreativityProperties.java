package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;

import java.math.BigDecimal;
import java.util.Map;

// Reads and checks typed values in Creativity graph properties.
// JSON numbers can arrive as Integer, Long or BigDecimal, so integers are read through Number.
public final class CreativityProperties {

    // Limit for TEXT columns, well below MySQL's 65,535 bytes
    public static final int LONG_TEXT = 20000;

    private CreativityProperties() {
    }

    // A text property, or null when it is missing
    public static String text(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        return value instanceof String text ? text : null;
    }

    // An integer property, or null when it is missing
    public static Integer integer(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof Number number)) {
            throw new ApiException("Property " + key + " must be a whole number");
        }

        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ApiException("Property " + key + " must be a whole number");
        }
    }

    // The property must be non-blank text with at most limit characters
    public static void requireText(Map<String, Object> properties, String key, int limit) {
        GraphRuleChecks.requireText(properties.get(key), limit, "Property " + key);
    }

    // When present, the property must be text with at most limit characters
    public static void optionalText(Map<String, Object> properties, String key, int limit) {
        GraphRuleChecks.optionalText(properties, key, limit);
    }

    // The property must be a whole number
    public static void requireInteger(Map<String, Object> properties, String key) {

        if (integer(properties, key) == null) {
            throw new ApiException("Property " + key + " is required");
        }
    }

    // When present, the property must be a positive whole number
    public static void optionalPositiveInteger(Map<String, Object> properties, String key) {

        Integer value = integer(properties, key);

        if (value != null && value <= 0) {
            throw new ApiException("Property " + key + " must be greater than zero");
        }
    }
}
