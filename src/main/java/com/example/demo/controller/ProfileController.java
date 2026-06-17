package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.service.ProfileService;
import com.example.demo.service.StorageService;
import com.example.demo.service.PdfExportService;
import com.example.demo.service.QrCodeService;
import com.example.demo.service.BarcodeService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final StorageService storageService;
    private final PdfExportService pdfExportService;
    private final QrCodeService qrCodeService;
    private final BarcodeService barcodeService;
    
    @GetMapping
    public List<Profile> getAll() {
        return profileService.findAll();
    }

    @GetMapping("/{id}")
    public Profile getById(@PathVariable Long id) {
        return profileService.findById(id);
    }

    @PostMapping
    public Profile create(@RequestBody Profile profile) {
        return profileService.create(profile);
    }

    @PutMapping("/{id}")
    public Profile update(
            @PathVariable Long id,
            @RequestBody Profile profile) {

        return profileService.update(id, profile);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        profileService.delete(id);
    }

    @PostMapping("/{id}/photo")
    public Profile uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file)
            throws IOException {

        Profile profile = profileService.findById(id);
        String filename = storageService.store(file);

        profile.setPhotoFileName(filename);
        profile.setPhotoContentType(file.getContentType());

        return profileService.save(profile);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) throws IOException {
        Profile profile = profileService.findById(id);
        if (profile.getPhotoFileName() != null && !profile.getPhotoFileName().isBlank()) {
            Path path = Paths.get("uploads").resolve(profile.getPhotoFileName());
            if (Files.exists(path)) {
                byte[] data = Files.readAllBytes(path);
                String contentType = profile.getPhotoContentType() != null 
                        ? profile.getPhotoContentType() 
                        : MediaType.IMAGE_JPEG_VALUE;
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(data);
            }
        }
        
        byte[] dummyPng = new byte[] {
            (byte)137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, (byte)196, (byte)137, 0, 0, 0, 13, 73, 68, 65, 84, 120, (byte)156, 99, 96, 96, 96, 0, 0, 0, 5, 0, 1, (byte)164, (byte)211, (byte)208, (byte)143, 0, 0, 0, 0, 73, 69, 78, 68, (byte)174, 66, 96, (byte)130
        };
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(dummyPng);
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getQrCode(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        String qrUrl = "http://localhost:8081/verify/profile/" + profile.getUuid();
        return qrCodeService.generateQrCode(qrUrl, 200, 200);
    }

    @GetMapping(value = "/{id}/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getBarcode(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        com.example.demo.model.BarcodeType bcType = profile.getBarcodeType() != null 
                ? profile.getBarcodeType() 
                : com.example.demo.model.BarcodeType.CODE_128;
        return barcodeService.generateBarcode(profile.getRegistrationNumber(), bcType, 300, 80);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        byte[] pdf = pdfExportService.generateIdCardPdf(profile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"id_card_" + profile.getRegistrationNumber() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/batch/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getBatchPdf(@RequestParam(value = "ids", required = false) List<Long> ids) {
        List<Profile> profiles = (ids == null || ids.isEmpty())
                ? profileService.findAll()
                : ids.stream().map(profileService::findById).toList();
        byte[] pdf = pdfExportService.generateBatchPdf(profiles);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch_id_cards.pdf\"")
                .body(pdf);
    }
}