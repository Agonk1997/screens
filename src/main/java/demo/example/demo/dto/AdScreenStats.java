package demo.example.demo.dto;

public class AdScreenStats {

    private Integer screenId;
    private String screenName;

    private long plays;        // how many times on this screen
    private long totalSeconds; // runtime on this screen

    public AdScreenStats(Integer screenId, String screenName, long plays, long totalSeconds) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.plays = plays;
        this.totalSeconds = totalSeconds;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public long getPlays() {
        return plays;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void addPlays(long morePlays) {
        this.plays += morePlays;
    }

    public void addSeconds(long moreSeconds) {
        this.totalSeconds += moreSeconds;
    }
}
