package com.project.extension.infrastructure.config;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.application.usecases.GerarPdfUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public GerarPdfUseCase gerarPdfUseCase(PdfGenerator pdfGenerator, PdfStorageService storageService) {
        return new GerarPdfUseCase(pdfGenerator, storageService);
    }
}