package hr.tvz.brewlog.brewlogapp.dto;

public record CafeLocationResponse (

        Long id,
        String name,
        String city,
        String address,
        Double rating,
        String instagram,
        Double latitude,
        Double longitude
){}
