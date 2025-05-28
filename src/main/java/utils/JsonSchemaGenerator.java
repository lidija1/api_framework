package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Iterator;
import java.util.Collections;

/**
 * Utility class for generating JSON schemas from sample data
 */
public class JsonSchemaGenerator {
    private static final ObjectMapper mapper = JsonUtils.getObjectMapper();

    /**
     * Generate a JSON schema from sample data
     */
    public static JsonNode generateSchema(JsonNode sampleData) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        
        ObjectNode properties = generateProperties(sampleData);
        schema.put("type", "object");
        schema.set("properties", properties);
        
        // Add required properties
        ArrayNode required = mapper.createArrayNode();
        properties.fieldNames().forEachRemaining(required::add);
        schema.set("required", required);
        
        return schema;
    }

    /**
     * Generate schema properties recursively
     */
    private static ObjectNode generateProperties(JsonNode node) {
        ObjectNode properties = mapper.createObjectNode();
        
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            properties.set(fieldName, generatePropertySchema(fieldValue));
        }
        
        return properties;
    }

    /**
     * Generate schema for a single property
     */
    private static ObjectNode generatePropertySchema(JsonNode value) {
        ObjectNode property = mapper.createObjectNode();

        if (value.isObject()) {
            property.put("type", "object");
            property.set("properties", generateProperties(value));
        } else if (value.isArray()) {
            property.put("type", "array");
            if (value.size() > 0) {
                property.set("items", generatePropertySchema(value.get(0)));
            }
        } else if (value.isTextual()) {
            property.put("type", "string");
            // Add format if detectable
            String format = detectFormat(value.asText());
            if (format != null) {
                property.put("format", format);
            }
        } else if (value.isNumber()) {
            property.put("type", value.isIntegralNumber() ? "integer" : "number");
        } else if (value.isBoolean()) {
            property.put("type", "boolean");
        } else if (value.isNull()) {
            property.put("type", "null");
        }

        return property;
    }

    /**
     * Detect common string formats
     */
    private static String detectFormat(String value) {
        if (value.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return "email";
        } else if (value.matches("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$")) {
            return "uri";
        } else if (value.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(([+-]\\d{2}:\\d{2})|Z)?$")) {
            return "date-time";
        }
        return null;
    }

    /**
     * Generate schema documentation
     */    
    public static String generateSchemaDocumentation(JsonNode schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        StringBuilder doc = new StringBuilder();
        doc.append("# JSON Schema Documentation\n\n");
            
        if (schema.has("title")) {
            doc.append("## ").append(schema.get("title").asText()).append("\n\n");
        }
            
        if (schema.has("description")) {
            doc.append(schema.get("description").asText()).append("\n\n");
        }
            
        doc.append("## Properties\n\n");
        JsonNode properties = schema.get("properties");
        if (properties != null) {
            properties.fields().forEachRemaining(field -> {
                doc.append("### ").append(field.getKey()).append("\n\n");
                documentProperty(field.getValue(), doc, 0);
                doc.append("\n");
            });
        }
        
        return doc.toString();
    }

    private static void documentProperty(JsonNode property, StringBuilder doc, int depth) {
        if (property == null || !property.isObject()) {
            LoggerUtils.warn("Invalid property node encountered during documentation generation");
            return;
        }

        String indent = String.join("", Collections.nCopies(depth, "  "));
        
        JsonNode typeNode = property.get("type");
        if (typeNode != null) {
            doc.append(indent).append("- **Type:** ").append(typeNode.asText()).append("\n");
        }
        
        if (property.has("format")) {
            doc.append(indent).append("- **Format:** ").append(property.get("format").asText()).append("\n");
        }
        
        if (property.has("description")) {
            doc.append(indent).append("- **Description:** ").append(property.get("description").asText()).append("\n");
        }
        
        if (property.has("properties")) {
            doc.append(indent).append("- **Properties:**\n");
            property.get("properties").fields().forEachRemaining(field -> {
                doc.append(indent).append("  #### ").append(field.getKey()).append("\n");
                documentProperty(field.getValue(), doc, depth + 1);
            });
        }
        
        if (property.has("items")) {
            doc.append(indent).append("- **Items:**\n");
            documentProperty(property.get("items"), doc, depth + 1);
        }
    }
}
