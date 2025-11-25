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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AdStatsService {

    private final ScheduleRepository scheduleRepository;

    // 🛑 CRITICAL: Placeholder for the actual play interval (e.g., 10 minutes between plays).
    private static final long DEFAULT_PLAY_INTERVAL_SECONDS = 10 * 60L; 

    public AdStatsService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // =====================================================================================
    //                                LIFETIME STATS (Debugging Added)
    // =====================================================================================
    /**
     * Lifetime stats for an ad across all schedules & screens.
     */
    public AdGlobalStats computeTotalStats(Integer adId) {
        List<Schedule> schedules = scheduleRepository.findByMediaAsset_Id(adId);

        if (schedules.isEmpty()) {
             System.out.println("DEBUG: No schedules found for adId " + adId);
            return new AdGlobalStats(adId, null, 0L, 0L, List.of());
        }

        MediaAsset ad = schedules.get(0).getMediaAsset();
        String adName = ad != null ? ad.getName() : null;
        long adDurationSeconds = getAdDurationSeconds(ad);
        
        System.out.println("DEBUG: Ad Duration Seconds: " + adDurationSeconds + " | Interval: " + DEFAULT_PLAY_INTERVAL_SECONDS + "s");

        if (adDurationSeconds <= 0) {
            System.out.println("DEBUG: Ad duration is zero or less. Exiting.");
            return new AdGlobalStats(adId, adName, 0L, 0L, List.of());
        }

        Map<Integer, AdPerScreenStats> perScreenMap = new LinkedHashMap<>();
        long totalPlays = 0L;
        long totalSeconds = 0L;

        for (Schedule s : schedules) {
            
            // Check 1: Schedule Dates (NULL check)
            if (s.getFromdate() == null || s.getTodate() == null) {
                System.out.println("DEBUG: Schedule " + s.getId() + " skipped: Missing fromdate/todate.");
                continue;
            }

            LocalDate start = s.getFromdate();
            LocalDate end = s.getTodate();
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            
            // Check 2: Active Days (<= 0 check)
            if (days <= 0) {
                System.out.println("DEBUG: Schedule " + s.getId() + " skipped: Days (" + days + ") <= 0.");
                continue;
            }
            
            long playsPerDay = computePlaysPerDay(s);

            // Check 3: Plays Per Day
            if (playsPerDay <= 0) {
                System.out.println("DEBUG: Schedule " + s.getId() + " skipped: PlaysPerDay (" + playsPerDay + ") <= 0.");
                continue;
            }

            long plays = playsPerDay * days;
            long seconds = plays * adDurationSeconds;
            
            Screen screen = s.getScreen();
            Integer screenId = (screen != null ? screen.getId() : null);
            String screenName = (screen != null ? screen.getName() : "(unknown)");
            
            System.out.println("DEBUG: Schedule " + s.getId() + 
                               " | Days: " + days + 
                               " | Plays/Day: " + playsPerDay + 
                               " | Plays: " + plays + 
                               " | Seconds: " + seconds + 
                               " | Screen: " + screenName + " (" + screenId + ")");

            AdPerScreenStats row = perScreenMap.get(screenId);
            if (row == null) {
                row = new AdPerScreenStats(screenId, screenName, 0L, 0L);
                perScreenMap.put(screenId, row);
            }
            row.add(plays, seconds);

            totalPlays += plays;
            totalSeconds += seconds;
        }

        System.out.println("DEBUG: FINAL TOTALS: Plays=" + totalPlays + ", Seconds=" + totalSeconds);
        return new AdGlobalStats(adId, adName, totalPlays, totalSeconds, new ArrayList<>(perScreenMap.values()));
    }

    // =====================================================================================
    //                                RANGE STATS (Debugging Added)
    // =====================================================================================
    /**
     * Stats for a specific date range (day, week, month, etc.).
     */
    public List<AdPerScreenStats> computeStatsForRange(Integer adId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            System.out.println("DEBUG: Invalid range dates provided.");
            return List.of();
        }

        List<Schedule> schedules = scheduleRepository.findByMediaAssetIdAndDateRange(adId, start, end);
        if (schedules.isEmpty()) {
            System.out.println("DEBUG: No schedules found in date range for adId " + adId);
            return List.of();
        }

        MediaAsset ad = schedules.get(0).getMediaAsset();
        long adDurationSeconds = getAdDurationSeconds(ad);
        if (adDurationSeconds <= 0) {
            return List.of();
        }

        Map<Integer, AdPerScreenStats> perScreenMap = new LinkedHashMap<>();

        for (Schedule s : schedules) {
            if (s.getFromdate() == null || s.getTodate() == null) {
                continue;
            }

            LocalDate effStart = s.getFromdate().isAfter(start) ? s.getFromdate() : start;
            LocalDate effEnd = s.getTodate().isBefore(end) ? s.getTodate() : end;

            long days = ChronoUnit.DAYS.between(effStart, effEnd) + 1;
            if (days <= 0) {
                continue;
            }

            long playsPerDay = computePlaysPerDay(s);
            if (playsPerDay <= 0) {
                continue;
            }

            long plays = playsPerDay * days;
            long seconds = plays * adDurationSeconds;
            
            System.out.println("DEBUG Range: Schedule " + s.getId() + 
                               " | EffDays: " + days + 
                               " | Plays: " + plays + 
                               " | Seconds: " + seconds);

            Screen screen = s.getScreen();
            Integer screenId = (screen != null ? screen.getId() : null);
            String screenName = (screen != null ? screen.getName() : "(unknown)");

            AdPerScreenStats row = perScreenMap.get(screenId);
            if (row == null) {
                row = new AdPerScreenStats(screenId, screenName, 0L, 0L);
                perScreenMap.put(screenId, row);
            }
            row.add(plays, seconds);
        }

        return new ArrayList<>(perScreenMap.values());
    }

    // ===== Helpers =====

    private long getAdDurationSeconds(MediaAsset ad) {
        if (ad == null || ad.getDuration() == null) {
            return 0L;
        }
        return Math.round(ad.getDuration());
    }

    /**
     * Calculates the expected number of plays per day for a schedule based on 
     * its daily time window and the fixed play interval.
     */
    private long computePlaysPerDay(Schedule s) {
        LocalTime from = s.getFromtime();
        LocalTime to = s.getTotime();

        if (from == null || to == null) {
            long defaultPlays = (24L * 60L * 60L) / DEFAULT_PLAY_INTERVAL_SECONDS;
            return defaultPlays;
        }

        long windowSeconds = Duration.between(from, to).getSeconds();
        if (windowSeconds < 0) {
            windowSeconds += 24L * 60L * 60L;
        }
        
        // DEBUG: Print the calculated active window
        System.out.println("DEBUG plays/day: Sched " + s.getId() + " | WindowSeconds: " + windowSeconds);

        if (windowSeconds <= 0) {
            return 0L;
        }

        long plays = windowSeconds / DEFAULT_PLAY_INTERVAL_SECONDS;
        
        // FIX for the "Still 0" bug (guaranteed minimum 1 play per active day)
        if (plays == 0) {
            // DEBUG: Print when the minimum play is applied
            System.out.println("DEBUG plays/day: Sched " + s.getId() + " Calculated 0, returning MINIMUM 1 play.");
            return 1L; 
        }
        
        return plays;
    }
}