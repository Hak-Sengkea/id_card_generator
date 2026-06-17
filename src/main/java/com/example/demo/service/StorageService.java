package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    private final Path rootLocation = Paths.get("uploads");

    public String store(MultipartFile file) throws IOException {

        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        String contentType = file.getContentType();

        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)) {

            throw new RuntimeException(
                    "Only JPG and PNG files are allowed");
        }

        String filename =
                UUID.randomUUID()
                        + "_"
                        + file.getOriginalFilename();

        Files.copy(
                file.getInputStream(),
                rootLocation.resolve(filename));

        return filename;
    }
}