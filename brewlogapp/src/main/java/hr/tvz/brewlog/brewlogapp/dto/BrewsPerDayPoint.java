package hr.tvz.brewlog.brewlogapp.dto;

import java.time.LocalDate;

public record BrewsPerDayPoint(LocalDate date, long count) {}
