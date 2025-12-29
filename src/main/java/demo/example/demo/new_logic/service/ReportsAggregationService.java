package demo.example.demo.new_logic.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportsAggregationService {

    private final EntityManager em;

    // =====================
    // DAILY (from EventLog)
    // =====================
    @Transactional
    public void aggregateDaily(LocalDate day) {

        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();

        em.createNativeQuery("""
                    INSERT INTO reports_ads (
                        period_type, period_start, period_end,
                        ad_id, screen_id, plays, seconds
                    )
                    SELECT
                        'DAILY',
                        DATE(e.start),
                        DATE(e.start),
                        e.mediaassetid,
                        e.screenid,
                        COUNT(*),
                        SUM(EXTRACT(EPOCH FROM (COALESCE(e."end", e."start") - e."start")))
                    FROM "EventLog" e
                    WHERE e.start >= :from
                      AND e.start <  :to
                      AND e.mediaassetid IS NOT NULL
                      AND e.screenid IS NOT NULL
                    GROUP BY DATE(e.start), e.mediaassetid, e.screenid
                    ON CONFLICT (period_type, period_start, ad_id, screen_id)
                    DO UPDATE SET
                        plays   = EXCLUDED.plays,
                        seconds = EXCLUDED.seconds
                """)

                .setParameter("from", from)
                .setParameter("to", to)
                .executeUpdate();
    }

    // =====================
    // WEEKLY (from DAILY)
    // =====================
    @Transactional
    public void aggregateWeekly(LocalDate from, LocalDate to) {

        em.createNativeQuery("""
                    INSERT INTO reports_ads (
                        period_type, period_start, period_end,
                        ad_id, screen_id, plays, seconds
                    )
                    SELECT
                        'WEEKLY',
                        DATE_TRUNC('week', period_start)::date,
                        (DATE_TRUNC('week', period_start) + INTERVAL '6 days')::date,
                        ad_id,
                        screen_id,
                        SUM(plays),
                        SUM(seconds)
                    FROM reports_ads
                    WHERE period_type = 'DAILY'
                      AND period_start >= :from
                      AND period_start <= :to
                    GROUP BY DATE_TRUNC('week', period_start), ad_id, screen_id
                    ON CONFLICT (period_type, period_start, ad_id, screen_id)
                    DO UPDATE SET
                        plays   = EXCLUDED.plays,
                        seconds = EXCLUDED.seconds
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .executeUpdate();
    }

    // =====================
    // MONTHLY (from DAILY)
    // =====================
    @Transactional
    public void aggregateMonthly(LocalDate from, LocalDate to) {

        em.createNativeQuery("""
                    INSERT INTO reports_ads (
                        period_type, period_start, period_end,
                        ad_id, screen_id, plays, seconds
                    )
                    SELECT
                        'MONTHLY',
                        DATE_TRUNC('month', period_start)::date,
                        (DATE_TRUNC('month', period_start)
                            + INTERVAL '1 month - 1 day')::date,
                        ad_id,
                        screen_id,
                        SUM(plays),
                        SUM(seconds)
                    FROM reports_ads
                    WHERE period_type = 'DAILY'
                      AND period_start >= :from
                      AND period_start <= :to
                    GROUP BY DATE_TRUNC('month', period_start), ad_id, screen_id
                    ON CONFLICT (period_type, period_start, ad_id, screen_id)
                    DO UPDATE SET
                        plays   = EXCLUDED.plays,
                        seconds = EXCLUDED.seconds
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .executeUpdate();
    }
}
