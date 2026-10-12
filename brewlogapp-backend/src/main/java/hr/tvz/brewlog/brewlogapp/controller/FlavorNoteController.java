package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.FlavorNoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/flavorNotes")
public class
FlavorNoteController {

    private final FlavorNoteService flavorNoteService;

    public FlavorNoteController(FlavorNoteService flavorNoteService) {
        this.flavorNoteService = flavorNoteService;
    }

    @GetMapping
    public ResponseEntity<List<FlavorNoteResponse>> getAllFlavorNotes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(flavorNoteService.findAllFlavorNotes(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlavorNoteResponse> getFlavorNoteById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return flavorNoteService.findFlavorNoteById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<FlavorNoteResponse> getFlavorNoteByName(
            @PathVariable String name,
            @AuthenticationPrincipal UserDetails userDetails) {
        return flavorNoteService.findFlavorNoteByName(name, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FlavorNoteResponse>> getFlavorNoteByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<FlavorNoteResponse> responses = flavorNoteService.findFlavorNotesByCategory(category, userDetails.getUsername());
        return responses.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(responses);
    }

    @GetMapping("/coffeeBean/{beanId}")
    public ResponseEntity<List<FlavorNoteResponse>> getFlavorNotesForBean(
            @PathVariable Long beanId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(flavorNoteService.findFlavorNotesByCoffeeBeanId(beanId, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<FlavorNoteResponse> createFlavorNote(
            @Valid @RequestBody FlavorNoteCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        FlavorNoteResponse createResponse = flavorNoteService.createFlavorNote(request, userDetails.getUsername());
        return ResponseEntity
                .created(URI.create("/api/flavorNotes/" + createResponse.id()))
                .body(createResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlavorNoteById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return flavorNoteService.removeFlavorNote(id, userDetails.getUsername())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlavorNoteResponse> updateFlavorNote(
            @PathVariable Long id,
            @Valid @RequestBody FlavorNoteUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return flavorNoteService.updateFlavorNote(id, request, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
