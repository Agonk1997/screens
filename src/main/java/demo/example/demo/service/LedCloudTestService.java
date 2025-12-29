// package demo.example.demo.service;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.HttpClientErrorException;
// import org.springframework.web.client.RestTemplate;

// import java.util.*;

// @Service
// public class LedCloudTestService {

//     private final RestTemplate restTemplate = new RestTemplate();
//     private final LedCloudAuthService authService;

//     @Autowired
//     public LedCloudTestService(LedCloudAuthService authService) {
//         this.authService = authService;
//     }

//     private HttpHeaders headers(String token) {
//         HttpHeaders headers = new HttpHeaders();

//         headers.set("Authorization", "bearer" + token);

//         headers.set("Cookie",
//                 "hd_lang=en-US; " +
//                 "hd_token=" + token + "&4b02cba0-9cbb-4956-a2ed-6dbf4429d228&bearer"
//         );

//         headers.set("User-Agent",
//                 "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) " +
//                 "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
//         );

//         headers.set("Accept", "application/json, text/plain, */*");
//         headers.set("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8,de;q=0.7");
//         headers.set("Origin", "https://led-cloud.com");
//         headers.set("Referer", "https://led-cloud.com/");
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         return headers;
//     }

//     public void rebootScreen(String deviceSn) {

//         String url = "https://led-cloud.com/v1/task/add/command";

//         Map<String, Object> body = new HashMap<>();
//         body.put("command", "reboot");
//         body.put("devices", List.of(deviceSn));
//         body.put("args", Map.of("displayId", 0));

//         // First attempt with current token
//         String token = authService.getToken();
//         HttpEntity<Map<String, Object>> entity =
//                 new HttpEntity<>(body, headers(token));

//         try {
//             ResponseEntity<String> response =
//                     restTemplate.postForEntity(url, entity, String.class);
//             System.out.println("REBOOT RESPONSE (first try): " + response.getBody());

//         } catch (HttpClientErrorException.Unauthorized ex) {
//             // Token probably expired -> refresh + retry once
//             System.out.println("Token expired, refreshing and retrying reboot...");

//             authService.refreshToken();
//             String newToken = authService.getToken();

//             HttpEntity<Map<String, Object>> retryEntity =
//                     new HttpEntity<>(body, headers(newToken));

//             ResponseEntity<String> retryResponse =
//                     restTemplate.postForEntity(url, retryEntity, String.class);

//             System.out.println("REBOOT RESPONSE (after refresh): " + retryResponse.getBody());

//         } catch (Exception ex) {
//             System.out.println("FAILED TO SEND REBOOT COMMAND");
//             ex.printStackTrace();
//         }
//     }
// }
