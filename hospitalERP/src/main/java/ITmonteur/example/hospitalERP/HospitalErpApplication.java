package ITmonteur.example.hospitalERP;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // used by NotificationService so emails/SMS never block a request
public class HospitalErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalErpApplication.class, args);
	}
	@Bean
	public ModelMapper modelMapper()
	{
		return new ModelMapper();
	}

}
