package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminCafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest;
import hr.tvz.brewlog.brewlogapp.mapper.CafeLocationMapper;
import hr.tvz.brewlog.brewlogapp.model.CafeLocation;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.CafeLocationRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CafeLocationServiceImpl implements CafeLocationService {

    private final CafeLocationRepository cafeLocationRepository;
    private final CafeLocationMapper cafeLocationMapper;
    private final UserRepository userRepository;

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public List<CafeLocationResponse> findAllCafeLocations(String username) {
        User user = resolveUser(username);
        return cafeLocationRepository.findAllByUser(user).stream()
                .map(cafeLocationMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<CafeLocationResponse> findCafeLocationById(Long id, String username) {
        User user = resolveUser(username);
        return cafeLocationRepository.findByIdAndUser(id, user)
                .map(cafeLocationMapper::toResponse);
    }

    @Override
    public CafeLocationResponse createCafeLocation(CafeLocationCreateRequest request, String username) {
        User user = resolveUser(username);
        CafeLocation cafe = cafeLocationMapper.toEntity(request);
        cafe.setUser(user);
        normalize(cafe);
        return cafeLocationMapper.toResponse(cafeLocationRepository.save(cafe));
    }

    @Override
    public boolean removeCafeLocation(Long id, String username) {
        User user = resolveUser(username);
        Optional<CafeLocation> existing = cafeLocationRepository.findByIdAndUser(id, user);
        if(existing.isEmpty()) {
            return false;
        }
        cafeLocationRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<CafeLocationResponse> updateCafeLocation(Long id, CafeLocationUpdateRequest request, String username) {
        User user = resolveUser(username);
        return cafeLocationRepository.findByIdAndUser(id, user)
                .map(cafeLocation -> {
                    cafeLocationMapper.updateEntityFromRequest(request, cafeLocation);
                    normalize(cafeLocation);
                    return cafeLocationMapper.toResponse(cafeLocationRepository.save(cafeLocation));
                });
    }

    @Override
    public List<AdminCafeLocationResponse> findAllCafeLocationsGlobal() {
        return cafeLocationRepository.findAll().stream()
                .map(cafeLocationMapper::toAdminResponse)
                .toList();
    }

    @Override
    public Optional<AdminCafeLocationResponse> findCafeLocationByIdGlobal(Long id) {
        return cafeLocationRepository.findById(id)
                .map(cafeLocationMapper::toAdminResponse);
    }

    @Override
    public boolean removeCafeLocationGlobal(Long id) {
        Optional<CafeLocation> existing = cafeLocationRepository.findById(id);
        if(existing.isEmpty()) {
            return false;
        }
        cafeLocationRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<AdminCafeLocationResponse> updateCafeLocationGlobal(Long id, CafeLocationUpdateRequest request) {
        return cafeLocationRepository.findById(id)
                .map(cafeLocation -> {
                    cafeLocationMapper.updateEntityFromRequest(request, cafeLocation);
                    normalize(cafeLocation);
                    return cafeLocationMapper.toAdminResponse(cafeLocationRepository.save(cafeLocation));
                });
    }

    private void normalize(CafeLocation cafe) {
        cafe.setName(trimToNull(cafe.getName()));
        cafe.setCity(trimToNull(cafe.getCity()));
        cafe.setAddress(trimToNull(cafe.getAddress()));
        cafe.setInstagram(normalizeInstagram(cafe.getInstagram()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeInstagram(String value) {
        String handle = trimToNull(value);
        if (handle == null) {
            return null;
        }
        int domainIdx = handle.toLowerCase().indexOf("instagram.com/");
        if (domainIdx >= 0) {
            handle = handle.substring(domainIdx + "instagram.com/".length());
        }
        if (handle.startsWith("@")) {
            handle = handle.substring(1);
        }
        int cut = handle.length();
        for (int i = 0; i < handle.length(); i++) {
            char c = handle.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                cut = i;
                break;
            }
        }
        handle = handle.substring(0, cut).toLowerCase();
        return handle.isEmpty() ? null : handle;
    }
}
