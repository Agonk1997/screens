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
    
    // --- SYSTEM AD CONSTANTS ---
    private static final int SYSTEM_AD_BUFFER_SECONDS = 20; // 2 * 10 seconds
    private static final int PARKING_AD_ID = 998;
    private static final String PARKING_AD_NAME = "Parking Ad (System)";
    private static final int WEATHER_AD_ID = 999;
    private static final String WEATHER_AD_NAME = "Weather Ad (System)";
    private static final Set<Integer> SYSTEM_AD_IDS = Set.of(PARKING_AD_ID, WEATHER_AD_ID);
    private static final int SYSTEM_AD_DURATION = 10; // 10 seconds per system ad
    // ---------------------------

    public AdStatsService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // =====================================================================================
    //                                LIFETIME STATS (Updated)
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
        
        System.out.println("DEBUG: Ad Duration Seconds: " + adDurationSeconds + " | System Buffer: " + SYSTEM_AD_BUFFER_SECONDS + "s");

        if (adDurationSeconds <= 0) {
             System.out.println("DEBUG: Ad duration is zero or less. Exiting.");
             return new AdGlobalStats(adId, adName, 0L, 0L, List.of());
        }

        Map<Integer, AdPerScreenStats> perScreenMap = new LinkedHashMap<>();
        long totalScheduledPlaysX = 0L; // Multiplier X for system ads

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
            
            // Plays per day calculated using AD DURATION + SYSTEM BUFFER
            long playsPerDay = computePlaysPerDay(s, adDurationSeconds);

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
            
            // Accumulate the multiplier (X) for the system ads
            totalScheduledPlaysX += plays; 
        }

        // Add System Ad Statistics
        List<AdPerScreenStats> finalStats = addSystemAdStats(new ArrayList<>(perScreenMap.values()), totalScheduledPlaysX);
        
        // Recalculate global totals including system ads
        long totalPlays = finalStats.stream().mapToLong(AdPerScreenStats::getPlays).sum();
        long totalSeconds = finalStats.stream().mapToLong(AdPerScreenStats::getTotalSeconds).sum();
        
        System.out.println("DEBUG: FINAL TOTALS (Including System Ads): Plays=" + totalPlays + ", Seconds=" + totalSeconds);
        return new AdGlobalStats(adId, adName, totalPlays, totalSeconds, finalStats);
    }

    // =====================================================================================
    //                                RANGE STATS (Updated)
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
        long totalScheduledPlaysX = 0L; // Multiplier X for system ads

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

            // Plays per day calculated using AD DURATION + SYSTEM BUFFER
            long playsPerDay = computePlaysPerDay(s, adDurationSeconds);
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
            
            // Accumulate the multiplier (X) for the system ads
            totalScheduledPlaysX += plays;
        }

        // Add System Ad Statistics
        return addSystemAdStats(new ArrayList<>(perScreenMap.values()), totalScheduledPlaysX);
    }

    // ===== System Ad Helper =====
    
    /**
     * Synthesizes AdPerScreenStats for the Parking and Weather ads 
     * using the total Scheduled Plays (X) as the multiplier.
     */
    private List<AdPerScreenStats> addSystemAdStats(List<AdPerScreenStats> existingStats, long totalScheduledPlaysX) {
        
        if (totalScheduledPlaysX == 0) {
            return existingStats;
        }

        List<AdPerScreenStats> finalStats = new ArrayList<>(existingStats);

        // 1. Synthesize Parking Ad Stats
        AdPerScreenStats parkingStats = new AdPerScreenStats(
            null, // Use null ID for a global system ad summary
            PARKING_AD_NAME, 
            totalScheduledPlaysX, 
            totalScheduledPlaysX * SYSTEM_AD_DURATION
        );
        
        // 2. Synthesize Weather Ad Stats
        AdPerScreenStats weatherStats = new AdPerScreenStats(
            null, // Use null ID for a global system ad summary
            WEATHER_AD_NAME, 
            totalScheduledPlaysX, 
            totalScheduledPlaysX * SYSTEM_AD_DURATION
        );
        
        finalStats.add(parkingStats);
        finalStats.add(weatherStats);
        
        return finalStats;
    }

    // ===== Utility Helpers (Updated Logic) =====

    private long getAdDurationSeconds(MediaAsset ad) {
        if (ad == null || ad.getDuration() == null) {
            return 0L;
        }
        return Math.round(ad.getDuration());
    }

    /**
     * Calculates the expected number of plays per day for a schedule based on 
     * its daily time window and the dynamic Total Loop Time (Ad Duration + Buffer).
     * * @param s The schedule object.
     * @param adDurationSeconds The duration of the scheduled ad.
     * @return The expected play count per day.
     */
    private long computePlaysPerDay(Schedule s, long adDurationSeconds) {
        
        // The total time consumed by one instance of this ad and its fills.
        // Total Loop Time = Ad Duration + 20s System Buffer
        long totalLoopTime = adDurationSeconds + SYSTEM_AD_BUFFER_SECONDS;

        if (totalLoopTime <= 0) {
            return 0L;
        }
        
        LocalTime from = s.getFromtime();
        LocalTime to = s.getTotime();

        long windowSeconds;
        
        if (from == null || to == null) {
            // Assume 24 hours (86,400 seconds) if no time window is defined
            windowSeconds = 24L * 60L * 60L;
        } else {
            windowSeconds = Duration.between(from, to).getSeconds();
            if (windowSeconds < 0) {
                // Handle midnight crossover (e.g., 23:00 to 01:00)
                windowSeconds += 24L * 60L * 60L;
            }
        }
        
        // DEBUG: Print the calculated active window
        System.out.println("DEBUG plays/day: Sched " + s.getId() + 
                           " | Ad+Buffer Loop Time: " + totalLoopTime + "s" + 
                           " | WindowSeconds: " + windowSeconds);

        if (windowSeconds <= 0) {
            return 0L;
        }

        // Calculation: Divide the active window by the total time consumed per loop.
        long plays = windowSeconds / totalLoopTime;
        
        // FIX for the "Still 0" bug (guaranteed minimum 1 play per active day)
        if (plays == 0) {
            // DEBUG: Print when the minimum play is applied
            System.out.println("DEBUG plays/day: Sched " + s.getId() + " Calculated 0, returning MINIMUM 1 play.");
            return 1L; 
        }
        
        return plays;
    }
}