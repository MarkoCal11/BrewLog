package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.*;
import hr.tvz.brewlog.brewlogapp.exception.DuplicateResourceException;
import hr.tvz.brewlog.brewlogapp.mapper.CoffeeBeanMapper;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.CoffeeBeanRepository;
import hr.tvz.brewlog.brewlogapp.repository.FlavorNoteRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Transactional
@RequiredArgsConstructor
public class CoffeeBeanServiceImpl implements CoffeeBeanService {

    private final CoffeeBeanRepository coffeeBeanRepository;
    private final CoffeeBeanMapper coffeeBeanMapper;
    private final FlavorNoteRepository flavorNoteRepository;
    private final BrewEntryRepository brewEntryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public List<CoffeeBeanResponse> findAllCoffeeBeans(String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findAllByUser(user).stream()
                .map(this :: toResponse)
                .toList();
    }

    @Override
    public Optional<CoffeeBeanResponse> findCoffeeBeanById(Long id, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByIdAndUser(id, user)
                .map(this::toResponse);
    }

    @Override
    public Optional<CoffeeBeanResponse> findCoffeeBeanByName(String name, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByNameAndUser(name, user)
                .map(this::toResponse);
    }

    @Override
    public List<CoffeeBeanResponse> findCoffeeBeansByOrigin(String origin, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByOriginAndUser(origin, user)
                .stream()
                .map(this :: toResponse)
                .toList();
    }

    @Override
    public CoffeeBeanResponse createCoffeeBean(CoffeeBeanCreateRequest request, String username) {
        User user = resolveUser(username);
        String name = request.name().trim();
        String roasterName = request.roasterName().trim();
        if (!coffeeBeanRepository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(
                name, roasterName, user).isEmpty()) {
            throw new DuplicateResourceException("A coffee bean with this name already exists.");
        }
        CoffeeBean bean = coffeeBeanMapper.toEntity(request);
        bean.setName(name);
        bean.setRoasterName(roasterName);
        bean.setUser(user);
        return toResponse(bean);
    }

    @Override
    public boolean removeCoffeeBean(Long id, String username) {
        User user = resolveUser(username);
        Optional<CoffeeBean> existing = coffeeBeanRepository.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return false;
        }
        if (brewEntryRepository.existsByCoffeeBean_Id(existing.get().getId())) {
            throw new IllegalStateException("Cannot delete coffee bean: it has brew entries.");
        }
        coffeeBeanRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<CoffeeBeanResponse> addFlavorNote(Long coffeeBeanId, Long flavorNoteId, String username) {
        User user = resolveUser(username);
        CoffeeBean coffeeBean = coffeeBeanRepository.findByIdAndUser(coffeeBeanId, user)
                .orElseThrow(() -> new IllegalArgumentException("Coffee bean not found"));
        FlavorNote flavorNote = flavorNoteRepository.findByIdVisibleToUser(flavorNoteId, user)
                .orElseThrow(() -> new IllegalArgumentException("Flavor note not found"));
        coffeeBean.getFlavorNotes().add(flavorNote);

        return Optional.of(toResponse(coffeeBean));
    }

    @Override
    public Optional<CoffeeBeanResponse> removeFlavorNote(Long coffeeBeanId, Long flavorNoteId, String username) {
        User user = resolveUser(username);
        CoffeeBean coffeeBean = coffeeBeanRepository.findByIdAndUser(coffeeBeanId, user)
                .orElseThrow(() -> new IllegalArgumentException("Coffee bean not found"));
        FlavorNote flavorNote = flavorNoteRepository.findByIdVisibleToUser(flavorNoteId, user)
                .orElseThrow(() -> new IllegalArgumentException("Flavor note not found"));
        coffeeBean.getFlavorNotes().remove(flavorNote);

        return Optional.of(toResponse(coffeeBean));
    }

    @Override
    public Optional<CoffeeBeanResponse> updateCoffeeBean(Long id, CoffeeBeanUpdateRequest request, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByIdAndUser(id, user)
                .map(coffeeBean -> {
                    String name = request.name() == null ? null : request.name().trim();
                    String roasterName = request.roasterName() == null ? null : request.roasterName().trim();
                    String effectiveName = name != null ? name : coffeeBean.getName();
                    String effectiveRoaster = roasterName != null ? roasterName : coffeeBean.getRoasterName();
                    boolean pairChanged = !effectiveName.equalsIgnoreCase(coffeeBean.getName())
                            || !effectiveRoaster.equalsIgnoreCase(coffeeBean.getRoasterName());
                    if (pairChanged) {
                        Optional<CoffeeBean> duplicate = coffeeBeanRepository
                                .findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(effectiveName, effectiveRoaster, user)
                                .stream()
                                .filter(bean -> !bean.getId().equals(id))
                                .findFirst();
                        if (duplicate.isPresent()) {
                            throw new DuplicateResourceException("A coffee bean of this roaster with the same name already exists.");
                        }
                    }
                    coffeeBeanMapper.updateEntityFromRequest(request, coffeeBean);
                    if (name != null) {
                        coffeeBean.setName(name);
                    }
                    if (roasterName != null) {
                        coffeeBean.setRoasterName(roasterName);
                    }
                    Double average = brewEntryRepository.findAverageRatingByCoffeeId(coffeeBean.getId());
                    Integer count = brewEntryRepository.countByCoffeeBean_Id(coffeeBean.getId());
                    return coffeeBeanMapper.toResponse(coffeeBeanRepository.save(coffeeBean), average != null ? average : 0.0, count);
                });
    }

