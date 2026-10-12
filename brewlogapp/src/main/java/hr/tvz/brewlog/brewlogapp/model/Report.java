package hr.tvz.brewlog.brewlogapp.model;

import hr.tvz.brewlog.brewlogapp.converter.BrewsPerDayConverter;
import hr.tvz.brewlog.brewlogapp.converter.MethodBreakdownConverter;
import hr.tvz.brewlog.brewlogapp.converter.RatingTrendConverter;
import hr.tvz.brewlog.brewlogapp.dto.BrewsPerDayPoint;
import hr.tvz.brewlog.brewlogapp.dto.MethodBreakdownPoint;
import hr.tvz.brewlog.brewlogapp.dto.RatingTrendPoint;
import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.enums.ReportSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 10)
    private ReportSource source;

    @Column(name = "brew_count", nullable = false)
    private int brewCount;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "top_bean_name", length = 50)
    private String topBeanName;

    @Enumerated(EnumType.STRING)
    @Column(name = "most_used_method", length = 50)
    private Method mostUsedMethod;

    @Column(name = "total_dose_grams")
    private Double totalDoseGrams;

    @Convert(converter = BrewsPerDayConverter.class)
    @Column(name = "brews_per_day", columnDefinition = "CLOB")
    private List<BrewsPerDayPoint> brewsPerDay;

    @Convert(converter = MethodBreakdownConverter.class)
    @Column(name = "method_breakdown", columnDefinition = "CLOB")
    private List<MethodBreakdownPoint> methodBreakdown;

    @Convert(converter = RatingTrendConverter.class)
    @Column(name = "rating_trend", columnDefinition = "CLOB")
    private List<RatingTrendPoint> ratingTrend;
}
