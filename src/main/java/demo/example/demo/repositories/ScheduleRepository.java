package demo.example.demo.repositories;

import demo.example.demo.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    // All schedules for a given screen
    List<Schedule> findByScreen_Id(Integer screenId);

    // 🔥 All schedules for a given MediaAsset (Ad), eagerly fetching Screen
    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.screen
            WHERE s.mediaAsset.id = :mediaAssetId
            """)
    List<Schedule> findByMediaAssetId(@Param("mediaAssetId") Integer mediaAssetId);

    // All schedules whose [fromdate, todate] overlaps the given [start, end]
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.fromdate <= :end
              AND s.todate >= :start
            """)
    List<Schedule> findOverlapping(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    // Same thing, but only for one screen
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.screen.id = :screenId
              AND s.fromdate <= :end
              AND s.todate >= :start
            """)
    List<Schedule> findOverlappingForScreen(@Param("screenId") Integer screenId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    // Filter by MediaAsset ID and date range, eagerly fetching Screen
    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.screen
            WHERE s.mediaAsset.id = :adId
              AND s.fromdate <= :end
              AND s.todate >= :start
            """)
    List<Schedule> findByMediaAssetIdAndDateRange(@Param("adId") Integer adId,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);

    // 🚀 CRITICAL QUERY: Find ALL schedules active on a specific screen and date.
    // Eagerly fetch MediaAsset and Screen for capacity calculation.
    @Query("""
            SELECT s FROM Schedule s 
            JOIN FETCH s.mediaAsset 
            JOIN FETCH s.screen 
            WHERE s.screen.id = :screenId
              AND s.fromdate <= :day
              AND s.todate >= :day
            """)
    List<Schedule> findAllActiveSchedulesByScreenIdAndDate(
            @Param("screenId") Integer screenId,
            @Param("day") LocalDate day
    );
}
