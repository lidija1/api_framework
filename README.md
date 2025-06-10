# API Testing Framework

A robust API testing framework built with RestAssured, TestNG, and Java.

## Features

- **Environment Configuration**: Support for different environments (dev, test, prod)
- **Parallel Test Execution**: Run tests in parallel for faster execution
- **Allure Reporting**: Detailed test reports with screenshots and logs
- **Centralized API Configuration**: Single point of control for API endpoints and settings
- **CI/CD Integration**: GitHub Actions workflow included
- **Test Data Generation**: Factory pattern for creating test data

## Getting Started

### Prerequisites

- Java 21
- Maven

### Installation

1. Clone the repository
2. Install dependencies:

```bash
mvn clean install
```

### Running Tests

Run all tests:

```bash
mvn test
```

Run tests for a specific environment:

```bash
mvn test -Denv=test
```

Run specific test:

```bash
mvn test -Dtest="examples.UserApiTest#testGetUserById"
```

Generate Allure report:

```bash
mvn allure:report
```

## Project Structure

```
├── src
│   ├── main
│   │   ├── java
│   │   │   ├── api                  # API configuration
│   │   │   ├── models               # POJO models for API requests/responses
│   │   │   └── utils                # Utility classes
│   │   └── resources
│   │       └── env                  # Environment-specific properties
│   └── test
│       ├── java
│       │   ├── data                 # Test data factories
│       │   ├── examples             # Example test classes
│       │   └── utils                # Test utilities
│       └── resources
│           └── testng.xml           # TestNG configuration
├── .github
│   └── workflows                    # CI/CD workflows
└── pom.xml                          # Maven configuration
```

## Key Components

### ApiConfig

Centralizes all API configuration:

```java
public class ApiConfig {
    // Base URI loaded from environment properties
    public static final String BASE_URI = properties.getProperty("api.base.url");
    
    // Loads configuration based on environment
    private static Properties loadProperties() {
        String env = System.getProperty("env", "dev");
        // ...
    }
    
    // Get pre-configured request specification
    public static RequestSpecification getRequestSpec() {
        return RestAssured.given()
                .header(API_KEY_HEADER, API_KEY);
    }
}
```

### Test Data Factories

Generate test data using the Builder pattern:

```java
public class UserTestDataFactory {
    // Create a valid user with random data
    public static User createValidUser() {
        return User.builder()
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .email(faker.internet().emailAddress())
                .job(faker.job().position())
                .build();
    }
}
```

## Extending the Framework

### Adding New Tests

1. Create a new test class extending `BaseTest`
2. Use `ApiConfig` for endpoint management
3. Use test data factories for creating test data
4. Add assertions using RestAssured's fluent API

### Adding New Endpoints

Update the `ApiConfig` class with new endpoint constants and utility methods.

### Adding New Environments

Create a new properties file in `src/main/resources/env/` and add a Maven profile in `pom.xml`. 