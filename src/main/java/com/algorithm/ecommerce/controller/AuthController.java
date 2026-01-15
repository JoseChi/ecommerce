package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.User;
import com.algorithm.ecommerce.repository.UserRepository;
import com.algorithm.ecommerce.dto.AuthRequest;
import com.algorithm.ecommerce.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // --- LOGIN ---
    // Cambié Map<String, String> a Map<String, Object> para poder enviar el ID numérico
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody AuthRequest authRequest) {

        User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.getPassword().equals(authRequest.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // MODIFICACIÓN: Pasamos el ID al generar el token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);

        // RESPALDO: Enviamos también los datos claros por si el frontend no quiere decodificar
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        return response;
    }

    // --- REGISTRO ---
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody AuthRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: El nombre de usuario ya está en uso");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());
        newUser.setRole("ROLE_USER");
        newUser.setFirstName("Usuario");
        newUser.setLastName("Nuevo");
        newUser.setEmail(request.getUsername() + "@ejemplo.com");

        // Guardamos para que la base de datos genere el ID
        User savedUser = userRepository.save(newUser);

        // MODIFICACIÓN: Usamos el ID generado
        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente");
        response.put("token", token);
        response.put("id", savedUser.getId());

        return response;
    }
}