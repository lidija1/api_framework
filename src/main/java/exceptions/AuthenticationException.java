package exceptions;

/**
 * Exception for authentication-related errors
 */
public class AuthenticationException extends APITestException {
    
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
