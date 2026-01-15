package com.algorithm.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan; // <--- 1. Importar esto

@SpringBootApplication
// 2. Agregar esta línea para obligar a Spring a leer tu seguridad
@ComponentScan(basePackages = {
		"com.algorithm.ecommerce",
		"com.algorithm.ecommerce.security",
		"com.algorithm.ecommerce.controller",
		"com.algorithm.ecommerce.service",
		"com.algorithm.ecommerce.repository"
})
public class AlgorithmEcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlgorithmEcommerceApplication.class, args);
	}
}