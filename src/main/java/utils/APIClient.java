package utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.Map;
import java.net.URI;

public class APIClient {
    private static volatile RequestSpecification requestSpec;
    private static final Object lock = new Object();
    private final ConfigManager config;

    public APIClient() {
        this.config = ConfigManager.getInstance();
        this.requestSpec = CustomRequestSpecBuilder.getDefaultRequestSpec();
    }

    public static RequestSpecification getRequestSpec() {
        RequestSpecification localSpec = requestSpec;
        if (localSpec == null) {
            synchronized (lock) {
                localSpec = requestSpec;
                if (localSpec == null) {
                    try {
                        ConfigManager config = ConfigManager.getInstance();
                        String baseUrl = config.getValue("base", "url").toString();
                        Map<String, Object> headers = config.getValue("api", "headers", "default");
                        Map<String, Object> authHeaders = config.getValue("auth", "headers");
                        
                        RequestSpecBuilder builder = new RequestSpecBuilder()
                            .setBaseUri(URI.create(baseUrl))
                            .setContentType(ContentType.valueOf(config.getValue("api", "content_type").toString()));
                        
                        // Add default headers
                        if (headers != null) {
                            for (Map.Entry<String, Object> header : headers.entrySet()) {
                                builder.addHeader(header.getKey(), header.getValue().toString());
                            }
                        }

                        // Add API key if configured
                        if (authHeaders != null && authHeaders.containsKey("x-api-key")) {
                            builder.addHeader("x-api-key", authHeaders.get("x-api-key").toString());
                        }
                        
                        // Only add base path if api version is specified
                        String apiVersion = config.getValue("api", "version").toString();
                        if (apiVersion != null && !apiVersion.isEmpty()) {
                            builder.setBasePath("/" + apiVersion);
                        }

                        // Configure timeouts
                        int connectionTimeout = (Integer) config.getValue("connection", "timeout");
                        int readTimeout = (Integer) config.getValue("connection", "read_timeout");
                        RestAssuredConfig restConfig = RestAssuredConfig.config()
                            .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.socket.timeout", readTimeout)
                                .setParam("http.connection.timeout", connectionTimeout));
                        
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

    public Response get(String endpoint) {
        return RestAssured
            .given()
                .spec(requestSpec)
            .when()
                .get(endpoint)
            .then()
                .extract()
                .response();
    }

    public Response post(String endpoint, Object body) {
        return RestAssured
            .given()
                .spec(requestSpec)
                .body(body)
            .when()
                .post(endpoint)
            .then()
                .extract()
                .response();
    }

    public Response put(String endpoint, Object body) {
        return RestAssured
            .given()
                .spec(requestSpec)
                .body(body)
            .when()
                .put(endpoint)
            .then()
                .extract()
                .response();
    }

    public Response delete(String endpoint) {
        return RestAssured
            .given()
                .spec(requestSpec)
            .when()
                .delete(endpoint)
            .then()
                .extract()
                .response();
    }

    public Response patch(String endpoint, Object body) {
        return RestAssured
            .given()
                .spec(requestSpec)
                .body(body)
            .when()
                .patch(endpoint)
            .then()
                .extract()
                .response();
    }
}
