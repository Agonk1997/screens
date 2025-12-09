package demo.example.demo.api;

import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdListDto;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.entity.Schedule;
import demo.example.demo.repositories.MediaAssetRepository;
import demo.example.demo.repositories.ScheduleRepository;
import demo.example.demo.service.AdStatsService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/ads-reports")
public class AdStatsRestController {

    private final MediaAssetRepository mediaAssetRepository;
    private final AdStatsService adStatsService;
    private final ScheduleRepository scheduleRepository;

    public AdStatsRestController(
            MediaAssetRepository mediaAssetRepository,
            AdStatsService adStatsService,
            ScheduleRepository scheduleRepository
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.adStatsService = adStatsService;
        this.scheduleRepository = scheduleRepository;
    }

    // =====================================================================
    // 1) LIST ADS (JSON) — SAME URL: GET /ads-reports
    // =====================================================================
    @GetMapping(produces = "application/json")
    public Map<String, List<AdListDto>> listAdsJson() {

        List<MediaAsset> mediaAssets = mediaAssetRepository.findAll();

        List<AdListDto> activeAds = new ArrayList<>();
        List<AdListDto> scheduledAds = new ArrayList<>();
        List<AdListDto> expiredAds = new ArrayList<>();
        List<AdListDto> inactiveAds = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (MediaAsset ad : mediaAssets) {

            Integer id = ad.getId();
            AdListDto dto = new AdListDto(ad);

            // Stats (total plays, total seconds)
            var stats = adStatsService.computeTotalStats(id);
            dto.setTotalPlays(stats.getTotalPlays());
            dto.setTotalSeconds(stats.getTotalSeconds());

            // Company name
            dto.setCompanyname(ad.getCompanyname());

            // Status
            String status = computeAdStatus(id, now);
            dto.setStatus(status);

            // Categorize ads by computed status
            switch (status) {
                case "Active" -> activeAds.add(dto);
                case "Scheduled" -> scheduledAds.add(dto);
                case "Expired" -> expiredAds.add(dto);
                default -> inactiveAds.add(dto);
            }
        }

        // Build JSON response
        Map<String, List<AdListDto>> response = new HashMap<>();
        response.put("active", activeAds);
        response.put("scheduled", scheduledAds);
        response.put("expired", expiredAds);
        response.put("inactive", inactiveAds);

        return response;
    }

    // =====================================================================
    // 2) FULL AD STATS (JSON) — SAME URL: GET /ads-reports/{id}/stats
    // =====================================================================
    @GetMapping(value = "/{id}/stats", produces = "application/json")
    public Map<String, Object> adStatsJson(
            @PathVariable Integer id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
    ) {

        MediaAsset ad = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate refDay = (day != null ? day : LocalDate.now());

        AdGlobalStats global = adStatsService.computeTotalStats(id);
        global.setAdName(ad.getName());
        global.setCompanyname(ad.getCompanyname());

        Map<String, Object> result = new HashMap<>();
        result.put("ad", ad);
        result.put("companyname", ad.getCompanyname());
        result.put("lifetime", global);

        // Day stats
        result.put("dayStats", adStatsService.computeStatsForRange(id, refDay, refDay));

        // Week stats
        LocalDate startW = refDay.with(DayOfWeek.MONDAY);
        LocalDate endW = refDay.with(DayOfWeek.SUNDAY);
        result.put("weekStats", adStatsService.computeStatsForRange(id, startW, endW));

        // Month stats
        var ym = YearMonth.from(refDay);
        result.put("monthStats",
                adStatsService.computeStatsForRange(id, ym.atDay(1), ym.atEndOfMonth()));

        return result;
    }

    // =====================================================================
    // 3) COMPUTE STATUS (same logic used in your Thymeleaf UI)
    // =====================================================================
    private String computeAdStatus(Integer adId, LocalDateTime now) {

        List<Schedule> schedules = scheduleRepository.findByMediaAssetId(adId);

        if (schedules == null || schedules.isEmpty()) {
            return "Inactive";
        }

        boolean hasFuture = false;
        boolean hasPast = false;

        LocalDate today = now.toLocalDate();
        LocalTime timeNow = now.toLocalTime();

        for (Schedule s : schedules) {

            LocalDate fromDate = s.getFromdate();
            LocalDate toDate = s.getTodate();
            LocalTime fromTime = s.getFromtime();
            LocalTime toTime = s.getTotime();

            boolean dateOk =
                    (fromDate == null || !today.isBefore(fromDate)) &&
                    (toDate == null || !today.isAfter(toDate));

            boolean timeOk =
                    (fromTime == null || !timeNow.isBefore(fromTime)) &&
                    (toTime == null || !timeNow.isAfter(toTime));

            if (dateOk && timeOk) {
                return "Active";
            }

            if (fromDate != null && fromDate.isAfter(today)) {
                hasFuture = true;
            }

            if (toDate != null && toDate.isBefore(today)) {
                hasPast = true;
            }
        }

        if (hasFuture) return "Scheduled";
        if (hasPast) return "Expired";

        return "Inactive";
    }
}
