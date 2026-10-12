package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.ProcessingMethod;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;

import java.time.LocalDate;
import java.util.List;

public record AdminCoffeeBeanResponse(

        Long id,
        String name,
        String origin,
        RoastLevel roastLevel,
        ProcessingMethod processingMethod,
        String roasterName,
        Double averageRating,
        Integer brewCount,
        LocalDate roastDate,
        Double price,
        Integer altitude,
        String tastingNotes,
        boolean favorite,
        Integer weightGrams,
        boolean finished,
        String photoUrl,
        List<FlavorNoteResponse> flavorNotes,
        Long ownerId,
        String ownerUsername
) {}
