package com.project.extension.application.usecases;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.domain.dto.OrcamentoDTO;
import org.springframework.stereotype.Service;

@Service
public class GerarPdfUseCase {
    private final PdfGenerator pdfGenerator;
    private final PdfStorageService pdfStorageService;

    public GerarPdfUseCase(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
        this.pdfStorageService = pdfStorageService;
    }

    public byte[] executar(OrcamentoDTO dados) {
        byte[] pdf = pdfGenerator.generateFromOrcamento(dados);
        System.out.println("PDF Gerado em memória para: " + dados.numeroOrcamento());
        return pdf;
    }
}