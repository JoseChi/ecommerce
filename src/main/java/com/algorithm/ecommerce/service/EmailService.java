package com.algorithm.ecommerce.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOrderConfirmation(String to, Long orderId, Double total) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("AlgorithmStore <noreply@algorithmstore.com>");
            helper.setTo(to);
            helper.setSubject("¡Tu Orden #" + orderId + " ha sido confirmada! 🚀");

            String htmlContent = """
                <div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden;'>
                    <div style='background-color: #000; color: #fff; padding: 20px; text-align: center;'>
                        <h1 style='margin: 0;'>Algorithm<span style='color: #3b82f6;'>Store</span></h1>
                    </div>
                    <div style='padding: 30px;'>
                        <h2 style='color: #000;'>¡Gracias por tu compra!</h2>
                        <p>Tu orden <strong>#%d</strong> ha sido procesada exitosamente.</p>
                        <p style='font-size: 18px; font-weight: bold;'>Total Pagado: <span style='color: #2563eb;'>$%.2f</span></p>
                        <hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>
                        <p style='font-size: 12px; color: #666;'>Pronto recibirás otro correo cuando tu paquete sea enviado.</p>
                        <a href='http://localhost:5173/orders' style='display: inline-block; background-color: #000; color: #fff; text-decoration: none; padding: 10px 20px; border-radius: 5px; font-weight: bold; margin-top: 10px;'>Ver mi Pedido</a>
                    </div>
                </div>
            """.formatted(orderId, total);

            helper.setText(htmlContent, true); // true = Es HTML
            mailSender.send(message);
            System.out.println("📧 Correo enviado a " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Error enviando correo: " + e.getMessage());
        }
    }
}