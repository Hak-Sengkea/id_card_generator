package com.example.demo.service;

import com.example.demo.model.BarcodeType;
import com.example.demo.model.Profile;
import com.example.demo.model.Template;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final QrCodeService qrCodeService;
    private final BarcodeService barcodeService;

    // Card sizes in points (72 points = 1 inch)
    // CR80 standard is 3.375" x 2.125" (approx 243pt x 153pt)
    // We will use 210pt x 330pt for Vertical and 330pt x 210pt for Horizontal for better readability.
    private static final float CARD_WIDTH_V = 210f;
    private static final float CARD_HEIGHT_V = 330f;

    private static final float CARD_WIDTH_H = 330f;
    private static final float CARD_HEIGHT_H = 210f;

    public byte[] generateIdCardPdf(Profile profile) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Template template = profile.getTemplate();
        if (template == null) {
            template = Template.builder()
                    .name("Default Theme")
                    .code("DEFAULT")
                    .organizationName("ID CARD CENTER")
                    .layout("VERTICAL")
                    .primaryColor("#1d4ed8")
                    .secondaryColor("#e0e7ff")
                    .textColor("#111827")
                    .tagline("Identity Verification System")
                    .build();
        }

        boolean isVertical = "VERTICAL".equalsIgnoreCase(template.getLayout());
        Rectangle pageSize = isVertical 
                ? new Rectangle(CARD_WIDTH_V, CARD_HEIGHT_V)
                : new Rectangle(CARD_WIDTH_H, CARD_HEIGHT_H);

        Document document = new Document(pageSize, 8f, 8f, 8f, 8f);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            renderCard(document, profile, template, isVertical);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateBatchPdf(List<Profile> profiles) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (profiles.isEmpty()) {
            return out.toByteArray();
        }

        // We assume default layout matches first profile's template, or vertical as default.
        Profile first = profiles.get(0);
        Template firstTemplate = first.getTemplate() != null ? first.getTemplate() : Template.builder().layout("VERTICAL").build();
        boolean firstVertical = "VERTICAL".equalsIgnoreCase(firstTemplate.getLayout());
        
        Rectangle defaultPageSize = firstVertical 
                ? new Rectangle(CARD_WIDTH_V, CARD_HEIGHT_V)
                : new Rectangle(CARD_WIDTH_H, CARD_HEIGHT_H);

        Document document = new Document(defaultPageSize, 8f, 8f, 8f, 8f);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            for (int i = 0; i < profiles.size(); i++) {
                Profile profile = profiles.get(i);
                Template template = profile.getTemplate();
                if (template == null) {
                    template = Template.builder()
                            .name("Default Theme")
                            .code("DEFAULT")
                            .organizationName("ID CARD CENTER")
                            .layout("VERTICAL")
                            .primaryColor("#1d4ed8")
                            .secondaryColor("#e0e7ff")
                            .textColor("#111827")
                            .tagline("Identity Verification System")
                            .build();
                }

                boolean isVertical = "VERTICAL".equalsIgnoreCase(template.getLayout());
                Rectangle currentSize = isVertical 
                        ? new Rectangle(CARD_WIDTH_V, CARD_HEIGHT_V)
                        : new Rectangle(CARD_WIDTH_H, CARD_HEIGHT_H);
                
                document.setPageSize(currentSize);
                
                if (i > 0) {
                    document.newPage();
                }

                renderCard(document, profile, template, isVertical);
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering batch PDF", e);
        }

        return out.toByteArray();
    }

    private void renderCard(Document document, Profile profile, Template template, boolean isVertical) throws Exception {
        Color primary = parseColor(template.getPrimaryColor(), Color.decode("#1d4ed8"));
        Color secondary = parseColor(template.getSecondaryColor(), Color.decode("#e0e7ff"));
        Color textCol = parseColor(template.getTextColor(), Color.decode("#111827"));
        Color white = Color.WHITE;

        Font orgFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, white);
        Font taglineFont = FontFactory.getFont(FontFactory.HELVETICA, 6, Font.ITALIC, secondary);
        Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, textCol);
        
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, primary);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6, Font.BOLD, textCol);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 6, Font.NORMAL, textCol);
        Font regFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Font.BOLD, primary);

        if (isVertical) {
            // ==========================================
            // VERTICAL LAYOUT (210 x 330)
            // ==========================================
            PdfPTable cardTable = new PdfPTable(1);
            cardTable.setWidthPercentage(100);
            
            // 1. Header (Primary Color Background)
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            
            PdfPCell orgCell = new PdfPCell(new Phrase(template.getOrganizationName().toUpperCase(), orgFont));
            orgCell.setBackgroundColor(primary);
            orgCell.setBorder(Rectangle.NO_BORDER);
            orgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            orgCell.setPaddingTop(6);
            orgCell.setPaddingBottom(2);
            headerTable.addCell(orgCell);

            PdfPCell tagCell = new PdfPCell(new Phrase(template.getTagline(), taglineFont));
            tagCell.setBackgroundColor(primary);
            tagCell.setBorder(Rectangle.NO_BORDER);
            tagCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tagCell.setPaddingBottom(6);
            headerTable.addCell(tagCell);

            PdfPCell headerContainerCell = new PdfPCell(headerTable);
            headerContainerCell.setBorder(Rectangle.BOX);
            headerContainerCell.setBorderColor(primary);
            headerContainerCell.setBorderWidth(1.5f);
            cardTable.addCell(headerContainerCell);

            // 2. Profile Type Badge (STUDENT / EMPLOYEE)
            PdfPCell badgeCell = new PdfPCell(new Phrase(profile.getType().name(), badgeFont));
            badgeCell.setBackgroundColor(secondary);
            badgeCell.setBorder(Rectangle.BOX);
            badgeCell.setBorderColor(primary);
            badgeCell.setBorderWidth(1f);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setPadding(3);
            cardTable.addCell(badgeCell);

            // Spacer
            cardTable.addCell(createEmptyCell(4));

            // 3. Photo (Centered with Border)
            Image photoImg = loadPhoto(profile);
            photoImg.scaleAbsolute(55, 65);
            photoImg.setAlignment(Image.MIDDLE);
            
            PdfPTable photoTable = new PdfPTable(1);
            photoTable.setWidthPercentage(100);
            PdfPCell photoCell = new PdfPCell(photoImg);
            photoCell.setBorder(Rectangle.BOX);
            photoCell.setBorderColor(primary);
            photoCell.setBorderWidth(1f);
            photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoCell.setPadding(2);
            photoTable.addCell(photoCell);
            
            PdfPCell photoContainer = new PdfPCell(photoTable);
            photoContainer.setBorder(Rectangle.NO_BORDER);
            photoContainer.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoContainer.setPaddingLeft(60);
            photoContainer.setPaddingRight(60);
            cardTable.addCell(photoContainer);

            // Spacer
            cardTable.addCell(createEmptyCell(4));

            // 4. Details
            PdfPCell nameCell = new PdfPCell(new Phrase(profile.getFullName(), nameFont));
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nameCell.setPaddingBottom(4);
            cardTable.addCell(nameCell);

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(90);
            detailsTable.setWidths(new float[]{35, 65});

            addDetailRow(detailsTable, "REG NO:", profile.getRegistrationNumber(), labelFont, regFont);
            addDetailRow(detailsTable, "TITLE:", profile.getTitle(), labelFont, valueFont);
            addDetailRow(detailsTable, "DEPT:", profile.getDepartment(), labelFont, valueFont);
            if (profile.getBloodGroup() != null && !profile.getBloodGroup().isBlank()) {
                addDetailRow(detailsTable, "BLOOD GRP:", profile.getBloodGroup(), labelFont, valueFont);
            }
            if (profile.getExpiryDate() != null) {
                String expDate = profile.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                addDetailRow(detailsTable, "EXPIRY:", expDate, labelFont, valueFont);
            }

            PdfPCell detailsContainer = new PdfPCell(detailsTable);
            detailsContainer.setBorder(Rectangle.NO_BORDER);
            detailsContainer.setHorizontalAlignment(Element.ALIGN_CENTER);
            detailsContainer.setPaddingLeft(10);
            detailsContainer.setPaddingRight(10);
            cardTable.addCell(detailsContainer);

            // Spacer
            cardTable.addCell(createEmptyCell(8));

            // 5. Codes Footer (QR Code on Left, Barcode on Right)
            PdfPTable codesTable = new PdfPTable(2);
            codesTable.setWidthPercentage(95);
            codesTable.setWidths(new float[]{30, 70});

            // QR Code (verification URL or uuid)
            String qrUrl = "http://localhost:8081/verify/profile/" + profile.getUuid();
            byte[] qrBytes = qrCodeService.generateQrCode(qrUrl, 80, 80);
            Image qrImg = Image.getInstance(qrBytes);
            qrImg.scaleAbsolute(40, 40);
            PdfPCell qrCell = new PdfPCell(qrImg);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            codesTable.addCell(qrCell);

            // Barcode
            BarcodeType bcType = profile.getBarcodeType() != null ? profile.getBarcodeType() : BarcodeType.CODE_128;
            byte[] bcBytes = barcodeService.generateBarcode(profile.getRegistrationNumber(), bcType, 120, 30);
            Image bcImg = Image.getInstance(bcBytes);
            bcImg.scaleAbsolute(110, 25);
            
            PdfPTable barcodeContainerTable = new PdfPTable(1);
            barcodeContainerTable.setWidthPercentage(100);
            
            PdfPCell bcCell = new PdfPCell(bcImg);
            bcCell.setBorder(Rectangle.NO_BORDER);
            bcCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            barcodeContainerTable.addCell(bcCell);

            PdfPCell bcTextCell = new PdfPCell(new Phrase(profile.getRegistrationNumber(), valueFont));
            bcTextCell.setBorder(Rectangle.NO_BORDER);
            bcTextCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            bcTextCell.setPaddingTop(1);
            barcodeContainerTable.addCell(bcTextCell);

            PdfPCell bcContainerCell = new PdfPCell(barcodeContainerTable);
            bcContainerCell.setBorder(Rectangle.NO_BORDER);
            codesTable.addCell(bcContainerCell);

            PdfPCell codesContainer = new PdfPCell(codesTable);
            codesContainer.setBorder(Rectangle.NO_BORDER);
            codesContainer.setPaddingLeft(5);
            codesContainer.setPaddingRight(5);
            cardTable.addCell(codesContainer);

            // Border for the whole card
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);
            PdfPCell outerCell = new PdfPCell(cardTable);
            outerCell.setBorder(Rectangle.BOX);
            outerCell.setBorderColor(primary);
            outerCell.setBorderWidth(2f);
            outerCell.setPadding(6);
            outerTable.addCell(outerCell);

            document.add(outerTable);
        } else {
            // ==========================================
            // HORIZONTAL LAYOUT (330 x 210)
            // ==========================================
            PdfPTable cardTable = new PdfPTable(2);
            cardTable.setWidthPercentage(100);
            cardTable.setWidths(new float[]{35, 65});

            // 1. Header spans 2 columns
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70, 30});

            PdfPCell orgCell = new PdfPCell(new Phrase(template.getOrganizationName().toUpperCase(), orgFont));
            orgCell.setBackgroundColor(primary);
            orgCell.setBorder(Rectangle.NO_BORDER);
            orgCell.setPaddingTop(5);
            orgCell.setPaddingBottom(5);
            orgCell.setPaddingLeft(6);
            headerTable.addCell(orgCell);

            PdfPCell badgeCell = new PdfPCell(new Phrase(profile.getType().name(), badgeFont));
            badgeCell.setBackgroundColor(secondary);
            badgeCell.setBorder(Rectangle.BOX);
            badgeCell.setBorderColor(primary);
            badgeCell.setBorderWidth(1f);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setPadding(2);
            headerTable.addCell(badgeCell);

            PdfPCell headerContainer = new PdfPCell(headerTable);
            headerContainer.setColspan(2);
            headerContainer.setBorder(Rectangle.BOX);
            headerContainer.setBorderColor(primary);
            headerContainer.setBorderWidth(1.5f);
            cardTable.addCell(headerContainer);

            // Left column (Photo & QR Code)
            PdfPTable leftTable = new PdfPTable(1);
            leftTable.setWidthPercentage(100);

            // Spacer
            leftTable.addCell(createEmptyCell(2));

            // Photo
            Image photoImg = loadPhoto(profile);
            photoImg.scaleAbsolute(55, 65);
            PdfPCell photoCell = new PdfPCell(photoImg);
            photoCell.setBorder(Rectangle.BOX);
            photoCell.setBorderColor(primary);
            photoCell.setBorderWidth(1f);
            photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoCell.setPadding(2);
            leftTable.addCell(photoCell);

            leftTable.addCell(createEmptyCell(3));

            // QR Code
            String qrUrl = "http://localhost:8081/verify/profile/" + profile.getUuid();
            byte[] qrBytes = qrCodeService.generateQrCode(qrUrl, 70, 70);
            Image qrImg = Image.getInstance(qrBytes);
            qrImg.scaleAbsolute(35, 35);
            PdfPCell qrCell = new PdfPCell(qrImg);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            leftTable.addCell(qrCell);

            PdfPCell leftContainer = new PdfPCell(leftTable);
            leftContainer.setBorder(Rectangle.NO_BORDER);
            leftContainer.setPaddingRight(5);
            cardTable.addCell(leftContainer);

            // Right column (Details & Barcode)
            PdfPTable rightTable = new PdfPTable(1);
            rightTable.setWidthPercentage(100);

            rightTable.addCell(createEmptyCell(2));

            // Name
            PdfPCell nameCell = new PdfPCell(new Phrase(profile.getFullName(), nameFont));
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setPaddingBottom(4);
            rightTable.addCell(nameCell);

            // Details list
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{30, 70});

            addDetailRow(detailsTable, "REG NO:", profile.getRegistrationNumber(), labelFont, regFont);
            addDetailRow(detailsTable, "TITLE:", profile.getTitle(), labelFont, valueFont);
            addDetailRow(detailsTable, "DEPT:", profile.getDepartment(), labelFont, valueFont);
            if (profile.getBloodGroup() != null && !profile.getBloodGroup().isBlank()) {
                addDetailRow(detailsTable, "BLOOD GRP:", profile.getBloodGroup(), labelFont, valueFont);
            }
            if (profile.getExpiryDate() != null) {
                String expDate = profile.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                addDetailRow(detailsTable, "EXPIRY:", expDate, labelFont, valueFont);
            }

            PdfPCell detailsContainer = new PdfPCell(detailsTable);
            detailsContainer.setBorder(Rectangle.NO_BORDER);
            rightTable.addCell(detailsContainer);

            rightTable.addCell(createEmptyCell(4));

            // Barcode
            BarcodeType bcType = profile.getBarcodeType() != null ? profile.getBarcodeType() : BarcodeType.CODE_128;
            byte[] bcBytes = barcodeService.generateBarcode(profile.getRegistrationNumber(), bcType, 150, 25);
            Image bcImg = Image.getInstance(bcBytes);
            bcImg.scaleAbsolute(130, 20);
            PdfPCell bcCell = new PdfPCell(bcImg);
            bcCell.setBorder(Rectangle.NO_BORDER);
            bcCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rightTable.addCell(bcCell);

            PdfPCell bcTextCell = new PdfPCell(new Phrase(profile.getRegistrationNumber(), valueFont));
            bcTextCell.setBorder(Rectangle.NO_BORDER);
            bcTextCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bcTextCell.setPaddingTop(1);
            rightTable.addCell(bcTextCell);

            PdfPCell rightContainer = new PdfPCell(rightTable);
            rightContainer.setBorder(Rectangle.NO_BORDER);
            rightContainer.setPaddingLeft(5);
            cardTable.addCell(rightContainer);

            // Border for the whole card
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);
            PdfPCell outerCell = new PdfPCell(cardTable);
            outerCell.setBorder(Rectangle.BOX);
            outerCell.setBorderColor(primary);
            outerCell.setBorderWidth(2f);
            outerCell.setPadding(6);
            outerTable.addCell(outerCell);

            document.add(outerTable);
        }
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingTop(1);
        lCell.setPaddingBottom(1);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingTop(1);
        vCell.setPaddingBottom(1);
        table.addCell(vCell);
    }

    private PdfPCell createEmptyCell(float height) {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(height);
        return cell;
    }

    private Image loadPhoto(Profile profile) throws BadElementException, IOException {
        if (profile.getPhotoFileName() != null && !profile.getPhotoFileName().isBlank()) {
            Path photoPath = Paths.get("uploads").resolve(profile.getPhotoFileName());
            if (Files.exists(photoPath)) {
                return Image.getInstance(photoPath.toAbsolutePath().toString());
            }
        }
        
        // Generate a simple dummy image with ZXing or drawing to a graphics context if file is missing.
        // Let's create a solid placeholder. A colored box can be generated by creating a ZXing barcode format with solid bytes,
        // or we can use a built-in OpenPDF dummy or a standard placeholder image.
        // Let's just create a small 1x1 pixel JPEG image dynamically as a byte array to act as a placeholder.
        byte[] dummyJpg = new byte[] {
            (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, (byte)0x00, (byte)0x10, (byte)0x4A, (byte)0x46,
            (byte)0x49, (byte)0x46, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x60,
            (byte)0x00, (byte)0x60, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xDB, (byte)0x00, (byte)0x43,
            (byte)0x00, (byte)0x08, (byte)0x06, (byte)0x06, (byte)0x07, (byte)0x06, (byte)0x05, (byte)0x08,
            (byte)0x07, (byte)0x07, (byte)0x07, (byte)0x09, (byte)0x09, (byte)0x08, (byte)0x0A, (byte)0x0C,
            (byte)0x14, (byte)0x0D, (byte)0x0C, (byte)0x0B, (byte)0x0B, (byte)0x0C, (byte)0x19, (byte)0x12,
            (byte)0x13, (byte)0x0F, (byte)0x14, (byte)0x1D, (byte)0x1A, (byte)0x1F, (byte)0x1E, (byte)0x1D,
            (byte)0x1A, (byte)0x1C, (byte)0x1C, (byte)0x20, (byte)0x24, (byte)0x2E, (byte)0x27, (byte)0x20,
            (byte)0x22, (byte)0x2C, (byte)0x23, (byte)0x1C, (byte)0x1C, (byte)0x28, (byte)0x37, (byte)0x29,
            (byte)0x2C, (byte)0x30, (byte)0x31, (byte)0x34, (byte)0x34, (byte)0x34, (byte)0x1F, (byte)0x27,
            (byte)0x39, (byte)0x3D, (byte)0x38, (byte)0x32, (byte)0x3C, (byte)0x2E, (byte)0x33, (byte)0x34,
            (byte)0x32, (byte)0xFF, (byte)0xC0, (byte)0x00, (byte)0x0B, (byte)0x08, (byte)0x00, (byte)0x08,
            (byte)0x00, (byte)0x08, (byte)0x01, (byte)0x01, (byte)0x22, (byte)0x00, (byte)0xFF, (byte)0xC4,
            (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0xFF, (byte)0xDA, (byte)0x00, (byte)0x08,
            (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x3F, (byte)0x00, (byte)0x37, (byte)0xFF,
            (byte)0xD9
        };
        return Image.getInstance(dummyJpg);
    }

    private Color parseColor(String hex, Color fallback) {
        try {
            if (hex != null && hex.startsWith("#")) {
                return Color.decode(hex);
            }
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
