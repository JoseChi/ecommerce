package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.User;
import com.algorithm.ecommerce.repository.UserRepository;
import com.algorithm.ecommerce.dto.AuthRequest;
import com.algorithm.ecommerce.security.JwtUtil;

// --- IMPORTACIONES DE GOOGLE ---
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
// ------------------------------

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // Para generar contraseñas aleatorias a usuarios de Google

@RestController
@RequestMapping("/api/auth")
// Permitimos conexiones desde cualquier lado (Vercel/Localhost)
@CrossOrigin(originPatterns = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // TU CLIENT ID DE GOOGLE (El que acabamos de crear)
    private static final String GOOGLE_CLIENT_ID = "1089912669985-3j99seb7eol3vbdk3kqpirijh6ht0pof.apps.googleusercontent.com";

    // --- 1. LOGIN NORMAL (Usuario y Contraseña) ---
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody AuthRequest authRequest) {

        User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.getPassword().equals(authRequest.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        return response;
    }

    // --- 2. REGISTRO NORMAL ---
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

        User savedUser = userRepository.save(newUser);
        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente");
        response.put("token", token);
        response.put("id", savedUser.getId());

        return response;
    }

    // --- 3. LOGIN CON GOOGLE (NUEVO) ---
    @PostMapping("/google")
    public Map<String, Object> googleLogin(@RequestBody Map<String, String> payload) {
        String tokenGoogle = payload.get("token");

        try {
            // A. Configurar el verificador de Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();

            // B. Verificar el token recibido del Frontend
            GoogleIdToken idToken = verifier.verify(tokenGoogle);

            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();

                // C. Obtener datos del usuario de Google
                String email = googlePayload.getEmail();
                String name = (String) googlePayload.get("name");
                String givenName = (String) googlePayload.get("given_name"); // Nombre pila
                String familyName = (String) googlePayload.get("family_name"); // Apellido

                // D. Buscar si ya existe en nuestra base de datos (usamos el email como username)
                User user = userRepository.findByUsername(email).orElse(null);

                if (user == null) {
                    // SI NO EXISTE -> LO REGISTRAMOS AUTOMÁTICAMENTE
                    user = new User();
                    user.setUsername(email); // El usuario será su correo
                    user.setEmail(email);
                    user.setFirstName(givenName != null ? givenName : "Usuario");
                    user.setLastName(familyName != null ? familyName : "Google");
                    user.setRole("ROLE_USER");
                    // Generamos una contraseña aleatoria compleja porque entrará con Google
                    user.setPassword(UUID.randomUUID().toString());

                    user = userRepository.save(user);
                }

                // E. Generar nuestro propio token JWT (Igual que en el login normal)
                String jwtToken = jwtUtil.generateToken(user.getUsername(), user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("token", jwtToken);
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("role", user.getRole());

                return response;

            } else {
                throw new RuntimeException("Token de Google inválido");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error en autenticación con Google: " + e.getMessage());
        }
    }
}