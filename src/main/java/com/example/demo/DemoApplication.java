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
						.code("classic")
						.name("Classic Blue")
						.organizationName("Orderzone University")
						.layout("VERTICAL")
						.primaryColor("#1d4ed8")
						.secondaryColor("#e0e7ff")
						.textColor("#111827")
						.tagline("Knowledge • Integrity • Service")
						.build());

				templateRepository.save(Template.builder()
						.code("modern")
						.name("Modern Teal")
						.organizationName("Orderzone Corp")
						.layout("HORIZONTAL")
						.primaryColor("#0f766e")
						.secondaryColor("#ccfbf1")
						.textColor("#0f172a")
						.tagline("Building the future")
						.build());

				templateRepository.save(Template.builder()
						.code("crimson")
						.name("Crimson Vertical")
						.organizationName("Orderzone Institute")
						.layout("VERTICAL")
						.primaryColor("#b91c1c")
						.secondaryColor("#fee2e2")
						.textColor("#1f2937")
						.tagline("Excellence in education")
						.build());

				templateRepository.save(Template.builder()
						.code("slate")
						.name("Slate Horizontal")
						.organizationName("Orderzone Labs")
						.layout("HORIZONTAL")
						.primaryColor("#334155")
						.secondaryColor("#e2e8f0")
						.textColor("#0f172a")
						.tagline("Research & Innovation")
						.build());
			}
		};
	}
}
