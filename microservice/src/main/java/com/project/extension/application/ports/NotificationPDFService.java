package com.project.extension.application.ports;

public interface NotificationPDFService {
    void notificarPdfPronto(String numeroOrcamento, String referencia, byte[] pdfBytes);
}
