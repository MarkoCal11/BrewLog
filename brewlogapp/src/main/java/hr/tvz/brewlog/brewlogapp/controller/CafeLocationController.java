package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.CafeLocationCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.CafeLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cafeLocations")
public class CafeLocationController {

    private final CafeLocationService cafeLocationService;

    public CafeLocationController(CafeLocationService cafeLocationService) {
        this.cafeLocationService = cafeLocationService;
    }

    @GetMapping
    public ResponseEntity<List<CafeLocationResponse>> getAllCafeLocations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cafeLocationService.findAllCafeLocations(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CafeLocationResponse> getCafeLocationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cafeLocationService.findCafeLocationById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CafeLocationResponse> createCafeLocation (
            @Valid @RequestBody CafeLocationCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CafeLocationResponse createResponse = cafeLocationService.createCafeLocation(request, userDetails.getUsername());
        return ResponseEntity
                .created(URI.create("/api/cafeLocations/" + createResponse.id()))
                .body(createResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CafeLocationResponse> updateCafeLocation(
            @PathVariable Long id,
            @Valid @RequestBody CafeLocationUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return cafeLocationService.updateCafeLocation(id, request, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCafeLocation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean deleted = cafeLocationService.removeCafeLocation(id, userDetails.getUsername());
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
