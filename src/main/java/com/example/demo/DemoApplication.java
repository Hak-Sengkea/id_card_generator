package com.example.demo;

import com.example.demo.model.Template;
import com.example.demo.repository.TemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	CommandLineRunner initTemplates(TemplateRepository templateRepository) {
		return args -> {
			if (templateRepository.count() == 0) {
				templateRepository.save(Template.builder()
						.code("BLUE_CORP")
						.name("Blue Corporate")
						.organizationName("Global Tech Institute")
						.layout("VERTICAL")
						.primaryColor("#1e3a8a")
						.secondaryColor("#dbeafe")
						.textColor("#1e293b")
						.tagline("Excellence in Technology")
						.build());

				templateRepository.save(Template.builder()
						.code("RED_ELEGANT")
						.name("Red Elegant")
						.organizationName("Aero Aerospace Corp")
						.layout("HORIZONTAL")
						.primaryColor("#991b1b")
						.secondaryColor("#fee2e2")
						.textColor("#111827")
						.tagline("Innovating the Future")
						.build());

				templateRepository.save(Template.builder()
						.code("GREEN_CAMPUS")
						.name("Green Emerald")
						.organizationName("Eco Valley College")
						.layout("VERTICAL")
						.primaryColor("#065f46")
						.secondaryColor("#d1fae5")
						.textColor("#1f2937")
						.tagline("Green Planet, Bright Future")
						.build());

				templateRepository.save(Template.builder()
						.code("DARK_PREMIUM")
						.name("Dark Premium")
						.organizationName("Apex Design Agency")
						.layout("HORIZONTAL")
						.primaryColor("#111827")
						.secondaryColor("#f3f4f6")
						.textColor("#111827")
						.tagline("Creative Excellence")
						.build());
			}
		};
	}
}
