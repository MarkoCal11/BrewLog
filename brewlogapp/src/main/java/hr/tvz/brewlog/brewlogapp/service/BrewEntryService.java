package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminBrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface BrewEntryService {

    List<BrewEntryResponse> findAllBrewEntries(String username);

    Optional<BrewEntryResponse> findBrewEntryById(Long id, String username);

    BrewEntryResponse createBrewEntry(BrewEntryCreateRequest request, String username);

    boolean removeBrewEntryById(Long id, String username);

    Optional<BrewEntryResponse> updateBrewEntry(Long id, BrewEntryUpdateRequest request, String username);

    List<AdminBrewEntryResponse> findAllBrewEntriesGlobal();

    Optional<AdminBrewEntryResponse> findBrewEntryByIdGlobal(Long id);

    boolean removeBrewEntryByIdGlobal(Long id);

    Optional<AdminBrewEntryResponse> updateBrewEntryGlobal(Long id, BrewEntryUpdateRequest request);
}
