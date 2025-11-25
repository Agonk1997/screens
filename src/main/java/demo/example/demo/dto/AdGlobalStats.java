package demo.example.demo.dto;

import java.util.List;

public class AdGlobalStats {

    private Integer adId;
    private String adName;
    private long totalPlays;
    private long totalSeconds;
    private List<AdPerScreenStats> perScreen;

    public AdGlobalStats() {
    }

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

    public Integer getAdId() {
        return adId;
    }

    public void setAdId(Integer adId) {
        this.adId = adId;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public long getTotalPlays() {
        return totalPlays;
    }

    public void setTotalPlays(long totalPlays) {
        this.totalPlays = totalPlays;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(long totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public List<AdPerScreenStats> getPerScreen() {
        return perScreen;
    }

    public void setPerScreen(List<AdPerScreenStats> perScreen) {
        this.perScreen = perScreen;
    }
}
