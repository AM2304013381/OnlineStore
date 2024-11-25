package com.system.store.services;

import com.system.store.models.Product;
import com.system.store.repositories.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductsRepository productsRepository;

    public List<Product> findAll() {
        return productsRepository.findAll(Sort.by("id"));
    }

    public List<Product> searchByNameorCategory(String query) {
        return productsRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query);
    }
}
