package utils;

import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.authentication.OAuth2Scheme;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import static io.restassured.RestAssured.given;

public class AuthenticationManager {    private static final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    /**
     * Add OAuth2 token to request
     */
    public static RequestSpecification addOAuth2Token(RequestSpecification spec, String token) {
        OAuth2Scheme oauth2 = new OAuth2Scheme();
        oauth2.setAccessToken(token);
        return spec.auth().oauth2(token);
    }

    /**
     * Add Basic Authentication to request
     */
    public static RequestSpecification addBasicAuth(RequestSpecification spec, String username, String password) {
        PreemptiveBasicAuthScheme basicAuth = new PreemptiveBasicAuthScheme();
        basicAuth.setUserName(username);
        basicAuth.setPassword(password);
        return spec.auth().preemptive().basic(username, password);
    }

    /**
     * Add API Key to request
     */
    public static RequestSpecification addApiKey(RequestSpecification spec, String apiKey) {
        return spec.header("X-API-Key", apiKey);
    }

    /**
     * Get cached token or generate new one
     */
    public static String getToken(String tokenType) {
        return tokenCache.computeIfAbsent(tokenType, k -> generateNewToken(tokenType));
    }

    /**
     * Generate new token (example implementation for Reqres.in)
     */    private static String generateNewToken(String tokenType) {
        // Use the shared APIClient configuration for consistency
        return given()
            .spec(APIClient.getRequestSpec())
            .body(createLoginCredentials())
            .when()
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .path("token");
    }

    /**
     * Clear token cache
     */
    public static void clearTokenCache() {
        tokenCache.clear();
    }

    /**
     * Invalidate specific token
     */
    public static void invalidateToken(String tokenType) {
        tokenCache.remove(tokenType);
    }

    /**
     * Creates a map of login credentials for authentication
     */
    private static Map<String, String> createLoginCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "eve.holt@reqres.in");
        credentials.put("password", "cityslicka");
        return credentials;
    }

}
