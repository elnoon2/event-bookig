package com.badyauniversity.eventbooking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for generating QR codes.
 * Handles both data URLs (Base64) and file-based storage.
 */
@Service
public class QrCodeService {

    private static final int DEFAULT_SIZE = 256;
    private static final Path QR_DIR = Paths.get("uploads", "qr");

    public String newStudentToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * PNG as data URL; {@code payload} is what scanners read.
     */
    public String encodeToPngDataUrl(String payload) {
        String b64 = Base64.getEncoder().encodeToString(encodeToPngBytes(payload, DEFAULT_SIZE));
        return "data:image/png;base64," + b64;
    }

    /**
     * Generates PNG bytes for the given payload.
     */
    public byte[] encodeToPngBytes(String payload, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    /**
     * Generates a PNG QR image for {@code text} and saves it under uploads/qr/{fileName}.
     * Returns the public path (e.g. /qr/profile_1.png).
     */
    public String generateAndSaveQRCode(String text, String fileName) {
        try {
            Files.createDirectories(QR_DIR);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, DEFAULT_SIZE, DEFAULT_SIZE);

            Path target = QR_DIR.resolve(fileName).normalize();
            if (!target.startsWith(QR_DIR)) {
                throw new RuntimeException("Invalid file name");
            }

            MatrixToImageWriter.writeToPath(matrix, "PNG", target);
            return "/qr/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create QR output folder", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
}
