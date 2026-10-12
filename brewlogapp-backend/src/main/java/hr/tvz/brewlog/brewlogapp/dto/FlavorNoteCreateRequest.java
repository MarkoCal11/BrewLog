package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FlavorNoteCreateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @NotNull(message = "Category is required")
        Category category
) {}