package base;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.Filter;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;
import utils.AuthenticationManager;
import utils.LoggerUtils;
import utils.DebugUtils;
import exceptions.APITestException;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import io.restassured.response.Response;
import config.ConfigManager;
import utils.RetryHandler;
import exceptions.ApiResponseException;
import utils.ResponseValidator;

public abstract class BaseTest {
    protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected static final ConfigManager config = ConfigManager.getInstance();
    
    protected RequestSpecification requestSpec;
    protected RetryHandler retryHandler;
    protected List<String> createdResources = new ArrayList<>();
    protected String authToken;
    protected long startTime;

    @BeforeSuite
    @Step("Setting up test suite")
    public void suiteSetup() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        RestAssured.useRelaxedHTTPSValidation();
        retryHandler = new RetryHandler();
        logEnvironmentInfo();
    }

    private void logEnvironmentInfo() {
        String env = config.getValue("environment");
        String baseUrl = config.getValue("base", "url");
        String apiVersion = config.getValue("api", "version");
        logger.info("Test Environment: {}", env);
        logger.info("Base URL: {}", baseUrl);
        logger.info("API Version: {}", apiVersion);
    }

    @BeforeClass
    @Step("Setting up test class")
    public void classSetup() {
        Filter allureFilter = new AllureRestAssured();
        if (requestSpec != null) {
            requestSpec = requestSpec.filter(allureFilter);
        }
        try {
            authToken = AuthenticationManager.getToken(getAuthTokenType());
            Allure.step("Successfully obtained auth token");
        } catch (Exception e) {
            logger.warn("Failed to get auth token: {}", e.getMessage());
            Allure.step("Warning: Failed to get auth token");
        }
    }

    @BeforeMethod
    @Step("Setting up test method: {0}")
    public void methodSetup(Method method, ITestResult result) {
        String className = result.getTestClass().getName();
        String methodName = method.getName();
        LoggerUtils.setTestContext(className, methodName);
        LoggerUtils.logStep("Starting test: " + methodName);
        Allure.step("Starting test: " + methodName);
        startTime = System.currentTimeMillis();
        createdResources.clear();
    }

    @AfterMethod
    @Step("Finishing test method: {0}")
    public void methodTeardown(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        long duration = System.currentTimeMillis() - startTime;
        LoggerUtils.info("Test duration: " + duration + "ms");
        if (result.getStatus() == ITestResult.FAILURE) {
            handleTestFailure(result);
        } else {
            Allure.step("Test passed successfully");
        }
        LoggerUtils.logStep("Finished test: " + methodName);
        LoggerUtils.clearTestContext();
    }

    private void handleTestFailure(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();
        String errorMessage = throwable.getMessage();
        logger.error("Test failed: {} - Error: {}", methodName, errorMessage);
        Allure.addAttachment("Error Details", errorMessage);
        Allure.step("Test failed: " + errorMessage);
        if (throwable instanceof APITestException) {
            DebugUtils.createErrorReport((APITestException) throwable, methodName);
        }
    }

    @AfterClass
    @Step("Cleaning up test class")
    public void classTeardown() {
        cleanupResources();
    }

    protected void addResourceForCleanup(String resourceId) {
        createdResources.add(resourceId);
    }

    protected void cleanupResources() {
        for (String resourceUrl : createdResources) {
            try {
                executeWithRetry(() -> 
                    RestAssured.given()
                        .spec(requestSpec)
                        .delete(resourceUrl)
                        .then()
                        .extract()
                        .response()
                );
            } catch (Exception e) {
                logger.error("Failed to cleanup resource: {}", resourceUrl, e);
            }
        }
        createdResources.clear();
    }

    protected Response executeWithRetry(RetryHandler.RetryableRequest request) throws Exception {
        return retryHandler.executeWithRetry(request);
    }

    protected abstract String getAuthTokenType();

    protected void validateResponse(Response response, Object requestBody, int expectedStatus) {
        try {
            ResponseValidator.validate(response, expectedStatus);
        } catch (ApiResponseException e) {
            DebugUtils.captureFailureDetails(response, requestBody,
                Thread.currentThread().getStackTrace()[2].getMethodName());
            throw new ApiResponseException(e.getMessage(), response, e);
        }
        DebugUtils.logPerformanceMetrics(response, response.getHeaders().hasHeaderWithName("Location") ? response.getHeaders().get("Location").getValue() : "");
    }

    protected void handleAuthError(Response response, String message) {
        DebugUtils.captureFailureDetails(response, null,
            Thread.currentThread().getStackTrace()[2].getMethodName());
        throw ApiResponseException.unauthorized(response);
    }

    protected void handleValidationError(Response response, String field, Object value, String message) {
        DebugUtils.captureFailureDetails(response, null,
            Thread.currentThread().getStackTrace()[2].getMethodName());
        throw ApiResponseException.badRequest(response);
    }
}
