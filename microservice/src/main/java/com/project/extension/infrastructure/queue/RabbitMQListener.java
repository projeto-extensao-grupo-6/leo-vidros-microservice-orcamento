package com.project.extension.infrastructure.queue;

import com.project.extension.application.usecases.GerarPdfUseCase;
import com.project.extension.domain.dto.OrcamentoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMQListener {
    private final GerarPdfUseCase useCase;
    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private static final String RESPONSE_EXCHANGE = "exchange.leovidros.direct";
    private static final String RESPONSE_ROUTING_KEY = "orcamento.resposta";

    public RabbitMQListener(GerarPdfUseCase useCase, RabbitTemplate rabbitTemplate) {
        this.useCase = useCase;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "fila.orcamento.pdf")
    public void receberMensagem(OrcamentoDTO payload) {
        log.info("Mensagem recebida da fila para o ID: {}", payload.id());
        processarComRetry(payload, 1);
    }


    private void processarComRetry(OrcamentoDTO payload, int tentativa) {
        try {
            if (payload.numeroOrcamento() == null || payload.numeroOrcamento().isEmpty()) {
                log.error(" Número do orçamento inválido ou vazio");
                return;
            }
            
            log.info(" Tentativa {}/{} - Gerando PDF para orçamento: {}",
                    tentativa, MAX_RETRIES, payload.numeroOrcamento());

            byte[] pdf = useCase.executar(payload);
            
            if (pdf == null || pdf.length == 0) {
                log.error(" PDF gerado com tamanho inválido: {} bytes", pdf != null ? pdf.length : 0);
                return;
            }

            PdfResponse response = new PdfResponse(
                payload.numeroOrcamento(),
                pdf
            );
            response.setOrcamentoId(payload.id().intValue());

            rabbitTemplate.convertAndSend(RESPONSE_EXCHANGE, RESPONSE_ROUTING_KEY, response);
            
            log.info("PDF enviado de volta via exchange: {}", RESPONSE_EXCHANGE);
            log.info("   → Routing Key: {}", RESPONSE_ROUTING_KEY);
            log.info("   → Orçamento: {}", payload.numeroOrcamento());
            log.info("   → Tamanho: {} bytes", pdf.length);
            
        } catch (Exception e) {
            log.error(" Erro na tentativa {}/{} para orçamento {}: {}",
                    tentativa, MAX_RETRIES, payload.numeroOrcamento(), e.getMessage(), e);

            if (tentativa < MAX_RETRIES) {
                log.info(" Aguardando {} ms antes de tentar novamente...", RETRY_DELAY_MS);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                    processarComRetry(payload, tentativa + 1);
                } catch (InterruptedException ie) {
                    log.error(" Interrupção durante espera de retry", ie);
                    Thread.currentThread().interrupt();
                }
            } else {
                log.error(" FALHOU após {} tentativas. Desistindo.", MAX_RETRIES);
            }
        }
    }
}
