package utils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;

public class DateUtils {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Get current UTC timestamp in ISO format
     */
    public static String getCurrentUtcTimestamp() {
        return ZonedDateTime.now(UTC)
                .format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    /**
     * Parse ISO timestamp string to LocalDateTime
     */
    public static LocalDateTime parseTimestamp(String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    /**
     * Format LocalDateTime to ISO timestamp string
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    /**
     * Convert Date to ISO timestamp string
     */
    public static String formatDate(Date date) {
        return date.toInstant()
                .atZone(UTC)
                .format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    /**
     * Check if a timestamp string is valid ISO format
     */
    public static boolean isValidTimestamp(String timestamp) {
        try {
            parseTimestamp(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate time difference in milliseconds
     */
    public static long getTimeDifferenceInMillis(String timestamp1, String timestamp2) {
        LocalDateTime dt1 = parseTimestamp(timestamp1);
        LocalDateTime dt2 = parseTimestamp(timestamp2);
        return java.time.Duration.between(dt1, dt2).toMillis();
    }
}
