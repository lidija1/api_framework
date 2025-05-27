package utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import config.ConfigManager;

public class APIClient {
    private static volatile RequestSpecification requestSpec;
    private static final Object lock = new Object();
    
    public static RequestSpecification getRequestSpec() {
        RequestSpecification localSpec = requestSpec;
        if (localSpec == null) {
            synchronized (lock) {
                localSpec = requestSpec;
                if (localSpec == null) {
                    try {
                        ConfigManager config = ConfigManager.getInstance();
                        RequestSpecBuilder builder = new RequestSpecBuilder()
                            .setBaseUri(config.getBaseUrl())
                            .setContentType(ContentType.JSON);
                        
                        // Only add base path if api version is specified
                        String apiVersion = config.getApiVersion();
                        if (apiVersion != null && !apiVersion.isEmpty()) {
                            builder.setBasePath("/" + apiVersion);
                        }

                        // Configure timeouts with proper error handling
                        RestAssuredConfig restConfig = RestAssuredConfig.config()
                            .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.socket.timeout", config.getReadTimeout())
                                .setParam("http.connection.timeout", config.getConnectionTimeout()));
                        
                        builder.setConfig(restConfig);
                        requestSpec = builder.build();
                        localSpec = requestSpec;
                    } catch (Exception e) {
                        LoggerUtils.error("Failed to initialize request specification", e);
                        throw new RuntimeException("API Client initialization failed", e);
                    }
                }
            }
        }
        return localSpec;
    }
}
