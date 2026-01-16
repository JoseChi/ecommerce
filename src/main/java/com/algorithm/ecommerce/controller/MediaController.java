package com.algorithm.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder; // <--- ¡IMPORTANTE NUEVA IMPORTACIÓN!

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
// Permitimos acceso desde cualquier lugar (*) para evitar bloqueos cuando subas el Front a la nube
@CrossOrigin(origins = "*")
public class MediaController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // A. Asegurar que la carpeta exista
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // B. Generar nombre único
            String originalName = file.getOriginalFilename();
            String extension = "";
            if(originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // C. Guardar el archivo físicamente
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // D. Devolver la URL pública (DINÁMICA) 🧠
            // Esto detecta si estás en localhost o en Railway automáticamente
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/")
                    .path(filename)
                    .toUriString();

            return Collections.singletonMap("url", fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al subir archivo: " + e.getMessage());
        }
    }
}