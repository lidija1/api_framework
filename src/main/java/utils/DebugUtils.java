package utils;

import exceptions.APITestException;
import io.qameta.allure.Attachment;
import io.restassured.response.Response;
import config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Utility class for debugging and troubleshooting API tests
 */
public class DebugUtils {
    private static final Logger logger = LoggerFactory.getLogger(DebugUtils.class);

    /**
     * Captures request and response details for failed tests
     * 
     * @param response The REST Assured Response object
     * @param requestBody The original request body
     * @param testName Name of the test that failed
     */
    @Attachment(value = "Failed Test Details", type = "text/plain")
    public static String captureFailureDetails(Response response, Object requestBody, String testName) {
        StringBuilder details = new StringBuilder();
        details.append("=== Test Failure Details ===\n");
        details.append("Test: ").append(testName).append("\n\n");

        // Request Details        details.append("--- Request Details ---\n");
        details.append("Method: ").append(ApiTestUtils.getHeaderValue(response, "Request-Method")).append("\n");
        details.append("URL: ").append(ApiTestUtils.getHeaderValue(response, "Request-URL")).append("\n");
        if (requestBody != null) {
            details.append("Request Body: ").append(JsonUtils.toJson(requestBody)).append("\n");
        }
        details.append("Headers: ").append(formatHeaders(response.getHeaders().asList().stream()
            .collect(java.util.stream.Collectors.toMap(
                header -> header.getName(),
                header -> header.getValue(),
                (v1, v2) -> v1 + "," + v2)))).append("\n\n");

        // Response Details
        details.append("--- Response Details ---\n");
        details.append("Status Code: ").append(response.getStatusCode()).append("\n");
        details.append("Status Line: ").append(response.getStatusLine()).append("\n");
        details.append("Response Time: ").append(response.getTime()).append("ms\n");
        details.append("Response Body: ").append(response.getBody().asPrettyString()).append("\n");

        logger.error("Test Failed: {}\nDetails: {}", testName, details.toString());
        return details.toString();
    }

    /**
     * Log performance metrics for API calls
     */
    public static void logPerformanceMetrics(Response response, String endpoint) {
        long responseTime = response.getTime();
        logger.info("Performance Metrics - Endpoint: {}", endpoint);
        logger.info("Response Time: {}ms", responseTime);
        
        // Add warning for slow responses
        if (responseTime > 2000) {
            logger.warn("Slow Response detected! Endpoint: {}, Time: {}ms", endpoint, responseTime);
        }
    }

    /**
     * Format headers for readable output
     */    private static String formatHeaders(Map<String, String> headers) {
        StringBuilder result = new StringBuilder();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                result.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
        }
        return result.toString();
    }

    /**
     * Create a detailed error report for exceptions
     */
    @Attachment(value = "Error Report", type = "text/plain")
    public static String createErrorReport(APITestException e, String testName) {
        StringBuilder report = new StringBuilder();
        report.append("=== Error Report ===\n");
        report.append("Test: ").append(testName).append("\n");
        report.append("Error Type: ").append(e.getClass().getSimpleName()).append("\n");
        report.append("Message: ").append(e.getMessage()).append("\n");
        
        if (e.getStatusCode() != 0) {
            report.append("Status Code: ").append(e.getStatusCode()).append("\n");
        }
        if (e.getResponseBody() != null) {
            report.append("Response Body: ").append(e.getResponseBody()).append("\n");
        }
        
        report.append("Stack Trace: \n");
        for (StackTraceElement element : e.getStackTrace()) {
            report.append("  ").append(element.toString()).append("\n");
        }
        
        logger.error("Error Report Generated: {}", report.toString());
        return report.toString();
    }

    /**
     * Log environment details for debugging
     */
    public static void logEnvironmentInfo() {
        logger.info("=== Environment Information ===");
        logger.info("Java Version: {}", System.getProperty("java.version"));        logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        String env = ConfigManager.getInstance().getEnvironment();
        logger.info("Environment: {}", env);
        logger.info("Base URL: {}", ConfigManager.getInstance().getBaseUrl());
    }
}
