package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.CartItem;
import com.algorithm.ecommerce.entity.Product;
import com.algorithm.ecommerce.entity.User;
import com.algorithm.ecommerce.repository.CartRepository;
import com.algorithm.ecommerce.repository.ProductRepository;
import com.algorithm.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    // Agregar producto al carrito
    @PostMapping("/add")
    public String addToCart(@RequestParam Long userId, @RequestParam Long productId, @RequestParam Integer quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        CartItem item = new CartItem();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);

        cartRepository.save(item);
        return "Agregaste " + quantity + " unidades de " + product.getName() + " al carrito.";
    }

    // Ver el carrito de un usuario
    @GetMapping("/user/{userId}")
    public List<CartItem> getCartByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return cartRepository.findByUser(user);
    }

    // Eliminar un item del carrito
    @DeleteMapping("/remove/{cartItemId}")
    public String removeFromCart(@PathVariable Long cartItemId) {
        cartRepository.deleteById(cartItemId);
        return "Producto eliminado del carrito.";
    }
}