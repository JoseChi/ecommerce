package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.*;
import com.algorithm.ecommerce.repository.*;
import com.algorithm.ecommerce.service.EmailService; // <--- 1. IMPORTANTE: Importar el servicio de email

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
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
    private EmailService emailService; // <--- 2. INYECCIÓN DEL SERVICIO DE EMAIL

    // 1. PROCESO DE CHECKOUT MEJORADO
    @PostMapping("/checkout/{userId}")
    @Transactional
    public ResponseEntity<?> createOrder(@PathVariable Long userId) {

        System.out.println("Iniciando checkout para el usuario ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("El carrito está vacío.");
        }

        // --- VALIDACIÓN DE STOCK ANTES DE CREAR NADA ---
        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();
            if (product.getStock() < cart.getQuantity()) {
                // Si falta stock, detenemos todo y avisamos qué producto falló
                return ResponseEntity.badRequest().body("❌ Stock insuficiente para: " + product.getName());
            }
        }

        // --- CREACIÓN DE LA ORDEN ---
        Order order = new Order();
        order.setUser(user);
        order.setStatus("PAGADO"); // Asumimos pagado porque viene de Stripe

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();

            // 1. RESTAR STOCK (Protección de inventario)
            product.setStock(product.getStock() - cart.getQuantity());
            productRepository.save(product);

            // 2. Crear detalle
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItems.add(orderItem);

            BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(cart.getQuantity()));
            total = total.add(subtotal);
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("El total no puede ser 0.");
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        cartRepository.deleteAll(cartItems);

        System.out.println("Checkout completado. Orden ID: " + savedOrder.getId());

        // --- 3. ENVIAR CORREO DE CONFIRMACIÓN (Async) ---
        // Lo ejecutamos en un hilo aparte para no hacer esperar al usuario
        try {
            String userEmail = user.getEmail();
            if (userEmail != null && !userEmail.isEmpty()) {
                new Thread(() -> {
                    // Convertimos BigDecimal a Double para el servicio de email
                    emailService.sendOrderConfirmation(
                            userEmail,
                            savedOrder.getId(),
                            savedOrder.getTotalAmount().doubleValue()
                    );
                }).start();
            }
        } catch (Exception e) {
            System.err.println("No se pudo enviar el correo, pero la orden sí se guardó: " + e.getMessage());
        }

        // Devolvemos la orden guardada con estatus 200 OK
        return ResponseEntity.ok(savedOrder);
    }

    // 2. CANCELAR ORDEN
    @PutMapping("/cancel/{orderId}")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if ("CANCELADO".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("La orden ya estaba cancelada.");
        }

        System.out.println("Cancelando orden ID: " + orderId + ". Devolviendo stock...");

        // Devolver productos al inventario
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("CANCELADO");
        Order updatedOrder = orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }

    // 3. Ver historial de pedidos
    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return orderRepository.findByUser(user);
    }

    // 4. Detalle de orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        return ResponseEntity.ok(order);
    }
}