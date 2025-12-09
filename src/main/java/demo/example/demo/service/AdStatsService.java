package demo.example.demo.service;

import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.dto.AdStatsDto;
import demo.example.demo.entity.EventLog;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.entity.Screen;
import demo.example.demo.repositories.EventLogRepository;
import demo.example.demo.repositories.MediaAssetRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class AdStatsService {

    private final EventLogRepository eventLogRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public AdStatsService(EventLogRepository eventLogRepository,
                          MediaAssetRepository mediaAssetRepository) {
        this.eventLogRepository = eventLogRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    // ============================================================
    // Helper for native SQL stats conversion
    // ============================================================

    private List<AdStatsDto> convertNativeRows(List<Object[]> rows) {
        List<AdStatsDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            Long adId    = ((Number) r[0]).longValue();
            Long scrId   = ((Number) r[1]).longValue();
            Long seconds = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            Long plays   = r[3] != null ? ((Number) r[3]).longValue() : 0L;

            list.add(new AdStatsDto(adId, scrId, seconds, plays));
        }
        return list;
    }

    // ============================================================
    // DAILY / WEEKLY / MONTHLY STATS (native query)
    // ============================================================

    public List<AdStatsDto> getDailyStats(LocalDate day, Long screenId, String apiKey) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to   = day.plusDays(1).atStartOfDay();
        return convertNativeRows(eventLogRepository.getStatsNative(from, to, screenId, apiKey));
    }

    public List<AdStatsDto> getWeeklyStats(LocalDate anyDayInWeek, Long screenId, String apiKey) {
        LocalDate monday = anyDayInWeek.with(java.time.DayOfWeek.MONDAY);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to   = monday.plusWeeks(1).atStartOfDay();
        return convertNativeRows(eventLogRepository.getStatsNative(from, to, screenId, apiKey));
    }

    public List<AdStatsDto> getMonthlyStats(YearMonth month, Long screenId, String apiKey) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to   = month.plusMonths(1).atDay(1).atStartOfDay();
        return convertNativeRows(eventLogRepository.getStatsNative(from, to, screenId, apiKey));
    }

    // ============================================================
    // FULL LIFETIME EVENT-BASED REPORT
    // adId = mediaAsset.id
    // ============================================================

    public AdGlobalStats computeTotalStats(Integer adId) {

        Long mediaAssetId = adId.longValue();

        // Fetch MediaAsset → for adName + companyName
        MediaAsset asset = mediaAssetRepository.findById(adId)
                .orElse(null);

        String adName = (asset != null ? asset.getName() : null);
        String companyName = (asset != null ? asset.getCompanyname() : null);

        List<EventLog> logs = eventLogRepository.findByMediaAssetId(mediaAssetId);

        if (logs.isEmpty()) {
            AdGlobalStats empty = new AdGlobalStats(adId, adName, 0, 0, List.of(), null, null);
            empty.setCompanyname(companyName);
            return empty;
        }

        long totalPlays = logs.size();
        long totalSeconds = 0L;

        Map<Integer, ScreenAgg> aggMap = new HashMap<>();
        LocalDate lifetimeFrom = null;
        LocalDate lifetimeTo   = null;

        for (EventLog log : logs) {

            LocalDateTime start = log.getStart();
            if (start == null) continue;

            LocalDateTime end = (log.getEnd() != null ? log.getEnd() : start);

            long duration = Duration.between(start, end).getSeconds();
            if (duration < 0) duration = 0;

            totalSeconds += duration;

            LocalDate s = start.toLocalDate();
            LocalDate e = end.toLocalDate();

            if (lifetimeFrom == null || s.isBefore(lifetimeFrom)) lifetimeFrom = s;
            if (lifetimeTo   == null || e.isAfter(lifetimeTo))    lifetimeTo   = e;

            Screen screen = log.getScreen();
            if (screen == null) continue;

            Integer scrId = screen.getId();
            String screenName = screen.getName();

            ScreenAgg agg = aggMap.computeIfAbsent(scrId, id -> {
                ScreenAgg a = new ScreenAgg();
                a.name = screenName;
                return a;
            });

            agg.plays++;
            agg.seconds += duration;
        }

        List<AdPerScreenStats> perScreen = new ArrayList<>();
        for (Map.Entry<Integer, ScreenAgg> entry : aggMap.entrySet()) {
            Integer sid = entry.getKey();
            ScreenAgg a = entry.getValue();

            String sname = (a.name != null && !a.name.isBlank())
                    ? a.name
                    : "Screen " + sid;

            perScreen.add(new AdPerScreenStats(
                    sid,
                    sname,
                    a.plays,
                    a.seconds
            ));
        }

        perScreen.sort(Comparator.comparingLong(AdPerScreenStats::getTotalSeconds).reversed());

        AdGlobalStats result = new AdGlobalStats(
                adId,
                adName,
                totalPlays,
                totalSeconds,
                perScreen,
                lifetimeFrom,
                lifetimeTo
        );

        // Add company name
        result.setCompanyname(companyName);

        return result;
    }

    // ============================================================
    // RANGE REPORT
    // ============================================================

    public List<AdPerScreenStats> computeStatsForRange(Integer adId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to   = toDate.plusDays(1).atStartOfDay();

        Long mediaAssetId = adId.longValue();

        List<EventLog> logs = eventLogRepository
                .findByMediaAssetIdAndStartBetween(mediaAssetId, from, to);

        if (logs.isEmpty()) return List.of();

        Map<Integer, ScreenAgg> aggMap = new HashMap<>();

        for (EventLog log : logs) {

            LocalDateTime start = log.getStart();
            if (start == null) continue;

            LocalDateTime end = (log.getEnd() != null ? log.getEnd() : start);

            long duration = Duration.between(start, end).getSeconds();
            if (duration < 0) duration = 0;

            Screen screen = log.getScreen();
            if (screen == null) continue;

            Integer scrId = screen.getId();
            String screenName = screen.getName();

            ScreenAgg agg = aggMap.computeIfAbsent(scrId, id -> {
                ScreenAgg a = new ScreenAgg();
                a.name = screenName;
                return a;
            });

            agg.plays++;
            agg.seconds += duration;
        }

        List<AdPerScreenStats> perScreen = new ArrayList<>();
        for (Map.Entry<Integer, ScreenAgg> entry : aggMap.entrySet()) {
            Integer sid = entry.getKey();
            ScreenAgg a = entry.getValue();

            String sname = (a.name != null && !a.name.isBlank())
                    ? a.name
                    : "Screen " + sid;

            perScreen.add(new AdPerScreenStats(
                    sid,
                    sname,
                    a.plays,
                    a.seconds
            ));
        }

        perScreen.sort(Comparator.comparingLong(AdPerScreenStats::getTotalSeconds).reversed());

        return perScreen;
    }

    // helper
    private static class ScreenAgg {
        long plays = 0;
        long seconds = 0;
        String name;
    }
}
