// package demo.example.demo.controller;

// import demo.example.demo.service.LedCloudAuthService;
// import demo.example.demo.service.LedCloudCommandService;
// import org.springframework.web.bind.annotation.*;

// @RestController
// @RequestMapping("/test/ledcloud")
// public class LedCloudTestController {

//     private final LedCloudAuthService authService;
//     private final LedCloudCommandService cmdService;

//     public LedCloudTestController(LedCloudAuthService authService,
//                                   LedCloudCommandService cmdService) {
//         this.authService = authService;
//         this.cmdService = cmdService;
//     }

//     @GetMapping("/token")
//     public String testToken() {
//         String token = authService.getAccessToken();
//         return "Token: " + token;
//     }

//     @PostMapping("/reboot/{deviceId}")
//     public String testReboot(@PathVariable String deviceId) {
//         cmdService.sendReboot(deviceId);
//         return "Reboot sent to " + deviceId;
//     }
// }
