package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Category;

public record FlavorNoteResponse(

        Long id,
        String name,
        Category category,
        boolean global
) {}