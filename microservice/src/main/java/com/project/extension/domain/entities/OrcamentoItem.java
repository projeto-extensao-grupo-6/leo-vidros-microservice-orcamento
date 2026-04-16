package com.project.extension.domain.entities;

import com.project.extension.domain.exception.GeracaoPdfException;

public class OrcamentoItem {
    private final String descricao;
    private final Double quantidade;
    private final Double precoUnitario;
    private final String observacao;

    public OrcamentoItem(String descricao, Double quantidade, Double precoUnitario, String observacao) {
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.observacao = observacao;

        validar();
    }

    private void validar() {
        if (descricao == null || descricao.isBlank()) {
            throw new GeracaoPdfException("Regra de Negócio: O item deve ter uma descrição.");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new GeracaoPdfException("Regra de Negócio: A quantidade do item deve ser maior que zero.");
        }
        if (precoUnitario == null || precoUnitario < 0) {
            throw new GeracaoPdfException("Regra de Negócio: O preço unitário não pode ser negativo.");
        }
    }

    public String getDescricao() { return descricao; }
    public Double getQuantidade() { return quantidade; }
    public Double getPrecoUnitario() { return precoUnitario; }
    public String getObservacao() { return observacao; }

    public Double getSubtotalItem() {
        return this.quantidade * this.precoUnitario;
    }
}