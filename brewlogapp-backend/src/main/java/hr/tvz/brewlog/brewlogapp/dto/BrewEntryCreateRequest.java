package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Method;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BrewEntryCreateRequest(

        @NotNull(message = "Method is required")
        Method method,
        
        Double grindSize,

        Double waterTempC,

        Double brewTimeSeconds,

        @NotNull(message = "Dose is required")
        Double doseGrams,

        Double waterGrams,

        Double yieldGrams,

        @Min(1)
        @Max(5)
        Double rating,

        @Size(max = 100, message = "Notes must be at most 100 characters")
        String notes,

        @NotNull(message = "Date of brew is required")
        LocalDateTime brewedAt,

        @NotNull(message = "Coffee bean is required")
        Long coffeeBeanId
){}
