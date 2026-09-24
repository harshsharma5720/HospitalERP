package ITmonteur.example.hospitalERP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Needs a running MySQL, so it only runs when DB_URL is set in the environment
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class HospitalErpApplicationTests {

	@Test
	void contextLoads() {
	}

}
