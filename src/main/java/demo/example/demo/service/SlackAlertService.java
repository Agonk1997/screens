package demo.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SlackAlertService {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 1000;

    public void sendMessage(String message) {
        try {
            // Rate limiting
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime < MIN_REQUEST_INTERVAL) {
                Thread.sleep(MIN_REQUEST_INTERVAL - (currentTime - lastRequestTime));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Slack expects a different format than Discord
            Map<String, Object> body = Map.of(
                "text", message,
                "username", "Screen Monitor",
                "icon_emoji", ":computer:"
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            lastRequestTime = System.currentTimeMillis();

        } catch (Exception e) {
            System.err.println("❌ Failed to send Slack alert: " + e.getMessage());
        }
    }
}