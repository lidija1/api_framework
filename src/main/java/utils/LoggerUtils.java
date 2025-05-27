package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import io.qameta.allure.Step;

/**
 * Utility class for centralized logging with MDC context support
 */
public class LoggerUtils {
    private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);
    private static final String TEST_NAME = "testName";
    private static final String TEST_CLASS = "testClass";
    
    /**
     * Get logger for specific class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Set test context for logging
     */
    public static void setTestContext(String className, String methodName) {
        MDC.put(TEST_CLASS, className);
        MDC.put(TEST_NAME, methodName);
    }
    
    /**
     * Clear test context
     */
    public static void clearTestContext() {
        MDC.clear();
    }
    
    /**
     * Log request details
     */
    public static void logRequest(String endpoint, String method, String body) {
        Logger logger = getLogger(LoggerUtils.class);
        logger.info("API Request - Endpoint: {} - Method: {}", endpoint, method);
        if (body != null) {
            logger.debug("Request Body: {}", body);
        }
    }
    
    /**
     * Log response details
     */
    public static void logResponse(int statusCode, String body, long responseTime) {
        Logger logger = getLogger(LoggerUtils.class);
        logger.info("API Response - Status: {} - Time: {} ms", statusCode, responseTime);
        if (body != null) {
            logger.debug("Response Body: {}", body);
        }
    }
    
    /**
     * Log an info message
     */
    @Step("{message}")
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Log an error message with exception
     */
    @Step("Error: {message}")
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Log an error message
     */
    @Step("Error: {message}")
    public static void error(String message) {
        logger.error(message);
    }

    /**
     * Log a warning message
     */
    @Step("Warning: {message}")
    public static void warn(String message) {
        logger.warn(message);
    }

    /**
     * Log a debug message
     */
    public static void debug(String message) {
        logger.debug(message);
    }

    /**
     * Log a test step with info level
     */
    @Step("{stepDescription}")
    public static void logStep(String stepDescription) {
        logger.info("Step: {}", stepDescription);
    }
}
