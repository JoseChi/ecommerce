package com.algorithm.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    // El total final de la compra (Productos + Envío)
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalAmount;

    private String status; // "PENDIENTE", "PAGADO", "ENVIADO", "ENTREGADO", "CANCELADO"

    // --- 🚚 NUEVOS CAMPOS PARA LOGÍSTICA 🚚 ---

    @Column(name = "shipping_cost")
    private BigDecimal shippingCost; // Cuánto se cobró de envío (ej. 150.00 o 0.00)

    @Column(name = "payment_id")
    private String paymentId;        // El ID de Stripe (ej. pi_3Mg...) para rastrear el pago

    @Column(name = "shipping_address")
    private String shippingAddress;  // Guardamos la dirección escrita (snapshot) por si el usuario se muda después

    // ------------------------------------------

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        if (status == null) status = "PENDIENTE";
        if (shippingCost == null) shippingCost = BigDecimal.ZERO; // Por seguridad
    }

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    // --- GETTERS Y SETTERS DE LOS NUEVOS CAMPOS ---

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}