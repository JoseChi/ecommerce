package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.Order;
import com.algorithm.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/stats")
    public Map<String, Object> getSalesStats() {
        // 1. Obtener todas las órdenes pagadas
        List<Order> paidOrders = orderRepository.findAll().stream()
                .filter(o -> "PAGADO".equals(o.getStatus()))
                .toList();

        // 2. Obtener todas las órdenes canceladas
        long cancelledCount = orderRepository.findAll().stream()
                .filter(o -> "CANCELADO".equals(o.getStatus()))
                .count();

        // 3. Calcular ingresos totales
        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Preparar respuesta
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", paidOrders.size());
        stats.put("totalRevenue", totalRevenue);
        stats.put("cancelledOrders", cancelledCount);
        stats.put("averageOrderValue", paidOrders.isEmpty() ? 0 :
                totalRevenue.divide(new BigDecimal(paidOrders.size()), 2, BigDecimal.ROUND_HALF_UP));

        return stats;
    }
}