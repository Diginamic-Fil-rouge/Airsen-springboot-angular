package fr.airsen.api.init;

import fr.airsen.api.entity.Region;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.repository.RegionRepository;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.CommuneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RegionRepository regionRepository;
    private final DepartmentRepository departmentRepository;
    private final CommuneRepository communeRepository;

    public DataInitializer(RegionRepository regionRepository,
                           DepartmentRepository departmentRepository,
                           CommuneRepository communeRepository) {
        this.regionRepository = regionRepository;
        this.departmentRepository = departmentRepository;
        this.communeRepository = communeRepository;
    }

    @Override
    public void run(String... args) {
        if (regionRepository.count() == 0) {
            Region region = new Region();
            region.setName("Occitanie");
            region.setCodeRegion("76");
            regionRepository.save(region);

            Department dep = new Department();
            dep.setName("Haute-Garonne");
            dep.setCodeDepartment("31");
            dep.setRegion(region);
            departmentRepository.save(dep);

            Commune commune = new Commune();
            commune.setInseeCode("31555");
            commune.setName("Toulouse");
            commune.setPopulation(493465);
            commune.setLatitude(new BigDecimal("43.6045"));
            commune.setLongitude(new BigDecimal("1.4442"));
            commune.setDepartment(dep);
            commune.setRegion(region);
            communeRepository.save(commune);

            System.out.println("✅ Données de test insérées !");
        }
    }
}
