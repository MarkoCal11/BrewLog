package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface FlavorNoteService {

    List<FlavorNoteResponse> findAllFlavorNotes(String username);

    Optional<FlavorNoteResponse> findFlavorNoteById(Long id, String username);

    Optional<FlavorNoteResponse> findFlavorNoteByName(String name, String username);

    List<FlavorNoteResponse> findFlavorNotesByCategory(String categoryName, String username);

    List<FlavorNoteResponse> findFlavorNotesByCoffeeBeanId(Long coffeeBeanId, String username);

    FlavorNoteResponse createFlavorNote(FlavorNoteCreateRequest request, String username);

    boolean removeFlavorNote(Long id, String username);

    Optional<FlavorNoteResponse> updateFlavorNote(Long id, FlavorNoteUpdateRequest request, String username);

    List<FlavorNoteResponse> findAllFlavorNotesGlobal();

    Optional<FlavorNoteResponse> findFlavorNoteByIdGlobal(Long id);

    Optional<FlavorNoteResponse> findFlavorNoteByNameGlobal(String name);

    FlavorNoteResponse createFlavorNoteGlobal(FlavorNoteCreateRequest request);

    boolean removeFlavorNoteGlobal(Long id);

    Optional<FlavorNoteResponse> updateFlavorNoteGlobal(Long id, FlavorNoteUpdateRequest request);
}
