package com.project.extension.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrcamentoDTO(
        Long id,
        String numeroOrcamento,
        String dataOrcamento,
        ClienteDTO cliente,
        List<OrcamentoItemDTO> itens,
        Double valorSubtotal,
        Double valorDesconto,
        Double valorTotal,
        String prazoInstalacao,
        String garantia,
        String formaPagamento,
        String observacoes,
        List<ProdutoInstalacaoDTO> produtosInstalacao
) {}