package utils.http;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import config.ConfigManager;
import exceptions.ConnectionException;
import exceptions.ApiResponseException;
import exceptions.SerializationException;
import exceptions.ConfigurationException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import utils.LoggerUtils;

import java.util.Map;
import java.net.URI;

public class APIClient {
    private static volatile RequestSpecification requestSpec;
    private static final Object lock = new Object();

    public APIClient() {
        requestSpec = CustomRequestSpecBuilder.getDefaultRequestSpec();
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
                        if (baseUrl == null || baseUrl.isEmpty()) {
                            throw new ConfigurationException("Base URL is missing in configuration");
                        }
                        Map<String, Object> headers = config.getValue("api", "headers", "default");
                        Map<String, Object> authHeaders = config.getValue("auth", "headers");
                        RequestSpecBuilder builder = new RequestSpecBuilder()
                            .setBaseUri(URI.create(baseUrl))
                            .setContentType(ContentType.valueOf(config.getValue("api", "content_type").toString()));
                        if (headers != null) {
                            for (Map.Entry<String, Object> header : headers.entrySet()) {
                                builder.addHeader(header.getKey(), header.getValue().toString());
                            }
                        }
                        if (authHeaders != null && authHeaders.containsKey("x-api-key")) {
                            builder.addHeader("x-api-key", authHeaders.get("x-api-key").toString());
                        }
                        String apiVersion = config.getValue("api", "version").toString();
                        if (apiVersion != null && !apiVersion.isEmpty()) {
                            builder.setBasePath("/" + apiVersion);
                        }
                        int connectionTimeout, readTimeout;
                        try {
                            connectionTimeout = (Integer) config.getValue("connection", "timeout");
                            readTimeout = (Integer) config.getValue("connection", "read_timeout");
                        } catch (Exception e) {
                            throw new ConfigurationException("Invalid or missing timeout configuration", e);
                        }
                        RestAssuredConfig restConfig = RestAssuredConfig.config()
                            .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.socket.timeout", readTimeout)
                                .setParam("http.connection.timeout", connectionTimeout));
                        builder.setConfig(restConfig);
                        requestSpec = builder.build();
                        localSpec = requestSpec;
                    } catch (ConfigurationException ce) {
                        throw ce;
                    } catch (Exception e) {
                        LoggerUtils.error("Failed to initialize request specification", e);
                        throw new ConfigurationException("API Client initialization failed", e);
                    }
                }
            }
        }
        return localSpec;
    }

    public Response get(String endpoint) {
        try {
            long startTime = System.currentTimeMillis();
            LoggerUtils.logRequest(endpoint, "GET", null);
            Response response = RestAssured
                .given()
                    .spec(getRequestSpec())
                    .header("X-Request-Method", "GET")
                    .header("X-Request-URL", endpoint)
                .when()
                    .get(endpoint)
                .then()
                    .extract()
                    .response();
            LoggerUtils.logResponse(response.getStatusCode(), response.getBody().asString(), 
                               System.currentTimeMillis() - startTime);
            if (response.getStatusCode() >= 400) {
                throw new ApiResponseException("API returned error status: " + response.getStatusCode(), response);
            }
            return response;
        } catch (ConnectionException e) {
            LoggerUtils.error("Connection error during GET request to " + endpoint, e);
            throw new ConnectionException("Failed to connect to API endpoint: " + endpoint, 
                                          endpoint, "GET", e);
        } catch (ApiResponseException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtils.error("Error during GET request to " + endpoint, e);
            throw e;
        }
    }

    public Response post(String endpoint, Object body) {
        try {
            long startTime = System.currentTimeMillis();
            String bodyStr;
            try {
                bodyStr = body != null ? body.toString() : null;
            } catch (Exception e) {
                throw new SerializationException("Failed to serialize request body", e);
            }
            LoggerUtils.logRequest(endpoint, "POST", bodyStr);
            Response response = RestAssured
                .given()
                    .spec(getRequestSpec())
                    .header("X-Request-Method", "POST")
                    .header("X-Request-URL", endpoint)
                    .body(body)
                .when()
                    .post(endpoint)
                .then()
                    .extract()
                    .response();
            LoggerUtils.logResponse(response.getStatusCode(), response.getBody().asString(), 
                               System.currentTimeMillis() - startTime);
            if (response.getStatusCode() >= 400) {
                throw new ApiResponseException("API returned error status: " + response.getStatusCode(), response);
            }
            return response;
        } catch (SerializationException | ConnectionException | ApiResponseException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtils.error("Error during POST request to " + endpoint, e);
            throw e;
        }
    }

    public Response put(String endpoint, Object body) {
        try {
            long startTime = System.currentTimeMillis();
            String bodyStr;
            try {
                bodyStr = body != null ? body.toString() : null;
            } catch (Exception e) {
                throw new SerializationException("Failed to serialize request body", e);
            }
            LoggerUtils.logRequest(endpoint, "PUT", bodyStr);
            Response response = RestAssured
                .given()
                    .spec(getRequestSpec())
                    .header("X-Request-Method", "PUT")
                    .header("X-Request-URL", endpoint)
                    .body(body)
                .when()
                    .put(endpoint)
                .then()
                    .extract()
                    .response();
            LoggerUtils.logResponse(response.getStatusCode(), response.getBody().asString(), 
                               System.currentTimeMillis() - startTime);
            if (response.getStatusCode() >= 400) {
                throw new ApiResponseException("API returned error status: " + response.getStatusCode(), response);
            }
            return response;
        } catch (SerializationException | ConnectionException | ApiResponseException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtils.error("Error during PUT request to " + endpoint, e);
            throw e;
        }
    }

    public Response delete(String endpoint) {
        try {
            long startTime = System.currentTimeMillis();
            LoggerUtils.logRequest(endpoint, "DELETE", null);
            Response response = RestAssured
                .given()
                    .spec(getRequestSpec())
                    .header("X-Request-Method", "DELETE")
                    .header("X-Request-URL", endpoint)
                .when()
                    .delete(endpoint)
                .then()
                    .extract()
                    .response();
            LoggerUtils.logResponse(response.getStatusCode(), response.getBody().asString(), 
                               System.currentTimeMillis() - startTime);
            if (response.getStatusCode() >= 400) {
                throw new ApiResponseException("API returned error status: " + response.getStatusCode(), response);
            }
            return response;
        } catch (ConnectionException | ApiResponseException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtils.error("Error during DELETE request to " + endpoint, e);
            throw e;
        }
    }

    public Response patch(String endpoint, Object body) {
        try {
            long startTime = System.currentTimeMillis();
            String bodyStr;
            try {
                bodyStr = body != null ? body.toString() : null;
            } catch (Exception e) {
                throw new SerializationException("Failed to serialize request body", e);
            }
            LoggerUtils.logRequest(endpoint, "PATCH", bodyStr);
            Response response = RestAssured
                .given()
                    .spec(getRequestSpec())
                    .header("X-Request-Method", "PATCH")
                    .header("X-Request-URL", endpoint)
                    .body(body)
                .when()
                    .patch(endpoint)
                .then()
                    .extract()
                    .response();
            LoggerUtils.logResponse(response.getStatusCode(), response.getBody().asString(), 
                               System.currentTimeMillis() - startTime);
            if (response.getStatusCode() >= 400) {
                throw new ApiResponseException("API returned error status: " + response.getStatusCode(), response);
            }
            return response;
        } catch (SerializationException | ConnectionException | ApiResponseException e) {
            throw e;
        } catch (Exception e) {
            LoggerUtils.error("Error during PATCH request to " + endpoint, e);
            throw e;
        }
    }
}
