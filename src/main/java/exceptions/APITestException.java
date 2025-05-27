package exceptions;

/**
 * Base exception class for API testing framework
 */
public class APITestException extends RuntimeException {
    
    private final int statusCode;
    private final String responseBody;

    public APITestException(String message) {
        super(message);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public APITestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public APITestException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
