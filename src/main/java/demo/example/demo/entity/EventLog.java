package demo.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"EventLog\"")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // matches identity ALWAYS in Postgres
    @Column(name = "id")
    private Long id;

    @Column(name = "start", columnDefinition = "timestamp without time zone", nullable = true)
    private LocalDateTime start;

    @Column(name = "end", columnDefinition = "timestamp without time zone", nullable = true)
    private LocalDateTime end;

    @Column(name = "\"key\"", nullable = true) // key can be reserved, safest is quoting
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screenid")
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediaassetid")
    private MediaAsset mediaAsset;

    @CreationTimestamp
    @Column(name = "createdts", columnDefinition = "timestamp without time zone", updatable = false)
    private LocalDateTime createdTs;

    // ---- getters & setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public MediaAsset getMediaAsset() {
        return mediaAsset;
    }

    public void setMediaAsset(MediaAsset mediaAsset) {
        this.mediaAsset = mediaAsset;
    }

    public LocalDateTime getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(LocalDateTime createdTs) {
        this.createdTs = createdTs;
    }
}
