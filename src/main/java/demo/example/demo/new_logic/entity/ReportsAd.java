package demo.example.demo.new_logic.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reports_ads",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"period_type", "period_start", "ad_id", "screen_id"}
    )
)
public class ReportsAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_type", nullable = false)
    private String periodType; // DAILY / WEEKLY / MONTHLY

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "ad_id", nullable = false)
    private Long adId;

    @Column(name = "screen_id")
    private Long screenId;

    @Column(nullable = false)
    private long plays;

    @Column(nullable = false)
    private long seconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters & setters

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getPeriodType() {
        return periodType;
    }
    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
    public Long getAdId() {
        return adId;
    }
    public void setAdId(Long adId) {
        this.adId = adId;
    }
    public Long getScreenId() {
        return screenId;
    }
    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }
    public long getPlays() {
        return plays;
    }
    public void setPlays(long plays) {
        this.plays = plays;
    }
    public long getSeconds() {
        return seconds;
    }
    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    
}
