package ITmonteur.example.hospitalERP;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableAsync // used by NotificationService so emails/SMS never block a request
@EnableScheduling // appointment reminders
public class HospitalErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalErpApplication.class, args);
	}
	// Injected where "today" matters, so tests can use a fixed date
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

	@Bean
	public ModelMapper modelMapper()
	{
		return new ModelMapper();
	}

}
