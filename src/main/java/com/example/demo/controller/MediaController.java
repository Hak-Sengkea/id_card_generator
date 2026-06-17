package com.example.demo.controller;

import com.example.demo.model.BarcodeType;
import com.example.demo.model.Profile;
import com.example.demo.model.ProfileType;
import com.example.demo.service.BarcodeService;
import com.example.demo.service.PdfExportService;
import com.example.demo.service.ProfileService;
import com.example.demo.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final ProfileService profileService;
    private final PdfExportService pdfExportService;
    private final QrCodeService qrCodeService;
    private final BarcodeService barcodeService;

    @GetMapping(value = "/media/qr/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getQrCode(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        String qrUrl = "http://localhost:8081/verify/" + profile.getUuid();
        return qrCodeService.generateQrCode(qrUrl, 200, 200);
    }

    @GetMapping(value = "/media/barcode/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getBarcode(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        BarcodeType bcType = profile.getBarcodeType() != null 
                ? profile.getBarcodeType() 
                : BarcodeType.CODE_128;
        return barcodeService.generateBarcode(profile.getRegistrationNumber(), bcType, 300, 80);
    }

    @GetMapping(value = "/media/photo/{id}")
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

    @GetMapping(value = "/api/profiles/{id}/card.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Profile profile = profileService.findById(id);
        byte[] pdf = pdfExportService.generateIdCardPdf(profile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"id-card-" + profile.getRegistrationNumber() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/api/batch/cards.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getBatchPdfGet(@RequestParam(value = "type", required = false) ProfileType type) {
        List<Profile> profiles = type == null 
                ? profileService.findAll()
                : profileService.findAll().stream().filter(p -> p.getType() == type).toList();
        byte[] pdf = pdfExportService.generateBatchPdf(profiles);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"id-cards-batch.pdf\"")
                .body(pdf);
    }

    @PostMapping(value = "/api/batch/cards.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getBatchPdfPost(@RequestBody List<Long> ids) {
        List<Profile> profiles = ids.stream()
                .map(profileService::findById)
                .toList();
        byte[] pdf = pdfExportService.generateBatchPdf(profiles);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"id-cards-batch.pdf\"")
                .body(pdf);
    }
}
