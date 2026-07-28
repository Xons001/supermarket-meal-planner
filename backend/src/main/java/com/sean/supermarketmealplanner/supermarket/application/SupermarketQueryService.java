package com.sean.supermarketmealplanner.supermarket.application;

import com.sean.supermarketmealplanner.supermarket.infrastructure.persistence.SupermarketRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupermarketQueryService {

    private final SupermarketRepository supermarketRepository;

    public SupermarketQueryService(SupermarketRepository supermarketRepository) {
        this.supermarketRepository = supermarketRepository;
    }

    @Transactional(readOnly = true)
    public List<SupermarketResponse> findAll() {
        return supermarketRepository.findAll(Sort.by(
                        Sort.Order.desc("enabled"),
                        Sort.Order.asc("name")
                )).stream()
                .map(supermarket -> new SupermarketResponse(
                        supermarket.getCode().name(),
                        supermarket.getName(),
                        supermarket.isEnabled(),
                        supermarket.getCatalogSource().name(),
                        supermarket.getCountryCode(),
                        supermarket.getCurrencyCode()
                ))
                .toList();
    }
}
