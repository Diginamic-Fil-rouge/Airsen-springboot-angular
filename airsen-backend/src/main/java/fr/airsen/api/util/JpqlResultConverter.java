package fr.airsen.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utility class for safely converting JPQL native query results to typed values.
 *
 * Handles the complexity of type conversion from JDBC driver results, which may return:
 * - Various numeric types (BigDecimal, Integer, Long, Double) depending on database and driver
 * - Nested Object[] arrays (MariaDB JDBC driver quirk for single-row results)
 * - Null values throughout the result set
 */
public final class JpqlResultConverter {

    private static final Logger log = LoggerFactory.getLogger(JpqlResultConverter.class);

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private JpqlResultConverter() {
        throw new AssertionError("Cannot instantiate utility class JpqlResultConverter");
    }

    public static final class UnwrapResult {
        public final Object[] data;
        public final boolean wasNested;

        UnwrapResult(Object[] data, boolean wasNested) {
            this.data = data;
            this.wasNested = wasNested;
        }
    }

    /**
     * Unwraps nested Object[] arrays that may be created by certain JDBC drivers.
     *
     * MariaDB JDBC driver has a known behavior where single-row native query results
     * are wrapped in an extra Object[] layer:
     * - Expected from query: Object[11] containing actual data
     * - Actual from driver: Object[1] containing Object[11]
     *
     * This method detects and unwraps this nesting transparently, making it safe to
     * apply to all query results regardless of driver or query complexity.
     *
     * @param row Object array from JPQL query result (may be nested)
     * @return UnwrapResult containing unwrapped data and whether nesting was detected
     * @throws IllegalArgumentException if row is null
     */
    public static UnwrapResult unwrapNestedArray(Object[] row) {
        if (row == null) {
            throw new IllegalArgumentException("Query result row cannot be null");
        }

        // Check if this is a nested array (JDBC driver wrapped it)
        // Condition: array with exactly one element that is itself an Object[]
        if (row.length == 1 && row[0] instanceof Object[]) {
            Object[] unwrappedRow = (Object[]) row[0];
            log.debug("Unwrapped nested Object[] from JDBC driver: original length={}, unwrapped length={}",
                    row.length, unwrappedRow.length);
            return new UnwrapResult(unwrappedRow, true);
        }

        // Row is not nested, return as-is
        return new UnwrapResult(row, false);
    }

