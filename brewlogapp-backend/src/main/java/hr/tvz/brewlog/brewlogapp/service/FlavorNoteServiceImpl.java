package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteUpdateRequest;
import hr.tvz.brewlog.brewlogapp.enums.Category;
import hr.tvz.brewlog.brewlogapp.exception.DuplicateResourceException;
import hr.tvz.brewlog.brewlogapp.mapper.FlavorNoteMapper;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.FlavorNoteRepository;
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
public class FlavorNoteServiceImpl implements FlavorNoteService {

    private final FlavorNoteRepository flavorNoteRepository;
    private final FlavorNoteMapper flavorNoteMapper;
    private final UserRepository userRepository;

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public List<FlavorNoteResponse> findAllFlavorNotes(String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findAllVisibleToUser(user).stream()
                .map(flavorNoteMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<FlavorNoteResponse> findFlavorNoteById(Long id, String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findByIdVisibleToUser(id, user)
                .map(flavorNoteMapper::toResponse);
    }

    @Override
    public Optional<FlavorNoteResponse> findFlavorNoteByName(String name, String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findByNameIgnoreCaseVisibleToUser(name, user)
                .map(flavorNoteMapper::toResponse);
    }

    @Override
    public List<FlavorNoteResponse> findFlavorNotesByCategory(String categoryName, String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findByCategoryVisibleToUser(Category.valueOf(categoryName), user).stream()
                .map(flavorNoteMapper::toResponse)
                .toList();
    }

    @Override
    public List<FlavorNoteResponse> findFlavorNotesByCoffeeBeanId(Long coffeeBeanId, String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findByCoffeeBeanIdVisibleToUser(coffeeBeanId, user).stream()
                .map(flavorNoteMapper::toResponse)
                .toList();
    }

    @Override
    public FlavorNoteResponse createFlavorNote(FlavorNoteCreateRequest request, String username) {
        User user = resolveUser(username);
        String name = request.name().trim();
        if (nameTakenForUserOrGlobal(name, user)) {
            throw new DuplicateResourceException("This flavor already exists.");
        }
        FlavorNote flavorNote = flavorNoteMapper.toEntity(request);
        flavorNote.setName(name);
        flavorNote.setUser(user);
        flavorNote = flavorNoteRepository.save(flavorNote);
        return flavorNoteMapper.toResponse(flavorNote);
    }


    @Override
    public boolean removeFlavorNote(Long id, String username) {
        User user = resolveUser(username);
        Optional<FlavorNote> existing = flavorNoteRepository.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return false;
        }
        detachAndDelete(existing.get());
        return true;
    }

    @Override
    public Optional<FlavorNoteResponse> updateFlavorNote(Long id, FlavorNoteUpdateRequest request, String username) {
        User user = resolveUser(username);
        return flavorNoteRepository.findByIdAndUser(id, user)
                .map(flavorNote -> {
                    String name = request.name() == null ? null : request.name().trim();
                    boolean nameChanged = name != null && !name.equalsIgnoreCase(flavorNote.getName());
                    if (nameChanged && nameTakenForUserOrGlobalExcluding(name, user, id)) {
                        throw new DuplicateResourceException("This flavor already exists.");
                    }
                    flavorNoteMapper.updateEntityFromRequest(request, flavorNote);
                    if (name != null) {
                        flavorNote.setName(name);
                    }
                    return flavorNoteMapper.toResponse(flavorNoteRepository.save(flavorNote));
                });
    }

    @Override
    public List<FlavorNoteResponse> findAllFlavorNotesGlobal() {
        return flavorNoteRepository.findAllByUserIsNull().stream()
                .map(flavorNoteMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<FlavorNoteResponse> findFlavorNoteByIdGlobal(Long id) {
        return flavorNoteRepository.findByIdAndUserIsNull(id)
                .map(flavorNoteMapper::toResponse);
    }

    @Override
    public Optional<FlavorNoteResponse> findFlavorNoteByNameGlobal(String name) {
        return flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull(name)
                .map(flavorNoteMapper::toResponse);
    }

    @Override
    public FlavorNoteResponse createFlavorNoteGlobal(FlavorNoteCreateRequest request) {
        String name = request.name().trim();
        if (flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull(name).isPresent()) {
            throw new DuplicateResourceException("This flavor already exists.");
        }
        FlavorNote flavorNote = flavorNoteMapper.toEntity(request);
        flavorNote.setName(name);
        flavorNote.setUser(null);
        flavorNote = flavorNoteRepository.save(flavorNote);
        return flavorNoteMapper.toResponse(flavorNote);
    }


    @Override
    public boolean removeFlavorNoteGlobal(Long id) {
        Optional<FlavorNote> existing = flavorNoteRepository.findByIdAndUserIsNull(id);
        if (existing.isEmpty()) {
            return false;
        }
        detachAndDelete(existing.get());
        return true;
    }

    @Override
    public Optional<FlavorNoteResponse> updateFlavorNoteGlobal(Long id, FlavorNoteUpdateRequest request) {
        return flavorNoteRepository.findByIdAndUserIsNull(id)
                .map(flavorNote -> {
                    String name = request.name() == null ? null : request.name().trim();
                    if (name != null) {
                        Optional<FlavorNote> duplicate = flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull(name)
                                .filter(note -> !note.getId().equals(id));
                        if (duplicate.isPresent()) {
                            throw new DuplicateResourceException("This flavor already exists.");
                        }
                    }
                    flavorNoteMapper.updateEntityFromRequest(request, flavorNote);
                    if (name != null) {
                        flavorNote.setName(name);
                    }
                    return flavorNoteMapper.toResponse(flavorNoteRepository.save(flavorNote));
                });
    }

    private boolean nameTakenForUserOrGlobal(String name, User user) {
        return flavorNoteRepository.findByNameIgnoreCaseAndUser(name, user).isPresent()
                || flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull(name).isPresent();
    }

    private boolean nameTakenForUserOrGlobalExcluding(String name, User user, Long excludeId) {
        boolean ownDuplicate = flavorNoteRepository.findByNameIgnoreCaseAndUser(name, user)
                .filter(note -> !note.getId().equals(excludeId))
                .isPresent();
        boolean globalDuplicate = flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull(name).isPresent();
        return ownDuplicate || globalDuplicate;
    }

    private void detachAndDelete(FlavorNote note) {
        for (CoffeeBean bean : note.getCoffeeBean()) {
            bean.getFlavorNotes().remove(note);
        }
        flavorNoteRepository.delete(note);
    }
}
