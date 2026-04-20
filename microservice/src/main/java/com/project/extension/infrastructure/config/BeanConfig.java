package com.project.extension.infrastructure.config;

import com.project.extension.application.ports.NotificationPDFService;
import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.application.usecases.GerarPdfUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public GerarPdfUseCase gerarPdfUseCase(@Qualifier("excelTemplateAdapter")PdfGenerator pdfGenerator, PdfStorageService storageService, NotificationPDFService notificationPDFService) {
        return new GerarPdfUseCase(pdfGenerator, storageService, notificationPDFService);
    }
}