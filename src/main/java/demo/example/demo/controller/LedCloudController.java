// package demo.example.demo.controller;

// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import demo.example.demo.service.LedCloudCommandService;
// import demo.example.demo.service.LedCloudDeviceService;

// @RestController
// @RequestMapping("/led")
// public class LedCloudController {

//     private final LedCloudCommandService commandService;
//     private final LedCloudDeviceService deviceService;

//     public LedCloudController(LedCloudCommandService commandService,
//                               LedCloudDeviceService deviceService) {
//         this.commandService = commandService;
//         this.deviceService = deviceService;
//     }

//     @PostMapping("/reboot/{deviceId}")
//     public String reboot(@PathVariable String deviceId) {
//         commandService.sendReboot(deviceId);
//         return "Reboot command sent for device " + deviceId;
//     }

//     @GetMapping("/devices")
//     public Object listDevices() {
//         return deviceService.getDevices();
//     }
// }
