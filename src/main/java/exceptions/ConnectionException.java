package exceptions;

import lombok.Getter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Exception class for handling connection-related errors
 * such as network issues, timeouts, or server unavailability.
 */
@Getter
public class ConnectionException extends APITestException {
    private final String endpoint;
    private final String requestMethod;

    /**
     * Constructs a new ConnectionException with the specified message
     */
    public ConnectionException(String message) {
        super(message, ErrorCode.CONNECTION_ERROR, RecoveryStrategy.RETRY);
        this.endpoint = null;
        this.requestMethod = null;
    }

    /**
     * Constructs a new ConnectionException with the specified message and cause
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, ErrorCode.CONNECTION_ERROR, RecoveryStrategy.RETRY, cause);
        this.endpoint = null;
        this.requestMethod = null;
    }

    /**
     * Constructs a new ConnectionException with detailed connection information
     */
    public ConnectionException(String message, String endpoint, String requestMethod) {
        super(message, ErrorCode.CONNECTION_ERROR, RecoveryStrategy.RETRY);
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    /**
     * Constructs a new ConnectionException with detailed connection information and cause
     */
    public ConnectionException(String message, String endpoint, String requestMethod, Throwable cause) {
        super(message, determineErrorCode(cause), determineRecoveryStrategy(cause), cause);
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    /**
     * Constructs a new ConnectionException with detailed connection information, error code, and recovery strategy
     */
    public ConnectionException(String message, ErrorCode errorCode, RecoveryStrategy recoveryStrategy,
                               String endpoint, String requestMethod, Throwable cause) {
        super(message, errorCode, recoveryStrategy, cause);
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    private static ErrorCode determineErrorCode(Throwable cause) {
        if (cause instanceof ConnectException) {
            return ErrorCode.CONNECTION_ERROR;
        } else if (cause instanceof SocketTimeoutException) {
            return ErrorCode.NETWORK_TIMEOUT;
        } else if (cause instanceof UnknownHostException) {
            return ErrorCode.DNS_RESOLUTION_ERROR;
        } else if (cause instanceof javax.net.ssl.SSLException) {
            return ErrorCode.SSL_ERROR;
        }
        return ErrorCode.CONNECTION_ERROR;
    }

    private static RecoveryStrategy determineRecoveryStrategy(Throwable cause) {
        if (cause instanceof SocketTimeoutException) {
            return RecoveryStrategy.RETRY_WITH_BACKOFF;
        } else if (cause instanceof UnknownHostException) {
            return RecoveryStrategy.FAILOVER; // Try an alternative endpoint
        } else if (cause instanceof javax.net.ssl.SSLException) {
            return RecoveryStrategy.NO_RECOVERY; // SSL issues usually require manual intervention
        }
        return RecoveryStrategy.RETRY;
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());

        if (endpoint != null) {
            message.append(" [Endpoint: ").append(endpoint).append("]");
        }

        if (requestMethod != null) {
            message.append(" [Method: ").append(requestMethod).append("]");
        }

        return message.toString();
    }

    /**
     * Factory method to create a ConnectionException from a ConnectException
     */
    public static ConnectionException fromConnectException(ConnectException connectException,
                                                           String endpoint,
                                                           String requestMethod) {
        return new ConnectionException(
                "Failed to connect to endpoint: " + endpoint,
                ErrorCode.CONNECTION_ERROR,
                RecoveryStrategy.RETRY_WITH_BACKOFF,
                endpoint,
                requestMethod,
                connectException
        );
    }

    /**
     * Factory method to create a ConnectionException from a SocketTimeoutException
     */
    public static ConnectionException fromTimeoutException(SocketTimeoutException timeoutException,
                                                           String endpoint,
                                                           String requestMethod) {
        return new ConnectionException(
                "Connection timed out for endpoint: " + endpoint,
                ErrorCode.NETWORK_TIMEOUT,
                RecoveryStrategy.RETRY_WITH_BACKOFF,
                endpoint,
                requestMethod,
                timeoutException
        );
    }

    /**
     * Factory method to create a ConnectionException from an UnknownHostException
     */
    public static ConnectionException fromUnknownHostException(UnknownHostException unknownHostException,
                                                               String endpoint,
                                                               String requestMethod) {
        return new ConnectionException(
                "Unknown host when connecting to endpoint: " + endpoint,
                ErrorCode.DNS_RESOLUTION_ERROR,
                RecoveryStrategy.FAILOVER,
                endpoint,
                requestMethod,
                unknownHostException
        );
    }

    /**
     * Factory method to create a ConnectionException from an SSLException
     */
    public static ConnectionException fromSSLException(javax.net.ssl.SSLException sslException,
                                                       String endpoint,
                                                       String requestMethod) {
        return new ConnectionException(
                "SSL/TLS error when connecting to endpoint: " + endpoint,
                ErrorCode.SSL_ERROR,
                RecoveryStrategy.NO_RECOVERY,
                endpoint,
                requestMethod,
                sslException
        );
    }
}