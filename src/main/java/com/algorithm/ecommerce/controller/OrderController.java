package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.*;
import com.algorithm.ecommerce.repository.*;
import com.algorithm.ecommerce.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort; // <--- IMPORTANTE: Para ordenar por fecha

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
// Permitimos acceso desde Vercel y Localhost
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
    public ResponseEntity<?> createOrder(@PathVariable Long userId) {
        System.out.println("Iniciando checkout para el usuario ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("El carrito está vacío.");
        }

        // Validación de stock
        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();
            if (product.getStock() < cart.getQuantity()) {
                return ResponseEntity.badRequest().body("❌ Stock insuficiente para: " + product.getName());
            }
        }

        // Creación de la Orden
        Order order = new Order();
        order.setUser(user);
        order.setStatus("PAGADO");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();

            // Restar Stock
            product.setStock(product.getStock() - cart.getQuantity());
            productRepository.save(product);

            // Crear detalle
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

        // Enviar correo (Async)
        try {
            String userEmail = user.getEmail();
            if (userEmail != null && !userEmail.isEmpty()) {
                new Thread(() -> {
                    emailService.sendOrderConfirmation(
                            userEmail,
                            savedOrder.getId(),
                            savedOrder.getTotalAmount().doubleValue()
                    );
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error enviando correo: " + e.getMessage());
        }

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

        // Devolver stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("CANCELADO");
        Order updatedOrder = orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }

    // 3. Ver historial de un usuario específico
    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        // Ordenamos para ver la más reciente primero
        return orderRepository.findByUser(user);
    }

    // 4. Detalle de orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        return ResponseEntity.ok(order);
    }

    // 5. --- NUEVO: ENDPOINT PARA EL ADMINISTRADOR ---
    // Trae TODAS las órdenes de la base de datos
    @GetMapping("/admin/all")
    public List<Order> getAllOrders() {
        // Sort.by(Sort.Direction.DESC, "id") hará que las últimas ventas salgan primero
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
}