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
import java.util.Map; // Importante para recibir el paymentId

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

    // 1. PROCESO DE CHECKOUT MEJORADO (Con Envío y Stripe)
    @PostMapping("/checkout/{userId}")
    @Transactional
    public ResponseEntity<?> createOrder(@PathVariable Long userId, @RequestBody Map<String, String> paymentData) {
        System.out.println("Iniciando checkout para el usuario ID: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        // A. VALIDAR DIRECCIÓN
        // No podemos enviar si no sabemos a dónde.
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Error: No tienes una dirección de envío registrada. Por favor ve a 'Mi Cuenta' y actualiza tu dirección antes de comprar.");
        }

        List<CartItem> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body("El carrito está vacío.");
        }

        // B. PREPARAR LA ORDEN
        Order order = new Order();
        order.setUser(user);
        order.setStatus("PAGADO"); // Asumimos que si llega aquí, Stripe ya cobró

        // Guardamos la dirección "congelada" en el momento de la compra
        String fullAddress = user.getAddress() + ", " + user.getCity() + ", CP " + user.getZipCode();
        order.setShippingAddress(fullAddress);

        // Guardamos el ID del pago de Stripe (ej. pi_3Mg...)
        if (paymentData.containsKey("paymentId")) {
            order.setPaymentId(paymentData.get("paymentId"));
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal productsTotal = BigDecimal.ZERO;

        // C. PROCESAR PRODUCTOS Y STOCK
        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();

            // Validación de Stock
            if (product.getStock() < cart.getQuantity()) {
                return ResponseEntity.badRequest().body("❌ Stock insuficiente para: " + product.getName());
            }

            // Restar Stock
            product.setStock(product.getStock() - cart.getQuantity());
            productRepository.save(product);

            // Crear detalle del item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItems.add(orderItem);

            // Sumar al subtotal de productos
            BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(cart.getQuantity()));
            productsTotal = productsTotal.add(subtotal);
        }

        // D. CÁLCULO DE LOGÍSTICA DE ENVÍO 🚚
        BigDecimal shippingCost = new BigDecimal("150.00"); // Costo base de envío
        BigDecimal freeShippingThreshold = new BigDecimal("999.00"); // Envío gratis arriba de esto

        // Si el total de productos es mayor o igual a 999, el envío es GRATIS
        if (productsTotal.compareTo(freeShippingThreshold) >= 0) {
            shippingCost = BigDecimal.ZERO;
        }

        // Guardamos costos
        order.setShippingCost(shippingCost);
        order.setTotalAmount(productsTotal.add(shippingCost)); // Total Final = Productos + Envío
        order.setItems(orderItems);

        // E. GUARDAR TODO
        Order savedOrder = orderRepository.save(order);
        cartRepository.deleteAll(cartItems);

        // F. ENVIAR CORREO DE CONFIRMACIÓN (Hilo secundario para no trabar la respuesta)
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
        // Ordenamos por ID descendente para ver las nuevas primero
        return orderRepository.findByUser(user);
    }

    // 4. Detalle de orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        return ResponseEntity.ok(order);
    }

    // 5. ENDPOINT PARA EL ADMINISTRADOR (Listar todo)
    @GetMapping("/admin/all")
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 6. CAMBIAR ESTADO DE LA ORDEN (Para Admin: ENVIADO, ENTREGADO)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        return ResponseEntity.ok(updatedOrder);
    }
}