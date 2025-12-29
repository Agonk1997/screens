package demo.example.demo.repositories;

import demo.example.demo.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

  // For lifetime stats (adId = mediaAsset.id)
  @Query("SELECT e FROM EventLog e WHERE e.mediaAsset.id = :mediaAssetId")
  List<EventLog> findByMediaAssetId(@Param("mediaAssetId") Long mediaAssetId);

  // For range reports (PDF / UI) based on start
  @Query("""
      SELECT e
      FROM EventLog e
      WHERE e.mediaAsset.id = :mediaAssetId
        AND e.start >= :from
        AND e.start < :to
      """)
  List<EventLog> findByMediaAssetIdAndStartBetween(@Param("mediaAssetId") Long mediaAssetId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  // Native SQL for daily/weekly/monthly (using new column names)
  @Query(value = """
      SELECT
          mediaassetid AS ad_id,
          screenid     AS screen_id,
          SUM(EXTRACT(EPOCH FROM (COALESCE("end","start") - "start"))) AS seconds,
          COUNT(*) AS plays
      FROM "EventLog"
      WHERE "start" >= :from
        AND "start" <  :to
        AND (:screenId IS NULL OR screenid = :screenId)
        AND (:apiKey   IS NULL OR "key" = :apiKey)
      GROUP BY mediaassetid, screenid
      """, nativeQuery = true)
  List<Object[]> getStatsNative(@Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("screenId") Long screenId,
      @Param("apiKey") String apiKey);

  @Query(value = """
      SELECT
          s.id   AS screen_id,
          s.name AS screen_name,
          (
              SELECT MAX(e."start")
              FROM "EventLog" e
              WHERE e.screenid = s.id
          ) AS last_seen
      FROM "Screen" s
      ORDER BY s.id
      """, nativeQuery = true)
  List<Object[]> findLastSeenForScreensFixed();
}