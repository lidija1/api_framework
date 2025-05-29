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
import utils.auth.AuthenticationManager;
import utils.LoggerUtils;
import utils.DebugUtils;
import exceptions.APITestException;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import io.restassured.response.Response;
import config.ConfigManager;
import utils.RetryHandler;
import exceptions.ApiResponseException;
import utils.validation.ResponseValidator;
import utils.reporting.PerformanceMetrics;
import utils.reporting.RequestResponseLogger;
import utils.reporting.ApiTestMarkers;

public abstract class BaseTest {
    protected static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected static final ConfigManager config = ConfigManager.getInstance();
    
    protected RequestSpecification requestSpec;
    protected RetryHandler retryHandler;
    protected List<String> createdResources = new ArrayList<>();
    protected String authToken;
    protected long startTime;
    
    // Performance metrics collector
    protected static final PerformanceMetrics performanceMetrics = PerformanceMetrics.getInstance();
    // Enhanced request/response logger
    protected static final RequestResponseLogger requestResponseLogger = RequestResponseLogger.getInstance();

    @BeforeSuite
    @Step("Setting up test suite")
    public void suiteSetup() {
        // Configure REST Assured with enhanced logging
        RestAssured.filters(new AllureRestAssured(), 
                           new RequestLoggingFilter(), 
                           new ResponseLoggingFilter());
        RestAssured.useRelaxedHTTPSValidation();
        
        // Initialize retry handler
        retryHandler = new RetryHandler();
        
        // Reset performance metrics and request logs
        performanceMetrics.reset();
        requestResponseLogger.clearLogs();
        
        // Log environment info
        logEnvironmentInfo();
    }

    private void logEnvironmentInfo() {
        String env = config.getValue("environment");
        String baseUrl = config.getValue("base", "url");
        String apiVersion = config.getValue("api", "version");
        logger.info("Test Environment: {}", env);
        logger.info("Base URL: {}", baseUrl);
        logger.info("API Version: {}", apiVersion);
        
        // Add environment info to Allure report
        Allure.addAttachment("Environment Information", "text/plain", 
            "Environment: " + env + "\n" +
            "Base URL: " + baseUrl + "\n" +
            "API Version: " + apiVersion
        );
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
        
        // Add class-level markers to Allure report
        addClassMarkersToAllure(this.getClass());
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
        
        // Add method-level markers to Allure report
        addMethodMarkersToAllure(method);
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
        
        // Attach failed request/response details to report
        requestResponseLogger.attachFailedRequestsToAllure(methodName);
        
        if (throwable instanceof APITestException) {
            DebugUtils.createErrorReport((APITestException) throwable, methodName);
        }
    }

    @AfterClass
    @Step("Cleaning up test class")
    public void classTeardown() {
        cleanupResources();
    }
    
    @AfterSuite
    @Step("Finishing test suite")
    public void suiteTeardown() {
        // Attach performance metrics to Allure report
        performanceMetrics.attachMetricsToAllureReport();
        performanceMetrics.generatePerformanceReport();
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
        String requestPath = getRequestPath(response);
        String requestMethod = getRequestMethod(response);
        long responseTime = response.getTime();
        
        try {
            ResponseValidator.validate(response, expectedStatus);
            
            // Record response details for successful requests
            requestResponseLogger.recordRequestResponse(
                requestMethod,
                requestPath,
                requestBody != null ? requestBody.toString() : null,
                response,
                responseTime
            );
            
        } catch (ApiResponseException e) {
            // Record response details for failed requests
            requestResponseLogger.recordRequestResponse(
                requestMethod,
                requestPath,
                requestBody != null ? requestBody.toString() : null,
                response,
                responseTime
            );
            
            DebugUtils.captureFailureDetails(response, requestBody,
                Thread.currentThread().getStackTrace()[2].getMethodName());
            throw new ApiResponseException(e.getMessage(), response, e);
        }
        
        // Log performance metrics
        performanceMetrics.recordResponseMetrics(response, requestPath, requestMethod);
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
    
    /**
     * Extract request path from response for logging purposes
     */
    private String getRequestPath(Response response) {
        try {
            if (response.getHeaders().hasHeaderWithName("Request-URI")) {
                return response.getHeader("Request-URI");
            } else if (response.getHeaders().hasHeaderWithName("Location")) {
                return response.getHeader("Location");
            }
        } catch (Exception e) {
            logger.debug("Could not extract request path from response headers", e);
        }
        return "unknown-path";
    }
    
    /**
     * Extract request method from response for logging purposes
     */
    private String getRequestMethod(Response response) {
        try {
            if (response.getHeaders().hasHeaderWithName("Request-Method")) {
                return response.getHeader("Request-Method");
            }
        } catch (Exception e) {
            logger.debug("Could not extract request method from response headers", e);
        }
        return "unknown-method";
    }
    
    /**
     * Add class-level Allure markers based on annotations
     */
    private void addClassMarkersToAllure(Class<?> testClass) {
        ApiTestMarkers.ApiTest apiTest = testClass.getAnnotation(ApiTestMarkers.ApiTest.class);
        if (apiTest != null) {
            Allure.label("layer", "api");
        }
        
        ApiTestMarkers.ApiFeature apiFeature = testClass.getAnnotation(ApiTestMarkers.ApiFeature.class);
        if (apiFeature != null) {
            Allure.feature("API: " + apiFeature.value());
        }
        
        if (testClass.isAnnotationPresent(ApiTestMarkers.PerformanceTest.class)) {
            Allure.label("testType", "performance");
        }
        
        if (testClass.isAnnotationPresent(ApiTestMarkers.RegressionTest.class)) {
            Allure.label("testType", "regression");
        }
        
        if (testClass.isAnnotationPresent(ApiTestMarkers.SmokeTest.class)) {
            Allure.label("testType", "smoke");
        }
    }
    
    /**
     * Add method-level Allure markers based on annotations
     */
    private void addMethodMarkersToAllure(Method method) {
        ApiTestMarkers.Endpoint endpoint = method.getAnnotation(ApiTestMarkers.Endpoint.class);
        if (endpoint != null) {
            Allure.label("endpoint", endpoint.value());
        }
        
        ApiTestMarkers.Method httpMethod = method.getAnnotation(ApiTestMarkers.Method.class);
        if (httpMethod != null) {
            Allure.label("method", httpMethod.value());
        }
        
        ApiTestMarkers.ResponseTime responseTime = method.getAnnotation(ApiTestMarkers.ResponseTime.class);
        if (responseTime != null) {
            Allure.label("responseTime", String.valueOf(responseTime.maxMs()));
        }
        
        ApiTestMarkers.StatusCode statusCode = method.getAnnotation(ApiTestMarkers.StatusCode.class);
        if (statusCode != null) {
            Allure.label("statusCode", String.valueOf(statusCode.value()));
        }
        
        if (method.isAnnotationPresent(ApiTestMarkers.AuthenticationTest.class)) {
            Allure.label("testType", "authentication");
        }
        
        if (method.isAnnotationPresent(ApiTestMarkers.ErrorHandlingTest.class)) {
            Allure.label("testType", "errorHandling");
        }
        
        if (method.isAnnotationPresent(ApiTestMarkers.InputValidationTest.class)) {
            Allure.label("testType", "inputValidation");
        }
        
        if (method.isAnnotationPresent(ApiTestMarkers.BusinessLogicTest.class)) {
            Allure.label("testType", "businessLogic");
        }
    }
}
