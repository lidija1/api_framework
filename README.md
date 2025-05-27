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
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

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
      "policy_type": "auto",
      "premium": 1200.00
    },
    {
      "id": 2,
      "first_name": "Jane",
      "last_name": "Smith",
      "email": "jane.smith@example.com",
      "policy_type": "home",
      "premium": 800.00
    }
  ],
  "scenarios": {
    "new_customer": {
      "name": "Michael Johnson",
      "job": "Software Engineer",
      "email": "michael.j@insurance.com",
      "policy_type": "auto"
    },
    "invalid_customer": {
      "name": "",
      "job": "Engineer"
    },
    "login_credentials": {
      "email": "eve.holt@reqres.in",
      "password": "cityslicka"
    }
  }
}
```

### Data Provider Usage

```java
@DataProvider(name = "customerTestData")
public Object[][] getCustomerTestData() {
    return DataProvider.getTestData("customers.json", "customers");
}

@Test(dataProvider = "customerTestData")
public void testCreateCustomer(Customer customer) {
    // Test implementation
}
```

## Writing Tests

### Basic API Test Structure

```java
public class CustomerAPITests extends BaseTest {
    
    @Test
    public void testGetAllCustomers() {
        Response response = given()
            .when()
            .get("/users")  // Reqres.in endpoint
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("page", equalTo(1))
            .body("data", hasSize(6))
            .extract().response();
        
        // Treat response as customer data
        List<Customer> customers = response.jsonPath().getList("data", Customer.class);
        assertThat(customers).isNotEmpty();
        
        // Insurance-specific validations
        customers.forEach(customer -> {
            assertThat(customer.getEmail()).contains("@");
            assertThat(customer.getFirstName()).isNotEmpty();
        });
    }
    
    @Test
    public void testCreateNewCustomer() {
        Map<String, Object> newCustomer = DataProvider.getTestData("customers.json", "scenarios.new_customer");
        
        Response response = given()
            .contentType(ContentType.JSON)
            .body(newCustomer)
            .when()
            .post("/users")  // Create new customer
            .then()
            .statusCode(201)
            .body("name", equalTo(newCustomer.get("name")))
            .body("job", equalTo(newCustomer.get("job")))
            .body("id", notNullValue())
            .body("createdAt", notNullValue())
            .extract().response();
        
        // Verify customer creation timestamp
        String createdAt = response.jsonPath().getString("createdAt");
        assertThat(createdAt).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z");
    }
    
    @Test 
    public void testCustomerLogin() {
        // Simulate agent/customer portal login
        Map<String, String> credentials = DataProvider.getTestData("customers.json", "scenarios.login_credentials");
        
        Response response = given()
            .contentType(ContentType.JSON)
            .body(credentials)
            .when()
            .post("/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract().response();
        
        String token = response.jsonPath().getString("token");
        assertThat(token).isNotEmpty();
        
        // Store token for subsequent requests
        setAuthToken(token);
    }
    
    @Test
    public void testUpdateCustomerInformation() {
        Map<String, Object> updateData = Map.of(
            "name", "Updated Customer Name",
            "job", "Senior Insurance Agent"
        );
        
        given()
            .contentType(ContentType.JSON)
            .body(updateData)
            .when()
            .put("/users/2")  // Update customer ID 2
            .then()
            .statusCode(200)
            .body("name", equalTo("Updated Customer Name"))
            .body("job", equalTo("Senior Insurance Agent"))
            .body("updatedAt", notNullValue());
    }
    
    @Test
    public void testDeleteCustomer() {
        given()
            .when()
            .delete("/users/2")  // Remove customer
            .then()
            .statusCode(204);
    }
    
    @Test
    public void testCustomerNotFound() {
        given()
            .when()
            .get("/users/999")  // Non-existent customer
            .then()
            .statusCode(404);
    }
}
```

### Schema Validation

```java
@Test
public void testCustomerResponseSchema() {
    given()
        .when()
        .get("/users/1")  // Get customer details
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/customer-schema.json"));
}

@Test
public void testProcessingTimeValidation() {
    // Test insurance processing time simulation
    long startTime = System.currentTimeMillis();
    
    given()
        .queryParam("delay", 3)  // 3 second delay
        .when()
        .get("/users")
        .then()
        .statusCode(200);
    
    long endTime = System.currentTimeMillis();
    long processingTime = endTime - startTime;
    assertThat(processingTime).isGreaterThan(3000);  // At least 3 seconds
}
```

## Running Tests

### Command Line Execution

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerAPITests

# Run with specific environment
mvn test -Denvironment=staging

# Run tests in parallel
mvn test -DthreadCount=4

# Run with custom test suite
mvn test -DsuiteXmlFile=testng.xml

# Run demo tests with Reqres.in
mvn test -Dtest=CustomerAPITests#testGetAllCustomers
```

### TestNG Configuration

```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Insurance API Test Suite" parallel="classes" thread-count="3">
    <test name="Customer API Tests">
        <classes>
            <class name="tests.CustomerAPITests"/>
        </classes>
    </test>
    <test name="Policy API Tests">
        <classes>
            <class name="tests.PolicyAPITests"/>
        </classes>
    </test>
    <test name="Claims API Tests">
        <classes>
            <class name="tests.ClaimsAPITests"/>
        </classes>
    </test>
</suite>
```

## Reporting

### Allure Reports

```bash
# Generate Allure report
mvn allure:report

# Serve report locally
mvn allure:serve
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

---
