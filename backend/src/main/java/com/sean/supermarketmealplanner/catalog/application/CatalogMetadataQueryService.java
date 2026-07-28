package com.sean.supermarketmealplanner.catalog.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.AllergenRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.CategoryRepository;
import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.DietaryTagRepository;
import com.sean.supermarketmealplanner.supermarket.domain.SupermarketCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogMetadataQueryService {

    private final CategoryRepository categoryRepository;
    private final DietaryTagRepository dietaryTagRepository;
    private final AllergenRepository allergenRepository;

    public CatalogMetadataQueryService(
            CategoryRepository categoryRepository,
            DietaryTagRepository dietaryTagRepository,
            AllergenRepository allergenRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.dietaryTagRepository = dietaryTagRepository;
        this.allergenRepository = allergenRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findCategories(SupermarketCode supermarketCode) {
        var categories = supermarketCode == null
                ? categoryRepository.findAllByActiveTrueOrderByNameAsc()
                : categoryRepository.findAllBySupermarketCodeAndActiveTrueOrderByNameAsc(
                        supermarketCode
                );
        return categories.stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getExternalId(),
                        category.getName(),
                        category.getParentCategory() == null
                                ? null
                                : category.getParentCategory().getId(),
                        category.getSupermarket().getCode().name()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogOptionResponse> findDietaryTags() {
        return dietaryTagRepository.findAllByOrderByNameAsc().stream()
                .map(tag -> new CatalogOptionResponse(tag.getId(), tag.getCode(), tag.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogOptionResponse> findAllergens() {
        return allergenRepository.findAllByOrderByNameAsc().stream()
                .map(allergen -> new CatalogOptionResponse(
                        allergen.getId(),
                        allergen.getCode(),
                        allergen.getName()
                ))
                .toList();
    }
}
