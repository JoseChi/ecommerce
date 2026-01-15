package com.algorithm.ecommerce.service;

import com.algorithm.ecommerce.entity.*;
import com.algorithm.ecommerce.repository.CartRepository;
import com.algorithm.ecommerce.repository.OrderRepository;
import com.algorithm.ecommerce.repository.ProductRepository;
import com.algorithm.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // <--- Importante para el dinero
import java.time.LocalDateTime; // <--- Importante para la fecha
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Order createOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        Order order = new Order();
        order.setUser(user);
        // 1. CORRECCIÓN FECHA: Usamos LocalDateTime en lugar de Date
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PAGADO");

        // 2. CORRECCIÓN DINERO: Usamos BigDecimal para sumar
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // --- VALIDACIÓN DE STOCK ---
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName()
                        + ". Quedan: " + product.getStock());
            }

            // Restar stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Crear Item de Orden
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setOrder(order);

            orderItems.add(orderItem);

            // 3. CÁLCULO PRECIO: Convertimos Double a BigDecimal para sumar sin errores
            BigDecimal price = product.getPrice();
            BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
            BigDecimal itemTotal = price.multiply(quantity);

            totalAmount = totalAmount.add(itemTotal);
        }

        order.setItems(orderItems);
        // Asignamos el total convertido
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        cartRepository.deleteByUser(user);

        return savedOrder;
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}