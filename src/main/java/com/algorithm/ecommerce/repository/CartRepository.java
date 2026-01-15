package com.algorithm.ecommerce.repository;

import com.algorithm.ecommerce.entity.CartItem;
import com.algorithm.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {

    // Buscar items del usuario
    List<CartItem> findByUser(User user);

    // --- ESTE ES EL QUE NECESITAS PARA LA LÍNEA 85 ---
    void deleteByUser(User user);
}