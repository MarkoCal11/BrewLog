package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.*;
import hr.tvz.brewlog.brewlogapp.service.FlavorNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/flavorNotes")
@RequiredArgsConstructor
public class AdminFlavorNoteController {

    private final FlavorNoteService flavorNoteService;

    @GetMapping
    public ResponseEntity<List<FlavorNoteResponse>> getAllFlavorNotes() {
        return ResponseEntity.ok(flavorNoteService.findAllFlavorNotesGlobal());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlavorNoteResponse> getFlavorNoteById(@PathVariable Long id) {
        return flavorNoteService.findFlavorNoteByIdGlobal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FlavorNoteResponse> createGlobalFlavorNote(
            @Valid @RequestBody FlavorNoteCreateRequest request) {
        FlavorNoteResponse created = flavorNoteService.createFlavorNoteGlobal(request);
        return ResponseEntity
                .created(URI.create("/api/admin/flavorNotes/" + created.id()))
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlavorNoteById(@PathVariable Long id) {
        boolean deleted = flavorNoteService.removeFlavorNoteGlobal(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlavorNoteResponse> updateFlavorNote(
            @PathVariable Long id,
            @Valid @RequestBody FlavorNoteUpdateRequest request) {
        return flavorNoteService.updateFlavorNoteGlobal(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
