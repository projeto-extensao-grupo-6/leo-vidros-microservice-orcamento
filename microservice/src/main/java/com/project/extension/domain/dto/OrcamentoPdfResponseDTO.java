package com.project.extension.domain.dto;

public record OrcamentoPdfResponseDTO(
        Long orcamentoId,
        String numeroOrcamento,
        byte[] pdf
) {}
