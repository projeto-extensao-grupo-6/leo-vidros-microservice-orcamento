package com.project.extension.domain.dto;

public record PdfResponseDTO(
        String numeroOrcamento,
        String nomeArquivo,
        byte[] pdfBytes
) {}
