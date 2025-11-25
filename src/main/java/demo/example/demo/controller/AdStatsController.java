package demo.example.demo.controller;

import demo.example.demo.dto.AdGlobalStats;
import demo.example.demo.dto.AdPerScreenStats;
import demo.example.demo.entity.MediaAsset;
import demo.example.demo.repositories.MediaAssetRepository;
import demo.example.demo.service.AdStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;

@Controller
@RequestMapping("/ads")
public class AdStatsController {

    private final MediaAssetRepository mediaAssetRepository;
    private final AdStatsService adStatsService;

    public AdStatsController(MediaAssetRepository mediaAssetRepository,
                             AdStatsService adStatsService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.adStatsService = adStatsService;
    }

    @GetMapping("/{id}/stats")
    public String adStats(@PathVariable Integer id,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                          LocalDate day,
                          Model model) {

        MediaAsset ad = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDate refDay = (day != null ? day : LocalDate.now());

        // ---- GLOBAL (lifetime) stats ----
        AdGlobalStats global = adStatsService.computeTotalStats(id);
        if (global == null) {
            // Safety: ensure 'global' is never null for Thymeleaf
            global = new AdGlobalStats(id, ad.getName(), 0L, 0L, java.util.List.of());
        }

        // ---- Day stats ----
        java.util.List<AdPerScreenStats> dayStats =
                adStatsService.computeStatsForRange(id, refDay, refDay);

        // ---- Week stats ----
        LocalDate weekStart = refDay.with(DayOfWeek.MONDAY);
        LocalDate weekEnd  = refDay.with(DayOfWeek.SUNDAY);
        java.util.List<AdPerScreenStats> weekStats =
                adStatsService.computeStatsForRange(id, weekStart, weekEnd);

        // ---- Month stats ----
        YearMonth ym = YearMonth.from(refDay);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        java.util.List<AdPerScreenStats> monthStats =
                adStatsService.computeStatsForRange(id, monthStart, monthEnd);

        model.addAttribute("ad", ad);
        model.addAttribute("refDay", refDay);

        // IMPORTANT: this is what your template is using
        model.addAttribute("global", global);

        model.addAttribute("dayStats", dayStats);
        model.addAttribute("weekStats", weekStats);
        model.addAttribute("monthStats", monthStats);

        // Template expected path: /src/main/resources/templates/ads/stats.html
        return "ads/stats";
    }
    
    @GetMapping
    public String listAds(Model model) {
       List<MediaAsset> ads = mediaAssetRepository.findAll();
       model.addAttribute("ads", ads);
       return "ads/list";
    }

    @GetMapping("/{id}/stats/debug")
    @ResponseBody
    public String debug(@PathVariable Integer id) {
        AdGlobalStats global = adStatsService.computeTotalStats(id);
        return "Ad " + id +
                " -> totalPlays=" + (global != null ? global.getTotalPlays() : "N/A") +
                ", totalSeconds=" + (global != null ? global.getTotalSeconds() : "N/A");
    }

}