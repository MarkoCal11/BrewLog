package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminCafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface CafeLocationService {

    List<CafeLocationResponse> findAllCafeLocations(String username);

    Optional<CafeLocationResponse> findCafeLocationById(Long id, String username);

    CafeLocationResponse createCafeLocation(CafeLocationCreateRequest request, String username);

    boolean removeCafeLocation(Long id, String username);

    Optional<CafeLocationResponse> updateCafeLocation(Long id, CafeLocationUpdateRequest request, String username);

    List<AdminCafeLocationResponse> findAllCafeLocationsGlobal();

    Optional<AdminCafeLocationResponse> findCafeLocationByIdGlobal(Long id);

    boolean removeCafeLocationGlobal(Long id);

    Optional<AdminCafeLocationResponse> updateCafeLocationGlobal(Long id, CafeLocationUpdateRequest request);
 }
