package hr.tvz.brewlog.brewlogapp.dto;

import java.time.LocalDate;

public record RatingTrendPoint(LocalDate date, Double avgRating) {}
