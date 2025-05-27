package base;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.Filter;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.specification.RequestSpecification;
import org.testng.ITestResult;
import org.testng.annotations.*;
import utils.APIClient;
import utils.AuthenticationManager;
import utils.LoggerUtils;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import io.restassured.response.Response;

public class BaseTest {
    protected RequestSpecification requestSpec;
    protected List<String> createdCustomerIds;
    protected static String authToken;
    protected long startTime;

    @BeforeSuite
    @Step("Setting up test suite")
    public void suiteSetup() {
        // Enable logging for all requests in test suite
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        // Set relaxed HTTPS validation
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeClass
    @Step("Setting up test class")
    public void classSetup() {
        // Add Allure reporting filter to REST Assured
        Filter allureFilter = new AllureRestAssured();
        requestSpec = APIClient.getRequestSpec().filter(allureFilter);
        createdCustomerIds = new ArrayList<>();

        try {
            authToken = AuthenticationManager.getToken("customer");
            Allure.step("Successfully obtained auth token");
        } catch (Exception e) {
            LoggerUtils.getLogger(getClass()).warn("Failed to get auth token: {}", e.getMessage());
            Allure.step("Warning: Failed to get auth token");
        }
    }

    @BeforeMethod
    @Step("Setting up test method: {0}")
    public void methodSetup(ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();

        LoggerUtils.setTestContext(className, methodName);
        LoggerUtils.logStep("Starting test: " + methodName);
        Allure.step("Starting test: " + methodName);
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(Method method) {
        LoggerUtils.info("Starting test: " + method.getName());
        DebugUtils.logEnvironmentInfo();
        startTime = System.currentTimeMillis();
    }

    @AfterMethod
    @Step("Finishing test method: {0}")
    public void methodTeardown(ITestResult result) {
        String methodName = result.getMethod().getMethodName();

        if (result.getStatus() == ITestResult.FAILURE) {
            String errorMessage = result.getThrowable().getMessage();
            LoggerUtils.getLogger(getClass()).error("Test failed: {} - Error: {}", methodName, errorMessage);

            // Attach error details to Allure report
            Allure.addAttachment("Error Details", errorMessage);
            Allure.step("Test failed: " + errorMessage);
        } else {
            Allure.step("Test passed successfully");
        }

        LoggerUtils.logStep("Finished test: " + methodName);
        LoggerUtils.clearTestContext();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        long duration = System.currentTimeMillis() - startTime;
        LoggerUtils.info("Test duration: " + duration + "ms");

        if (result.getStatus() == ITestResult.FAILURE) {
            Throwable throwable = result.getThrowable();
            if (throwable instanceof APITestException) {
                APITestException apiException = (APITestException) throwable;
                DebugUtils.createErrorReport(apiException, result.getName());
            }
            LoggerUtils.error("Test failed: " + result.getName(), throwable);
        }
    }

    @AfterClass
    @Step("Cleaning up test class")
    public void classTeardown() {
        cleanupTestData();
    }

    @Step("Cleaning up test data")
    protected void cleanupTestData() {
        createdCustomerIds.forEach(id -> {
            try {
                requestSpec
                    .when()
                    .delete("/users/" + id)
                    .then()
                    .statusCode(204);
                Allure.step("Cleaned up customer with ID: " + id);
            } catch (Exception e) {
                String errorMessage = "Failed to delete test customer " + id + ": " + e.getMessage();
                LoggerUtils.getLogger(getClass()).warn(errorMessage);
                Allure.step(errorMessage);
            }
        });
        createdCustomerIds.clear();
    }

    /**
     * Validate API response and capture details on failure
     */
    protected void validateResponse(Response response, Object requestBody, int expectedStatus) {
        try {
            response.then().statusCode(expectedStatus);
        } catch (AssertionError e) {
            String details = DebugUtils.captureFailureDetails(response, requestBody,
                Thread.currentThread().getStackTrace()[2].getMethodName());
            throw new APITestException("Response validation failed. Details captured in report.",
                response.getStatusCode(), details);
        }

        // Log performance metrics
        DebugUtils.logPerformanceMetrics(response, response.getUrl());
    }

    /**
     * Handle authentication errors with proper logging
     */
    protected void handleAuthError(Response response, String message) {
        String details = DebugUtils.captureFailureDetails(response, null,
            Thread.currentThread().getStackTrace()[2].getMethodName());
        throw new AuthenticationException(message, response.getStatusCode(), details);
    }

    /**
     * Handle validation errors with proper logging
     */
    protected void handleValidationError(Response response, String field, Object value, String message) {
        String details = DebugUtils.captureFailureDetails(response, null,
            Thread.currentThread().getStackTrace()[2].getMethodName());
        throw new ValidationException(message, field, value, response.getStatusCode(), details);
    }
}
