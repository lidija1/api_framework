# API Testing Framework

A robust and scalable automated API testing framework built with Java, REST Assured, and JSON-based test data management.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation](#installation)
- [Configuration](#configuration)
- [Test Data Management](#test-data-management)
- [Writing Tests](#writing-tests)
- [Running Tests](#running-tests)
- [Reporting](#reporting)
- [Retry Mechanism](#retry-mechanism)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Enhanced Schema Validation](#enhanced-schema-validation)
- [Performance Monitoring](#performance-monitoring)
- [Test Parameterization](#test-parameterization)

## Overview

This framework provides a comprehensive solution for API testing automation with the following key components:

- **Java**: Core programming language for test development
- **REST Assured**: Powerful library for API testing and validation
- **TestNG/JUnit**: Test execution and management
- **JSON**: Flexible test data management and configuration
- **Maven/Gradle**: Build automation and dependency management
- **Allure/ExtentReports**: Enhanced test reporting

## Features

✅ **Multi-environment Support** - Test against different environments (dev, staging, prod)  
✅ **JSON-based Test Data** - Externalized test data management  
✅ **Request/Response Validation** - Comprehensive API contract testing  
✅ **Authentication Support** - OAuth, JWT, Basic Auth, and API Key authentication  
✅ **Parallel Execution** - Multi-threaded test execution for faster feedback  
✅ **Detailed Reporting** - Rich HTML reports with request/response logs  
✅ **CI/CD Integration** - Jenkins, GitHub Actions, GitLab CI ready  
✅ **Database Validation** - Optional database verification capabilities  
✅ **Smart Retry Mechanism** - Enhanced retry capabilities with multiple backoff strategies and statistics  
✅ **Enhanced Schema Validation** - Multi-version schema support with evolution tracking and automatic generation

## Prerequisites

- **Java 11+** - JDK installation required
- **Maven 3.6+** or **Gradle 6+** - Build tool
- **IDE** - IntelliJ IDEA, Eclipse, or VS Code recommended
- **Git** - Version control

## Project Structure

```
api-testing-framework/
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── config/
│   │       │   ├── ConfigManager.java
│   │       │   └── TestConfig.java
│   │       ├── utils/
│   │       │   ├── APIClient.java
│   │       │   ├── DataProvider.java
│   │       │   ├── RetryHandler.java
│   │       │   ├── RetryConfig.java
│   │       │   ├── BackoffStrategy.java
│   │       │   ├── RetryStatistics.java
│   │       │   └── JsonUtils.java
│   │       └── models/
│   │           ├── User.java
│   │           └── Product.java
│   └── test/
│       ├── java/
│       │   ├── tests/
│       │   │   ├── UserAPITests.java
│       │   │   └── ProductAPITests.java
│       │   └── base/
│       │       └── BaseTest.java
│       └── resources/
│           ├── testdata/
│           │   ├── users.json
│           │   └── products.json
│           ├── config/
│           │   ├── dev.properties
│           │   ├── staging.properties
│           │   └── prod.properties
│           └── schemas/
│               ├── user-schema.json
│               └── product-schema.json
├── pom.xml
├── testng.xml
└── README.md
```

## Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/api-testing-framework.git
   cd api-testing-framework
   ```

2. **Install dependencies:**
   ```bash
   # For Maven
   mvn clean install
   
   # For Gradle
   gradle build
   ```

3. **Configure environment:**
   ```bash
   cp src/test/resources/config/dev.properties.example src/test/resources/config/dev.properties
   # Update configuration values as needed
   ```

## Demo API Setup

For demonstration and testing purposes, this framework is pre-configured to work with **Reqres.in** - a free REST API service that doesn't require authentication or setup.

### Using Reqres.in for Insurance Testing

The framework treats Reqres.in endpoints as insurance-related services:

| Reqres Endpoint | Insurance Context | Purpose |
|-----------------|-------------------|---------|
| `/api/users` | `/api/customers` | Customer management |
| `/api/users/{id}` | `/api/customers/{id}` | Individual customer operations |
| `POST /api/users` | `POST /api/customers` | New customer registration |
| `PUT /api/users/{id}` | `PUT /api/customers/{id}` | Update customer information |
| `DELETE /api/users/{id}` | `DELETE /api/customers/{id}` | Remove customer record |
| `POST /api/login` | `POST /api/agent/login` | Agent/customer authentication |

### Quick Start with Demo API

1. **No API setup required** - Reqres.in is always available
2. **Update configuration:**
   ```properties
   # src/test/resources/config/dev.properties
   base.url=https://reqres.in/api
   api.version=
   timeout.connection=30000
   timeout.read=60000
   ```

3. **Run sample tests:**
   ```bash
   mvn test -Dtest=CustomerAPITests
   ```

## Configuration

### Environment Configuration

Update the properties file for your target environment:

```properties
# src/test/resources/config/dev.properties
base.url=https://api.dev.example.com
api.version=v1
timeout.connection=30000
timeout.read=60000
auth.token=your-api-token
database.url=jdbc:mysql://localhost:3306/testdb
```

### Maven Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.3.2</version>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>7.8.0</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-testng</artifactId>
        <version>2.24.0</version>
    </dependency>
</dependencies>
```

## Test Data Management

### Test Data Structure

```json
{
  "customers": [
    {
      "id": 1,
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@example.com",
      "policy_number": "POL-1234-5678",
      "status": "active"
    }
  ]
}
```

## Reporting

### Allure Reports

The framework uses Allure for comprehensive test reporting with enhanced features:

```bash
# Generate Allure report
mvn allure:report

# Serve report locally
mvn allure:serve
```

### Enhanced Reporting Features

The framework includes several reporting enhancements:

#### 1. Custom Allure Annotations

Use custom annotations to better categorize tests in Allure reports:

```java
// Mark a class as testing API features
@ApiTestMarkers.ApiTest
@ApiTestMarkers.ApiFeature("Customer Management")

// Add specific test categorization
@ApiTestMarkers.Endpoint("/api/customers")
@ApiTestMarkers.Method("GET")
@ApiTestMarkers.StatusCode(200)
@ApiTestMarkers.ResponseTime(maxMs = 1000)

// Categorize by test type
@ApiTestMarkers.PerformanceTest
@ApiTestMarkers.RegressionTest
@ApiTestMarkers.SmokeTest
@ApiTestMarkers.AuthenticationTest
@ApiTestMarkers.ErrorHandlingTest
@ApiTestMarkers.InputValidationTest
@ApiTestMarkers.BusinessLogicTest
```

These annotations help organize your tests in Allure reports and make it easier to analyze test results by:
- API endpoints
- HTTP methods
- Expected status codes
- Performance requirements
- Test types

#### 2. Performance Metrics Collection

The framework automatically collects detailed performance metrics for API calls:

- Response times by endpoint and method
- Percentile statistics (p50, p75, p90, p95, p99)
- Success/failure rates
- Endpoint-specific performance metrics
- Identification of slow API calls

Performance reports are automatically attached to Allure reports and include:
- Overall performance metrics
- Endpoint-specific metrics
- Slowest API calls list

#### 3. Enhanced Request/Response Logging

All requests and responses are comprehensively logged:

- Detailed request information (headers, body, query parameters)
- Response details (status code, headers, body)
- Performance metrics for each request
- Automatic attachment of failed request/response details to Allure reports

For failed tests, detailed request and response information is automatically captured and attached to the Allure report, making it easier to diagnose issues.

### Using Enhanced Reporting

The enhanced reporting features are automatically applied when you extend `BaseTest`. For manual control:

```java
// Record custom performance metrics
PerformanceMetrics.getInstance().recordMetrics(endpoint, method, statusCode, responseTime);

// Generate performance report
PerformanceMetrics.getInstance().generatePerformanceReport();

// Record request/response details
RequestResponseLogger.getInstance().recordRequestResponse(method, url, requestBody, response, duration);
```

### Custom Logging

```java
@Test
public void testWithLogging() {
    Response response = given()
        .log().all()  // Log request details
        .when()
        .get("/users")
        .then()
        .log().all()  // Log response details
        .extract().response();
}
```

## Retry Mechanism

The framework includes a sophisticated retry mechanism designed to handle transient API failures and network issues. The retry system provides multiple backoff strategies, conditional retries based on error types, and comprehensive statistics collection.

### Key Retry Features

- **Multiple Backoff Strategies** - Choose from various delay calculation algorithms:
  - Fixed delay
  - Linear backoff
  - Exponential backoff
  - Fibonacci backoff
  - Exponential with jitter (full/equal)
  - Decorrelated jitter

- **Conditional Retries** - Configure retry behavior based on:
  - HTTP status codes
  - Error types
  - Exception classes

- **Comprehensive Statistics** - Collect and analyze retry metrics:
  - Total requests and retries
  - Success/failure rates
  - Retry distribution by error code
  - Detailed retry history per request

### Retry Configuration

Configure the retry behavior in your properties file:

```properties
# src/test/resources/config/dev.properties
connection.retry.max_retries=3
connection.retry.initial_delay_ms=1000
connection.retry.backoff_strategy=EXPONENTIAL_WITH_EQUAL_JITTER
connection.retry.max_delay_ms=60000
```

Or programmatically using the builder pattern:

```java
RetryConfig config = RetryConfig.builder()
    .maxRetries(3)
    .initialDelayMs(1000)
    .backoffStrategy(BackoffStrategy.EXPONENTIAL_WITH_EQUAL_JITTER)
    .addRetryableStatusCodes(429, 503, 504)
    .addRetryableErrorCodes(ErrorCode.CONNECTION_ERROR, ErrorCode.NETWORK_TIMEOUT)
    .errorSpecificBackoff(ErrorCode.NETWORK_TIMEOUT, BackoffStrategy.EXPONENTIAL)
    .build();
```

### Using the Retry Handler

```java
// With default configuration
RetryHandler retryHandler = new RetryHandler();
Response response = retryHandler.executeWithRetry(() -> {
    // Your API call here
    return RestAssured.given()
        .spec(requestSpec)
        .get("/api/resource")
        .then()
        .extract()
        .response();
});

// With custom configuration
RetryHandler retryHandler = new RetryHandler(customConfig);
Response response = retryHandler.executeWithRetry(() -> {
    // Your API call here
}, customConfig);

// Access retry statistics
Map<String, Object> stats = retryHandler.getStatistics().getAggregateStatistics();
```

### Sample Retry Statistics Output

```
=== Retry Statistics ===
totalRequests: 100
requestsWithRetries: 15
retryPercentage: 15.0
totalRetryAttempts: 28
successfulRetries: 13
failedRetries: 2
retrySuccessRate: 86.67
retryByStatusCode: {429=10, 503=15, 504=3}
retryByErrorCode: {NETWORK_TIMEOUT=5, CONNECTION_ERROR=3, CLIENT_THROTTLED=10}
retryByStrategy: {RETRY_WITH_BACKOFF=25, RETRY=3}
```

## Best Practices

### Test Organization
- Group related tests in separate classes
- Use meaningful test names that describe the scenario
- Implement proper setup and teardown methods
- Use data providers for parameterized tests

### Data Management
- Keep test data separate from test logic
- Use environment-specific configuration files
- Implement data cleanup after test execution
- Version control your test data files

### Error Handling
- Implement proper exception handling
- Use soft assertions where appropriate
- Add meaningful assertion messages
- Log important test steps and validations

### Performance
- Use parallel execution for independent tests
- Implement connection pooling for database tests
- Set appropriate timeouts for API calls
- Monitor test execution times

## Troubleshooting

### Common Issues

**Connection Timeouts:**
```java
RestAssured.config = RestAssured.config()
    .httpClient(HttpClientConfig.httpClientConfig()
        .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000)
        .setParam(CoreConnectionPNames.SO_TIMEOUT, 60000));
```

**SSL Certificate Issues:**
```java
RestAssured.useRelaxedHTTPSValidation();
```

**JSON Parsing Errors:**
```java
// Validate JSON structure before parsing
if (response.jsonPath().get("data") != null) {
    List<User> users = response.jsonPath().getList("data", User.class);
}
```

### Debug Mode

Enable detailed logging for troubleshooting:

```properties
# logback.xml
<logger name="io.restassured" level="DEBUG"/>
<logger name="org.apache.http" level="DEBUG"/>
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow Java naming conventions
- Write comprehensive JavaDoc comments
- Maintain test coverage above 80%
- Run checkstyle and spotbugs before submitting

## Schema Validation

The framework includes a comprehensive schema validation system that supports multiple schema versions, schema generation from responses, and schema evolution tracking.

### Key Schema Validation Features

#### 1. Multiple Schema Version Support

The framework can maintain multiple versions of JSON schemas, allowing for:

- Validation against specific schema versions
- Automatic detection of compatible schema versions
- Backward compatibility checks between versions

```java
// Validate against a specific version
boolean isValid = VersionedSchemaValidator.validateResponse(response, "customer", 2);

// Validate against the latest version
boolean isValid = VersionedSchemaValidator.validateResponseAgainstLatestSchema(response, "customer");

// Find the most compatible version
boolean isCompatible = VersionedSchemaValidator.validateResponseAgainstCompatibleSchema(response, "customer");
```

#### 2. Schema Generation from Responses

Automatically generate JSON schemas from API responses:

```java
// Generate a schema from a single response
int version = VersionedSchemaValidator.generateSchema(
    response, 
    "customer", 
    "Customer Schema", 
    "JSON Schema for the Customer API"
);

// Generate a more accurate schema from multiple response examples
int version = VersionedSchemaValidator.generateSchemaFromResponses(
    responsesList,
    "customer",
    "Customer Schema",
    "JSON Schema for the Customer API"
);
```

#### 3. Schema Difference Detection

Compare schema versions and identify breaking changes:

```java
// Compare two schema versions
String diffReport = SchemaVersionManager.compareSchemaVersions("customer", 1, 2);

// Check if a new version is backward compatible
boolean isCompatible = SchemaVersionManager.isBackwardCompatible("customer", 1, 2);

// Generate a compatibility report for all versions
String report = SchemaVersionManager.generateVersionCompatibilityReport("customer");
```

#### 4. Auto-validation with Schema Generation

Validate against existing schemas or generate new ones as needed:

```java
// Try to validate, generating a new schema if needed
int version = VersionedSchemaValidator.validateOrGenerateSchema(
    response,
    "customer",
    "Customer Schema",
    "JSON Schema for the Customer API"
);
```

#### 5. Detailed Validation Error Reporting

Get detailed validation errors for debugging:

```java
List<String> errors = VersionedSchemaValidator.getValidationErrors(response, "customer", 1);
errors.forEach(error -> logger.info(error));
```

### Schema Versioning Conventions

The framework uses a simple versioning convention for schema files:

- Schema files are stored in `src/test/resources/schemas/`
- Files are named with the pattern `{schemaName}_v{version}.json`
- For example: `customer_v1.json`, `customer_v2.json`, etc.

### Schema Report Generation

The framework automatically generates schema-related reports:

- Schema difference reports between versions
- Compatibility reports for schema evolution
- Summary reports of all available schemas
- Reports of schema versions used in test runs

These reports are automatically attached to Allure test reports for easy access.

## Enhanced Schema Validation

The framework includes advanced schema validation capabilities that extend beyond basic JSON Schema validation, providing support for multiple schema versions, automatic schema generation, and schema evolution tracking.

### Multiple Schema Versions

Support for multiple versions of JSON schemas enables more robust API testing:

```java
// Validate against a specific schema version
boolean isValid = VersionedSchemaValidator.validateResponse(response, "customer", 2);

// Validate against the latest schema version
boolean isValid = VersionedSchemaValidator.validateResponseAgainstLatestSchema(response, "customer");

// Find the highest compatible schema version for a response
boolean isCompatible = VersionedSchemaValidator.validateResponseAgainstCompatibleSchema(response, "customer");
```

### Automatic Schema Generation

Automatically generate JSON schemas from API responses to streamline testing:

```java
// Generate a schema from a single response
int version = VersionedSchemaValidator.generateSchema(
    response, 
    "customer", 
    "Customer Schema", 
    "JSON Schema for the Customer API"
);

// Generate a more accurate schema from multiple response examples
int version = VersionedSchemaValidator.generateSchemaFromResponses(
    responsesList,
    "customer",
    "Customer Schema",
    "JSON Schema for the Customer API"
);

// Auto-validate or generate as needed
int version = VersionedSchemaValidator.validateOrGenerateSchema(
    response,
    "customer",
    "Customer Schema",
    "JSON Schema for Customer API"
);
```

### Schema Difference Detection

Track schema evolution and identify breaking changes:

```java
// Compare two schema versions
String diffReport = SchemaVersionManager.compareSchemaVersions("customer", 1, 2);

// Check if a new version is backward compatible
boolean isCompatible = SchemaVersionManager.isBackwardCompatible("customer", 1, 2);

// Generate a compatibility report for all schema versions
String report = SchemaVersionManager.generateVersionCompatibilityReport("customer");
```

### Detailed Validation Errors

Get detailed information about validation failures:

```java
// Get specific validation error messages
List<String> errors = VersionedSchemaValidator.getValidationErrors(response, "customer", 1);
errors.forEach(error -> logger.info(error));
```

### Schema Reporting

Generate comprehensive reports for schema management:

```java
// List all available schemas and versions
String schemaSummary = SchemaVersionManager.generateSchemaSummaryReport();

// Report which schema versions were used in tests
String usageReport = VersionedSchemaValidator.generateUsedSchemasReport();
```

All schema-related reports are automatically attached to Allure test reports for easy reference.

## Performance Monitoring

The framework includes comprehensive performance monitoring capabilities to track API response times, set performance thresholds, and detect performance regressions.

### Response Time Tracking

The framework automatically tracks response times for all API calls:

```java
// Record metrics for a response
metrics.recordResponseMetrics(response, "/users/{id}", "GET");

// Get overall performance metrics
Map<String, Object> overallMetrics = metrics.getOverallMetrics();

// Get metrics for a specific endpoint
EndpointMetrics endpointMetrics = metrics.getEndpointMetrics("/users/{id}");
```

### Performance Thresholds

You can define performance thresholds globally or per endpoint/method:

```java
// Define thresholds in performance/thresholds.json or config.json
{
  "global": {
    "p50": 500,
    "p90": 1000,
    "p95": 1500,
    "p99": 3000,
    "max": 5000
  },
  "endpoints": {
    "/users/{id}": {
      "GET": {
        "p50": 300,
        "p90": 600,
        "p95": 900,
        "p99": 1500,
        "max": 2000
      }
    }
  }
}

// Compare metrics against thresholds
PerformanceThresholds.ThresholdConfig threshold = thresholds.getThreshold("/users/{id}", "GET");
if (avgResponseTime > threshold.getP90()) {
    logger.warn("Response time exceeds threshold!");
}
```

### Performance Regression Detection

The framework can detect performance regressions by comparing current metrics with baseline performance data:

```java
// Generate a baseline from current metrics
regressionDetector.generateBaseline();

// Compare current metrics with baseline
List<RegressionIssue> regressions = regressionDetector.detectRegressions();

// Generate regression and threshold reports
String regressionReport = regressionDetector.generateRegressionReport();
String thresholdReport = regressionDetector.generateThresholdReport();
```

### Performance Reports

The framework automatically generates performance reports with detailed metrics:

- Overall response time statistics
- Percentile calculations (p50, p90, p95, p99)
- Endpoint-specific metrics
- Method-specific metrics
- Lists of the slowest API calls
- Regression analysis reports
- Threshold violation reports

All performance reports are automatically attached to Allure test reports for easy analysis.

## Test Parameterization

The framework provides comprehensive support for parameterized testing, enabling data-driven tests, test matrices, and dynamic data generation.

### Data-Driven Testing

Create parameterized tests using various data sources:

```java
// 1. Simple data provider with static data
@DataProvider(name = "userScenarios")
public Object[][] provideUserScenarios() {
    return new Object[][] {
        { "John", "Developer", 200 },
        { "Mary", "QA", 200 },
        { "", "Developer", 400 }
    };
}

@Test(dataProvider = "userScenarios")
public void testUserCreation(String name, String job, int expectedStatus) {
    // Test implementation
}

// 2. JSON file-based test data using annotation
@Test(dataProvider = "jsonDataProvider", dataProviderClass = DynamicDataProvider.class)
@TestData(dataFile = "users.json", jsonPath = "test_cases")
public void testWithJsonData(Map<String, Object> userData) {
    // Test implementation
}
```

### Test Matrices

Generate exhaustive combinations of test parameters:

```java
// Create a test matrix from parameter combinations
@DataProvider(name = "userQueryMatrix")
public Object[][] provideUserQueryMatrix() {
    Map<String, Object[]> parameters = new HashMap<>();
    parameters.put("page", new Integer[] {1, 2});
    parameters.put("per_page", new Integer[] {3, 6});
    parameters.put("delay", new Integer[] {0, 1});
    
    return DynamicDataProvider.provideTestMatrix(parameters);
}

@Test(dataProvider = "userQueryMatrix")
public void testUserListingMatrix(Map<String, Object> queryParams) {
    // Test implementation using all combinations of parameters
}
```

### Dynamic Data Generation

Generate test data dynamically using various methods:

```java
// 1. Generate test data using templates
Map<String, Object> template = new HashMap<>();
template.put("name", "{{name}}");
template.put("job", "{{word}}");
template.put("email", "{{email}}");

Map<String, Object> userData = TestDataGenerator.generateApiRequestBody(template);

// 2. Generate test data programmatically
Map<String, Object> customer = TestDataGenerator.generateInsuranceCustomer();

// 3. Generate randomized test data for specific fields
user.put("name", RandomDataGenerator.generateName());
user.put("email", RandomDataGenerator.generateEmail());
user.put("phone", RandomDataGenerator.generatePhoneNumber());
```

### Structured Test Cases

Use a consistent format for test cases with input and expected sections:

```json
{
  "testData": {
    "TC_ID_0001": {
      "description": "Successful user login",
      "category": "Authentication",
      "tags": ["positive", "smoke"],
      "input": {
        "email": "user@example.com",
        "password": "password123"
      },
      "expected": {
        "statusCode": 200,
        "hasToken": true,
        "responseTime": 1000
      }
    }
  }
}
```

```java
@Test(dataProvider = "testCaseProvider", dataProviderClass = DynamicDataProvider.class)
@TestData(dataFile = "test_suite.json", testCaseIds = {"TC_ID_0001", "TC_ID_0002"})
public void testWithStructuredData(Map<String, Object> testCase) {
    // Extract test data
    Map<String, Object> input = (Map<String, Object>) testCase.get("input");
    Map<String, Object> expected = (Map<String, Object>) testCase.get("expected");
    
    // Test implementation
}
```

### Random Sampling

Select a random subset of test cases from a larger dataset:

```java
// Sample 5 random test cases from a larger dataset
@Test(dataProvider = "randomSampleProvider", dataProviderClass = DynamicDataProvider.class)
public void testWithRandomSample(Map<String, Object> testCase) {
    // Test implementation
}

// In setup or data provider configuration:
DynamicDataProvider.provideRandomSample("large_dataset.json", "test_cases", 5);
```

All test data and test matrices are automatically documented in Allure reports for clear test case traceability.

---
