package com.example.demo.repository;

import com.example.demo.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateRepository
        extends JpaRepository<Template, Long> {

    Optional<Template> findByCode(String code);

    boolean existsByCode(String code);
}