package com.example.carpool.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppNotificationService {

    // You can replace these with your Twilio / UltraMsg credentials later
    private final String API_URL = "YOUR_WHATSAPP_API_URL_HERE"; 
    private final String AUTH_TOKEN = "YOUR_API_TOKEN_HERE";

    public void sendWhatsAppMessage(String recipientPhoneNumber, String messageText) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + AUTH_TOKEN);

            Map<String, String> body = new HashMap<>();
            body.put("to", recipientPhoneNumber);
            body.put("message", messageText);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            
            // Uncomment below line when you connect a real API provider like Twilio/UltraMsg
            // restTemplate.postForEntity(API_URL, request, String.class);
            
            System.out.println("WhatsApp Notification sent to " + recipientPhoneNumber + ": " + messageText);
        } catch (Exception e) {
            System.err.println("Failed to send WhatsApp notification: " + e.getMessage());
        }
    }
}