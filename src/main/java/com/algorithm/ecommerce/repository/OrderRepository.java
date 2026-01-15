package com.algorithm.ecommerce.repository;

import com.algorithm.ecommerce.entity.Order;
import com.algorithm.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Método para buscar órdenes usando el objeto User completo
    List<Order> findByUser(User user);

    // --- ESTE ES EL QUE TE FALTABA PARA LA LÍNEA 91 ---
    // Spring Boot es listo: entiende que quieres buscar por el "id" dentro de "User"
    List<Order> findByUserId(Long userId);
}