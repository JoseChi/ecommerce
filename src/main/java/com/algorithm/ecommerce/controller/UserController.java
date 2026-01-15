package com.algorithm.ecommerce.controller;

import com.algorithm.ecommerce.entity.User;
import com.algorithm.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173") // <--- ¡Vital para React!
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 1. REGISTRO DE USUARIO
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        System.out.println("Registrando usuario: " + user.getUsername());
        return userRepository.save(user);
    }

    // 2. LOGIN DE USUARIO
    @PostMapping("/login")
    public String login(@RequestBody User loginDetails) {
        return userRepository.findByEmail(loginDetails.getEmail())
                .map(user -> {
                    if (user.getPassword().equals(loginDetails.getPassword())) {
                        // En un futuro, aquí devolveríamos un JSON con el token,
                        // pero por ahora mantenemos tu lógica simple.
                        return "Login exitoso. ¡Bienvenido " + user.getFirstName() + "!";
                    } else {
                        return "Error: Contraseña incorrecta.";
                    }
                })
                .orElse("Error: El usuario con email " + loginDetails.getEmail() + " no existe.");
    }

    // 3. LISTAR TODOS LOS USUARIOS
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 4. OBTENER UN USUARIO POR ID (Usado para cargar el perfil)
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // 5. NUEVO: ACTUALIZAR PERFIL (Dirección, Teléfono, etc.)
    @PutMapping("/{id}")
    public User updateUserProfile(@PathVariable Long id, @RequestBody User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    // Solo actualizamos los campos de contacto/dirección
                    user.setAddress(userDetails.getAddress());
                    user.setCity(userDetails.getCity());
                    user.setZipCode(userDetails.getZipCode());
                    user.setPhoneNumber(userDetails.getPhoneNumber());

                    // Si quisieras permitir cambiar nombre/apellido:
                    if(userDetails.getFirstName() != null) user.setFirstName(userDetails.getFirstName());
                    if(userDetails.getLastName() != null) user.setLastName(userDetails.getLastName());

                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
}