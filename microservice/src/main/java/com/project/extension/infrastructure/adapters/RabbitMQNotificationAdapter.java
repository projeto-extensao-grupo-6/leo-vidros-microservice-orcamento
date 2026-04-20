package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.NotificationPDFService;
import com.project.extension.domain.dto.PdfResponseDTO;
import com.project.extension.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQNotificationAdapter implements NotificationPDFService {
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    public RabbitMQNotificationAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void notificarPdfPronto(String numeroOrcamento, String referencia, byte[] pdfBytes) {
        PdfResponseDTO responseDTO = new PdfResponseDTO(numeroOrcamento, referencia, activeProfile.contains("production") ? null : pdfBytes);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.RESPONSE_ROUTING_KEY,
                responseDTO
        );

        System.out.println("🚀 Notificação de resposta enviada para o back-end principal.");
    }
}