package demo.example.demo.new_logic.controller;

import demo.example.demo.new_logic.entity.ReportsAd;
import demo.example.demo.new_logic.repository.ReportsAdRepository;
import demo.example.demo.new_logic.service.ReportsAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsAdRepository repo;
    private final ReportsAggregationService reportsAggregationService;

    @GetMapping("/daily")
    public List<ReportsAd> daily(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return repo.findByPeriodTypeAndPeriodStartBetween("DAILY", from, to);
    }

    @GetMapping("/weekly")
    public List<ReportsAd> weekly(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return repo.findByPeriodTypeAndPeriodStartBetween("WEEKLY", from, to);
    }

    @GetMapping("/monthly")
    public List<ReportsAd> monthly(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return repo.findByPeriodTypeAndPeriodStartBetween("MONTHLY", from, to);
    }

    @GetMapping("/lifetime")
    public List<Object[]> lifetime() {
        return repo.getLifetimeStats();
    }

    @GetMapping("/from-date")
    public List<Object[]> fromDate(@RequestParam LocalDate from) {
        return repo.getStatsFromDate(from);
    }

    // ⚠️ NOTE: fixed path (see below)
    @GetMapping("/run-daily")
    public String runDaily(@RequestParam LocalDate day) {
        reportsAggregationService.aggregateDaily(day);
        return "OK";
    }
    @GetMapping("/run-weekly")
public String runWeekly(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
) {
    reportsAggregationService.aggregateWeekly(from, to);
    return "OK";
}
    @GetMapping("/run-monthly")
    public String runMonthly(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        reportsAggregationService.aggregateMonthly(from, to);
        return "OK";
    }
}
