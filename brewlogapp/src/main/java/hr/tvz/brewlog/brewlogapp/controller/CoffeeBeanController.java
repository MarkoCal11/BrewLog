package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.CoffeeBeanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/coffeeBeans")
public class CoffeeBeanController {

    private final CoffeeBeanService coffeeBeanService;

    public CoffeeBeanController(CoffeeBeanService coffeeBeanService) {
        this.coffeeBeanService = coffeeBeanService;
    }

    @GetMapping
    public ResponseEntity<List<CoffeeBeanResponse>> getAllCoffeeBeans(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(coffeeBeanService.findAllCoffeeBeans(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoffeeBeanResponse> getCoffeeBeanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.findCoffeeBeanById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<CoffeeBeanResponse> getCoffeeBeanByName(
            @PathVariable String name,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.findCoffeeBeanByName(name, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/origin/{origin}")
    public ResponseEntity<List<CoffeeBeanResponse>> getCoffeeBeanByOrigin(
            @PathVariable String origin,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CoffeeBeanResponse> responses = coffeeBeanService.findCoffeeBeansByOrigin(origin, userDetails.getUsername());
        return responses.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<CoffeeBeanResponse> createCoffeeBean(
            @Valid @RequestBody CoffeeBeanCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CoffeeBeanResponse createResponse = coffeeBeanService.createCoffeeBean(request, userDetails.getUsername());
        return ResponseEntity
                .created(URI.create("/api/coffeeBeans/" + createResponse.id()))
                .body(createResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoffeeBean(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.removeCoffeeBean(id, userDetails.getUsername())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{coffeeBeanId}/flavorNotes/{flavorNoteId}")
    public ResponseEntity<CoffeeBeanResponse> addFlavorNote(
            @PathVariable Long coffeeBeanId,
            @PathVariable Long flavorNoteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.addFlavorNote(coffeeBeanId, flavorNoteId, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{coffeeBeanId}/flavorNotes/{flavorNoteId}")
    public ResponseEntity<CoffeeBeanResponse> removeFlavorNote(
            @PathVariable Long coffeeBeanId,
            @PathVariable Long flavorNoteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.removeFlavorNote(coffeeBeanId, flavorNoteId, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoffeeBeanResponse> updateCoffeeBean(
            @PathVariable Long id,
            @Valid @RequestBody CoffeeBeanUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.updateCoffeeBean(id, request, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<CoffeeBeanResponse> setFavorite(
            @PathVariable Long id,
            @RequestParam boolean value,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.setFavorite(id, value, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/finished")
    public ResponseEntity<CoffeeBeanResponse> setFinished(
            @PathVariable Long id,
            @RequestParam boolean value,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.setFinished(id, value, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<CoffeeBeanResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.uploadPhoto(id, file, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<CoffeeBeanResponse> deletePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return coffeeBeanService.deletePhoto(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
