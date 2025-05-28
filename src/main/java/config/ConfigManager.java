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
    public int getConnectionTimeout() {
        Map<String, Object> data = (Map<String, Object>) config.get("connection");
        return (Integer) data.get("timeout");
    }
    
    @SuppressWarnings("unchecked")
    public int getReadTimeout() {
        Map<String, Object> data = (Map<String, Object>) config.get("connection");
        return (Integer) data.get("read_timeout");
    }
}
