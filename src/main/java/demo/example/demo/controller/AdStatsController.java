package demo.example.demo.controller;

import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.dto.AdListDto;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.entity.Schedule;
import demo.example.demo.repositories.MediaAssetRepository;
import demo.example.demo.repositories.ScheduleRepository;
import demo.example.demo.service.AdStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ads-reports")
public class AdStatsController {

    private final MediaAssetRepository mediaAssetRepository;
    private final AdStatsService adStatsService;
    private final ScheduleRepository scheduleRepository;

    public AdStatsController(MediaAssetRepository mediaAssetRepository,
                             AdStatsService adStatsService,
                             ScheduleRepository scheduleRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.adStatsService = adStatsService;
        this.scheduleRepository = scheduleRepository;
    }

    // ------------------------------------------------------------
    // Stats for a single ad
    // ------------------------------------------------------------
    @GetMapping("/{id}/stats")
    public String adStats(@PathVariable Integer id,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                          LocalDate day,
                          Model model) {

        MediaAsset ad = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate refDay = (day != null ? day : LocalDate.now());

        // EVENT-BASED LIFETIME
        AdGlobalStats global = adStatsService.computeTotalStats(id);
        if (global == null) {
            global = new AdGlobalStats(
                    id,
                    ad.getName(),
                    0L,
                    0L,
                    List.of(),
                    null,
                    null
            );
        } else {
            global.setAdName(ad.getName());
        }

        // DAY
        List<AdPerScreenStats> dayStats =
                adStatsService.computeStatsForRange(id, refDay, refDay);

        // WEEK
        LocalDate startW = refDay.with(DayOfWeek.MONDAY);
        LocalDate endW   = refDay.with(DayOfWeek.SUNDAY);
        List<AdPerScreenStats> weekStats =
                adStatsService.computeStatsForRange(id, startW, endW);

        // MONTH
        YearMonth ym = YearMonth.from(refDay);
        List<AdPerScreenStats> monthStats =
                adStatsService.computeStatsForRange(id, ym.atDay(1), ym.atEndOfMonth());

        model.addAttribute("ad", ad);
        model.addAttribute("refDay", refDay);

        model.addAttribute("global", global);
        model.addAttribute("dayStats", dayStats);
        model.addAttribute("weekStats", weekStats);
        model.addAttribute("monthStats", monthStats);

        // ✅ MAKE COMPANY NAME AVAILABLE TO STATS PAGE
        model.addAttribute("companyName", ad.getCompanyname());

        return "ads/stats";
    }

    // ------------------------------------------------------------
    // List ads grouped by status using AdListDto
    // ------------------------------------------------------------
    @GetMapping
    public String listAds(Model model) {

        List<MediaAsset> mediaAssets = mediaAssetRepository.findAll();

        List<AdListDto> allAds       = new ArrayList<>();
        List<AdListDto> activeAds    = new ArrayList<>();
        List<AdListDto> inactiveAds  = new ArrayList<>();
        List<AdListDto> scheduledAds = new ArrayList<>();
        List<AdListDto> expiredAds   = new ArrayList<>();

        Map<Integer, AdGlobalStats> statsMap = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();

        for (MediaAsset ad : mediaAssets) {

            Integer id = ad.getId();

            // lifetime stats
            AdGlobalStats stats = adStatsService.computeTotalStats(id);
            if (stats != null) {
                statsMap.put(id, stats);
            }

            AdListDto dto = new AdListDto(ad);

            // COPY VALUES
            dto.setTotalPlays(stats != null ? stats.getTotalPlays() : 0L);
            dto.setTotalSeconds(stats != null ? stats.getTotalSeconds() : 0L);

            // ✅ NEW: include companyName in DTO
            dto.setCompanyname(ad.getCompanyname());

            // status ("Active", "Scheduled", "Expired", "Inactive")
            String status = computeAdStatus(id, now);
            dto.setStatus(status);

            allAds.add(dto);

            if ("Active".equals(status)) {
                activeAds.add(dto);
            } else if ("Scheduled".equals(status)) {
                scheduledAds.add(dto);
            } else if ("Expired".equals(status)) {
                expiredAds.add(dto);
            } else {
                inactiveAds.add(dto);
            }
        }

        // main list used by ads/list.html
        model.addAttribute("ads", allAds);
        model.addAttribute("statsMap", statsMap);

        model.addAttribute("activeAds", activeAds);
        model.addAttribute("scheduledAds", scheduledAds);
        model.addAttribute("expiredAds", expiredAds);
        model.addAttribute("inactiveAds", inactiveAds);

        return "ads/list";
    }

    // ------------------------------------------------------------
    // Debug endpoint
    // ------------------------------------------------------------
    @GetMapping("/{id}/stats/debug")
    @ResponseBody
    public String debug(@PathVariable Integer id) {
        AdGlobalStats global = adStatsService.computeTotalStats(id);
        return "Ad " + id + " -> plays=" + global.getTotalPlays() + ", seconds=" + global.getTotalSeconds();
    }

    // ------------------------------------------------------------
    // Helper: compute ad status
    // ------------------------------------------------------------
    private String computeAdStatus(Integer adId, LocalDateTime now) {

        List<Schedule> schedules = scheduleRepository.findByMediaAssetId(adId);

        if (schedules == null || schedules.isEmpty()) {
            return "Inactive";
        }

        boolean hasFuture = false;
        boolean hasPast   = false;

        LocalDate d = now.toLocalDate();
        LocalTime t = now.toLocalTime();

        for (Schedule s : schedules) {

            LocalDate fd = s.getFromdate();
            LocalDate td = s.getTodate();
            LocalTime ft = s.getFromtime();
            LocalTime tt = s.getTotime();

            boolean dateOk =
                    (fd == null || !d.isBefore(fd)) &&
                            (td == null || !d.isAfter(td));

            boolean timeOk =
                    (ft == null || !t.isBefore(ft)) &&
                            (tt == null || !t.isAfter(tt));

            if (dateOk && timeOk) {
                return "Active";
            }

            if (fd != null && fd.isAfter(d)) hasFuture = true;
            if (td != null && td.isBefore(d)) hasPast = true;
        }

        if (hasFuture) return "Scheduled";
        if (hasPast)   return "Expired";

        return "Inactive";
    }
}
