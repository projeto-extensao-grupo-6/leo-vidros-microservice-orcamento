package com.project.extension.application.usecases;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.domain.dto.OrcamentoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GerarPdfUseCase {
    private final PdfGenerator pdfGenerator;

    @Value("${app.orcamento.diretorio}")
    private String diretorioDestino;

    public GerarPdfUseCase(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public void executar(OrcamentoDTO dados) {
        byte[] pdf = pdfGenerator.generateFromOrcamento(dados);

        try {
            // 1. Garantir que o diretório existe
            Path pathDiretorio = Paths.get(diretorioDestino);
            if (!Files.exists(pathDiretorio)) {
                Files.createDirectories(pathDiretorio);
            }

            // 2. Definir o nome do arquivo (ex: orcamento_ORC-001.pdf)
            String nomeArquivo = "orcamento_" + dados.numeroOrcamento() + ".pdf";
            Path caminhoCompleto = pathDiretorio.resolve(nomeArquivo);

            // 3. Salvar o arquivo
            Files.write(caminhoCompleto, pdf);

            System.out.println("✅ PDF Gerado com sucesso em: " + caminhoCompleto.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ Erro ao salvar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}