package utils.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern CONFIG_PATTERN = Pattern.compile("\\$\\{config\\.(.*?)\\}");
    private static final ConfigManager config = ConfigManager.getInstance();

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    /**
     * Read JSON from file
     */
    public static JsonNode readJsonFile(String filePath) {
        try {
            return MAPPER.readTree(new File(filePath));
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
        return MAPPER.valueToTree(map);
    }

    /**
     * Convert JSON node to Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(JsonNode node) {
        try {
            return MAPPER.treeToValue(node, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }

    /**
     * Validate if string is valid JSON
     */
    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Merge two JSON nodes, with the second node taking precedence
     */
    public static JsonNode mergeJsonNodes(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode == null || updateNode == null) {
            return mainNode == null ? updateNode : mainNode;
        }

        JsonNode merged = mainNode.deepCopy();
        if (merged.isObject() && updateNode.isObject()) {
            ObjectNode mergedObj = (ObjectNode) merged;
            Iterator<Map.Entry<String, JsonNode>> fields = updateNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (merged.has(key) && merged.get(key).isObject() && value.isObject()) {
                    mergedObj.set(key, mergeJsonNodes(merged.get(key), value));
                } else {
                    mergedObj.set(key, value);
                }
            }
        }
        return merged;
    }

    /**
     * Filter JSON array node based on criteria
     */
    public static ArrayNode filterArrayNode(ArrayNode arrayNode, Map<String, Object> criteria) {
        ArrayNode filtered = MAPPER.createArrayNode();
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
        ObjectNode result = MAPPER.createObjectNode();
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
        return MAPPER;
    }

    public static JsonNode substituteConfigValues(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode fieldNode = entry.getValue();
                
                if (fieldNode.isTextual()) {
                    String text = fieldNode.asText();
                    Matcher matcher = CONFIG_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String configPath = matcher.group(1);
                        String[] pathParts = configPath.split("\\.");
                        Object value = config.getValue(pathParts);
                        if (value != null) {
                            objectNode.put(entry.getKey(), String.valueOf(value));
                        }
                    }
                } else if (fieldNode.isObject() || fieldNode.isArray()) {
                    substituteConfigValues(fieldNode);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                substituteConfigValues(element);
            }
        }
        return node;
    }

    public static Map<String, Object> toMap(String json) {
        try {
            return MAPPER.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to Map", e);
        }
    }

    public static Map<String, Object> cleanupMap(Map<String, Object> map) {
        Map<String, Object> cleanMap = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                cleanMap.put(entry.getKey(), entry.getValue());
            }
        }
        return cleanMap;
    }
}
