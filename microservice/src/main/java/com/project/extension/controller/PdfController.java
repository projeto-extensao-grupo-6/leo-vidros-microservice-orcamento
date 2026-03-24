package com.project.extension.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Value("${app.orcamento.diretorio}")
    private String diretorioDestino;

    @GetMapping("/download/{numeroOrcamento}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String numeroOrcamento) {
        try {
            String nomeArquivo = "orcamento_" + numeroOrcamento + ".pdf";
            Path caminhoCompleto = Paths.get(diretorioDestino).resolve(nomeArquivo);

            if (!Files.exists(caminhoCompleto)) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdf = Files.readAllBytes(caminhoCompleto);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verificar se o PDF foi gerado
     * GET /api/pdf/status/ORC-2026-MEDIDAS
     */
    @GetMapping("/status/{numeroOrcamento}")
    public ResponseEntity<?> checkPdfStatus(@PathVariable String numeroOrcamento) {
        String nomeArquivo = "orcamento_" + numeroOrcamento + ".pdf";
        Path caminhoCompleto = Paths.get(diretorioDestino).resolve(nomeArquivo);

        if (Files.exists(caminhoCompleto)) {
            return ResponseEntity.ok().body(new StatusResponse(true, "PDF gerado com sucesso"));
        } else {
            return ResponseEntity.ok().body(new StatusResponse(false, "PDF ainda não foi gerado"));
        }
    }

    public static class StatusResponse {
        public boolean gerado;
        public String mensagem;

        public StatusResponse(boolean gerado, String mensagem) {
            this.gerado = gerado;
            this.mensagem = mensagem;
        }
    }
}
