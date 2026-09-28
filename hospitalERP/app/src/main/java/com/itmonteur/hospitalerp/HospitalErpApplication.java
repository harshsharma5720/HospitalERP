package com.itmonteur.hospitalerp;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
// Each sub-package com.itmonteur.hospitalerp.<name> is an application module (docs/MULTI_MODULE_PLAN.md);
// common is the shared kernel every module may use.
@Modulithic(sharedModules = "common")
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
