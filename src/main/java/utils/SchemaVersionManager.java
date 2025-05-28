package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages versioned JSON schemas and provides validation capabilities
 */
public class SchemaVersionManager {
    private static final Logger logger = LoggerFactory.getLogger(SchemaVersionManager.class);
    private static final String SCHEMA_BASE_PATH = "src/test/resources/schemas/";
    private static final Pattern VERSION_PATTERN = Pattern.compile("v(\\d+)");
    private static final Map<String, Map<Integer, JsonNode>> schemaCache = new HashMap<>();

    /**
     * Get the latest version of a schema
     */
    public static JsonNode getLatestSchema(String schemaName) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        if (versions.isEmpty()) {
            throw new RuntimeException("No schemas found for: " + schemaName);
        }
        return versions.get(Collections.max(versions.keySet()));
    }

    /**
     * Get a specific version of a schema
     */
    public static JsonNode getSchema(String schemaName, int version) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        JsonNode schema = versions.get(version);
        if (schema == null) {
            throw new RuntimeException(
                    String.format("Schema version %d not found for: %s", version, schemaName));
        }
        return schema;
    }

    /**
     * Validate JSON against a schema version
     */
    public static boolean validateSchema(JsonNode data, String schemaName, int version) {
        try {
            JsonNode schema = getSchema(schemaName, version);
            ProcessingReport report = JsonSchemaFactory.byDefault()
                    .getJsonSchema(schema)
                    .validate(data);
            return report.isSuccess();
        } catch (Exception e) {
            logger.error("Schema validation failed", e);
            return false;
        }
    }

    /**
     * Load all versions of a schema
     */
    private static Map<Integer, JsonNode> loadSchemaVersions(String schemaName) {
        if (schemaCache.containsKey(schemaName)) {
            return schemaCache.get(schemaName);
        }

        Map<Integer, JsonNode> versions = new HashMap<>();
        try {
            Files.walk(Paths.get(SCHEMA_BASE_PATH))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(schemaName))
                    .forEach(path -> {
                        Matcher matcher = VERSION_PATTERN.matcher(path.getFileName().toString());
                        if (matcher.find()) {
                            int version = Integer.parseInt(matcher.group(1));
                            try {
                                JsonNode schema = JsonUtils.readJsonFile(path.toString());
                                versions.put(version, schema);
                            } catch (Exception e) {
                                logger.error("Failed to load schema: " + path, e);
                            }
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to scan schema directory", e);
        }

        schemaCache.put(schemaName, versions);
        return versions;
    }

    /**
     * Generate a new schema version based on sample data
     */
    public static JsonNode generateSchema(String schemaName, JsonNode sampleData) {
        try {
            JsonNode schema = JsonSchemaGenerator.generateSchema(sampleData);
            int nextVersion = getNextVersion(schemaName);
            String fileName = String.format("%s_v%d.json", schemaName, nextVersion);
            saveSchema(schema, fileName);
            return schema;
        } catch (Exception e) {
            logger.error("Failed to generate schema", e);
            throw new RuntimeException("Schema generation failed", e);
        }
    }

    /**
     * Get the next available version number for a schema
     */
    private static int getNextVersion(String schemaName) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        return versions.isEmpty() ? 1 : Collections.max(versions.keySet()) + 1;
    }

    /**
     * Save a schema to file
     */
    private static void saveSchema(JsonNode schema, String fileName) throws IOException {
        Path filePath = Paths.get(SCHEMA_BASE_PATH, fileName);
        Files.createDirectories(filePath.getParent());
        String schemaJson = JsonUtils.toJson(schema);
        Files.write(filePath, schemaJson.getBytes());
    }
}
