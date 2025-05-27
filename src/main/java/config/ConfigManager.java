package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager instance;
    private final Properties properties;    private ConfigManager() {
        properties = new Properties();
        String env = System.getProperty("environment", "dev");
        try {
            // Load default properties first
            properties.setProperty("base.url", "https://reqres.in/api");
            properties.setProperty("api.version", "");
            properties.setProperty("timeout.connection", "30000");
            properties.setProperty("timeout.read", "60000");
            
            // Load environment specific properties, overriding defaults if exists
            String configFile = "src/test/resources/config/" + env + ".properties";
            if (new File(configFile).exists()) {
                properties.load(new FileInputStream(configFile));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage());
        }
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getBaseUrl() {
        return getProperty("base.url");
    }

    public String getApiVersion() {
        return getProperty("api.version");
    }

    public int getConnectionTimeout() {
        return Integer.parseInt(getProperty("timeout.connection"));
    }

    public int getReadTimeout() {
        return Integer.parseInt(getProperty("timeout.read"));
    }

    public String getAuthToken() {
        return getProperty("auth.token");
    }

    public String getEnvironment() {
        return System.getProperty("environment", "dev");
    }
}
