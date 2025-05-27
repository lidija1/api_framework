package utils;

import io.restassured.response.Response;
import io.qameta.allure.Step;
import org.testng.Assert;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class containing helper methods for API testing with RestAssured.
 */
public class ApiTestUtils {
    
    /**
     * Extracts a header value from a RestAssured Response.
     * Returns null if the header is not present.
     *
     * @param response The RestAssured Response object
     * @param headerName The name of the header to extract
     * @return The header value or null if not found
     */
    @Step("Getting header value: {headerName}")
    public static String getHeaderValue(Response response, String headerName) {
        if (response == null || headerName == null) {
            return null;
        }
        return response.getHeader(headerName);
    }

    /**
     * Checks if a JSON response contains a non-empty list at a given JsonPath.
     *
     * @param response The RestAssured Response object
     * @param path The JsonPath to the list
     * @return true if the path contains a non-empty list, false otherwise
     */
    @Step("Checking for non-empty list at path: {path}")
    public static boolean hasNonEmptyList(Response response, String path) {
        if (response == null || path == null) {
            return false;
        }
        
        try {
            List<?> list = response.jsonPath().getList(path);
            return list != null && !list.isEmpty();
        } catch (Exception e) {
            LoggerUtils.error("Error checking list at path: " + path, e);
            return false;
        }
    }

    /**
     * Builds a request body Map from key-value pairs.
     * Useful for simple request bodies without nested objects.
     *
     * @param keyValuePairs Variable number of key-value pairs (must be even)
     * @return Map containing the key-value pairs
     * @throws IllegalArgumentException if odd number of arguments provided
     */
    public static Map<String, Object> buildRequestBody(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide key-value pairs (even number of arguments)");
        }

        Map<String, Object> requestBody = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            requestBody.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return requestBody;
    }

    /**
     * Validates if a date string in a response field matches expected format.
     * Default format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ss.SSS'Z').
     *
     * @param response The RestAssured Response object
     * @param path The JsonPath to the date field
     * @param format Optional date format pattern (uses ISO-8601 if not provided)
     * @return true if date format is valid, false otherwise
     */
    @Step("Validating date format at path: {path}")
    public static boolean isValidDateFormat(Response response, String path, String format) {
        if (response == null || path == null) {
            return false;
        }

        String dateStr = response.jsonPath().getString(path);
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }

        String dateFormat = format != null ? format : "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        try {
            LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(dateFormat));
            return true;
        } catch (DateTimeParseException e) {
            LoggerUtils.error("Invalid date format at path: " + path, e);
            return false;
        }
    }

    /**
     * Validates response status code and provides detailed logging.
     * Throws AssertionError if status code doesn't match expected value.
     *
     * @param response The RestAssured Response object
     * @param expectedStatusCode The expected HTTP status code
     */
    @Step("Validating status code: expected {expectedStatusCode}")
    public static void validateStatusCode(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        LoggerUtils.info(String.format("Status Code Validation - Expected: %d, Actual: %d", 
            expectedStatusCode, actualStatusCode));
        
        if (actualStatusCode != expectedStatusCode) {
            String responseBody = response.getBody().asString();
            LoggerUtils.error(String.format("Status code mismatch! Response body: %s", responseBody));
            Assert.assertEquals(actualStatusCode, expectedStatusCode, 
                "Unexpected status code received");
        }
    }

    /**
     * Creates a copy of a Map with null values removed.
     * Useful for cleaning up request bodies before sending.
     *
     * @param originalMap The original Map
     * @return A new Map with null values removed
     */
    public static Map<String, Object> removeNullValues(Map<String, Object> originalMap) {
        if (originalMap == null) {
            return new HashMap<>();
        }

        Map<String, Object> cleanMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            if (entry.getValue() != null) {
                cleanMap.put(entry.getKey(), entry.getValue());
            }
        }
        return cleanMap;
    }

    /**
     * Extracts all error messages from a standard error response format.
     * Assumes errors are in a list at the "errors" path.
     *
     * @param response The RestAssured Response object
     * @return List of error messages or empty list if none found
     */
    @Step("Extracting error messages from response")
    public static List<String> extractErrorMessages(Response response) {        try {
            return response.jsonPath().getList("errors.message");
        } catch (Exception e) {
            LoggerUtils.error("Failed to extract error messages from response", e);
            return Collections.emptyList();
        }
    }
}
