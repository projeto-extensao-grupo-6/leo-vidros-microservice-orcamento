package com.project.extension.domain.exception;

public class GeracaoPdfException extends RuntimeException {
    public GeracaoPdfException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public GeracaoPdfException(String mensagem) {
        super(mensagem);
    }
}
