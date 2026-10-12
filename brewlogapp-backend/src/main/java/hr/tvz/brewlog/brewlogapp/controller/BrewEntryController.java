package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.BrewEntryCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.BrewEntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/brewEntries")
public class BrewEntryController {

    private final BrewEntryService brewEntryService;

    public BrewEntryController(BrewEntryService brewEntryService) {
        this.brewEntryService = brewEntryService;
    }

    @GetMapping
    public ResponseEntity<List<BrewEntryResponse>> getAllBrewEntries(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(brewEntryService.findAllBrewEntries(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrewEntryResponse> getBrewEntryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return brewEntryService.findBrewEntryById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BrewEntryResponse> createBrewEntry(
            @Valid @RequestBody BrewEntryCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BrewEntryResponse createResponse = brewEntryService.createBrewEntry(request, userDetails.getUsername());
        return ResponseEntity
                .created(URI.create("/api/brewEntries/" + createResponse.id()))
                .body(createResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrewEntryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean deleted = brewEntryService.removeBrewEntryById(id, userDetails.getUsername());
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrewEntryResponse> updateBrewEntry(
            @PathVariable Long id,
            @Valid @RequestBody BrewEntryUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return brewEntryService.updateBrewEntry(id, request, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
