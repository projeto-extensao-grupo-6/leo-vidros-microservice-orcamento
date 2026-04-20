package com.project.extension.application.ports;

public interface PdfStorageService {
    String salvar(byte[] conteudo, String nomeArquivo);
}
