package utils.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.qameta.allure.Attachment;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.LoggerUtils;
import utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for generating JSON schemas from sample data
 */
public class JsonSchemaGenerator {
    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaGenerator.class);
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
     * Generate a schema from a REST Assured response
     *
     * @param response The REST Assured response
     * @param title Optional title for the schema
     * @param description Optional description for the schema
     * @return The generated schema
     */
    public static JsonNode generateSchemaFromResponse(Response response, String title, String description) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        
        // Extract the response body as a JsonNode
        String responseBody = response.getBody().asString();
        try {
            JsonNode responseJson = mapper.readTree(responseBody);
            return generateSchemaWithMetadata(responseJson, title, description);
        } catch (Exception e) {
            logger.error("Failed to parse response body as JSON", e);
            throw new RuntimeException("Failed to generate schema from response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a schema from a REST Assured response
     *
     * @param response The REST Assured response
     * @return The generated schema
     */
    public static JsonNode generateSchemaFromResponse(Response response) {
        return generateSchemaFromResponse(response, null, null);
    }
    
    /**
     * Generate a schema from multiple response examples to create a more accurate schema
     *
     * @param responses List of REST Assured responses
     * @param title Optional title for the schema
     * @param description Optional description for the schema
     * @return The generated schema
     */
    public static JsonNode generateSchemaFromResponses(List<Response> responses, String title, String description) {
        if (responses == null || responses.isEmpty()) {
            throw new IllegalArgumentException("Responses list cannot be null or empty");
        }
        
        List<JsonNode> examples = new ArrayList<>();
        for (Response response : responses) {
            try {
                String responseBody = response.getBody().asString();
                JsonNode responseJson = mapper.readTree(responseBody);
                examples.add(responseJson);
            } catch (Exception e) {
                logger.warn("Failed to parse response body as JSON, skipping example", e);
            }
        }
        
        if (examples.isEmpty()) {
            throw new RuntimeException("No valid JSON responses found in the provided list");
        }
        
        return generateSchemaFromExamples(examples, title, description);
    }
    
    /**
     * Generate a schema from multiple examples to create a more accurate schema
     *
     * @param examples List of example JSON nodes
     * @param title Optional title for the schema
     * @param description Optional description for the schema
     * @return The generated schema
     */
    public static JsonNode generateSchemaFromExamples(List<JsonNode> examples, String title, String description) {
        if (examples == null || examples.isEmpty()) {
            throw new IllegalArgumentException("Examples list cannot be null or empty");
        }
        
        // Start with the schema from the first example
        JsonNode baseExample = examples.get(0);
        ObjectNode schema = (ObjectNode) generateSchemaWithMetadata(baseExample, title, description);
        
        // If we only have one example, return it
        if (examples.size() == 1) {
            return schema;
        }
        
        // Merge additional examples to refine the schema
        for (int i = 1; i < examples.size(); i++) {
            mergeExample(schema, examples.get(i));
        }
        
        return schema;
    }
    
    /**
     * Generate a schema with metadata
     */
    private static JsonNode generateSchemaWithMetadata(JsonNode sampleData, String title, String description) {
        ObjectNode schema = (ObjectNode) generateSchema(sampleData);
        
        // Add metadata if provided
        if (title != null && !title.isEmpty()) {
            schema.put("title", title);
        }
        
        if (description != null && !description.isEmpty()) {
            schema.put("description", description);
        }
        
        return schema;
    }
    
    /**
     * Merge an example into an existing schema to refine the schema
     */
    private static void mergeExample(ObjectNode schema, JsonNode example) {
        // We only need to refine properties and required fields
        if (!schema.has("properties") || !schema.get("properties").isObject()) {
            return;
        }
        
        ObjectNode schemaProperties = (ObjectNode) schema.get("properties");
        ArrayNode requiredArray = schema.has("required") ? (ArrayNode) schema.get("required") : mapper.createArrayNode();
        
        // Track which properties are in this example
        Set<String> exampleProperties = new HashSet<>();
        Iterator<String> fieldNames = example.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            exampleProperties.add(fieldName);
            
            JsonNode fieldValue = example.get(fieldName);
            
            // If the field already exists in the schema
            if (schemaProperties.has(fieldName)) {
                JsonNode fieldSchema = schemaProperties.get(fieldName);
                
                // Merge the type if needed (e.g., if we see null and string, change type to ["string", "null"])
                if (fieldSchema.has("type") && !isTypeCompatible(fieldSchema.get("type"), fieldValue)) {
                    mergeType(fieldSchema, fieldValue);
                }
                
                // Recursively merge nested objects
                if (fieldValue.isObject() && fieldSchema.has("properties")) {
                    mergeExample((ObjectNode) fieldSchema, fieldValue);
                }
                
                // Refine array schemas
                if (fieldValue.isArray() && fieldSchema.has("items")) {
                    refineArraySchema((ObjectNode) fieldSchema, fieldValue);
                }
            } else {
                // New field found in this example
                schemaProperties.set(fieldName, generatePropertySchema(fieldValue));
            }
        }
        
        // Check which properties in the schema are missing from this example
        Set<String> optionalProperties = new HashSet<>();
        Iterator<String> schemaFieldNames = schemaProperties.fieldNames();
        while (schemaFieldNames.hasNext()) {
            String fieldName = schemaFieldNames.next();
            if (!exampleProperties.contains(fieldName)) {
                optionalProperties.add(fieldName);
            }
        }
        
        // Update required fields (only fields present in all examples are required)
        if (!optionalProperties.isEmpty() && schema.has("required")) {
            List<JsonNode> newRequired = new ArrayList<>();
            for (JsonNode node : requiredArray) {
                String field = node.asText();
                if (!optionalProperties.contains(field)) {
                    newRequired.add(node);
                }
            }
            
            schema.set("required", mapper.createArrayNode().addAll(newRequired));
        }
    }
    
    /**
     * Check if a type is compatible with a value
     */
    private static boolean isTypeCompatible(JsonNode typeNode, JsonNode value) {
        if (typeNode.isTextual()) {
            String type = typeNode.asText();
            return isTypeCompatible(type, value);
        } else if (typeNode.isArray()) {
            // Already a union type, check if any type is compatible
            for (JsonNode typeValue : typeNode) {
                if (typeValue.isTextual() && isTypeCompatible(typeValue.asText(), value)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if a type is compatible with a value
     */
    private static boolean isTypeCompatible(String type, JsonNode value) {
        if (value.isNull()) {
            return "null".equals(type);
        } else if (value.isTextual()) {
            return "string".equals(type);
        } else if (value.isInt() || value.isLong()) {
            return "integer".equals(type) || "number".equals(type);
        } else if (value.isNumber()) {
            return "number".equals(type);
        } else if (value.isBoolean()) {
            return "boolean".equals(type);
        } else if (value.isObject()) {
            return "object".equals(type);
        } else if (value.isArray()) {
            return "array".equals(type);
        }
        return false;
    }
    
    /**
     * Merge a type into a schema to handle union types
     */
    private static void mergeType(JsonNode fieldSchema, JsonNode value) {
        if (fieldSchema instanceof ObjectNode) {
            ObjectNode objSchema = (ObjectNode) fieldSchema;
            
            // Determine the type of the value
            String valueType = getJsonType(value);
            
            // Get the current type
            JsonNode typeNode = objSchema.get("type");
            
            if (typeNode.isTextual()) {
                String currentType = typeNode.asText();
                if (!currentType.equals(valueType)) {
                    // Need to create a union type
                    ArrayNode typeArray = mapper.createArrayNode();
                    typeArray.add(currentType);
                    typeArray.add(valueType);
                    objSchema.set("type", typeArray);
                }
            } else if (typeNode.isArray()) {
                // Already a union type, add the new type if not present
                boolean typeExists = false;
                for (JsonNode type : typeNode) {
                    if (type.isTextual() && type.asText().equals(valueType)) {
                        typeExists = true;
                        break;
                    }
                }
                
                if (!typeExists) {
                    ((ArrayNode) typeNode).add(valueType);
                }
            }
        }
    }
    
    /**
     * Get the JSON Schema type for a value
     */
    private static String getJsonType(JsonNode value) {
        if (value.isNull()) {
            return "null";
        } else if (value.isTextual()) {
            return "string";
        } else if (value.isInt() || value.isLong()) {
            return "integer";
        } else if (value.isNumber()) {
            return "number";
        } else if (value.isBoolean()) {
            return "boolean";
        } else if (value.isObject()) {
            return "object";
        } else if (value.isArray()) {
            return "array";
        }
        return "string"; // Default
    }
    
    /**
     * Refine an array schema based on an example
     */
    private static void refineArraySchema(ObjectNode arraySchema, JsonNode arrayValue) {
        if (!arrayValue.isArray() || arrayValue.size() == 0) {
            return;
        }
        
        // Only process if the schema has an items property
        if (!arraySchema.has("items")) {
            return;
        }
        
        JsonNode itemsSchema = arraySchema.get("items");
        
        // For heterogeneous arrays, we might need to use oneOf
        boolean isHeterogeneous = false;
        String firstType = null;
        
        for (JsonNode item : arrayValue) {
            String itemType = getJsonType(item);
            
            if (firstType == null) {
                firstType = itemType;
            } else if (!firstType.equals(itemType)) {
                isHeterogeneous = true;
                break;
            }
        }
        
        if (isHeterogeneous) {
            // Convert to oneOf schema for heterogeneous arrays
            Set<String> types = new HashSet<>();
            ArrayNode oneOfArray = mapper.createArrayNode();
            
            for (JsonNode item : arrayValue) {
                String itemType = getJsonType(item);
                if (!types.contains(itemType)) {
                    types.add(itemType);
                    ObjectNode typeSchema = mapper.createObjectNode();
                    typeSchema.put("type", itemType);
                    
                    if (itemType.equals("object")) {
                        typeSchema.set("properties", generateProperties(item));
                    } else if (itemType.equals("array")) {
                        typeSchema.set("items", generatePropertySchema(item.get(0)));
                    }
                    
                    oneOfArray.add(typeSchema);
                }
            }
            
            if (itemsSchema.isObject()) {
                ((ObjectNode) itemsSchema).set("oneOf", oneOfArray);
            } else {
                ObjectNode newItemsSchema = mapper.createObjectNode();
                newItemsSchema.set("oneOf", oneOfArray);
                arraySchema.set("items", newItemsSchema);
            }
        } else {
            // For homogeneous arrays, refine the existing item schema
            for (JsonNode item : arrayValue) {
                if (item.isObject() && itemsSchema.has("properties")) {
                    mergeExample((ObjectNode) itemsSchema, item);
                }
            }
        }
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
            
            // Add required properties
            ArrayNode required = mapper.createArrayNode();
            Iterator<String> fieldNames = value.fieldNames();
            while (fieldNames.hasNext()) {
                required.add(fieldNames.next());
            }
            property.set("required", required);
        } else if (value.isArray()) {
            property.put("type", "array");
            if (value.size() > 0) {
                property.set("items", generatePropertySchema(value.get(0)));
            } else {
                // Empty array, use a generic schema
                property.set("items", mapper.createObjectNode().put("type", "string"));
            }
        } else if (value.isTextual()) {
            property.put("type", "string");
            // Add format if detectable
            String format = detectFormat(value.asText());
            if (format != null) {
                property.put("format", format);
            }
            
            // Add example value
            property.put("example", value.asText());
        } else if (value.isNumber()) {
            if (value.isIntegralNumber()) {
                property.put("type", "integer");
                property.put("example", value.asInt());
            } else {
                property.put("type", "number");
                property.put("example", value.asDouble());
            }
        } else if (value.isBoolean()) {
            property.put("type", "boolean");
            property.put("example", value.asBoolean());
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
        } else if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return "date";
        } else if (value.matches("^\\d{2}:\\d{2}:\\d{2}$")) {
            return "time";
        } else if (value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return "uuid";
        }
        return null;
    }

    /**
     * Generate schema documentation
     */
    @Attachment(value = "Schema Documentation", type = "text/plain")
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
    
    /**
     * Generate schema documentation for a property
     */
    private static void documentProperty(JsonNode property, StringBuilder doc, int depth) {
        if (property.has("description")) {
            doc.append(property.get("description").asText()).append("\n\n");
        }
        
        // Document type
        if (property.has("type")) {
            JsonNode type = property.get("type");
            if (type.isTextual()) {
                doc.append("**Type:** ").append(type.asText());
            } else if (type.isArray()) {
                doc.append("**Type:** ");
                Iterator<JsonNode> elements = type.elements();
                while (elements.hasNext()) {
                    doc.append(elements.next().asText());
                    if (elements.hasNext()) {
                        doc.append(" or ");
                    }
                }
            }
            doc.append("\n\n");
        }
        
        // Document format
        if (property.has("format")) {
            doc.append("**Format:** ").append(property.get("format").asText()).append("\n\n");
        }
        
        // Document constraints
        Map<String, String> constraints = new HashMap<>();
        if (property.has("minimum")) constraints.put("Minimum", property.get("minimum").asText());
        if (property.has("maximum")) constraints.put("Maximum", property.get("maximum").asText());
        if (property.has("minLength")) constraints.put("Min Length", property.get("minLength").asText());
        if (property.has("maxLength")) constraints.put("Max Length", property.get("maxLength").asText());
        if (property.has("pattern")) constraints.put("Pattern", property.get("pattern").asText());
        
        if (!constraints.isEmpty()) {
            doc.append("**Constraints:**\n\n");
            constraints.forEach((key, value) -> doc.append("- ").append(key).append(": ").append(value).append("\n"));
            doc.append("\n");
        }
        
        // Document example
        if (property.has("example")) {
            doc.append("**Example:** `").append(property.get("example").asText()).append("`\n\n");
        }
        
        // Document enum values
        if (property.has("enum")) {
            doc.append("**Allowed Values:**\n\n");
            property.get("enum").elements().forEachRemaining(value -> 
                doc.append("- `").append(value.asText()).append("`\n"));
            doc.append("\n");
        }
        
        // Document nested properties
        if (property.has("properties")) {
            doc.append("**Properties:**\n\n");
            property.get("properties").fields().forEachRemaining(field -> {
                for (int i = 0; i < depth + 1; i++) doc.append("  ");
                doc.append("- **").append(field.getKey()).append("**\n\n");
                for (int i = 0; i < depth + 2; i++) doc.append("  ");
                documentProperty(field.getValue(), doc, depth + 2);
                doc.append("\n");
            });
        }
        
        // Document array items
        if (property.has("items")) {
            doc.append("**Array Items:**\n\n");
            documentProperty(property.get("items"), doc, depth + 1);
        }
    }
    
    /**
     * Generate a schema from a response and save it with a version
     *
     * @param response The REST Assured response
     * @param schemaName The base name for the schema file
     * @param version The version number
     * @param title Optional title for the schema
     * @param description Optional description for the schema
     * @return The path to the saved schema file
     */
    @Attachment(value = "Generated Schema", type = "application/json")
    public static String generateAndSaveSchema(Response response, String schemaName, int version,
                                              String title, String description) {
        JsonNode schema = generateSchemaFromResponse(response, title, description);
        return SchemaVersionManager.saveSchema(schema, schemaName, version);
    }
}
