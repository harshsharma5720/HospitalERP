package com.itmonteur.hospitalerp;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

// Temporary during Phase 2.2 (package-by-module move): classes not moved yet still live in the old package.
// Removed when the last module has moved.
@SpringBootApplication(scanBasePackages = {"com.itmonteur.hospitalerp", "ITmonteur.example.hospitalERP"})
@AutoConfigurationPackage(basePackages = {"com.itmonteur.hospitalerp", "ITmonteur.example.hospitalERP"}) // entities + repositories
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