    @Override
    public Optional<CoffeeBeanResponse> setFavorite(Long id, boolean value, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByIdAndUser(id, user)
                .map(coffeeBean -> {
                    coffeeBean.setFavorite(value);
                    return toResponse(coffeeBean);
                });
    }

    @Override
    public Optional<CoffeeBeanResponse> setFinished(Long id, boolean value, String username) {
        User user = resolveUser(username);
        return coffeeBeanRepository.findByIdAndUser(id, user)
                .map(coffeeBean -> {
                    coffeeBean.setFinished(value);
                    return toResponse(coffeeBean);
                });
    }

    @Override
    public List<AdminCoffeeBeanResponse> findAllCoffeeBeansGlobal() {
        return coffeeBeanRepository.findAll().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Override
    public Optional<AdminCoffeeBeanResponse> findCoffeeBeanByIdGlobal(Long id) {
        return coffeeBeanRepository.findById(id)
                .map(this::toAdminResponse);
    }

    @Override
    public boolean removeCoffeeBeanByIdGlobal(Long id) {
        Optional<CoffeeBean> existing = coffeeBeanRepository.findById(id);
        if(existing.isEmpty()) {
            return false;
        }
        if (brewEntryRepository.existsByCoffeeBean_Id(existing.get().getId())) {
            throw new IllegalStateException("Cannot delete coffee bean: it has brew entries.");
        }
        coffeeBeanRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<AdminCoffeeBeanResponse> updateCoffeeBeanGlobal(Long id, CoffeeBeanUpdateRequest request) {
        return coffeeBeanRepository.findById(id)
                .map(coffeeBean -> {
                    String name = request.name() == null ? null : request.name().trim();
                    String roasterName = request.roasterName() == null ? null : request.roasterName().trim();
                    String effectiveName = name != null ? name : coffeeBean.getName();
                    String effectiveRoaster = roasterName != null ? roasterName : coffeeBean.getRoasterName();
                    boolean pairChanged = !effectiveName.equalsIgnoreCase(coffeeBean.getName())
                            || !effectiveRoaster.equalsIgnoreCase(coffeeBean.getRoasterName());
                    if (pairChanged) {
                        Optional<CoffeeBean> duplicate = coffeeBeanRepository
                                .findByNameIgnoreCaseAndRoasterNameIgnoreCase(effectiveName, effectiveRoaster)
                                .stream()
                                .filter(bean -> !bean.getId().equals(id))
                                .findFirst();
                        if (duplicate.isPresent()) {
                            throw new DuplicateResourceException("A coffee bean of this roaster with the same name already exists.");
                        }
                    }
                    coffeeBeanMapper.updateEntityFromRequest(request, coffeeBean);
                    if (name != null) {
                        coffeeBean.setName(name);
                    }
                    if (roasterName != null) {
                        coffeeBean.setRoasterName(roasterName);
                    }
                    return toAdminResponse(coffeeBean);
                });
    }

    @Override
    public Optional<CoffeeBeanResponse> uploadPhoto(Long id, MultipartFile file, String username) {
        User user = resolveUser(username);
        return changePhoto(coffeeBeanRepository.findByIdAndUser(id, user),
                () -> fileStorageService.store(file, "coffee-beans"));
    }

    @Override
    public Optional<CoffeeBeanResponse> deletePhoto(Long id, String username) {
        User user = resolveUser(username);
        return changePhoto(coffeeBeanRepository.findByIdAndUser(id, user), () -> null);
    }

    @Override
    public Optional<CoffeeBeanResponse> uploadPhotoGlobal(Long id, MultipartFile file) {
        return changePhoto(coffeeBeanRepository.findById(id),
                () -> fileStorageService.store(file, "coffee-beans"));
    }

    @Override
    public Optional<CoffeeBeanResponse> deletePhotoGlobal(Long id) {
        return changePhoto(coffeeBeanRepository.findById(id), () -> null);
    }

    private Optional<CoffeeBeanResponse> changePhoto(Optional<CoffeeBean> bean, Supplier<String> newPhotoUrl) {
        return bean.map(coffeeBean -> {
            String oldUrl = coffeeBean.getPhotoUrl();
            coffeeBean.setPhotoUrl(newPhotoUrl.get());
            CoffeeBean saved = coffeeBeanRepository.save(coffeeBean);
            fileStorageService.delete(oldUrl);
            Double average = brewEntryRepository.findAverageRatingByCoffeeId(saved.getId());
            Integer count = brewEntryRepository.countByCoffeeBean_Id(saved.getId());
            return coffeeBeanMapper.toResponse(saved, average != null ? average : 0.0, count);
        });
    }

    private CoffeeBeanResponse toResponse(CoffeeBean bean) {
        Double average = brewEntryRepository.findAverageRatingByCoffeeId(bean.getId());
        Integer count = brewEntryRepository.countByCoffeeBean_Id(bean.getId());
        return coffeeBeanMapper.toResponse(coffeeBeanRepository.save(bean), average != null ? average : 0.0, count);
    }

    private AdminCoffeeBeanResponse toAdminResponse(CoffeeBean bean) {
        Double average = brewEntryRepository.findAverageRatingByCoffeeId(bean.getId());
        Integer count = brewEntryRepository.countByCoffeeBean_Id(bean.getId());
        return coffeeBeanMapper.toAdminResponse(coffeeBeanRepository.save(bean), average != null ? average : 0.0, count);
    }
}
