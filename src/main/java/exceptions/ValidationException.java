package exceptions;

/**
 * Exception for data validation errors
 */
public class ValidationException extends APITestException {
    
    private final String field;
    private final Object value;

    public ValidationException(String message, String field, Object value) {
        super(message);
        this.field = field;
        this.value = value;
    }

    public ValidationException(String message, String field, Object value, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
