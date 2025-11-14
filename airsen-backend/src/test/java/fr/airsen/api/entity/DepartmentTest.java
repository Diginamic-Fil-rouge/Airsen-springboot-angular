package fr.airsen.api.entity;

import jakarta.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {

    private Validator validator;
    private Region region;

    @BeforeEach
    void setup() {
        // Initialise le validateur Jakarta Bean Validation
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Création d'une région de test
        region = new Region();
        region.setId(1L);
        region.setName("Île-de-France");
    }

    // -----------------------------
    // Tests getters / setters
    // -----------------------------
    @Test
    void testGetSetId() {
        Department d = new Department();
        d.setId(10L);
        assertEquals(10L, d.getId());
    }

    @Test
    void testGetSetName() {
        Department d = new Department();
        d.setName("Paris");
        assertEquals("Paris", d.getName());
    }

    @Test
    void testGetSetDepartmentCode() {
        Department d = new Department();
        d.setDepartmentCode("75");
        assertEquals("75", d.getDepartmentCode());
    }

    @Test
    void testGetSetRegionCode() {
        Department d = new Department();
        d.setRegionCode("IDF");
        assertEquals("IDF", d.getRegionCode());
    }

    @Test
    void testGetSetRegion() {
        Department d = new Department();
        d.setRegion(region);
        assertEquals(region, d.getRegion());
    }

    @Test
    void testGetSetCommunes() {
        Department d = new Department();
        Commune c = new Commune();
        d.setCommunes(List.of(c));
        assertEquals(1, d.getCommunes().size());
    }

    @Test
    void testConstructor() {
        Department d = new Department("Paris", "75", "IDF", region);
        assertEquals("Paris", d.getName());
        assertEquals("75", d.getDepartmentCode());
        assertEquals("IDF", d.getRegionCode());
        assertEquals(region, d.getRegion());
    }

    // -----------------------------
    // Tests validation
    // -----------------------------
    @Test
    void testValidationFailsWhenNameIsBlank() {
        Department d = new Department("", "75", "IDF", region);
        Set<ConstraintViolation<Department>> violations = validator.validate(d);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidationFailsWhenDepartmentCodeTooLong() {
        Department d = new Department("Paris", "12345", "IDF", region);
        Set<ConstraintViolation<Department>> violations = validator.validate(d);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidationFailsWhenRegionIsNull() {
        Department d = new Department("Paris", "75", "IDF", null);
        Set<ConstraintViolation<Department>> violations = validator.validate(d);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidationPassesWhenAllFieldsValid() {
        Department d = new Department("Paris", "75", "IDF", region);
        Set<ConstraintViolation<Department>> violations = validator.validate(d);
        assertTrue(violations.isEmpty());
    }
}
