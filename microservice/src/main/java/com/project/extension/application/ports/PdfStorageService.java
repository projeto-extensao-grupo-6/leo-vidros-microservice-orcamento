package com.project.extension.application.ports;

public interface PdfStorageService {
    void salvar(byte[] conteudo, String nomeArquivo);
}
