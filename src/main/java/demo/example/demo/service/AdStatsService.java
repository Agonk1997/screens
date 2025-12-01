package demo.example.demo.service;

import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.entity.Schedule;
import demo.example.demo.entity.Screen;
import demo.example.demo.repositories.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdStatsService {

    private final ScheduleRepository scheduleRepository;

    // --- SYSTEM AD CONSTANTS ---
    private static final int SYSTEM_AD_DURATION = 10; // 10 seconds duration per system ad
    private static final int SYSTEM_AD_COUNT = 2;     // two static system ads
    // ---------------------------

    public AdStatsService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // =====================================================================================
    //                                LIFETIME STATS
    // =====================================================================================
    public AdGlobalStats computeTotalStats(Integer adId) {
        // All schedules for this ad across all time
        List<Schedule> allSchedulesForAd = scheduleRepository.findByMediaAsset_Id(adId);

        if (allSchedulesForAd.isEmpty()) {
            return new AdGlobalStats(adId, null, 0L, 0L, List.of());
        }

        // Basic ad info (name only for display)
        MediaAsset ad = allSchedulesForAd.get(0).getMediaAsset();
        String adName = ad != null ? ad.getName() : null;

        // Group schedules by screen
        Map<Integer, List<Schedule>> schedulesByScreen = allSchedulesForAd.stream()
                .filter(s -> s.getScreen() != null)
                .collect(Collectors.groupingBy(s -> s.getScreen().getId()));

        Map<Integer, AdPerScreenStats> perScreenMap = new LinkedHashMap<>();

        // Lifetime date range for this ad (min fromdate, max todate)
        LocalDate overallStart = allSchedulesForAd.stream()
                .map(Schedule::getFromdate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate overallEnd = allSchedulesForAd.stream()
                .map(Schedule::getTodate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (overallStart == null || overallEnd == null) {
            return new AdGlobalStats(adId, adName, 0L, 0L, List.of());
        }

        for (Map.Entry<Integer, List<Schedule>> entry : schedulesByScreen.entrySet()) {
            List<Schedule> screenSchedulesForThisAd = entry.getValue();

            // Calculate plays for this ad on this screen over its lifetime
            long plays = calculatePlaysPerScreen(screenSchedulesForThisAd, overallStart, overallEnd);

            // Use the duration stored in schedule table for this ad (assume consistent per ad)
            double adDurationSeconds = getAdDurationFromSchedules(screenSchedulesForThisAd);
            long seconds = Math.round(plays * adDurationSeconds);

            Screen screen = screenSchedulesForThisAd.get(0).getScreen();
            String screenName = screen != null ? screen.getName() : "(unknown)";

            AdPerScreenStats row = new AdPerScreenStats(screen.getId(), screenName, plays, seconds);
            perScreenMap.put(screen.getId(), row);
        }

        List<AdPerScreenStats> finalStats = new ArrayList<>(perScreenMap.values());
        long totalPlays = finalStats.stream().mapToLong(AdPerScreenStats::getPlays).sum();
        long totalSeconds = finalStats.stream().mapToLong(AdPerScreenStats::getTotalSeconds).sum();

        return new AdGlobalStats(adId, adName, totalPlays, totalSeconds, finalStats);
    }

    // =====================================================================================
    //                                RANGE STATS (Monthly / Custom)
    // =====================================================================================
    public List<AdPerScreenStats> computeStatsForRange(Integer adId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return List.of();
        }

        // Schedules for THIS AD that overlap the requested range
        List<Schedule> allSchedulesForAd = scheduleRepository.findByMediaAssetIdAndDateRange(adId, start, end);
        if (allSchedulesForAd.isEmpty()) {
            return List.of();
        }

        // Group schedules by screen
        Map<Integer, List<Schedule>> schedulesByScreen = allSchedulesForAd.stream()
                .filter(s -> s.getScreen() != null)
                .collect(Collectors.groupingBy(s -> s.getScreen().getId()));

        Map<Integer, AdPerScreenStats> perScreenMap = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<Schedule>> entry : schedulesByScreen.entrySet()) {
            List<Schedule> screenSchedulesForThisAd = entry.getValue();

            long plays = calculatePlaysPerScreen(screenSchedulesForThisAd, start, end);

            double adDurationSeconds = getAdDurationFromSchedules(screenSchedulesForThisAd);
            long seconds = Math.round(plays * adDurationSeconds);

            Screen screen = screenSchedulesForThisAd.get(0).getScreen();
            String screenName = screen != null ? screen.getName() : "(unknown)";

            AdPerScreenStats row = new AdPerScreenStats(screen.getId(), screenName, plays, seconds);
            perScreenMap.put(screen.getId(), row);
        }

        return new ArrayList<>(perScreenMap.values());
    }

    // =====================================================================================
    //                            CORE CAPACITY CALCULATOR
    // =====================================================================================

    /**
     * Calculates the total number of plays of THIS AD on a given screen within a specific date range.
     *
     * For each schedule row of this ad on this screen:
     *  - intersect its [fromdate, todate] with [rangeStart, rangeEnd]
     *  - for each day in that intersection:
     *      * get the full playlist for that screen & day
     *      * loopTime = sum(schedule.duration for all ads) + 2 * 10s static ads
     *      * windowSeconds = duration of this schedule's time window (fromtime–totime)
     *      * loopsInThisWindow = windowSeconds / loopTime
     *  - sum loops over all days and all schedule rows
     */
    private long calculatePlaysPerScreen(List<Schedule> adSchedulesForScreen,
                                         LocalDate rangeStart,
                                         LocalDate rangeEnd) {
        if (adSchedulesForScreen == null || adSchedulesForScreen.isEmpty()) {
            return 0L;
        }

        Screen screen = adSchedulesForScreen.get(0).getScreen();
        if (screen == null) {
            return 0L;
        }

        Integer screenId = screen.getId();
        long totalPlays = 0L;

        for (Schedule adSchedule : adSchedulesForScreen) {
            // Intersect schedule's date range with requested range
            LocalDate schedStart = maxDate(rangeStart, adSchedule.getFromdate());
            LocalDate schedEnd = minDate(rangeEnd, adSchedule.getTodate());
            if (schedEnd.isBefore(schedStart)) {
                continue;
            }

            // Time window for THIS schedule row
            LocalTime schedFrom = adSchedule.getFromtime() != null ? adSchedule.getFromtime() : LocalTime.MIN;
            LocalTime schedTo = adSchedule.getTotime() != null ? adSchedule.getTotime() : LocalTime.MAX;

            for (LocalDate day = schedStart; !day.isAfter(schedEnd); day = day.plusDays(1)) {
                // Full playlist for this screen and day (ALL ads)
                List<Schedule> playlistSchedules =
                        scheduleRepository.findAllActiveSchedulesByScreenIdAndDate(screenId, day);

                if (playlistSchedules == null || playlistSchedules.isEmpty()) {
                    continue;
                }

                long playlistLoopTimeSeconds = getPlaylistTotalLoopTimeSeconds(playlistSchedules);
                if (playlistLoopTimeSeconds <= 0) {
                    continue;
                }

                // How many loops fit in THIS schedule's time window on that day?
                long loopsInThisWindow = computeLoopsForWindow(schedFrom, schedTo, playlistLoopTimeSeconds);
                if (loopsInThisWindow <= 0) {
                    continue;
                }

                totalPlays += loopsInThisWindow;
            }
        }

        return totalPlays;
    }

    // =====================================================================================
    //                            LOOP / WINDOW HELPERS
    // =====================================================================================

    /**
     * Calculates the total time for one loop of the playlist:
     * loopTime = sum(all client ad durations from schedule.duration) + SYSTEM_AD_COUNT * SYSTEM_AD_DURATION
     */
    private long getPlaylistTotalLoopTimeSeconds(List<Schedule> playlistSchedules) {
        if (playlistSchedules == null || playlistSchedules.isEmpty()) {
            return 0L;
        }

        double clientDurationSeconds = playlistSchedules.stream()
                .map(Schedule::getDuration)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (clientDurationSeconds <= 0) {
            return 0L;
        }

        long staticAdsDurationSeconds = SYSTEM_AD_COUNT * (long) SYSTEM_AD_DURATION;

        return Math.round(clientDurationSeconds + staticAdsDurationSeconds);
    }

    /**
     * How many loops fit into a given time window (from–to) for a given loop length.
     */
    private long computeLoopsForWindow(LocalTime from, LocalTime to, long loopTimeSeconds) {
        if (loopTimeSeconds <= 0) return 0L;

        long windowSeconds = Duration.between(from, to).getSeconds();
        if (windowSeconds < 0) {
            // Handle midnight crossover
            windowSeconds += 24L * 60L * 60L;
        }

        if (windowSeconds <= 0) return 0L;

        return windowSeconds / loopTimeSeconds; // pure integer division, no “force 1 play” here
    }

    // =====================================================================================
    //                            UTILITY HELPERS
    // =====================================================================================

    /**
     * Gets the ad duration (seconds) from schedule rows.
     * Assumes all schedules for this ad use the same duration.
     */
    private double getAdDurationFromSchedules(List<Schedule> schedules) {
        return schedules.stream()
                .map(Schedule::getDuration)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .findFirst()
                .orElse(0.0);
    }

    // still here if you ever need MediaAsset-based duration
    private long getAdDurationSeconds(MediaAsset ad) {
        if (ad == null || ad.getDuration() == null) {
            return 0L;
        }
        return Math.round(ad.getDuration());
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return (a.isAfter(b)) ? a : b;
    }

    private LocalDate minDate(LocalDate a, LocalDate b) {
        return (a.isBefore(b)) ? a : b;
    }
}
