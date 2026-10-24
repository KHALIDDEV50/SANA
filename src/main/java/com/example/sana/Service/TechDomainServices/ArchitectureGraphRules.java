package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import java.util.Map;
import java.util.Set;

// Explicit initial Architecture vocabulary. Other declared diagram types are not enabled here.
public final class ArchitectureGraphRules {
    public static final Set<String> NODE_TYPES = Set.of("CLIENT", "APPLICATION", "SERVICE", "DATABASE", "QUEUE", "COMPONENT", "EXTERNAL_SYSTEM");
    public static final Set<String> EDGE_TYPES = Set.of("DEPENDENCY", "CONNECTS_TO", "DATA_FLOW");
    private ArchitectureGraphRules() {}

    public static void node(String type, Map<String, Object> properties, Double x, Double y) {
        if (!NODE_TYPES.contains(type == null ? "" : type)) throw new ApiException("Unsupported Architecture node type");
        if (properties == null) throw new ApiException("Node properties are required");
        text(properties.get("label"), 255, "Node label");
        optionalText(properties, "technology", 255);
        optionalText(properties, "description", 20000);
        if (x == null || y == null || !Double.isFinite(x) || !Double.isFinite(y)
                || Math.abs(x) > 1000000 || Math.abs(y) > 1000000) throw new ApiException("Node coordinates must be finite and within the canvas limits");
    }

    public static void edge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {
        if (!EDGE_TYPES.contains(type == null ? "" : type)) throw new ApiException("Unsupported Architecture edge type");
        text(label, 50, "Edge label");
        if (properties == null) throw new ApiException("Edge properties are required");
        if ("DATA_FLOW".equals(type) && !(Set.of("DATABASE", "QUEUE").contains(sourceType)
                || Set.of("DATABASE", "QUEUE").contains(targetType))) throw new ApiException("DATA_FLOW requires a DATABASE or QUEUE endpoint");
        optionalText(properties, "label", 50);
        optionalText(properties, "description", 20000);
        optionalText(properties, "protocol", 255);
    }

    public static void text(Object value, int limit, String field) {
        if (!(value instanceof String text) || text.isBlank() || text.length() > limit) throw new ApiException(field + " is required and exceeds no more than " + limit + " characters");
    }

    private static void optionalText(Map<String, Object> properties, String key, int limit) {
        if (properties.containsKey(key) && properties.get(key) != null
                && (!(properties.get(key) instanceof String text) || text.length() > limit)) throw new ApiException("Invalid " + key + " property");
    }
}
