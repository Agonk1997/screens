package demo.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "\"Schedule\"")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate fromdate;
    private LocalDate todate;

    private LocalTime fromtime;
    private LocalTime totime;

    // plays per day
    private Double duration;

    @Column(columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screenid")
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediaassetid")
    private MediaAsset mediaAsset;

    // GETTERS & SETTERS
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDate getFromdate() { return fromdate; }
    public void setFromdate(LocalDate fromdate) { this.fromdate = fromdate; }

    public LocalDate getTodate() { return todate; }
    public void setTodate(LocalDate todate) { this.todate = todate; }

    public LocalTime getFromtime() { return fromtime; }
    public void setFromtime(LocalTime fromtime) { this.fromtime = fromtime; }

    public LocalTime getTotime() { return totime; }
    public void setTotime(LocalTime totime) { this.totime = totime; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }

    public MediaAsset getMediaAsset() { return mediaAsset; }
    public void setMediaAsset(MediaAsset mediaAsset) { this.mediaAsset = mediaAsset; }

    // NEW helper (needed by AdStatsService)
    @Transient
    public Double getRepetition() {
        return duration;
    }
}
