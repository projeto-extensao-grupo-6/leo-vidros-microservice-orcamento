package com.project.extension.infrastructure.queue;

import com.project.extension.application.usecases.GerarPdfUseCase;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.exception.GeracaoPdfException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQListener {
    private final GerarPdfUseCase useCase;

    public RabbitMQListener(GerarPdfUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = "fila.orcamento.pdf")
    public void receberMensagem(OrcamentoDTO payload) {
        try
        {
            System.out.println("Mensagem recebida da fila para o ID: " + payload.id());
            useCase.executar(payload);

        } catch (GeracaoPdfException e) {
            System.err.println("❌ Erro no processamento assíncrono: " + e.getMessage());
                throw e;
        }
    }
}
