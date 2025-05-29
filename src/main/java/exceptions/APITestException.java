package exceptions;

/**
 * Base exception class for API testing framework.
 * Enhanced with error codes, categories, and recovery strategies.
 */
public class APITestException extends RuntimeException {

    private final ErrorCode errorCode;
    private final RecoveryStrategy recoveryStrategy;
    private final int statusCode;
    private final String responseBody;

    public APITestException(String message) {
        this(message, ErrorCode.UNKNOWN_ERROR, RecoveryStrategy.NO_RECOVERY);
    }

    public APITestException(String message, Throwable cause) {
        this(message, ErrorCode.UNKNOWN_ERROR, RecoveryStrategy.NO_RECOVERY, cause);
    }

    public APITestException(String message, ErrorCode errorCode) {
        this(message, errorCode, RecoveryStrategy.NO_RECOVERY);
    }

    public APITestException(String message, ErrorCode errorCode, RecoveryStrategy recoveryStrategy) {
        super(message);
        this.errorCode = errorCode;
        this.recoveryStrategy = recoveryStrategy;
        this.statusCode = 0;
        this.responseBody = null;
    }

    public APITestException(String message, ErrorCode errorCode, RecoveryStrategy recoveryStrategy, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoveryStrategy = recoveryStrategy;
        this.statusCode = 0;
        this.responseBody = null;
    }

    public APITestException(String message, int statusCode, String responseBody) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.recoveryStrategy = RecoveryStrategy.NO_RECOVERY;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public APITestException(String message, ErrorCode errorCode, int statusCode, String responseBody) {
        super(message);
        this.errorCode = errorCode;
        this.recoveryStrategy = RecoveryStrategy.NO_RECOVERY;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public APITestException(String message, ErrorCode errorCode, RecoveryStrategy recoveryStrategy,
                            int statusCode, String responseBody) {
        super(message);
        this.errorCode = errorCode;
        this.recoveryStrategy = recoveryStrategy;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getFullErrorCode() {
        return errorCode.getFullCode();
    }

    public ErrorCategory getErrorCategory() {
        return errorCode.getCategory();
    }

    public RecoveryStrategy getRecoveryStrategy() {
        return recoveryStrategy;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(" [Error Code: ").append(getFullErrorCode()).append("]");

        if (statusCode > 0) {
            message.append(" [Status: ").append(statusCode).append("]");
        }

        return message.toString();
    }

    /**
     * Creates a new exception with the same properties but a different recovery strategy
     */
    public APITestException withRecoveryStrategy(RecoveryStrategy newStrategy) {
        return new APITestException(
                super.getMessage(),
                this.errorCode,
                newStrategy,
                this.statusCode,
                this.responseBody
        );
    }
}