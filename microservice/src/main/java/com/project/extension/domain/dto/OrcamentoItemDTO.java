package com.project.extension.domain.dto;

public record OrcamentoItemDTO(
        String descricao,
        Double quantidade,
        Double precoUnitario,
        Double desconto,
        Double subtotal,
        String observacao
) {}
