package utils.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.json.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/**
 * Enhanced DataProvider for managing test data from JSON files.
 * Supports nested structures, data filtering, and environment-specific data.
 */
public class DataProvider {
    private static final Logger logger = LoggerFactory.getLogger(DataProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TEST_DATA_PATH = "src/test/resources/testdata/";
    private static final String ENV = System.getProperty("env", "dev");

    /**
     * Loads test data with environment-specific overrides
     */
    @Step("Loading test data: {fileName} - {jsonPath}")
    public static <T> T getTestData(String fileName, String jsonPath, Class<T> clazz) {
        try {
            JsonNode baseData = loadJsonFile(fileName);
            JsonNode envOverrides = loadEnvSpecificData(fileName);
            
            // Merge environment-specific overrides
            if (envOverrides != null) {
                baseData = JsonUtils.mergeJsonNodes(baseData, envOverrides);
            }
            
            JsonNode node = getNodeFromPath(baseData, jsonPath);
            logger.debug("Loaded test data for path: {}", jsonPath);
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            logger.error("Failed to load test data", e);
            throw new RuntimeException("Failed to load test data: " + e.getMessage());
        }
    }

    /**
     * Loads a list of test data with filtering support
     */
    @Step("Loading test data list: {fileName} - {jsonPath}")
    public static <T> List<T> getTestDataList(String fileName, String jsonPath, Class<T> clazz, Predicate<T> filter) {
        try {
            JsonNode root = loadJsonFile(fileName);
            JsonNode node = getNodeFromPath(root, jsonPath);
            List<T> items = new ArrayList<>();
            
            if (node.isArray()) {
                for (JsonNode item : node) {
                    T value = mapper.treeToValue(item, clazz);
                    if (filter == null || filter.test(value)) {
                        items.add(value);
                    }
                }
            }
            
            logger.debug("Loaded {} items for path: {}", items.size(), jsonPath);
            return items;
        } catch (IOException e) {
            logger.error("Failed to load test data list", e);
            throw new RuntimeException("Failed to load test data list: " + e.getMessage());
        }
    }

    /**
     * Get test data as Map for dynamic access
     */
    @Step("Loading dynamic test data: {fileName} - {jsonPath}")
    public static Map<String, Object> getDynamicTestData(String fileName, String jsonPath) {
        try {
            JsonNode root = loadJsonFile(fileName);
            JsonNode node = getNodeFromPath(root, jsonPath);
            return mapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            logger.error("Failed to load dynamic test data", e);
            throw new RuntimeException("Failed to load dynamic test data: " + e.getMessage());
        }
    }

    /**
     * Load and merge environment-specific test data
     */
    private static JsonNode loadEnvSpecificData(String fileName) throws IOException {
        String envFileName = fileName.replace(".json", "_" + ENV + ".json");
        File envFile = new File(TEST_DATA_PATH + envFileName);
        if (envFile.exists()) {
            return mapper.readTree(envFile);
        }
        return null;
    }

    /**
     * Load JSON file with error handling
     */
    private static JsonNode loadJsonFile(String fileName) throws IOException {
        File file = new File(TEST_DATA_PATH + fileName);
        if (!file.exists()) {
            throw new RuntimeException("Test data file not found: " + fileName);
        }
        return mapper.readTree(file);
    }

    /**
     * Get nested JSON node from path
     */
    private static JsonNode getNodeFromPath(JsonNode root, String path) {
        if (path == null || path.trim().isEmpty()) {
            return root;
        }

        JsonNode current = root;
        for (String part : path.split("\\.")) {
            if (current == null) {
                throw new RuntimeException("Invalid JSON path: " + path);
            }
            current = current.get(part);
        }
        return current;
    }
}
