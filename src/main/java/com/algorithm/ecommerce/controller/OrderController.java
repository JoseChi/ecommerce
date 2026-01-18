package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.*;
import com.algorithm.ecommerce.repository.*;
import com.algorithm.ecommerce.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(originPatterns = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmailService emailService;

    // 1. PROCESO DE CHECKOUT
    @PostMapping("/checkout/{userId}")
    @Transactional
    public ResponseEntity<?> createOrder(@PathVariable Long userId, @RequestBody Map<String, String> paymentData) {
        // ... (Tu código de checkout sigue igual, no cambia nada aquí) ...
        // Para ahorrar espacio, asumo que dejas el método createOrder tal cual lo tenías,
        // ya que ese SÍ estaba correcto en tu versión anterior.
        // Solo asegúrate de que sea el que valida dirección y stock.

        // ... (Pega aquí tu lógica de createOrder que ya tenías) ...
        System.out.println("Iniciando checkout para el usuario ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Error: No tienes dirección.");
        }

        List<CartItem> cartItems = cartRepository.findByUser(user);
        if (cartItems.isEmpty()) return ResponseEntity.badRequest().body("Carrito vacío.");

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PAGADO");
        order.setShippingAddress(user.getAddress() + ", " + user.getCity() + ", CP " + user.getZipCode());

        if (paymentData.containsKey("paymentId")) {
            order.setPaymentId(paymentData.get("paymentId"));
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal productsTotal = BigDecimal.ZERO;

        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();
            if (product.getStock() < cart.getQuantity()) {
                return ResponseEntity.badRequest().body("❌ Stock insuficiente: " + product.getName());
            }
            product.setStock(product.getStock() - cart.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            productsTotal = productsTotal.add(product.getPrice().multiply(new BigDecimal(cart.getQuantity())));
        }

        BigDecimal shippingCost = new BigDecimal("150.00");
        if (productsTotal.compareTo(new BigDecimal("999.00")) >= 0) {
            shippingCost = BigDecimal.ZERO;
        }

        order.setShippingCost(shippingCost);
        order.setTotalAmount(productsTotal.add(shippingCost));
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        cartRepository.deleteAll(cartItems);

        try {
            String userEmail = user.getEmail();
            if (userEmail != null) {
                new Thread(() -> emailService.sendOrderConfirmation(userEmail, savedOrder.getId(), savedOrder.getTotalAmount().doubleValue())).start();
            }
        } catch (Exception e) { System.err.println("Error email: " + e.getMessage()); }

        return ResponseEntity.ok(savedOrder);
    }

    // 2. CANCELAR ORDEN
    @PutMapping("/cancel/{orderId}")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        if ("CANCELADO".equals(order.getStatus())) return ResponseEntity.badRequest().body("Ya estaba cancelada.");

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus("CANCELADO");
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // 3. GET USER ORDERS
    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return orderRepository.findByUser(user);
    }

    // 4. GET ORDER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        return ResponseEntity.ok(order);
    }

    // 5. GET ALL (ADMIN)
    @GetMapping("/admin/all")
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 6. --- ACTUALIZADO: CAMBIAR ESTADO Y AGREGAR GUÍA ---
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String trackingNumber // <--- AQUI ESTA LA MAGIA
    ) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        order.setStatus(status);

        // Si el admin mandó un número de guía, lo guardamos
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber);
        }

        Order updatedOrder = orderRepository.save(order);
        return ResponseEntity.ok(updatedOrder);
    }
}