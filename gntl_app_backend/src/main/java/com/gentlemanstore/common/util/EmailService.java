package com.gentlemanstore.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWelcomeEmail(String to, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to Gentleman Store!");
        message.setText("Dear " + firstName + ",\n\n" +
                "Welcome to Gentleman Store! Your account has been successfully created.\n\n" +
                "Best regards,\nGentleman Store Team");
        mailSender.send(message);
    }

    @Transactional()
    public void sendOrderConfirmationEmail(String to, String firstName, Long orderId, BigDecimal totalPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order Confirmation #" + orderId);
        message.setText("Dear " + firstName + ",\n\n" +
                "Your order #" + orderId + " has been successfully placed.\n" +
                "Total amount: " + totalPrice + " RSD\n\n" +
                "Best regards,\nGentleman Store Team");
        mailSender.send(message);
    }

    @Transactional()
    public void sendOrderStatusEmail(String to, String firstName, Long orderId, String status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order #" + orderId + " Status Update");
        message.setText("Dear " + firstName + ",\n\n" +
                "The status of your order #" + orderId + " has been updated to: " + status + "\n\n" +
                "Best regards,\nGentleman Store Team");
        mailSender.send(message);
    }
}
