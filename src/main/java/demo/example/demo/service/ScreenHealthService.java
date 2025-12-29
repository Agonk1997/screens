package demo.example.demo.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.stereotype.Service;

import demo.example.demo.dto.ScreenHealthDto;
import demo.example.demo.repositories.EventLogRepository;

@Service
public class ScreenHealthService {

    private final EventLogRepository eventLogRepository;
    private final DiscordAlertService discordAlertService;
    private final SlackAlertService slackAlertService;

    // How long a screen can be silent before considered DOWN
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    // Keeps last known state to avoid alert spam
    private final Map<Integer, String> lastKnownStatus = new HashMap<>();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ScreenHealthService(
            EventLogRepository eventLogRepository,
            DiscordAlertService discordAlertService,
            SlackAlertService slackAlertService
    ) {
        this.eventLogRepository = eventLogRepository;
        this.discordAlertService = discordAlertService;
        this.slackAlertService = slackAlertService;
    }

    /**
     * Method used by REST controller
     */
    public List<ScreenHealthDto> getScreenHealth() {
        return checkScreensAndSendAlerts();
    }

    /**
     * Core logic:
     * - checks last seen time per screen
     * - determines OK / DOWN
     * - sends alerts only on state change
     */
    public List<ScreenHealthDto> checkScreensAndSendAlerts() {

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rows = eventLogRepository.findLastSeenForScreensFixed();

        List<ScreenHealthDto> result = new ArrayList<>();
        List<String> downScreens = new ArrayList<>();
        List<String> recoveredScreens = new ArrayList<>();

        for (Object[] r : rows) {

            Integer screenId = ((Number) r[0]).intValue();
            String screenName = (String) r[1];

            Object raw = r[2];
            LocalDateTime lastSeen = null;

            if (raw instanceof Timestamp ts) {
                lastSeen = ts.toLocalDateTime();
            }

            // Determine status
            String status;
            if (lastSeen == null) {
                status = "DOWN";
            } else if (Duration.between(lastSeen, now).compareTo(TIMEOUT) > 0) {
                status = "DOWN";
            } else {
                status = "OK";
            }

            String previousStatus = lastKnownStatus.get(screenId);

            boolean statusChanged =
                    previousStatus == null || !status.equals(previousStatus);

            if (statusChanged) {
                if ("DOWN".equals(status)) {
                    String formattedTime = lastSeen != null
                            ? lastSeen.format(TIME_FORMATTER)
                            : "Never";
                    downScreens.add(screenName + " - " + formattedTime);
                }

                if ("OK".equals(status) && "DOWN".equals(previousStatus)) {
                    recoveredScreens.add(screenName);
                }
            }

            lastKnownStatus.put(screenId, status);

            result.add(new ScreenHealthDto(
                    screenId,
                    screenName,
                    lastSeen,
                    status
            ));
        }

        sendConsolidatedAlerts(downScreens, recoveredScreens);

        return result;
    }

    // ---------------- ALERT SENDING ----------------

    private void sendConsolidatedAlerts(List<String> downScreens,
                                        List<String> recoveredScreens) {

        if (!downScreens.isEmpty()) {
            discordAlertService.sendMessage(formatDiscordDownAlert(downScreens));
            slackAlertService.sendMessage(formatSlackDownAlert(downScreens));
        }

        if (!recoveredScreens.isEmpty()) {
            discordAlertService.sendMessage(formatDiscordRecoveredAlert(recoveredScreens));
            slackAlertService.sendMessage(formatSlackRecoveredAlert(recoveredScreens));
        }
    }

    // ---------------- MESSAGE FORMATTERS ----------------

    private String formatDiscordDownAlert(List<String> downScreens) {
        StringBuilder message = new StringBuilder();
        message.append("⚠ **SCREENS DOWN** (")
               .append(downScreens.size())
               .append(")\n\n");

        for (String screen : downScreens) {
            message.append("• ").append(screen).append("\n");
        }

        return message.toString();
    }

    private String formatDiscordRecoveredAlert(List<String> recoveredScreens) {
        StringBuilder message = new StringBuilder();
        message.append("✅ **SCREENS RECOVERED** (")
               .append(recoveredScreens.size())
               .append(")\n\n");

        for (String screen : recoveredScreens) {
            message.append("• ").append(screen).append("\n");
        }

        return message.toString();
    }

    private String formatSlackDownAlert(List<String> downScreens) {
        StringBuilder message = new StringBuilder();
        message.append("⚠️ *SCREENS DOWN* (")
               .append(downScreens.size())
               .append(")\n");

        for (String screen : downScreens) {
            message.append("• ").append(screen).append("\n");
        }

        return message.toString();
    }

    private String formatSlackRecoveredAlert(List<String> recoveredScreens) {
        StringBuilder message = new StringBuilder();
        message.append("✅ *SCREENS RECOVERED* (")
               .append(recoveredScreens.size())
               .append(")\n");

        for (String screen : recoveredScreens) {
            message.append("• ").append(screen).append("\n");
        }

        return message.toString();
    }
}
