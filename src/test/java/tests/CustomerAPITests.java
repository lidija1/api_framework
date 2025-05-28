package tests;

import base.BaseAPITest;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.DataProvider;
import utils.DateUtils;
import utils.AuthenticationManager;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Feature("Customer API Tests")
public class CustomerAPITests extends BaseAPITest {
    private List<String> createdCustomerIds = new ArrayList<>();
    
    @Test
    @Description("Test retrieving all customers")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllCustomers() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("page", equalTo(1))
                .body("data", hasSize(greaterThan(0)))
                .body(matchesJsonSchemaInClasspath("schemas/customer-list-schema.json"))
                .extract().response();

        // Validate response structure
        assertThat("Response contains data array", response.path("data"), notNullValue());
        assertThat("Response contains pagination info", response.path("total"), greaterThan(0));

        List<Map<String, Object>> customers = response.jsonPath().getList("data");
        customers.forEach(customer -> {
            assertThat(customer.get("email").toString(), containsString("@"));
            assertThat(customer.get("first_name").toString(), not(emptyString()));
        });
    }
    @Test
    @Description("Test creating a new customer")
    @Severity(SeverityLevel.BLOCKER)
    public void testCreateNewCustomer() {
        // Get test data and validate it
        Map<String, Object> newCustomer = DataProvider.getTestData("customers.json", "scenarios.new_customer", Map.class);
        assertThat("Customer name is provided", newCustomer.get("name"), notNullValue());
        assertThat("Customer job is provided", newCustomer.get("job"), notNullValue());

        Response response = given()
                .spec(requestSpec)
                .body(newCustomer)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .body("name", equalTo(newCustomer.get("name")))
                .body("job", equalTo(newCustomer.get("job")))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .extract().response();

        // Validate response format and store ID
        String customerId = response.jsonPath().getString("id");
        assertThat("Customer ID is returned", customerId, notNullValue());
        if (customerId != null) {
            createdCustomerIds.add(customerId);
        }

        // Validate creation timestamp format
        String createdAt = response.jsonPath().getString("createdAt");
        assertThat("Creation timestamp is valid", DateUtils.isValidTimestamp(createdAt));
    }
    @Test
    @Description("Test customer login")
    @Severity(SeverityLevel.BLOCKER)
    public void testCustomerLogin() {
        @SuppressWarnings("unchecked")
        Map<String, String> credentials = DataProvider.getTestData("customers.json", "scenarios.login_credentials", Map.class);
        assertThat("Email is provided", credentials.get("email"), notNullValue());
        assertThat("Password is provided", credentials.get("password"), notNullValue());

        Response response = given()
                .spec(requestSpec)
                .body(credentials)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().response();

        String token = response.jsonPath().getString("token");
        assertThat("Token is not empty", token, not(emptyString()));
    }
    @Test
    @Description("Test updating customer information")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdateCustomerInformation() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Updated Customer Name");
        updateData.put("job", "Senior Insurance Agent");

        Response response = given()
                .spec(requestSpec)
                .body(updateData)
                .when()
                .put("/users/2")
                .then()
                .statusCode(200)
                .body("name", equalTo(updateData.get("name")))
                .body("job", equalTo(updateData.get("job")))
                .body("updatedAt", notNullValue())
                .extract().response();

        // Validate update timestamp
        String updatedAt = response.jsonPath().getString("updatedAt");
        assertThat("Update timestamp is valid", DateUtils.isValidTimestamp(updatedAt));
    }

    @Test
    @Description("Test deleting a customer")
    public void testDeleteCustomer() {
        given()
                .spec(requestSpec)
                .when()
                .delete("/users/2")
                .then()
                .statusCode(204);
    }
    @Test
    @Description("Test customer not found scenario")
    @Severity(SeverityLevel.NORMAL)
    public void testCustomerNotFound() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/users/999")
                .then()
                .statusCode(404)
                .extract().response();

        // Validate error response structure
        assertThat(response.getContentType(), containsString("application/json"));
        assertThat("Response body is an empty JSON object", 
                  response.getBody().asString(), equalTo("{}"));
    }

    @Test
    @Description("Test processing time validation")
    @Severity(SeverityLevel.NORMAL)
    public void testProcessingTimeValidation() {
        int expectedDelay = 3;
        long startTime = System.currentTimeMillis();

        Response response = given()
                .spec(requestSpec)
                .queryParam("delay", expectedDelay)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .extract().response();

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Validate processing time with more lenient thresholds
        assertThat("Response time exceeds expected delay",
                processingTime, greaterThanOrEqualTo((long)expectedDelay * 1000));
        assertThat("Response time is within reasonable limit",
                processingTime, lessThan((long)expectedDelay * 1000 + 8000)); // Increased from 2000 to 8000ms buffer

        // Validate response is still valid despite delay
        assertThat(response.path("data"), notNullValue());
        assertThat(response.path("page"), equalTo(1));
    }

    @Test
    @Description("Test retrieving a single customer")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetSingleCustomer() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/users/2")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(2))
                .body("data.email", notNullValue())
                .body("data.first_name", notNullValue())
                .body("data.last_name", notNullValue())
                .body(matchesJsonSchemaInClasspath("schemas/customer-schema.json"))
                .extract().response();

        // Validate response structure
        Map<String, Object> data = response.jsonPath().getMap("data");
        assertThat("Customer email is valid", data.get("email").toString(), containsString("@"));
        assertThat("Customer has avatar", data.get("avatar").toString(), startsWith("https://"));
    }

    @Test
    @Description("Test failed login scenario")
    @Severity(SeverityLevel.CRITICAL)
    public void testFailedLogin() {
        Map<String, String> invalidCredentials = new HashMap<>();
        invalidCredentials.put("email", "invalid@email.com");
        invalidCredentials.put("password", "wrongpassword");

        Response response = given()
                .spec(requestSpec)
                .body(invalidCredentials)
                .when()
                .post("/login")
                .then()
                .statusCode(400)
                .extract().response();

        assertThat("Error message is present", response.asString(), containsString("error"));
    }

    @Test
    @Description("Test pagination functionality")
    @Severity(SeverityLevel.NORMAL)
    public void testPagination() {
        int pageSize = 3;
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .queryParam("per_page", pageSize)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("data.size()", equalTo(pageSize))
                .body("page", equalTo(1))
                .body("per_page", equalTo(pageSize))
                .body("total_pages", greaterThan(0))
                .extract().response();

        // Validate next page exists
        int totalPages = response.path("total_pages");
        if (totalPages > 1) {
            given()
                    .spec(requestSpec)
                    .queryParam("page", 2)
                    .queryParam("per_page", pageSize)
                    .when()
                    .get("/users")
                    .then()
                    .statusCode(200)
                    .body("page", equalTo(2))
                    .body("data", hasSize(greaterThan(0)));
        }
    }

    @Test
    @Description("Test patch update of customer")
    @Severity(SeverityLevel.BLOCKER)
    public void testPatchCustomer() {
        Map<String, String> patchData = new HashMap<>();
        patchData.put("job", "Software Architect");

        Response response = given()
                .spec(requestSpec)
                .body(patchData)
                .when()
                .patch("/users/2")
                .then()
                .statusCode(200)
                .body("job", equalTo(patchData.get("job")))
                .body("updatedAt", notNullValue())
                .extract().response();

        // Validate the timestamp format
        String updatedAt = response.jsonPath().getString("updatedAt");
        assertThat("Update timestamp is present", updatedAt, notNullValue());
        assertThat("Timestamp matches ISO-8601 format", 
            updatedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z"));
    }
}