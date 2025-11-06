package fr.airsen.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ForumCategory entity.
 */
class ForumCategoryTest {

    private ForumCategory category;
    private Validator validator;

    @BeforeEach
    void setUp() {
        category = new ForumCategory("General", "General discussion area", "#FFFFFF", Collections.emptyList());

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testDefaultConstructor() {
        ForumCategory emptyCategory = new ForumCategory();
        assertNull(emptyCategory.getName());
        assertNull(emptyCategory.getDescription());
        assertNull(emptyCategory.getColor());
        assertNull(emptyCategory.getThreads());
    }

    @Test
    void testParameterizedConstructor() {
        assertEquals("General", category.getName());
        assertEquals("General discussion area", category.getDescription());
        assertEquals("#FFFFFF", category.getColor());
        assertTrue(category.getThreads().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        ForumCategory c = new ForumCategory();
        c.setId(1L);
        c.setName("Tech");
        c.setDescription("Technology topics");
        c.setColor("#000000");
        c.setThreads(List.of());

        assertEquals(1L, c.getId());
        assertEquals("Tech", c.getName());
        assertEquals("Technology topics", c.getDescription());
        assertEquals("#000000", c.getColor());
        assertTrue(c.getThreads().isEmpty());
    }

    @Test
    void testEqualsAndHashCode_sameName_shouldBeEqual() {
        ForumCategory c1 = new ForumCategory("News", "All about news", "#111111", null);
        ForumCategory c2 = new ForumCategory("News", "Different description", "#222222", null);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testEquals_differentName_shouldNotBeEqual() {
        ForumCategory c1 = new ForumCategory("Gaming", "Games", "#333333", null);
        ForumCategory c2 = new ForumCategory("Movies", "Films", "#444444", null);

        assertNotEquals(c1, c2);
    }

    @Test
    void testEquals_withNonForumCategoryObject_shouldReturnFalse() {
        assertNotEquals("Some String", category);
    }

    @Test
    void testToString_containsExpectedValues() {
        String result = category.toString();
        assertTrue(result.contains("id="));
        assertTrue(result.contains("name='General'"));
        assertTrue(result.contains("description='General discussion area'"));
        assertTrue(result.contains("color='#FFFFFF'"));
    }

    // --- Validation tests ---

    @Test
    void testNameTooShort_shouldFailValidation() {
        category.setName("Hi"); // too short (<3)
        Set<ConstraintViolation<ForumCategory>> violations = validator.validate(category);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("at least 3 characters")));
    }

    @Test
    void testColorInvalidLength_shouldFailValidation() {
        category.setColor("#FFF"); // too short
        Set<ConstraintViolation<ForumCategory>> violations = validator.validate(category);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid hex code")));
    }

    @Test
    void testValidEntity_shouldPassValidation() {
        Set<ConstraintViolation<ForumCategory>> violations = validator.validate(category);
        assertTrue(violations.isEmpty());
    }
}
