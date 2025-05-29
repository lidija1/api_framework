package utils.schema;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchemaValidator {
    private static final String SCHEMA_PATH = "src/test/resources/schemas/";

    /**
     * Validate JSON against schema using REST Assured's validator
     */
    public static JsonSchemaValidator matchesSchema(String schemaFileName) {
        return JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + schemaFileName);
    }

    /**
     * Validate JSON string against schema file
     */
    public static void validateAgainstSchema(String json, String schemaFileName) {
        try {
            String schemaPath = SCHEMA_PATH + schemaFileName;
            try (InputStream schemaStream = Files.newInputStream(Paths.get(schemaPath))) {
                JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
                Schema schema = SchemaLoader.load(jsonSchema);
                schema.validate(new JSONObject(json));
            }
        } catch (Exception e) {
            throw new RuntimeException("Schema validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if schema file exists
     */
    public static boolean schemaExists(String schemaFileName) {
        return Files.exists(Paths.get(SCHEMA_PATH + schemaFileName));
    }
}
