package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.enums.ReportSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        PeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDateTime generatedAt,
        ReportSource source,
        int brewCount,
        Double averageRating,
        String topBeanName,
        Method mostUsedMethod,
        Double totalDoseGrams,
        List<BrewsPerDayPoint> brewsPerDay,
        List<MethodBreakdownPoint> methodBreakdown,
        List<RatingTrendPoint> ratingTrend
) {}
