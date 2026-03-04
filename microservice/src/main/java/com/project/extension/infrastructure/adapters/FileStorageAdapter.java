package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.PdfStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileStorageAdapter implements PdfStorageService {

    @Value("${app.orcamento.diretorio}")
    private String diretorioDestino;

    @Override
    public void salvar(byte[] conteudo, String nomeArquivo) {
        try {
            Path pathDiretorio = Paths.get(diretorioDestino);
            if (!Files.exists(pathDiretorio)) {
                Files.createDirectories(pathDiretorio);
            }

            Path caminhoCompleto = pathDiretorio.resolve(nomeArquivo);
            Files.write(caminhoCompleto, conteudo);

            System.out.println("✅ Arquivo salvo fisicamente em: " + caminhoCompleto.toAbsolutePath());
        } catch (Exception e) {
            // Aqui depois você vai lançar sua Exception Personalizada
            throw new RuntimeException("Erro ao persistir arquivo no disco", e);
        }
    }
}