package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminCoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.CoffeeBeanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coffeeBeans")
public class AdminCoffeeBeanController {

    private final CoffeeBeanService coffeeBeanService;

    public AdminCoffeeBeanController(CoffeeBeanService coffeeBeanService) {
        this.coffeeBeanService = coffeeBeanService;
    }

    @GetMapping
    public ResponseEntity<List<AdminCoffeeBeanResponse>> getAllCoffeeBeans() {
        return ResponseEntity.ok(coffeeBeanService.findAllCoffeeBeansGlobal());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCoffeeBeanResponse> getCoffeeBeanById(@PathVariable Long id) {
        return coffeeBeanService.findCoffeeBeanByIdGlobal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoffeeBeanyById(@PathVariable Long id) {
        boolean deleted = coffeeBeanService.removeCoffeeBeanByIdGlobal(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCoffeeBeanResponse> updateBrewEntry(
            @PathVariable Long id,
            @Valid @RequestBody CoffeeBeanUpdateRequest request) {
        return coffeeBeanService.updateCoffeeBeanGlobal(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<CoffeeBeanResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return coffeeBeanService.uploadPhotoGlobal(id, file)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<CoffeeBeanResponse> deletePhoto(
            @PathVariable Long id) {
        return coffeeBeanService.deletePhotoGlobal(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
