package demo.example.demo.dto;

import demo.example.demo.entity.MediaAsset;

/**
 * DTO used to transfer ad data from the Controller to the ads/list.html template, 
 * enriching it with the computed status and historical statistics.
 */
public class AdListDto {

    private Integer id;
    private String name;
    private String url; // Crucial for Thymeleaf preview
    private String mimetype; // Crucial for Thymeleaf preview
    private Double duration;
    private Boolean active;
    private String status; // The new computed status (Active, Expired, Scheduled, Inactive)

    // Fields for historical data (Total Plays and Seconds)
    private Long totalPlays;
    private Long totalSeconds;
    
    public AdListDto() {}

    public AdListDto(MediaAsset ad) {
        this.id = ad.getId();
        this.name = ad.getName();
        this.url = ad.getUrl();
        this.mimetype = ad.getMimetype();
        this.duration = ad.getDuration();
        this.active = ad.getActive();
        // Status and stats will be set separately in the controller
    }

    // --- Getters (Required by Thymeleaf/SpringEL to access properties) ---

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() { // Thymeleaf looks for this method for the 'url' property
        return url;
    }

    public String getMimetype() { // Thymeleaf looks for this method for the 'mimetype' property
        return mimetype;
    }

    public Double getDuration() {
        return duration;
    }

    public Boolean getActive() {
        return active;
    }

    public String getStatus() {
        return status;
    }
    
    public Long getTotalPlays() {
        return totalPlays;
    }

    public Long getTotalSeconds() {
        return totalSeconds;
    }

    // --- Setters (Used by the controller to populate computed and statistical data) ---
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setTotalPlays(Long totalPlays) {
        this.totalPlays = totalPlays;
    }

    public void setTotalSeconds(Long totalSeconds) {
        this.totalSeconds = totalSeconds;
    }
}