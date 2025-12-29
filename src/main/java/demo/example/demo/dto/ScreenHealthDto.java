package demo.example.demo.dto;

import java.time.LocalDateTime;

public class ScreenHealthDto {

    private Integer screenId;
    private String screenName;
    private LocalDateTime lastSeen;
    private String status;

    public ScreenHealthDto(Integer screenId, String screenName, LocalDateTime lastSeen, String status) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.lastSeen = lastSeen;
        this.status = status;
    }

    // getters

    public Integer getScreenId() {
        return screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public String getStatus() {
        return status;
    }

    // setters
    public void setScreenId(Integer screenId) {
        this.screenId = screenId;
    }
    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    

    
}