package demo.example.demo.new_logic.scheduler;
import demo.example.demo.new_logic.service.ReportsAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReportsScheduler {

    private final ReportsAggregationService service;

    // DAILY – yesterday
    @Scheduled(cron = "0 5 0 * * *")
    public void daily() {
        service.aggregateDaily(LocalDate.now().minusDays(1));
    }

    // WEEKLY – last 2 weeks (safe for late events)
    @Scheduled(cron = "0 15 0 * * MON")
    public void weekly() {
        service.aggregateWeekly(
                LocalDate.now().minusWeeks(2),
                LocalDate.now().minusDays(1)
        );
    }

    // MONTHLY – last 2 months
    @Scheduled(cron = "0 25 0 1 * *")
    public void monthly() {
        service.aggregateMonthly(
                LocalDate.now().minusMonths(2),
                LocalDate.now().minusDays(1)
        );
    }
}
