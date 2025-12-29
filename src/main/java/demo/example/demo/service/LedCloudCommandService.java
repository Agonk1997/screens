// package demo.example.demo.service;

// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Service
// public class LedCloudCommandService {

//     private final LedCloudAuthService authService;
//     private final RestTemplate restTemplate = new RestTemplate();

//     public LedCloudCommandService(LedCloudAuthService authService) {
//         this.authService = authService;
//     }

//     public void sendReboot(String deviceId) {
//         String url = "https://led-cloud.com/v1/task/add/command";

//         String token = authService.getAccessToken();

//         HttpHeaders headers = new HttpHeaders();
//         headers.setBearerAuth(token);
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         headers.set("Origin", "https://led-cloud.com");
//         headers.set("Referer", "https://led-cloud.com/");

//         Map<String, Object> args = new HashMap<>();
//         args.put("displayId", 0);

//         Map<String, Object> body = new HashMap<>();
//         body.put("command", "reboot");
//         body.put("devices", List.of(deviceId));
//         body.put("args", args);

//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

//         ResponseEntity<String> response =
//                 restTemplate.postForEntity(url, entity, String.class);

//         System.out.println("Reboot response: " + response.getBody());
//     }
// }
