package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false) // <-- désactive les filtres Spring Security
public class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private fr.airsen.api.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private fr.airsen.api.service.JwtBlacklistService jwtBlacklistService;

    @MockBean
    private DepartmentService departmentService;

    private List<DepartmentDTO> departments;

    @BeforeEach
    public void setUp() {
        // Données réalistes
        departments = List.of(
                new DepartmentDTO(1L, "Paris", "75", "11"),
                new DepartmentDTO(2L, "Rhône", "69", "84")
        );
    }

    @Test
    public void testGetAllDepartments() throws Exception {
        // Mock du service
        when(departmentService.getAllDepartments()).thenReturn(departments);

        // Appel du endpoint et vérifications
        mockMvc.perform(get("/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Paris")))
                .andExpect(jsonPath("$[0].departmentCode", is("75")))
                .andExpect(jsonPath("$[0].regionCode", is("11")))
                .andExpect(jsonPath("$[1].name", is("Rhône")))
                .andExpect(jsonPath("$[1].departmentCode", is("69")))
                .andExpect(jsonPath("$[1].regionCode", is("84")));
    }
    @Test
    public void testGetDepartmentById() throws Exception {
        DepartmentDTO department = new DepartmentDTO(1L, "Paris", "75", "11");
        when(departmentService.getDepartmentById(1L)).thenReturn(department);

        mockMvc.perform(get("/departments/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Paris")))
                .andExpect(jsonPath("$.departmentCode", is("75")))
                .andExpect(jsonPath("$.regionCode", is("11")));
    }

    @Test
    public void testGetDepartmentById_NotFound() throws Exception {
        when(departmentService.getDepartmentById(99L)).thenThrow(new RuntimeException("Department not found"));

        mockMvc.perform(get("/departments/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


}
