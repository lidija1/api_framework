package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

public class JsonUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Read JSON from file
     */
    public static JsonNode readJsonFile(String filePath) {
        try {
            return mapper.readTree(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Update JSON node value
     */
    public static JsonNode updateNode(JsonNode node, String field, Object value) {
        if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) node;
            if (value instanceof String) {
                objectNode.put(field, (String) value);
            } else if (value instanceof Integer) {
                objectNode.put(field, (Integer) value);
            } else if (value instanceof Boolean) {
                objectNode.put(field, (Boolean) value);
            } else if (value instanceof Double) {
                objectNode.put(field, (Double) value);
            }
        }
        return node;
    }

    /**
     * Convert Map to JSON node
     */
    public static JsonNode mapToJson(Map<String, Object> map) {
        return mapper.valueToTree(map);
    }

    /**
     * Convert JSON node to Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(JsonNode node) {
        try {
            return mapper.treeToValue(node, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }

    /**
     * Validate if string is valid JSON
     */
    public static boolean isValidJson(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Merge two JSON nodes, with the second node taking precedence
     */
    public static JsonNode mergeJsonNodes(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode instanceof ObjectNode && updateNode instanceof ObjectNode) {
            ObjectNode merged = (ObjectNode) mainNode.deepCopy();
            updateNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (merged.has(key) && merged.get(key).isObject() && value.isObject()) {
                    merged.set(key, mergeJsonNodes(merged.get(key), value));
                } else {
                    merged.set(key, value);
                }
            });
            return merged;
        }
        return updateNode;
    }

    /**
     * Filter JSON array node based on criteria
     */
    public static ArrayNode filterArrayNode(ArrayNode arrayNode, Map<String, Object> criteria) {
        ArrayNode filtered = mapper.createArrayNode();
        arrayNode.elements().forEachRemaining(element -> {
            if (matchesCriteria(element, criteria)) {
                filtered.add(element);
            }
        });
        return filtered;
    }

    /**
     * Check if JSON node matches given criteria
     */
    private static boolean matchesCriteria(JsonNode node, Map<String, Object> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> {
                    JsonNode fieldNode = node.get(entry.getKey());
                    return fieldNode != null && fieldNode.asText().equals(entry.getValue().toString());
                });
    }

    /**
     * Extract specified fields from JSON node
     */
    public static JsonNode extractFields(JsonNode node, String... fields) {
        ObjectNode result = mapper.createObjectNode();
        for (String field : fields) {
            if (node.has(field)) {
                result.set(field, node.get(field));
            }
        }
        return result;
    }

    /**
     * Find nodes in a JSON tree that match a predicate
     */
    public static List<JsonNode> findNodes(JsonNode root, Predicate<JsonNode> predicate) {
        List<JsonNode> matches = new ArrayList<>();
        findNodesRecursive(root, predicate, matches);
        return matches;
    }

    private static void findNodesRecursive(JsonNode node, Predicate<JsonNode> predicate, List<JsonNode> matches) {
        if (predicate.test(node)) {
            matches.add(node);
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> findNodesRecursive(entry.getValue(), predicate, matches));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(element -> findNodesRecursive(element, predicate, matches));
        }
    }

    /**
     * Get the shared ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return mapper;
    }

    /**
     * Set custom ObjectMapper configuration
     */
    public static void configureObjectMapper(ObjectMapper customMapper) {
        if (customMapper != null) {
            mapper = customMapper;
        }
    }
}
