package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Method;

import java.time.LocalDateTime;

public record BrewEntryResponse(
        Long id,
        Method method,
        Double grindSize,
        Double waterTempC,
        Double brewTimeSeconds,
        Double doseGrams,
        Double waterGrams,
        Double yieldGrams,
        String notes,
        Double rating,
        LocalDateTime brewedAt,
        CoffeeBeanResponse coffeeBean
) {}
