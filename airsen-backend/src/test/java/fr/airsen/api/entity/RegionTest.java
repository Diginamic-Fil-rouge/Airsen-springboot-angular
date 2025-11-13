package fr.airsen.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionTest {

    private Region region;

    @BeforeEach
    void setUp() {
        region = new Region();
    }

    @Test
    void getId() {
        region.setId(10L);
        assertEquals(10L, region.getId());
    }

    @Test
    void setId() {
        region.setId(5L);
        assertEquals(5L, region.getId());
    }

    @Test
    void getName() {
        region.setName("Occitanie");
        assertEquals("Occitanie", region.getName());
    }

    @Test
    void setName() {
        region.setName("Normandie");
        assertEquals("Normandie", region.getName());
    }

    @Test
    void getRegionCode() {
        region.setRegionCode("NOR");
        assertEquals("NOR", region.getRegionCode());
    }

    @Test
    void setRegionCode() {
        region.setRegionCode("BRE");
        assertEquals("BRE", region.getRegionCode());
    }

    @Test
    void getDepartments() {
        Department d1 = new Department();
        Department d2 = new Department();
        List<Department> list = List.of(d1, d2);

        region.setDepartments(list);

        assertNotNull(region.getDepartments());
        assertEquals(2, region.getDepartments().size());
    }

    @Test
    void setDepartments() {
        Department d1 = new Department();
        List<Department> list = List.of(d1);

        region.setDepartments(list);

        assertSame(list, region.getDepartments()); // même instance
        assertEquals(1, region.getDepartments().size());
    }
}
