package com.project.extension.infrastructure.queue;

import com.project.extension.application.usecases.GerarPdfUseCase;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.dto.OrcamentoPdfProdResponseDTO;
import com.project.extension.domain.dto.OrcamentoPdfResponseDTO;
import com.project.extension.domain.exception.GeracaoPdfException;
import com.project.extension.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

public class RabbitMQListener {

    @Component
    @Profile("development")
    public static class Dev {
        private static final Logger log = LoggerFactory.getLogger(Dev.class);

        private final GerarPdfUseCase useCase;
        private final RabbitTemplate rabbitTemplate;

        public Dev(GerarPdfUseCase useCase, RabbitTemplate rabbitTemplate) {
            this.useCase = useCase;
            this.rabbitTemplate = rabbitTemplate;
        }

        @RabbitListener(queues = "fila.orcamento.pdf")
        public void receberMensagem(OrcamentoDTO payload) {
            try {
                log.info("Mensagem recebida da fila para o orçamento ID: {} número: {}", payload.id(), payload.numeroOrcamento());
                byte[] pdfBytes = useCase.executar(payload);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.RESPONSE_FANOUT_EXCHANGE,
                        "",
                        new OrcamentoPdfResponseDTO(payload.id(), payload.numeroOrcamento(), pdfBytes)
                );
            } catch (GeracaoPdfException e) {
                log.error("Erro no processamento assíncrono: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    @Component
    @Profile("production")
    public static class Prod {
        private static final Logger log = LoggerFactory.getLogger(Prod.class);

        private final GerarPdfUseCase useCase;
        private final RabbitTemplate rabbitTemplate;

        public Prod(GerarPdfUseCase useCase, RabbitTemplate rabbitTemplate) {
            this.useCase = useCase;
            this.rabbitTemplate = rabbitTemplate;
        }

        @RabbitListener(queues = "fila.orcamento.pdf")
        public void receberMensagem(OrcamentoDTO payload) {
            try {
                log.info("Mensagem recebida da fila para o orçamento ID: {} número: {}", payload.id(), payload.numeroOrcamento());
                useCase.executar(payload);

                String nomeArquivo = "orcamento_" + payload.numeroOrcamento() + ".pdf";
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.RESPONSE_FANOUT_EXCHANGE,
                        "",
                        new OrcamentoPdfProdResponseDTO(payload.id(), payload.numeroOrcamento(), nomeArquivo)
                );
            } catch (GeracaoPdfException e) {
                log.error("Erro no processamento assíncrono: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
}
