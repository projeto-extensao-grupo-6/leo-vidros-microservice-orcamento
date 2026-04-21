package com.project.extension.infrastructure.queue;

import com.project.extension.application.usecases.GerarPdfUseCase;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.dto.OrcamentoPdfProdResponseDTO;
import com.project.extension.domain.dto.OrcamentoPdfResponseDTO;
import com.project.extension.domain.exception.GeracaoPdfException;
import com.project.extension.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

public class RabbitMQListener {

    @Component
    @Profile("development")
    public static class Dev {
        private final GerarPdfUseCase useCase;
        private final RabbitTemplate rabbitTemplate;

        public Dev(GerarPdfUseCase useCase, RabbitTemplate rabbitTemplate) {
            this.useCase = useCase;
            this.rabbitTemplate = rabbitTemplate;
        }

        @RabbitListener(queues = "fila.orcamento.pdf")
        public void receberMensagem(OrcamentoDTO payload) {
            try {
                System.out.printf("Mensagem recebida da fila para o orcamento do ID: %d e número: %s\n", payload.id(), payload.numeroOrcamento());
                byte[] pdfBytes = useCase.executar(payload);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.RESPONSE_ROUTING_KEY,
                        new OrcamentoPdfResponseDTO(payload.id(), payload.numeroOrcamento(), pdfBytes)
                );
            } catch (GeracaoPdfException e) {
                System.err.println("Erro no processamento assíncrono: " + e.getMessage());
                throw e;
            }
        }
    }

    @Component
    @Profile("production")
    public static class Prod {
        private final GerarPdfUseCase useCase;
        private final RabbitTemplate rabbitTemplate;

        public Prod(GerarPdfUseCase useCase, RabbitTemplate rabbitTemplate) {
            this.useCase = useCase;
            this.rabbitTemplate = rabbitTemplate;
        }

        @RabbitListener(queues = "fila.orcamento.pdf")
        public void receberMensagem(OrcamentoDTO payload) {
            try {
                System.out.printf("Mensagem recebida da fila para o orcamento do ID: %d e número: %s\n", payload.id(), payload.numeroOrcamento());
                useCase.executar(payload);

                String nomeArquivo = "orcamento_" + payload.numeroOrcamento() + ".pdf";
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.RESPONSE_ROUTING_KEY,
                        new OrcamentoPdfProdResponseDTO(payload.id(), payload.numeroOrcamento(), nomeArquivo)
                );
            } catch (GeracaoPdfException e) {
                System.err.println("Erro no processamento assíncrono: " + e.getMessage());
                throw e;
            }
        }
    }
}
