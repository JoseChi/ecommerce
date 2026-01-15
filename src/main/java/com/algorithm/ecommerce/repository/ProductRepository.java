package com.algorithm.ecommerce.repository;

import com.algorithm.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Filtra productos por la categoría exacta (Playera, Hoddie, etc.)
    Page<Product> findByCategory(String category, Pageable pageable);

    // Busca productos que contengan el nombre, ignorando mayúsculas y minúsculas
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}