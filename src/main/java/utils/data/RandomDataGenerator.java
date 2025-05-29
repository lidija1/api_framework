package utils.data;

import com.github.javafaker.Faker;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import io.qameta.allure.Step;

/**
 * Utility class for generating random test data using JavaFaker.
 * Provides methods to generate various types of test data such as names, emails,
 * phone numbers, and other common data types needed for testing.
 */
public class RandomDataGenerator {
    private static Faker faker = new Faker(Locale.forLanguageTag("en-US"));
    
    /**
     * Reinitialize faker with a specific locale.
     * Useful for generating locale-specific test data.
     *
     * @param locale The locale to use (e.g., "fr", "es", "de")
     */
    public static void setLocale(String locale) {
        faker = new Faker(Locale.forLanguageTag(locale));
    }

    /**
     * Generates a random email address using realistic patterns.
     * 
     * @return A randomly generated email address
     */
    public static String generateEmail() {
        return faker.internet().emailAddress();
    }

    /**
     * Generates a random string of specified length.
     * Uses Lorem text to generate more realistic looking strings.
     * 
     * @param length The desired length of the string
     * @return A random string of the specified length
     */
    public static String generateString(int length) {
        return faker.lorem().characters(length, false);
    }

    /**
     * Generates a random integer within specified range.
     * 
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @return A random integer between min and max
     */
    public static int generateNumber(int min, int max) {
        return faker.number().numberBetween(min, max);
    }

    /**
     * Generates a random UUID.
     * Uses Java's UUID class as it's the standard way to generate UUIDs.
     * 
     * @return A random UUID as string
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a random phone number in a realistic format.
     * Uses Faker's phone number generator which provides locale-aware formatting.
     * 
     * @return A formatted phone number string
     */
    public static String generatePhoneNumber() {
        return faker.phoneNumber().cellPhone();
    }

    /**
     * Generates a random price within specified range with 2 decimal places.
     * 
     * @param min The minimum price value (inclusive)
     * @param max The maximum price value (inclusive)
     * @return A random price between min and max with 2 decimal places
     */
    public static double generatePrice(double min, double max) {
        double randomPrice = faker.number().randomDouble(2, (long)min, (long)max);
        // Ensure exactly 2 decimal places
        return new BigDecimal(randomPrice)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Generates a random boolean value.
     * 
     * @return A random boolean value
     */
    public static boolean generateBoolean() {
        return faker.random().nextBoolean();
    }

    /**
     * Generates a random full name using realistic name patterns.
     * 
     * @return A randomly generated full name
     */
    public static String generateName() {
        return faker.name().fullName();
    }

    /**
     * Generates a random username without special characters.
     * 
     * @return A random username
     */
    public static String generateUsername() {
        return faker.name().username();
    }

    /**
     * Generates a random company name.
     * 
     * @return A random company name
     */
    public static String generateCompanyName() {
        return faker.company().name();
    }

    /**
     * Generates a random street address.
     * 
     * @return A random street address
     */
    public static String generateAddress() {
        return faker.address().streetAddress();
    }

    /**
     * Generates a random date in ISO format within a range of days from now.
     *
     * @param minDays Minimum number of days from now
     * @param maxDays Maximum number of days from now
     * @return ISO formatted date string
     */
    @Step("Generating random date between {minDays} and {maxDays} days from now")
    public static String generateDate(int minDays, int maxDays) {
        return faker.date()
                .between(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(minDays)),
                        java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(maxDays)))
                .toInstant()
                .toString();
    }

    /**
     * Generates a random password meeting common complexity requirements.
     *
     * @param minLength Minimum length of password
     * @return A password string with mixed case, numbers and special characters
     */
    @Step("Generating random password with minimum length {minLength}")
    public static String generatePassword(int minLength) {
        String special = "!@#$%^&*";
        StringBuilder password = new StringBuilder(faker.internet().password(minLength, minLength + 4));
        // Ensure at least one number
        password.setCharAt(faker.number().numberBetween(0, password.length()),
                String.valueOf(faker.number().numberBetween(0, 9)).charAt(0));
        // Ensure at least one special character
        password.setCharAt(faker.number().numberBetween(0, password.length()),
                special.charAt(faker.number().numberBetween(0, special.length())));
        return password.toString();
    }

    /**
     * Generates a random URL.
     *
     * @param protocol Optional protocol (http/https), defaults to https if null
     * @return A random URL string
     */
    @Step("Generating random URL")
    public static String generateUrl(String protocol) {
        String actualProtocol = protocol != null ? protocol : "https";
        return String.format("%s://%s/%s",
                actualProtocol,
                faker.internet().domainName(),
                faker.internet().slug());
    }

    /**
     * Generates a random social security number (XXX-XX-XXXX format).
     *
     * @return A formatted SSN string
     */
    @Step("Generating random SSN")
    public static String generateSSN() {
        return faker.idNumber().ssnValid();
    }

    /**
     * Generates a random credit card number.
     *
     * @return A valid format credit card number
     */
    @Step("Generating random credit card number")
    public static String generateCreditCardNumber() {
        return faker.finance().creditCard();
    }

    /**
     * Generates a random IP address (v4).
     *
     * @return An IPv4 address string
     */
    @Step("Generating random IP address")
    public static String generateIpAddress() {
        return faker.internet().ipV4Address();
    }

    /**
     * Generates random JSON data as a string.
     *
     * @param depth Maximum depth of nested objects (1-3 recommended)
     * @return A JSON string with random data
     */
    @Step("Generating random JSON with depth {depth}")
    public static String generateJson(int depth) {
        if (depth <= 0) {
            return String.format("{\"%s\": \"%s\"}", 
                faker.lorem().word(), 
                faker.lorem().sentence());
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        int fields = faker.number().numberBetween(2, 5);
        for (int i = 0; i < fields; i++) {
            if (i > 0) json.append(",");
            String key = faker.lorem().word();
            if (faker.random().nextBoolean() && depth > 1) {
                json.append(String.format("\"%s\": %s", key, generateJson(depth - 1)));
            } else {
                json.append(String.format("\"%s\": \"%s\"", key, faker.lorem().sentence()));
            }
        }
        json.append("}");
        return json.toString();
    }
}
