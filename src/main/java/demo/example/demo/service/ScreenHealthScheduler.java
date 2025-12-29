package demo.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScreenHealthScheduler {

    private final ScreenHealthService screenHealthService;

    public ScreenHealthScheduler(ScreenHealthService screenHealthService) {
        this.screenHealthService = screenHealthService;
    }

    @Scheduled(fixedRate = 300_000)
    public void autoCheckScreens() {
        screenHealthService.checkScreensAndSendAlerts();
        System.out.println("🔄 Screen health check executed");
    }
}
