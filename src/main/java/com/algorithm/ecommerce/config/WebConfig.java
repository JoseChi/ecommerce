package com.algorithm.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 1. CONFIGURACIÓN DE IMÁGENES (Mantenemos esto para que se vean las fotos) 📸
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/");
    }

    // 2. CONFIGURACIÓN DE SEGURIDAD (CORS) - ¡Esto arregla Vercel! 🌍
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a TODAS las rutas (/products, /auth, etc)
                .allowedOriginPatterns("*") // Permite CUALQUIER dominio (incluido Vercel)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permite todos los verbos
                .allowedHeaders("*") // Permite todos los headers
                .allowCredentials(true); // Permite cookies y tokens
    }
}