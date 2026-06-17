package com.example.demo.service;

import com.example.demo.model.BarcodeType;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BarcodeService {

    /**
     * Generates a linear barcode as a PNG byte array.
     * Supports CODE_128 and EAN_13.
     *
     * @param text   the content to encode
     * @param type   the barcode symbology type (CODE_128 or EAN_13)
     * @param width  image width
     * @param height image height
     * @return the barcode image bytes
     */
    public byte[] generateBarcode(String text, BarcodeType type, int width, int height) {
        try {
            BarcodeFormat format;
            String encodedText = text;
            if (type == BarcodeType.EAN_13) {
                format = BarcodeFormat.EAN_13;
                encodedText = sanitizeForEan13(text);
            } else {
                format = BarcodeFormat.CODE_128;
            }

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(encodedText, format, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate Barcode of type " + type, e);
        }
    }

    private String sanitizeForEan13(String text) {
        // Remove all non-digits
        String digitsOnly = text.replaceAll("\\D", "");
        if (digitsOnly.length() < 12) {
            // Pad with leading zeros to 12 digits
            digitsOnly = String.format("%012d", digitsOnly.isEmpty() ? 0L : Long.parseLong(digitsOnly));
        } else if (digitsOnly.length() > 12) {
            digitsOnly = digitsOnly.substring(0, 12);
        }

        // Calculate EAN-13 checksum (13th digit)
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(digitsOnly.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return digitsOnly + checksum;
    }
}
