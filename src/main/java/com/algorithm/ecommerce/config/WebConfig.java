package com.algorithm.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapeamos la URL "/images/**" a la carpeta física "uploads/"
        // Ejemplo: http://localhost:8080/images/foto.jpg -> buscará en la carpeta uploads/foto.jpg
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/");
    }
}