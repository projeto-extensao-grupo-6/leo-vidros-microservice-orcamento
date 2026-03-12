package com.project.extension.application.usecases;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.domain.dto.OrcamentoDTO;

public class GerarPdfUseCase {
    private final PdfGenerator pdfGenerator;
    private final PdfStorageService pdfStorageService;

    public GerarPdfUseCase(PdfGenerator pdfGenerator, PdfStorageService pdfStorageService) {
        this.pdfGenerator = pdfGenerator;
        this.pdfStorageService = pdfStorageService;
    }

    public void executar(OrcamentoDTO dados) {
        byte[] pdf = pdfGenerator.generateFromOrcamento(dados);
        String nomeArquivo = "orcamento_" + dados.numeroOrcamento() + ".pdf";
        pdfStorageService.salvar(pdf, nomeArquivo);
        System.out.println("Log: Processamento do caso de uso concluído para " + nomeArquivo);

    }
}