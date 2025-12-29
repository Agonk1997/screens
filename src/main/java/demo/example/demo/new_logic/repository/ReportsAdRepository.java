package demo.example.demo.new_logic.repository;

import demo.example.demo.new_logic.entity.ReportsAd;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportsAdRepository extends JpaRepository<ReportsAd, Long> {

    List<ReportsAd> findByPeriodTypeAndPeriodStartBetween(
            String periodType,
            LocalDate from,
            LocalDate to
    );

    @Query("""
        SELECT r.adId, r.screenId, SUM(r.plays), SUM(r.seconds)
        FROM ReportsAd r
        WHERE r.periodType = 'DAILY'
        GROUP BY r.adId, r.screenId
    """)
    List<Object[]> getLifetimeStats();

    @Query("""
        SELECT r.adId, r.screenId, SUM(r.plays), SUM(r.seconds)
        FROM ReportsAd r
        WHERE r.periodType = 'DAILY'
          AND r.periodStart >= :from
        GROUP BY r.adId, r.screenId
    """)
    List<Object[]> getStatsFromDate(@Param("from") LocalDate from);
}
