package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminBrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;
import hr.tvz.brewlog.brewlogapp.mapper.BrewEntryMapper;
import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.CoffeeBeanRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BrewEntryServiceImpl implements BrewEntryService {

    private final BrewEntryRepository brewEntryRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewEntryMapper brewEntryMapper;
    private final UserRepository userRepository;
    private final ReportService reportService;

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public List<BrewEntryResponse> findAllBrewEntries(String username) {
        User user = resolveUser(username);
        return brewEntryRepository.findAllByUser(user).stream()
                .map(brewEntryMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<BrewEntryResponse> findBrewEntryById(Long id, String username) {
        User user = resolveUser(username);
        return brewEntryRepository.findByIdAndUser(id, user)
                .map(brewEntryMapper::toResponse);
    }

    @Override
    public BrewEntryResponse createBrewEntry(BrewEntryCreateRequest request, String username) {
        User user = resolveUser(username);
        CoffeeBean coffeeBean = coffeeBeanRepository.findByIdAndUser(request.coffeeBeanId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Coffee bean not found: " + request.coffeeBeanId()));
        BrewEntry brewEntry = brewEntryMapper.toEntity(request);
        brewEntry.setCoffeeBean(coffeeBean);
        brewEntry.setUser(user);
        BrewEntry saved = brewEntryRepository.save(brewEntry);
        reportService.reconcileReports(user, List.of(saved.getBrewedAt().toLocalDate()));
        return brewEntryMapper.toResponse(saved);
    }

    @Override
    public boolean removeBrewEntryById(Long id, String username) {
        User user = resolveUser(username);
        Optional<BrewEntry> existing = brewEntryRepository.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return false;
        }
        LocalDate brewedDate = existing.get().getBrewedAt().toLocalDate();
        brewEntryRepository.deleteById(id);
        reportService.reconcileReports(user, List.of(brewedDate));
        return true;
    }

    @Override
    public Optional<BrewEntryResponse> updateBrewEntry(Long id, BrewEntryUpdateRequest request, String username) {
        User user = resolveUser(username);
        return brewEntryRepository.findByIdAndUser(id, user)
                .map(brewEntry -> {
                    LocalDate oldDate = brewEntry.getBrewedAt().toLocalDate();
                    CoffeeBean coffeeBean = coffeeBeanRepository.findByIdAndUser(request.coffeeBeanId(), user)
                            .orElseThrow(() -> new IllegalArgumentException("Coffee bean not found: " + request.coffeeBeanId()));
                    brewEntryMapper.updateEntityFromRequest(request, brewEntry);
                    brewEntry.setCoffeeBean(coffeeBean);
                    BrewEntry saved = brewEntryRepository.save(brewEntry);
                    LocalDate newDate = saved.getBrewedAt().toLocalDate();
                    // A moved brew can empty/alter its old window as well as its new one.
                    reportService.reconcileReports(user, oldDate.equals(newDate)
                            ? List.of(oldDate)
                            : List.of(oldDate, newDate));
                    return brewEntryMapper.toResponse(saved);
                });
    }

    @Override
    public List<AdminBrewEntryResponse> findAllBrewEntriesGlobal() {
        return brewEntryRepository.findAll().stream()
                .map(brewEntryMapper::toAdminResponse)
                .toList();
    }

    @Override
    public Optional<AdminBrewEntryResponse> findBrewEntryByIdGlobal(Long id) {
        return brewEntryRepository.findById(id)
                .map(brewEntryMapper::toAdminResponse);
    }

    @Override
    public boolean removeBrewEntryByIdGlobal(Long id) {
        Optional<BrewEntry> existing = brewEntryRepository.findById(id);
        if(existing.isEmpty()) {
            return false;
        }
        User owner = existing.get().getUser();
        LocalDate brewedDate = existing.get().getBrewedAt().toLocalDate();
        brewEntryRepository.deleteById(id);
        // Reconcile against the brew's owner, not the acting admin, so their report + the aggregate stay in step.
        reportService.reconcileReports(owner, List.of(brewedDate));
        return true;
    }

    @Override
    public Optional<AdminBrewEntryResponse> updateBrewEntryGlobal(Long id, BrewEntryUpdateRequest request) {
        return brewEntryRepository.findById(id)
                .map(brewEntry -> {
                    User owner = brewEntry.getUser();
                    LocalDate oldDate = brewEntry.getBrewedAt().toLocalDate();
                    CoffeeBean coffeeBean = coffeeBeanRepository.findByIdAndUser(request.coffeeBeanId(), owner)
                            .orElseThrow(() -> new IllegalArgumentException("Coffee bean not found: " + request.coffeeBeanId()));
                    brewEntryMapper.updateEntityFromRequest(request, brewEntry);
                    brewEntry.setCoffeeBean(coffeeBean);
                    BrewEntry saved = brewEntryRepository.save(brewEntry);
                    LocalDate newDate = saved.getBrewedAt().toLocalDate();
                    // A moved brew can empty/alter its old window as well as its new one; reconcile against
                    // the owner so their report and the global aggregate both stay consistent.
                    reportService.reconcileReports(owner, oldDate.equals(newDate)
                            ? List.of(oldDate)
                            : List.of(oldDate, newDate));
                    return brewEntryMapper.toAdminResponse(saved);
                });
    }
}
