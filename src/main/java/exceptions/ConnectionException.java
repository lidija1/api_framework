package exceptions;

import lombok.Getter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Exception class for handling connection-related errors
 * such as network issues, timeouts, or server unavailability.
 * <p>
 * This exception is thrown when a connection to an API endpoint fails due to:
 * <ul>
 *   <li>Network connectivity issues</li>
 *   <li>Connection timeout</li>
 *   <li>Socket timeout</li>
 *   <li>Unknown host</li>
 *   <li>Other connectivity-related failures</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * try {
 *     apiClient.get("/users");
 * } catch (ConnectionException e) {
 *     logger.error("Connection failed to endpoint: {} using method: {}", 
 *                 e.getEndpoint(), e.getRequestMethod());
 *     // Implement retry or fallback logic
 * }
 * </pre>
 */
@Getter
public class ConnectionException extends APITestException {
    private final String endpoint;
    private final String requestMethod;

    /**
     * Constructs a new ConnectionException with the specified message
     * 
     * @param message Error message describing the connection issue
     */
    public ConnectionException(String message) {
        super(message);
        this.endpoint = null;
        this.requestMethod = null;
    }

    /**
     * Constructs a new ConnectionException with the specified message and cause
     * 
     * @param message Error message describing the connection issue
     * @param cause The underlying cause of the connection failure
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.endpoint = null;
        this.requestMethod = null;
    }

    /**
     * Constructs a new ConnectionException with detailed connection information
     * 
     * @param message Error message describing the connection issue
     * @param endpoint The API endpoint that was being accessed
     * @param requestMethod The HTTP method used (GET, POST, etc.)
     */
    public ConnectionException(String message, String endpoint, String requestMethod) {
        super(message);
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    /**
     * Constructs a new ConnectionException with detailed connection information and cause
     * 
     * @param message Error message describing the connection issue
     * @param endpoint The API endpoint that was being accessed
     * @param requestMethod The HTTP method used (GET, POST, etc.)
     * @param cause The underlying cause of the connection failure
     */
    public ConnectionException(String message, String endpoint, String requestMethod, Throwable cause) {
        super(message, cause);
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    /**
     * Factory method to create a ConnectionException from a ConnectException
     * 
     * @param connectException The original ConnectException
     * @param endpoint The API endpoint that was being accessed
     * @param requestMethod The HTTP method used
     * @return A new ConnectionException wrapping the ConnectException
     */
    public static ConnectionException fromConnectException(ConnectException connectException, 
                                                          String endpoint, 
                                                          String requestMethod) {
        return new ConnectionException(
            "Failed to connect to endpoint: " + endpoint, 
            endpoint, 
            requestMethod, 
            connectException
        );
    }

    /**
     * Factory method to create a ConnectionException from a SocketTimeoutException
     * 
     * @param timeoutException The original SocketTimeoutException
     * @param endpoint The API endpoint that was being accessed
     * @param requestMethod The HTTP method used
     * @return A new ConnectionException wrapping the SocketTimeoutException
     */
    public static ConnectionException fromTimeoutException(SocketTimeoutException timeoutException, 
                                                         String endpoint, 
                                                         String requestMethod) {
        return new ConnectionException(
            "Connection timed out for endpoint: " + endpoint, 
            endpoint, 
            requestMethod, 
            timeoutException
        );
    }

    /**
     * Factory method to create a ConnectionException from an UnknownHostException
     * 
     * @param unknownHostException The original UnknownHostException
     * @param endpoint The API endpoint that was being accessed
     * @param requestMethod The HTTP method used
     * @return A new ConnectionException wrapping the UnknownHostException
     */
    public static ConnectionException fromUnknownHostException(UnknownHostException unknownHostException, 
                                                             String endpoint, 
                                                             String requestMethod) {
        return new ConnectionException(
            "Unknown host when connecting to endpoint: " + endpoint, 
            endpoint, 
            requestMethod, 
            unknownHostException
        );
    }
}
