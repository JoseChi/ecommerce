package com.algorithm.ecommerce.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:5173") // Permitir acceso desde React
public class MediaController {

    // Definimos la carpeta donde se guardarán los archivos
    // Esta carpeta debe estar en la raíz del proyecto (junto a pom.xml)
    private static final String UPLOAD_DIR = "uploads/";

    // 1. SUBIR IMAGEN
    // POST http://localhost:8080/api/media/upload
    @PostMapping("/upload")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // A. Asegurar que la carpeta "uploads" exista
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // B. Generar nombre único para evitar sobrescribir fotos
            // Ejemplo: "a1b2c3d4-mi_foto.jpg"
            String originalName = file.getOriginalFilename();
            String extension = "";
            if(originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // C. Guardar el archivo físicamente en el disco
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // D. Devolver la URL pública
            // GRACIAS A TU WebConfig, Spring servirá esto en /images/
            String fileUrl = "http://localhost:8080/images/" + filename;

            return Collections.singletonMap("url", fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al subir archivo: " + e.getMessage());
        }
    }
}