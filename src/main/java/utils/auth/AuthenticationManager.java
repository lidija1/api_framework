package utils.auth;

import io.restassured.specification.RequestSpecification;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthenticationManager {
    // private static final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);
    private static final Map<String, AuthStrategy> strategies = new ConcurrentHashMap<>();
    
    static {
        // Initialize default strategies
        strategies.put("oauth2", new OAuth2Strategy("oauth2"));
        strategies.put("api_key", new ApiKeyStrategy("X-API-Key"));
    }

    /**
     * Register a new authentication strategy
     */
    public static void registerStrategy(String name, AuthStrategy strategy) {
        strategies.put(name, strategy);
    }

    /**
     * Get authentication token for specified type
     */
    public static String getToken(String authType) {
        AuthStrategy strategy = getStrategy(authType);
        return strategy.getToken();
    }

    /**
     * Apply authentication to request specification
     */
    public static RequestSpecification applyAuth(RequestSpecification spec, String authType) {
        AuthStrategy strategy = getStrategy(authType);
        return strategy.apply(spec);
    }

    private static AuthStrategy getStrategy(String authType) {
        AuthStrategy strategy = strategies.get(authType);
        if (strategy == null) {
            throw new IllegalArgumentException("No authentication strategy found for type: " + authType);
        }
        return strategy;
    }
}
