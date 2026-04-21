package com.project.extension.application.usecases;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.entities.Orcamento;
import com.project.extension.domain.entities.OrcamentoItem;

import java.util.List;

public class GerarPdfUseCase {
    private final PdfGenerator pdfGenerator;
    private final PdfStorageService pdfStorageService;

    public GerarPdfUseCase(PdfGenerator pdfGenerator, PdfStorageService pdfStorageService) {
        this.pdfGenerator = pdfGenerator;
        this.pdfStorageService = pdfStorageService;
    }

    public byte[] executar(OrcamentoDTO dados) {
        List<OrcamentoItem> itensDomain = dados.itens().stream()
                .map(dto -> new OrcamentoItem(
                        dto.descricao(),
                        dto.quantidade(),
                        dto.precoUnitario(),
                        dto.observacao()
                )).toList();

        new Orcamento(
                dados.id(),
                dados.numeroOrcamento(),
                dados.cliente().nome(),
                dados.valorTotal(),
                itensDomain
        );

        byte[] pdf = pdfGenerator.generateFromOrcamento(dados);
        String nomeArquivo = "orcamento_" + dados.numeroOrcamento() + ".pdf";
        pdfStorageService.salvar(pdf, nomeArquivo);
        return pdf;
    }
}