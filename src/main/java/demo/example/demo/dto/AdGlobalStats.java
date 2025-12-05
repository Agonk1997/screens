package demo.example.demo.dto;

import java.time.LocalDate;
import java.util.List;

public class AdGlobalStats {

    private Integer adId;
    private String adName;
    private long totalPlays;
    private long totalSeconds;
    private List<AdPerScreenStats> perScreen;

    private LocalDate lifetimeFrom;
    private LocalDate lifetimeTo;

    public AdGlobalStats() {}

    // OLD constructor (compatibility)
    public AdGlobalStats(Integer adId,
                         String adName,
                         long totalPlays,
                         long totalSeconds,
                         List<AdPerScreenStats> perScreen) {
        this.adId = adId;
        this.adName = adName;
        this.totalPlays = totalPlays;
        this.totalSeconds = totalSeconds;
        this.perScreen = perScreen;
    }

    // NEW full constructor
    public AdGlobalStats(Integer adId,
                         String adName,
                         long totalPlays,
                         long totalSeconds,
                         List<AdPerScreenStats> perScreen,
                         LocalDate lifetimeFrom,
                         LocalDate lifetimeTo) {
        this(adId, adName, totalPlays, totalSeconds, perScreen);
        this.lifetimeFrom = lifetimeFrom;
        this.lifetimeTo = lifetimeTo;
    }

    // Getters / setters
    public Integer getAdId() { return adId; }
    public void setAdId(Integer adId) { this.adId = adId; }

    public String getAdName() { return adName; }
    public void setAdName(String adName) { this.adName = adName; }

    public long getTotalPlays() { return totalPlays; }
    public void setTotalPlays(long totalPlays) { this.totalPlays = totalPlays; }

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }

    public List<AdPerScreenStats> getPerScreen() { return perScreen; }
    public void setPerScreen(List<AdPerScreenStats> perScreen) { this.perScreen = perScreen; }

    public LocalDate getLifetimeFrom() { return lifetimeFrom; }
    public void setLifetimeFrom(LocalDate lifetimeFrom) { this.lifetimeFrom = lifetimeFrom; }

    public LocalDate getLifetimeTo() { return lifetimeTo; }
    public void setLifetimeTo(LocalDate lifetimeTo) { this.lifetimeTo = lifetimeTo; }

    public double getAvgSecondsPerPlay() {
        return totalPlays > 0 ? (double) totalSeconds / totalPlays : 0.0;
    }
}
