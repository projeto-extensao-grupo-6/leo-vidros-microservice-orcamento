package com.project.extension.application.ports;

import com.project.extension.domain.dto.OrcamentoDTO;

public interface PdfGenerator {
    byte[] generateFromOrcamento(OrcamentoDTO dados);
}
