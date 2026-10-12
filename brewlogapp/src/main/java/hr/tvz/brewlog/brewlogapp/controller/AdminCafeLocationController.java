package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminCafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.CafeLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cafeLocations")
public class AdminCafeLocationController {

    private final CafeLocationService cafeLocationService;

    public AdminCafeLocationController(CafeLocationService cafeLocationService) {
        this.cafeLocationService = cafeLocationService;
    }

    @GetMapping
    public ResponseEntity<List<AdminCafeLocationResponse>> getAllCafeLocations() {
        return ResponseEntity.ok(cafeLocationService.findAllCafeLocationsGlobal());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCafeLocationResponse> getCafeLocationById(@PathVariable Long id) {
        return cafeLocationService.findCafeLocationByIdGlobal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCafeLocationResponse> updateCafeLocation(
            @PathVariable Long id,
            @Valid @RequestBody CafeLocationUpdateRequest request) {
        return cafeLocationService.updateCafeLocationGlobal(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCafeLocation(@PathVariable Long id) {
        boolean deleted = cafeLocationService.removeCafeLocationGlobal(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
