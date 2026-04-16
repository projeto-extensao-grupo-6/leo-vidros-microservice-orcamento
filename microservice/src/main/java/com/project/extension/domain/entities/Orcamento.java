package com.project.extension.domain.entities;

import com.project.extension.domain.exception.GeracaoPdfException;
import java.util.List;

public class Orcamento {
    private final Long id;
    private final String numero;
    private final String clienteNome;
    private final Double valorTotal;
    private final List<OrcamentoItem> itens;

    public Orcamento(Long id, String numero, String clienteNome, Double valorTotal, List<OrcamentoItem> itens) {
        this.id = id;
        this.numero = numero;
        this.clienteNome = clienteNome;
        this.valorTotal = valorTotal;
        this.itens = itens;
        validar();
    }

    private void validar() {
        if (numero == null || numero.isBlank()) {
            throw new GeracaoPdfException("Regra de Negócio: O orçamento deve possuir um número identificador.");
        }
        if (clienteNome == null || clienteNome.isBlank()) {
            throw new GeracaoPdfException("Regra de Negócio: O nome do cliente é obrigatório para emissão de PDF.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new GeracaoPdfException("Regra de Negócio: Não é possível gerar orçamento sem itens.");
        }
        if (valorTotal == null || valorTotal <= 0) {
            throw new GeracaoPdfException("Regra de Negócio: O valor total do orçamento deve ser positivo.");
        }
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getClienteNome() { return clienteNome; }
    public Double getValorTotal() { return valorTotal; }
    public List<OrcamentoItem> getItens() { return itens; }
}