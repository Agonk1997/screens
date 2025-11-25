package demo.example.demo.dto;

public class AdPerScreenStats {

    private Integer screenId;
    private String screenName;
    private long plays;
    private long totalSeconds;

    public AdPerScreenStats() {
    }

    public AdPerScreenStats(Integer screenId, String screenName, long plays, long totalSeconds) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.plays = plays;
        this.totalSeconds = totalSeconds;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public void setScreenId(Integer screenId) {
        this.screenId = screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public long getPlays() {
        return plays;
    }

    public void setPlays(long plays) {
        this.plays = plays;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(long totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public void add(long plays, long seconds) {
        this.plays += plays;
        this.totalSeconds += seconds;
    }
}
