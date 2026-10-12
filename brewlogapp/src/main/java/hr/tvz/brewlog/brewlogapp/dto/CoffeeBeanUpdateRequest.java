package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.ProcessingMethod;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CoffeeBeanUpdateRequest (

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @NotBlank(message = "Origin is required")
        @Size(max = 50, message = "Origin must be at most 50 characters")
        String origin,

        @NotNull(message = "Roast level is required")
        RoastLevel roastLevel,

        ProcessingMethod processingMethod,

        @NotBlank(message = "Roaster name is required")
        @Size(max = 50, message = "Roaster name must be at most 50 characters")
        String roasterName,

        LocalDate roastDate,
        Double price,
        Integer altitude,

        @Size(max = 255, message = "Tasting notes must be at most 255 characters")
        String tastingNotes,

        Integer weightGrams
){}
