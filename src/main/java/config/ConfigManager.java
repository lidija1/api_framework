package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central configuration manager that handles all configuration sources, environment selection,
 * validation, and secure access to configuration values.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    
    // Configuration sources in order of precedence (highest first)
    private static final String[] CONFIG_FILES = {
        "config/config.yaml",
        "config/config.yml",
        "config/config.json"
    };
    
    private final Map<String, Object> config;
    private final EnvironmentManager environmentManager;
    private final SecretManager secretManager;
    private final ConfigValidator configValidator;
    
    private ConfigManager() {
        // Initialize managers
        environmentManager = EnvironmentManager.getInstance();
        secretManager = SecretManager.getInstance();
        configValidator = new ConfigValidator();
        
        // Load configuration
        this.config = loadConfiguration();
        
        // Validate configuration
        validateConfiguration();
        
        logger.info("Configuration loaded for environment: {}", environmentManager.getCurrentEnvironment());
    }
    
    /**
     * Get the singleton instance of the ConfigManager
     * @return The ConfigManager instance
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load configuration from various sources
     * @return The merged configuration map
     */
    private Map<String, Object> loadConfiguration() {
        // Base configuration from file
        Map<String, Object> baseConfig = loadBaseConfiguration();
        
        // Override with environment-specific properties
        Map<String, Object> envConfig = loadEnvironmentOverrides();
        
        // Merge configurations
        return mergeConfigurations(baseConfig, envConfig);
    }
    
    /**
     * Load base configuration from YAML or JSON file
     * @return Base configuration map
     */
    private Map<String, Object> loadBaseConfiguration() {
        for (String configFile : CONFIG_FILES) {
            try {
                // Try file system path first
                File file = new File("src/test/resources/" + configFile);
                if (file.exists()) {
                    if (configFile.endsWith(".yaml") || configFile.endsWith(".yml")) {
                        YamlConfigLoader yamlLoader = new YamlConfigLoader();
                        return yamlLoader.loadConfig(file.getAbsolutePath());
                    } else if (configFile.endsWith(".json")) {
                        ConfigOperations jsonLoader = new ConfigOperations();
                        return jsonLoader.loadConfig(configFile);
                    }
                }
                
                // Try classpath
                if (getClass().getClassLoader().getResource(configFile) != null) {
                    if (configFile.endsWith(".yaml") || configFile.endsWith(".yml")) {
                        YamlConfigLoader yamlLoader = new YamlConfigLoader();
                        return yamlLoader.loadConfig(configFile);
                    } else if (configFile.endsWith(".json")) {
                        ConfigOperations jsonLoader = new ConfigOperations();
                        return jsonLoader.loadConfig(configFile);
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not load configuration from {}: {}", configFile, e.getMessage());
            }
        }
        
        logger.warn("No configuration file found. Using empty configuration.");
        return new HashMap<>();
    }
    
    /**
     * Load environment-specific overrides from properties file
     * @return Environment overrides map
     */
    private Map<String, Object> loadEnvironmentOverrides() {
        Map<String, Object> envOverrides = new HashMap<>();
        
        // Convert properties to nested map structure
        for (Map.Entry<Object, Object> entry : environmentManager.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            
            // Process value for secrets or encrypted content
            String processedValue = secretManager.processValue(value);
            
            // Split key by dots to create nested structure
            String[] parts = key.split("\\.");
            Map<String, Object> current = envOverrides;
            
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (!current.containsKey(part)) {
                    current.put(part, new HashMap<String, Object>());
                }
                current = (Map<String, Object>) current.get(part);
            }
            
            // Set the value at the final level
            current.put(parts[parts.length - 1], processedValue);
        }
        
        return envOverrides;
    }
    
    /**
     * Merge two configuration maps with the second taking precedence
     * @param base Base configuration
     * @param override Override configuration
     * @return Merged configuration
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeConfigurations(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new HashMap<>(base);
        
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && result.containsKey(key) && result.get(key) instanceof Map) {
                // If both are maps, merge recursively
                result.put(key, mergeConfigurations(
                        (Map<String, Object>) result.get(key),
                        (Map<String, Object>) value));
            } else {
                // Otherwise, override the value
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Validate the configuration
     */
    private void validateConfiguration() {
        List<String> errors = configValidator.validateConfig(config);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Configuration validation errors:\n" + 
                    errors.stream().collect(Collectors.joining("\n  - ", "  - ", ""));
            
            logger.error(errorMessage);
            
            // Determine if errors are critical
            boolean hasCriticalErrors = errors.stream()
                    .anyMatch(error -> error.contains("Missing required"));
            
            if (hasCriticalErrors) {
                throw new RuntimeException("Critical configuration errors: " + errorMessage);
            }
        }
    }
    
    /**
     * Get a configuration value by path
     * @param path The path components
     * @return The configuration value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String... path) {
        Object current = config;
        
        for (String key : path) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return null; // Return null instead of throwing exception
                }
            } else {
                return null; // Return null for invalid paths
            }
        }
        
        // If the value is a string, process it for secrets/encryption
        if (current instanceof String) {
            String processed = secretManager.processValue((String) current);
            // Convert back to expected type if possible
            try {
                // Try to convert string to number or boolean if that's what we're expecting
                if (processed.matches("^\\d+$")) {
                    return (T) Integer.valueOf(processed);
                } else if (processed.matches("^\\d+\\.\\d+$")) {
                    return (T) Double.valueOf(processed);
                } else if (processed.equalsIgnoreCase("true") || processed.equalsIgnoreCase("false")) {
                    return (T) Boolean.valueOf(processed);
                }
            } catch (Exception e) {
                // If conversion fails, return as string
            }
            return (T) processed;
        }
        
        return (T) current;
    }
    
    /**
     * Get a configuration value with a default if not found
     * @param defaultValue The default value
     * @param path The path components
     * @return The configuration value or default
     */
    public <T> T getValue(T defaultValue, String... path) {
        T value = getValue(path);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get a string configuration value
     * @param path The path components
     * @return The string value or null
     */
    public String getString(String... path) {
        Object value = getValue(path);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get an integer configuration value
     * @param path The path components
     * @return The integer value
     * @throws NumberFormatException if value is not an integer
     */
    public int getInt(String... path) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new NumberFormatException("Value is not an integer: " + value);
    }
    
    /**
     * Get a boolean configuration value
     * @param path The path components
     * @return The boolean value
     */
    public boolean getBoolean(String... path) {
        Object value = getValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
    
    /**
     * Get a map configuration value
     * @param path The path components
     * @return The map value or null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String... path) {
        Object value = getValue(path);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
    
    /**
     * Get connection timeout from configuration
     * @return Connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return getInt("connection", "timeout");
    }
    
    /**
     * Get read timeout from configuration
     * @return Read timeout in milliseconds
     */
    public int getReadTimeout() {
        return getInt("connection", "read_timeout");
    }
    
    /**
     * Reload the configuration
     * Useful when configuration files have changed
     */
    public void reloadConfiguration() {
        environmentManager.reloadProperties();
        instance = new ConfigManager();
    }
}
