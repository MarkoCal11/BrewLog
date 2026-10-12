package hr.tvz.brewlog.brewlogapp.dto;

import jakarta.validation.constraints.*;

public record CafeLocationUpdateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Size(max = 50, message = "City must be at most 50 characters")
        String city,

        @Size(max = 100, message = "Address must be at most 100 characters")
        String address,

        @DecimalMin("1.0")
        @DecimalMax("5.0")
        Double rating,

        @Size(max = 100, message = "Instagram must be at most 100 characters")
        @Pattern(
                regexp = "^$|^@?[A-Za-z0-9._]{1,30}$|^https://(www\\.)?instagram\\.com/@?[A-Za-z0-9._]{1,30}/?$",
                message = "Must be an Instagram handle (e.g. @cafe) or an instagram.com URL"
        )
        String instagram,

        @DecimalMin("-90")
        @DecimalMax("90")
        Double latitude,

        @DecimalMin("-180")
        @DecimalMax("180")
        Double longitude
){}
