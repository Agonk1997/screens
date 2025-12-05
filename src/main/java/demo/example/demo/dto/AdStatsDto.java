package demo.example.demo.dto;

public record AdStatsDto(
        Long adId,
        Long screenId,
        long totalSeconds,
        long playCount
) {}
