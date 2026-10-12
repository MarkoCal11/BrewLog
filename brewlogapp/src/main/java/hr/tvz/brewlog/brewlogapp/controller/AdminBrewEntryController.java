package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminBrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.BrewEntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brewEntries")
public class AdminBrewEntryController {

    private final BrewEntryService brewEntryService;

    public AdminBrewEntryController(BrewEntryService brewEntryService) {
        this.brewEntryService = brewEntryService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBrewEntryResponse>> getAllBrewEntries() {
        return ResponseEntity.ok(brewEntryService.findAllBrewEntriesGlobal());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminBrewEntryResponse> getBrewEntryById(@PathVariable Long id) {
        return brewEntryService.findBrewEntryByIdGlobal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrewEntryById(@PathVariable Long id) {
        boolean deleted = brewEntryService.removeBrewEntryByIdGlobal(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminBrewEntryResponse> updateBrewEntry(
            @PathVariable Long id,
            @Valid @RequestBody BrewEntryUpdateRequest request) {
        return brewEntryService.updateBrewEntryGlobal(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
