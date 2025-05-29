package utils.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.qameta.allure.Attachment;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     *
     * @param schemaName The base name of the schema
     * @return The latest version of the schema
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
     *
     * @param schemaName The base name of the schema
     * @param version The version number
     * @return The requested schema version
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
     * Check if a specific schema version exists
     *
     * @param schemaName The base name of the schema
     * @param version The version number
     * @return true if the schema version exists, false otherwise
     */
    public static boolean schemaVersionExists(String schemaName, int version) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        return versions.containsKey(version);
    }

    /**
     * Get all available versions for a schema
     *
     * @param schemaName The base name of the schema
     * @return A sorted set of available version numbers
     */
    public static Set<Integer> getAvailableVersions(String schemaName) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        return new TreeSet<>(versions.keySet());
    }

    /**
     * Validate JSON against a schema version
     *
     * @param data The JSON data to validate
     * @param schemaName The base name of the schema
     * @param version The version number
     * @return true if the data is valid against the schema, false otherwise
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
     * Validate a response against a schema version
     *
     * @param response The response to validate
     * @param schemaName The base name of the schema
     * @param version The version number
     * @return true if the response is valid against the schema, false otherwise
     */
    public static boolean validateResponse(Response response, String schemaName, int version) {
        if (response == null) {
            return false;
        }
        
        try {
            String responseBody = response.getBody().asString();
            JsonNode data = JsonUtils.getObjectMapper().readTree(responseBody);
            return validateSchema(data, schemaName, version);
        } catch (Exception e) {
            logger.error("Failed to validate response", e);
            return false;
        }
    }

    /**
     * Validate a response against the latest schema version
     *
     * @param response The response to validate
     * @param schemaName The base name of the schema
     * @return true if the response is valid against the latest schema, false otherwise
     */
    public static boolean validateResponseAgainstLatestSchema(Response response, String schemaName) {
        Set<Integer> versions = getAvailableVersions(schemaName);
        if (versions.isEmpty()) {
            throw new RuntimeException("No schemas found for: " + schemaName);
        }
        int latestVersion = Collections.max(versions);
        return validateResponse(response, schemaName, latestVersion);
    }

    /**
     * Find the highest compatible schema version for a response
     *
     * @param response The response to validate
     * @param schemaName The base name of the schema
     * @return The highest compatible version, or -1 if no compatible version is found
     */
    public static int findHighestCompatibleVersion(Response response, String schemaName) {
        Set<Integer> versions = getAvailableVersions(schemaName);
        if (versions.isEmpty()) {
            return -1;
        }
        
        List<Integer> sortedVersions = new ArrayList<>(versions);
        Collections.sort(sortedVersions, Collections.reverseOrder());
        
        for (int version : sortedVersions) {
            if (validateResponse(response, schemaName, version)) {
                return version;
            }
        }
        
        return -1;
    }

    /**
     * Load all versions of a schema
     */
    private static Map<Integer, JsonNode> loadSchemaVersions(String schemaName) {
        if (schemaCache.containsKey(schemaName)) {
            return schemaCache.get(schemaName);
        }

        Map<Integer, JsonNode> versions = new TreeMap<>();
        try {
            Files.walk(Paths.get(SCHEMA_BASE_PATH))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(schemaName + "_v") ||
                                   path.getFileName().toString().equals(schemaName + ".json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        int version = 1; // Default for non-versioned files
                        
                        // Extract version from filename
                        Matcher matcher = VERSION_PATTERN.matcher(fileName);
                        if (matcher.find()) {
                            version = Integer.parseInt(matcher.group(1));
                        }
                        
                        try {
                            JsonNode schema = JsonUtils.readJsonFile(path.toString());
                            versions.put(version, schema);
                        } catch (Exception e) {
                            logger.error("Failed to load schema: " + path, e);
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
     *
     * @param schemaName The base name of the schema
     * @param sampleData The sample data to use for generation
     * @return The generated schema
     */
    public static JsonNode generateSchema(String schemaName, JsonNode sampleData) {
        try {
            JsonNode schema = JsonSchemaGenerator.generateSchema(sampleData);
            int nextVersion = getNextVersion(schemaName);
            saveSchema(schema, schemaName, nextVersion);
            return schema;
        } catch (Exception e) {
            logger.error("Failed to generate schema", e);
            throw new RuntimeException("Schema generation failed", e);
        }
    }

    /**
     * Get the next available version number for a schema
     */
    public static int getNextVersion(String schemaName) {
        Map<Integer, JsonNode> versions = loadSchemaVersions(schemaName);
        return versions.isEmpty() ? 1 : Collections.max(versions.keySet()) + 1;
    }

    /**
     * Save a schema to file with a specific version
     *
     * @param schema The schema to save
     * @param schemaName The base name of the schema
     * @param version The version number
     * @return The path to the saved schema file
     */
    public static String saveSchema(JsonNode schema, String schemaName, int version) {
        try {
            String fileName = String.format("%s_v%d.json", schemaName, version);
            Path filePath = Paths.get(SCHEMA_BASE_PATH, fileName);
            Files.createDirectories(filePath.getParent());
            String schemaJson = JsonUtils.toJson(schema);
            Files.write(filePath, schemaJson.getBytes());
            
            // Update cache
            if (schemaCache.containsKey(schemaName)) {
                schemaCache.get(schemaName).put(version, schema);
            }
            
            logger.info("Saved schema {} version {} to {}", schemaName, version, filePath);
            return filePath.toString();
        } catch (IOException e) {
            logger.error("Failed to save schema", e);
            throw new RuntimeException("Failed to save schema: " + e.getMessage(), e);
        }
    }

    /**
     * Compare two versions of a schema and generate a difference report
     *
     * @param schemaName The base name of the schema
     * @param oldVersion The old version number
     * @param newVersion The new version number
     * @return A difference report
     */
    @Attachment(value = "Schema Version Difference Report", type = "text/plain")
    public static String compareSchemaVersions(String schemaName, int oldVersion, int newVersion) {
        JsonNode oldSchema = getSchema(schemaName, oldVersion);
        JsonNode newSchema = getSchema(schemaName, newVersion);
        
        return SchemaDifferenceDetector.generateDifferenceReport(oldSchema, newSchema);
    }

    /**
     * Check if a new schema version is backward compatible with a previous version
     *
     * @param schemaName The base name of the schema
     * @param oldVersion The old version number
     * @param newVersion The new version number
     * @return true if the new version is backward compatible, false otherwise
     */
    public static boolean isBackwardCompatible(String schemaName, int oldVersion, int newVersion) {
        JsonNode oldSchema = getSchema(schemaName, oldVersion);
        JsonNode newSchema = getSchema(schemaName, newVersion);
        
        List<SchemaDifferenceDetector.SchemaDifference> differences = 
            SchemaDifferenceDetector.compareSchemas(oldSchema, newSchema);
        
        // Check for breaking changes
        return differences.stream()
            .noneMatch(SchemaDifferenceDetector.SchemaDifference::isBreakingChange);
    }

    /**
     * Generate a compatibility report for all consecutive versions of a schema
     *
     * @param schemaName The base name of the schema
     * @return A compatibility report
     */
    @Attachment(value = "Schema Version Compatibility Report", type = "text/plain")
    public static String generateVersionCompatibilityReport(String schemaName) {
        Set<Integer> versions = getAvailableVersions(schemaName);
        if (versions.size() < 2) {
            return "Insufficient versions for comparison. Need at least 2 versions.";
        }
        
        List<Integer> sortedVersions = new ArrayList<>(versions);
        Collections.sort(sortedVersions);
        
        StringBuilder report = new StringBuilder();
        report.append("Schema Compatibility Report for ").append(schemaName).append("\n");
        report.append("==============================================\n\n");
        
        for (int i = 0; i < sortedVersions.size() - 1; i++) {
            int oldVersion = sortedVersions.get(i);
            int newVersion = sortedVersions.get(i + 1);
            
            boolean compatible = isBackwardCompatible(schemaName, oldVersion, newVersion);
            
            report.append("v").append(oldVersion).append(" → v").append(newVersion).append(": ");
            if (compatible) {
                report.append("✅ COMPATIBLE\n");
            } else {
                report.append("❌ INCOMPATIBLE\n");
            }
        }
        
        return report.toString();
    }

    /**
     * Clear the schema cache to force reloading from disk
     */
    public static void clearCache() {
        schemaCache.clear();
        logger.info("Schema cache cleared");
    }
    
    /**
     * List all available schemas
     *
     * @return A list of schema names with their available versions
     */
    public static Map<String, Set<Integer>> listAvailableSchemas() {
        Map<String, Set<Integer>> result = new HashMap<>();
        
        try {
            Map<String, Set<Integer>> schemas = Files.walk(Paths.get(SCHEMA_BASE_PATH))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> path.getFileName().toString())
                .map(fileName -> {
                    // Extract schema name and version
                    Matcher matcher = VERSION_PATTERN.matcher(fileName);
                    if (matcher.find()) {
                        String schemaName = fileName.substring(0, fileName.indexOf("_v"));
                        int version = Integer.parseInt(matcher.group(1));
                        return Map.entry(schemaName, version);
                    } else if (fileName.endsWith(".json")) {
                        String schemaName = fileName.substring(0, fileName.indexOf(".json"));
                        return Map.entry(schemaName, 1); // Default version
                    } else {
                        return null;
                    }
                })
                .filter(entry -> entry != null)
                .collect(Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toSet())
                ));
            
            return schemas;
        } catch (IOException e) {
            logger.error("Failed to list available schemas", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Generate a summary report of all available schemas
     *
     * @return A report listing all schemas and their versions
     */
    @Attachment(value = "Available Schemas Report", type = "text/plain")
    public static String generateSchemaSummaryReport() {
        Map<String, Set<Integer>> schemas = listAvailableSchemas();
        
        StringBuilder report = new StringBuilder();
        report.append("Available JSON Schemas\n");
        report.append("====================\n\n");
        
        schemas.forEach((schemaName, versions) -> {
            report.append(schemaName).append(":\n");
            report.append("  Versions: ");
            report.append(versions.stream()
                .sorted()
                .map(v -> "v" + v)
                .collect(Collectors.joining(", ")));
            report.append("\n\n");
        });
        
        return report.toString();
    }
}