    /**
     * Safely converts Object to Double, handling null values and various numeric types.
     *
     * Supported input types:
     * - null (returns null)
     * - Double (returns as-is)
     * - Integer, Long, Float (converts via doubleValue())
     * - BigDecimal (converts via doubleValue())
     * - String (attempts to parse as Double)
     *
     * This is the primary conversion method for floating-point database values like:
     * - Geographic coordinates (latitude, longitude)
     * - Temperature, wind speed measurements
     * - Distance calculations
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return Double value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to Double
     */
    public static Double toDoubleNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse string '%s' to Double", value), e);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot convert %s (%s) to Double",
                        value, value.getClass().getSimpleName()));
    }

    /**
     * Safely converts Object to Integer, handling null values and various numeric types.
     *
     * Supported input types:
     * - null (returns null)
     * - Integer (returns as-is)
     * - Long, BigDecimal, Double (converts via intValue(), truncating decimals)
     * - String (attempts to parse as Integer)
     *
     * This is the primary conversion method for integer database values like:
     * - ATMO air quality indices
     * - Weather parameters (humidity, wind direction, weather codes)
     * - Pollutant concentrations (PM10, PM2.5, NO2, O3, SO2)
     * - Population counts
     *
     * Note: For Double/BigDecimal conversions, decimal values are truncated (not rounded).
     * Example: 48.85660000 → 48
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return Integer value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to Integer
     */
    public static Integer toIntegerNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse string '%s' to Integer", value), e);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot convert %s (%s) to Integer",
                        value, value.getClass().getSimpleName()));
    }

    /**
     * Safely converts Object to Long, handling null values and various numeric types.
     *
     * Supported input types:
     * - null (returns null)
     * - Long (returns as-is)
     * - Integer, BigDecimal, Double (converts via longValue(), truncating decimals)
     * - String (attempts to parse as Long)
     *
     * This conversion method is useful for large integer values like:
     * - Population counts in communes
     * - Large aggregate counts
     * - Timestamps and durations
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return Long value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to Long
     */
    public static Long toLongNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse string '%s' to Long", value), e);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot convert %s (%s) to Long",
                        value, value.getClass().getSimpleName()));
    }

    /**
     * Safely converts Object to BigDecimal, handling null values and various numeric types.
     *
     * Supported input types:
     * - null (returns null)
     * - BigDecimal (returns as-is)
     * - Double, Float, Integer, Long (converts via constructor, preserving precision where possible)
     * - String (parses to BigDecimal, preserving exact decimal representation)
     *
     * This conversion method is useful for precise decimal values like:
     * - Geographic coordinates (stored as DECIMAL in database)
     * - Financial calculations requiring exact precision
     * - Database aggregate values with decimal places
     *
     * Note: Converting Double to BigDecimal may lose precision due to Double's binary representation.
     * For maximum precision, prefer converting from String or storing values as BigDecimal in database.
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return BigDecimal value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to BigDecimal
     */
    public static BigDecimal toBigDecimalNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Double) {
            // Note: Converting Double to BigDecimal may lose precision
            return BigDecimal.valueOf((Double) value);
        }

        if (value instanceof Integer) {
            return BigDecimal.valueOf((long) (Integer) value);
        }

        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value);
        }

        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse string '%s' to BigDecimal", value), e);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot convert %s (%s) to BigDecimal",
                        value, value.getClass().getSimpleName()));
    }

    /**
     * Safely converts Object to String, handling null values and nested Object arrays.
     *
     * Supported input types:
     * - null (returns null)
     * - String (returns as-is)
     * - Nested Object[] with String element (some JDBC drivers wrap strings in arrays)
     * - Other objects (converts via toString())
     *
     * This conversion method handles edge cases from various JDBC drivers:
     * - MariaDB driver may wrap strings in Object[] arrays
     * - Some drivers return VARCHAR columns as String directly
     * - Other database values can be converted to their string representation
     *
     * This is useful for string database values like:
     * - INSEE codes (commune identifiers)
     * - Commune names
     * - Color codes for visualization
     * - Qualifiers and descriptions
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return String value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to String
     */
    public static String toStringNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        // Handle nested Object[] - some JDBC drivers wrap results in extra array
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length > 0 && arr[0] instanceof String) {
                log.debug("Unwrapped String from nested Object[] array");
                return (String) arr[0];
            }
            // If array doesn't contain a string at [0], fall through to error handling
        }

        // Convert any other object to String via toString()
        try {
            return value.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Cannot convert %s (%s) to String",
                            value, value.getClass().getSimpleName()), e);
        }
    }

    /**
     * Safely converts Object to LocalDate, handling null values and various date representations.
     *
     * Supported input types:
     * - null (returns null)
     * - LocalDate (returns as-is)
     * - java.sql.Date (converts via toLocalDate())
     * - java.util.Date (converts via instant, then to LocalDate)
     * - String (parses using ISO-8601 format: yyyy-MM-dd)
     *
     * This conversion method is essential for handling database DATE columns like:
     * - Air quality measurement dates
     * - Weather data collection dates
     * - Alert detection timestamps
     *
     * Database-to-Java mapping:
     * - MySQL/MariaDB DATE → java.sql.Date → LocalDate
     * - PostgreSQL DATE → java.sql.Date → LocalDate
     *
     * @param value Object to convert (typically from JPQL query result)
     * @return LocalDate value, or null if input is null
     * @throws IllegalArgumentException if value cannot be converted to LocalDate
     */
    public static LocalDate toLocalDateNullable(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }

        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }

        if (value instanceof java.util.Date) {
            return new java.util.Date(((java.util.Date) value).getTime())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        if (value instanceof String) {
            try {
                return LocalDate.parse((String) value);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse string '%s' to LocalDate (expected format: yyyy-MM-dd)", value), e);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot convert %s (%s) to LocalDate",
                        value, value.getClass().getSimpleName()));
    }

    /**
     * Validates that a query result row has the expected number of columns.
     *
     * Useful for catching schema mismatches and native query definition errors early,
     * before attempting to access array indices.
     *
     * @param row Object array from query result
     * @param expectedSize Expected number of columns
     * @param queryName Name of the query for error messages
     * @throws IllegalArgumentException if row size doesn't match expected size
     */
    public static void validateRowSize(Object[] row, int expectedSize, String queryName) {
        if (row == null) {
            throw new IllegalArgumentException(
                    String.format("Query result row is null for %s", queryName));
        }

        if (row.length != expectedSize) {
            throw new IllegalArgumentException(
                    String.format("Query %s returned %d columns, expected %d",
                            queryName, row.length, expectedSize));
        }
    }
}
