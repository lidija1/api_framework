package config;

import java.util.Map;

public class ConfigManager {
    
    private static ConfigManager instance;
    private final Map<String, Object> config;
    
    private ConfigManager() {
        ConfigOperations configOps = new ConfigOperations();
        this.config = configOps.loadConfig("config/config.json");
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getValue(String... path) {
        Object current = config;
        for (String key : path) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    throw new RuntimeException("Configuration value not found for path: " + String.join(".", path));
                }
            } else {
                throw new RuntimeException("Invalid configuration path: " + String.join(".", path));
            }
        }
        return (T) current;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String... path) {
        return getValue(path);
    }

    @SuppressWarnings("unchecked")
    public String getBaseUrl() {
        Map<String, Object> data = (Map<String, Object>) config.get("base");
        return (String) data.get("url");
    }
    
    @SuppressWarnings("unchecked")
    public String getApiVersion() {
        Map<String, Object> data = (Map<String, Object>) config.get("api");
        return (String) data.get("version");
    }

    @SuppressWarnings("unchecked")
    public String getApiKey() {
        Map<String, Object> data = (Map<String, Object>) config.get("api");
        return (String) data.get("key");
    }
    
    @SuppressWarnings("unchecked")
    public int getConnectionTimeout() {
        Map<String, Object> data = (Map<String, Object>) config.get("connection");
        return (Integer) data.get("timeout");
    }
    
    @SuppressWarnings("unchecked")
    public int getReadTimeout() {
        Map<String, Object> data = (Map<String, Object>) config.get("connection");
        return (Integer) data.get("read_timeout");
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigSection(String section) {
        return (Map<String, Object>) config.get(section);
    }
    
    public Object getConfigValue(String section, String key) {
        Map<String, Object> sectionConfig = getConfigSection(section);
        return sectionConfig != null ? sectionConfig.get(key) : null;
    }

    public String getEnvironment() {
        return (String) config.get("environment");
    }
}
