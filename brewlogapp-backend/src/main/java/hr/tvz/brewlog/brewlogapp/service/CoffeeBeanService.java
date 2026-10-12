package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface CoffeeBeanService {

    List<CoffeeBeanResponse> findAllCoffeeBeans(String username);

    Optional<CoffeeBeanResponse> findCoffeeBeanById(Long id, String username);

    Optional<CoffeeBeanResponse> findCoffeeBeanByName(String name, String username);

    List<CoffeeBeanResponse> findCoffeeBeansByOrigin(String origin, String username);

    CoffeeBeanResponse createCoffeeBean(CoffeeBeanCreateRequest request, String username);

    Optional<CoffeeBeanResponse> addFlavorNote(Long coffeeBeanId, Long flavorNoteId, String username);

    Optional<CoffeeBeanResponse> removeFlavorNote(Long coffeeBeanId, Long flavorNoteId, String username);

    boolean removeCoffeeBean(Long id, String username);

    Optional<CoffeeBeanResponse> updateCoffeeBean(Long id, CoffeeBeanUpdateRequest request, String username);

    Optional<CoffeeBeanResponse> setFavorite(Long id, boolean value, String username);

    Optional<CoffeeBeanResponse> setFinished(Long id, boolean value, String username);

    List<AdminCoffeeBeanResponse> findAllCoffeeBeansGlobal();

    Optional<AdminCoffeeBeanResponse> findCoffeeBeanByIdGlobal(Long id);

    boolean removeCoffeeBeanByIdGlobal(Long id);

    Optional<AdminCoffeeBeanResponse> updateCoffeeBeanGlobal(Long id, CoffeeBeanUpdateRequest request);

    Optional<CoffeeBeanResponse> uploadPhoto(Long id, MultipartFile file, String username);

    Optional<CoffeeBeanResponse> deletePhoto(Long id, String username);

    Optional<CoffeeBeanResponse> uploadPhotoGlobal(Long id, MultipartFile file);

    Optional<CoffeeBeanResponse> deletePhotoGlobal(Long id);
}
