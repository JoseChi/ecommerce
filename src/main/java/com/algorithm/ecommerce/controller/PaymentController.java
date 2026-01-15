package com.algorithm.ecommerce.controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173") // Permite que React hable con este controlador
public class PaymentController {

    // Leemos la clave desde application.properties
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostMapping("/create-payment-intent")
    public Map<String, String> createPaymentIntent(@RequestBody Map<String, Object> data) {
        // Inicializamos Stripe con tu clave secreta
        Stripe.apiKey = stripeApiKey;

        // Leemos el monto que envía el Frontend (en centavos, no en pesos)
        // Ejemplo: $500.00 MXN -> 50000 centavos
        int amount = (int) data.get("amount");

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) amount)
                .setCurrency("mxn") // Moneda: Pesos Mexicanos
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        try {
            // Creamos la intención de pago en Stripe
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Devolvemos el "secreto" al Frontend para que pueda terminar el pago
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al crear PaymentIntent: " + e.getMessage());
        }
    }
}