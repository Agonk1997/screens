// package demo.example.demo.service;

// import demo.example.demo.dto.RsaKeyResponse;
// import demo.example.demo.dto.TokenResponse;
// import demo.example.demo.util.RsaUtil;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.client.RestTemplate;

// @Service
// public class LedCloudAuthService {

//     private static final String KEY_URL   = "https://led-cloud.com/oauth/key";
//     private static final String TOKEN_URL = "https://led-cloud.com/oauth/token";

//     // from application.properties
//     @Value("${ledcloud.username}")
//     private String username;

//     @Value("${ledcloud.password}")
//     private String password;

//     private final RestTemplate restTemplate = new RestTemplate();

//     private String accessToken;
//     private long   expiryTimeMillis = 0;

//     // Call this from your controller or command service
//     public synchronized String getAccessToken() {
//         long now = System.currentTimeMillis();
//         if (accessToken == null || now >= expiryTimeMillis) {
//             refreshAccessToken();
//         }
//         return accessToken;
//     }

//     private void refreshAccessToken() {

//         // === 1) Get RSA key ===
//         HttpHeaders keyHeaders = new HttpHeaders();
//         keyHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
//         keyHeaders.set("Origin", "https://led-cloud.com");
//         keyHeaders.set("Referer", "https://led-cloud.com/");

//         HttpEntity<Void> keyRequest = new HttpEntity<>(keyHeaders);

//         ResponseEntity<RsaKeyResponse> keyResponse =
//                 restTemplate.exchange(KEY_URL, HttpMethod.POST, keyRequest, RsaKeyResponse.class);

//         if (keyResponse.getBody() == null || keyResponse.getBody().getData() == null) {
//             throw new RuntimeException("Failed to fetch RSA key from LED-Cloud");
//         }

//         String base64Key = keyResponse.getBody().getData();
//         System.out.println("RSA key from LED-Cloud: " + base64Key.substring(0, 30) + "...");

//         // === 2) Encrypt password with that key ===
//         String encryptedPassword = RsaUtil.encryptPassword(password, base64Key);
//         System.out.println("Encrypted password (first 30 chars): " +
//                 encryptedPassword.substring(0, 30) + "...");

//         // === 3) Call /oauth/token (grant_type=password) ===
//         HttpHeaders tokenHeaders = new HttpHeaders();
//         tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//         tokenHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
//         tokenHeaders.set("Origin", "https://led-cloud.com");
//         tokenHeaders.set("Referer", "https://led-cloud.com/");

//         MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
//         form.add("client_id", "dms-browser2");
//         form.add("client_secret", "weAfA/sT9ql7ael7+R43+FP9tZnCKObrzIUnGUmZSQUEzl3STHJ1LYgrv6M6ZhguCmolwpw0vk8hQtcHH6/v0bh1AtvlRrL8Ar9awRiqbdUnrxzPklDVNtLlenHNHk/Lk6u52oMPy6bJ456a8O3uu6r+dlNA47/s6fQDxSx/sVQ=");
//         form.add("grant_type", "password");
//         form.add("scope", "all");
//         form.add("username", username);
//         form.add("password", encryptedPassword);
//         form.add("token_type", "bearer");

//         HttpEntity<MultiValueMap<String, String>> tokenRequest =
//                 new HttpEntity<>(form, tokenHeaders);

//         ResponseEntity<TokenResponse> tokenResponse =
//                 restTemplate.postForEntity(TOKEN_URL, tokenRequest, TokenResponse.class);

//         TokenResponse body = tokenResponse.getBody();
//         if (body == null || body.access_token == null) {
//             throw new RuntimeException("Failed to obtain access token from LED-Cloud. Response: " + tokenResponse);
//         }

//         this.accessToken = body.access_token;
//         this.expiryTimeMillis = System.currentTimeMillis() + (body.expires_in - 60L) * 1000L;

//         System.out.println("✅ New LED-Cloud access token acquired, expires in " + body.expires_in + " seconds");
//     }
// }
