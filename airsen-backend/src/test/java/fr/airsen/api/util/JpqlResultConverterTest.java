package fr.airsen.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JpqlResultConverter utility class.
 *
 * Validates safe type conversion from JPQL query results and JDBC driver outputs,
 * including edge cases like nested Object arrays and various numeric types.
 */
@DisplayName("JpqlResultConverter - Safe type conversion utility")
class JpqlResultConverterTest {

    @Test
    @DisplayName("Should convert null to null for Double")
    void testDoubleNullableWithNull() {
        assertThat(JpqlResultConverter.toDoubleNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should convert Double directly")
    void testDoubleNullableWithDouble() {
        assertThat(JpqlResultConverter.toDoubleNullable(48.8566))
                .isEqualTo(48.8566);
    }

    @Test
    @DisplayName("Should convert BigDecimal to Double")
    void testDoubleNullableWithBigDecimal() {
        assertThat(JpqlResultConverter.toDoubleNullable(new BigDecimal("48.8566")))
                .isCloseTo(48.8566, within(0.0001));
    }

    @Test
    @DisplayName("Should convert Integer to Double")
    void testDoubleNullableWithInteger() {
        assertThat(JpqlResultConverter.toDoubleNullable(50))
                .isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should convert String to Double")
    void testDoubleNullableWithString() {
        assertThat(JpqlResultConverter.toDoubleNullable("48.8566"))
                .isCloseTo(48.8566, within(0.0001));
    }

    @Test
    @DisplayName("Should throw exception for invalid String")
    void testDoubleNullableWithInvalidString() {
        assertThatThrownBy(() -> JpqlResultConverter.toDoubleNullable("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse string");
    }

    @Test
    @DisplayName("Should convert null to null for Integer")
    void testIntegerNullableWithNull() {
        assertThat(JpqlResultConverter.toIntegerNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should convert Integer directly")
    void testIntegerNullableWithInteger() {
        assertThat(JpqlResultConverter.toIntegerNullable(10))
                .isEqualTo(10);
    }

    @Test
    @DisplayName("Should convert BigDecimal to Integer (truncating decimals)")
    void testIntegerNullableWithBigDecimal() {
        assertThat(JpqlResultConverter.toIntegerNullable(new BigDecimal("48.8566")))
                .isEqualTo(48);
    }

    @Test
    @DisplayName("Should convert Double to Integer (truncating decimals)")
    void testIntegerNullableWithDouble() {
        assertThat(JpqlResultConverter.toIntegerNullable(48.8566))
                .isEqualTo(48);
    }

    @Test
    @DisplayName("Should convert String to Integer")
    void testIntegerNullableWithString() {
        assertThat(JpqlResultConverter.toIntegerNullable("10"))
                .isEqualTo(10);
    }

    @Test
    @DisplayName("Should convert null to null for Long")
    void testLongNullableWithNull() {
        assertThat(JpqlResultConverter.toLongNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should convert Long directly")
    void testLongNullableWithLong() {
        assertThat(JpqlResultConverter.toLongNullable(2161000L))
                .isEqualTo(2161000L);
    }

    @Test
    @DisplayName("Should convert Integer to Long")
    void testLongNullableWithInteger() {
        assertThat(JpqlResultConverter.toLongNullable(100))
                .isEqualTo(100L);
    }

    @Test
    @DisplayName("Should convert null to null for BigDecimal")
    void testBigDecimalNullableWithNull() {
        assertThat(JpqlResultConverter.toBigDecimalNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should convert BigDecimal directly")
    void testBigDecimalNullableWithBigDecimal() {
        BigDecimal value = new BigDecimal("48.8566");
        assertThat(JpqlResultConverter.toBigDecimalNullable(value))
                .isEqualTo(value);
    }

    @Test
    @DisplayName("Should convert Integer to BigDecimal")
    void testBigDecimalNullableWithInteger() {
        assertThat(JpqlResultConverter.toBigDecimalNullable(50))
                .isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("Should convert String to BigDecimal")
    void testBigDecimalNullableWithString() {
        assertThat(JpqlResultConverter.toBigDecimalNullable("48.8566"))
                .isEqualByComparingTo(new BigDecimal("48.8566"));
    }

    @Test
    @DisplayName("Should convert null to null for String")
    void testStringNullableWithNull() {
        assertThat(JpqlResultConverter.toStringNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should return String directly")
    void testStringNullableWithString() {
        assertThat(JpqlResultConverter.toStringNullable("Paris"))
                .isEqualTo("Paris");
    }

    @Test
    @DisplayName("Should handle nested Object[] array for String conversion")
    void testStringNullableWithNestedArray() {
        Object[] nestedArray = new Object[]{"Paris"};
        Object[] wrappedArray = new Object[]{nestedArray};

        // Must unwrap first, then convert
        JpqlResultConverter.UnwrapResult unwrapped = JpqlResultConverter.unwrapNestedArray(wrappedArray);
        assertThat(unwrapped.wasNested).isTrue();
        assertThat(JpqlResultConverter.toStringNullable(unwrapped.data[0]))
                .isEqualTo("Paris");
    }

    @Test
    @DisplayName("Should convert object to String via toString()")
    void testStringNullableWithObject() {
        Object obj = 12345;
        assertThat(JpqlResultConverter.toStringNullable(obj))
                .isEqualTo("12345");
    }

    @Test
    @DisplayName("Should convert null to null for LocalDate")
    void testLocalDateNullableWithNull() {
        assertThat(JpqlResultConverter.toLocalDateNullable(null)).isNull();
    }

    @Test
    @DisplayName("Should return LocalDate directly")
    void testLocalDateNullableWithLocalDate() {
        LocalDate date = LocalDate.of(2025, 11, 5);
        assertThat(JpqlResultConverter.toLocalDateNullable(date))
                .isEqualTo(date);
    }

    @Test
    @DisplayName("Should convert java.sql.Date to LocalDate")
    void testLocalDateNullableWithSqlDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2025, 11, 5));
        assertThat(JpqlResultConverter.toLocalDateNullable(sqlDate))
                .isEqualTo(LocalDate.of(2025, 11, 5));
    }

    @Test
    @DisplayName("Should convert String to LocalDate")
    void testLocalDateNullableWithString() {
        assertThat(JpqlResultConverter.toLocalDateNullable("2025-11-05"))
                .isEqualTo(LocalDate.of(2025, 11, 5));
    }

    @Test
    @DisplayName("Should unwrap nested Object[] array (MariaDB JDBC driver behavior)")
    void testUnwrapNestedArrayWithNestedData() {
        Object[] data = {"75056", "Paris", 48.8566, 2.3522};
        Object[] wrappedData = new Object[]{data};

        JpqlResultConverter.UnwrapResult result = JpqlResultConverter.unwrapNestedArray(wrappedData);

        assertThat(result.data).isEqualTo(data);
        assertThat(result.wasNested).isTrue();
    }

    @Test
    @DisplayName("Should return non-nested array as-is")
    void testUnwrapNestedArrayWithNonNested() {
        Object[] data = {"75056", "Paris", 48.8566, 2.3522};

        JpqlResultConverter.UnwrapResult result = JpqlResultConverter.unwrapNestedArray(data);

        assertThat(result.data).isEqualTo(data);
        assertThat(result.wasNested).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when validating row size mismatch")
    void testValidateRowSizeWithMismatch() {
        Object[] row = {"75056", "Paris", 48.8566, 2.3522};

        assertThatThrownBy(() -> JpqlResultConverter.validateRowSize(row, 11, "testQuery"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returned 4 columns, expected 11");
    }

    @Test
    @DisplayName("Should successfully validate correct row size")
    void testValidateRowSizeWithCorrectSize() {
        Object[] row = new Object[11];

        assertThatCode(() -> JpqlResultConverter.validateRowSize(row, 11, "testQuery"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for type mismatch")
    void testDoubleNullableWithIncompatibleType() {
        assertThatThrownBy(() -> JpqlResultConverter.toDoubleNullable(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot convert");
    }

    @Test
    @DisplayName("Should handle various Number types uniformly")
    void testDoubleNullableWithDifferentNumberTypes() {
        assertThat(JpqlResultConverter.toDoubleNullable(10)).isEqualTo(10.0);
        assertThat(JpqlResultConverter.toDoubleNullable(10L)).isEqualTo(10.0);
        assertThat(JpqlResultConverter.toDoubleNullable(10F)).isCloseTo(10.0, within(0.01));
        assertThat(JpqlResultConverter.toDoubleNullable(new BigDecimal("10"))).isEqualTo(10.0);
    }
}
