package com.badyauniversity.eventbooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Value("${app.whatsapp.service.url:http://localhost:3000}")
    private String whatsappServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendMessage(String phone, String message) {
        return sendMediaMessage(phone, message, null);
    }

    public boolean sendMediaMessage(String phone, String message, String mediaUrl) {
        System.out.println("[INFO] [BACKEND-WHATSAPP] Attempting to send WhatsApp message. Phone: " + phone + ", MediaUrl: " + mediaUrl);
        try {
            String url = whatsappServiceUrl + "/send-message";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("phone", phone);
            body.put("message", message);
            if (mediaUrl != null) {
                body.put("mediaUrl", mediaUrl);
            }

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, MAP_TYPE);

            System.out.println("[INFO] [BACKEND-WHATSAPP] Microservice response status: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                boolean success = Boolean.TRUE.equals(responseBody != null ? responseBody.get("success") : null);
                System.out.println("[INFO] [BACKEND-WHATSAPP] Microservice response success flag: " + success);
                return success;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] [BACKEND-WHATSAPP] WhatsApp Service Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a WhatsApp message with a QR code image generated server-side.
     * Instead of passing a mediaUrl (external API), we pass the raw QR data
     * and let the WhatsApp microservice generate the image locally.
     */
    public boolean sendQrMessage(String phone, String message, String qrData) {
        System.out.println("[INFO] [BACKEND-WHATSAPP] Attempting to send WhatsApp QR message. Phone: " + phone + ", QR Data length: " + (qrData != null ? qrData.length() : 0));
        try {
            String url = whatsappServiceUrl + "/send-message";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("phone", phone);
            body.put("message", message);
            if (qrData != null) {
                body.put("qrData", qrData);
            }

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, MAP_TYPE);

            System.out.println("[INFO] [BACKEND-WHATSAPP] Microservice response status: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                boolean success = Boolean.TRUE.equals(responseBody != null ? responseBody.get("success") : null);
                System.out.println("[INFO] [BACKEND-WHATSAPP] Microservice response success flag: " + success);
                return success;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[ERROR] [BACKEND-WHATSAPP] WhatsApp QR Service Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isServiceReady() {
        try {
            String url = whatsappServiceUrl + "/status";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, MAP_TYPE);
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                boolean ready = Boolean.TRUE.equals(responseBody != null ? responseBody.get("ready") : null);
                System.out.println("[INFO] [BACKEND-WHATSAPP] Service status checked. Ready: " + ready);
                return ready;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[WARN] [BACKEND-WHATSAPP] Failed to check WhatsApp service status: " + e.getMessage());
            return false;
        }
    }
}
