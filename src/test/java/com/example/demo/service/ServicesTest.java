package com.example.demo.service;

import com.example.demo.model.BarcodeType;
import com.example.demo.model.Profile;
import com.example.demo.model.ProfileBuilder;
import com.example.demo.model.ProfileType;
import com.example.demo.model.Template;
import com.example.demo.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServicesTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    private final QrCodeService qrCodeService = new QrCodeService();
    private final BarcodeService barcodeService = new BarcodeService();
    private PdfExportService pdfExportService;
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        storageService = new StorageService();
        pdfExportService = new PdfExportService(qrCodeService, barcodeService);
    }

    @Test
    public void testProfileBuilderDefaults() {
        Profile profile = ProfileBuilder.buildDefault();
        assertNotNull(profile);
        assertEquals("John Doe", profile.getFullName());
        assertEquals(ProfileType.STUDENT, profile.getType());
        assertEquals("Computer Science", profile.getDepartment());
        assertEquals("Undergraduate Student", profile.getTitle());
        assertEquals("O+", profile.getBloodGroup());
        assertEquals(BarcodeType.CODE_128, profile.getBarcodeType());
        assertNotNull(profile.getUuid());
    }

    @Test
    public void testProfileBuilderCustom() {
        Profile profile = new ProfileBuilder()
                .withName("Jane Smith")
                .withType(ProfileType.EMPLOYEE)
                .withDepartment("Human Resources")
                .withBloodGroup("A-")
                .build();

        assertEquals("Jane Smith", profile.getFullName());
        assertEquals(ProfileType.EMPLOYEE, profile.getType());
        assertEquals("Human Resources", profile.getDepartment());
        assertEquals("Staff Member", profile.getTitle());
        assertEquals("A-", profile.getBloodGroup());
    }

    @Test
    public void testProfileServiceCreateWithCustomRegNum() {
        Profile profile = new ProfileBuilder()
                .withName("Dave")
                .withType(ProfileType.STUDENT)
                .withDepartment("Engineering")
                .build();

        when(profileRepository.countByType(ProfileType.STUDENT)).thenReturn(4L);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Profile saved = profileService.create(profile);

        assertNotNull(saved.getUuid());
        // 2026 - ENG (from Engineering) - 005 (count 4 + 1)
        assertTrue(saved.getRegistrationNumber().contains("-ENG-005"));
        verify(profileRepository, times(1)).save(profile);
    }

    @Test
    public void testProfileServiceCreateFallbackRegNum() {
        Profile profile = new ProfileBuilder()
                .withName("Dave")
                .withType(ProfileType.EMPLOYEE)
                .withDepartment("") // No dept
                .build();

        when(profileRepository.countByType(ProfileType.EMPLOYEE)).thenReturn(0L);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Profile saved = profileService.create(profile);

        assertNotNull(saved.getUuid());
        // 2026 - EMP (from Employee fallback) - 001 (count 0 + 1)
        assertTrue(saved.getRegistrationNumber().contains("-EMP-001"));
    }

    @Test
    public void testStorageServiceInvalidFileType() {
        MultipartFile textFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello world".getBytes()
        );

        assertThrows(RuntimeException.class, () -> {
            storageService.store(textFile);
        });
    }

    @Test
    public void testStorageServiceValidFileValidation() {
        // We test validation logic only. Since storing writes to the real filesystem "uploads",
        // we verify it throws a specific error if we send invalid content type, and we check that png/jpg is processed.
        MultipartFile imageFile = new MockMultipartFile(
                "file", "test.png", "image/png", "pngbytes".getBytes()
        );

        // This will try to write to filesystem. Let's make sure it doesn't crash on type validation,
        // it should pass type check and attempt to save (might succeed if permissions allow).
        try {
            String filename = storageService.store(imageFile);
            assertNotNull(filename);
            assertTrue(filename.contains("test.png"));
        } catch (IOException e) {
            // IO exception is fine (means directory setup or copy write error), but it shouldn't throw "Only JPG and PNG allowed"
            assertFalse(e.getMessage() != null && e.getMessage().contains("Only JPG and PNG"));
        }
    }

    @Test
    public void testQrCodeService() {
        QrCodeService qr = new QrCodeService();
        byte[] qrBytes = qr.generateQrCode("http://example.com", 100, 100);
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);
    }

    @Test
    public void testBarcodeServiceCode128() {
        BarcodeService bc = new BarcodeService();
        byte[] bcBytes = bc.generateBarcode("2026-ENG-001", BarcodeType.CODE_128, 100, 30);
        assertNotNull(bcBytes);
        assertTrue(bcBytes.length > 0);
    }

    @Test
    public void testBarcodeServiceEan13() {
        BarcodeService bc = new BarcodeService();
        // EAN-13 will auto-pad and checksum
        byte[] bcBytes = bc.generateBarcode("12345", BarcodeType.EAN_13, 100, 30);
        assertNotNull(bcBytes);
        assertTrue(bcBytes.length > 0);
    }

    @Test
    public void testPdfExportServiceVertical() {
        Profile profile = ProfileBuilder.buildDefault();
        profile.setRegistrationNumber("2026-STU-001");
        
        Template template = Template.builder()
                .layout("VERTICAL")
                .primaryColor("#1d4ed8")
                .secondaryColor("#e0e7ff")
                .textColor("#111827")
                .organizationName("Test Org")
                .build();
        profile.setTemplate(template);

        byte[] pdf = pdfExportService.generateIdCardPdf(profile);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
