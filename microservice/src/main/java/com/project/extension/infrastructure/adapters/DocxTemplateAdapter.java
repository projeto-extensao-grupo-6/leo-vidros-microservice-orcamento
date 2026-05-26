package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.dto.OrcamentoItemDTO;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;


import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocxTemplateAdapter implements PdfGenerator {

    private final ResourceLoader carregadorRecursos;

    @Value("${app.orcamento.template-path}")
    private String caminhoTemplate;

    private byte[] templateBytes;

    public DocxTemplateAdapter(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        try (InputStream is = resourceLoader.getResource(templatePath).getInputStream()) {
            this.templateBytes = is.readAllBytes();
        }
    }

    @Override
    public byte[] generateFromOrcamento(OrcamentoDTO orcamento) {
        try {
            XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(templateBytes));

            Map<String, String> variaveis = construirVariaveis(orcamento);

            for (XWPFParagraph paragrafo : documento.getParagraphs()) {
                substituirNoParagrafo(paragrafo, variaveis);
            }

            for (XWPFTable tabela : documento.getTables()) {
                processarTabela(tabela, orcamento, variaveis);
            }

            preservarBordasTabela(documento);
            return converterParaPdf(documento);

        } catch (Exception e) {
            throw new RuntimeException("Falha na geração do PDF via DOCX: " + e.getMessage(), e);
        }
    }

    private void processTable(XWPFTable table, OrcamentoDTO orcamento, Map<String, String> vars) {
        int templateRowIndex = -1;
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rowHasItemPlaceholder(rows.get(i))) {
                templateRowIndex = i;
                break;
            }
        }

        if (templateRowIndex < 0) {
            for (XWPFTableRow row : rows) {
                replaceInRow(row, vars);
            }
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            if (i != templateRowIndex) replaceInRow(rows.get(i), vars);
        }

        XWPFTableRow linhaTemplate = tabela.getRow(indiceLinhaTemplate);
        List<OrcamentoItemDTO> itens = orcamento.itens();

        for (int i = itens.size() - 1; i >= 0; i--) {
            CTRow linhaClonada = (CTRow) linhaTemplate.getCtRow().copy();
            tabela.addRow(new XWPFTableRow(linhaClonada, tabela), indiceLinhaTemplate);
            substituirNaLinha(tabela.getRow(indiceLinhaTemplate), construirVariaveisItem(itens.get(i)));
        }

        tabela.removeRow(indiceLinhaTemplate + itens.size());
    }

    private void substituirNaLinha(XWPFTableRow linha, Map<String, String> variaveis) {
        for (XWPFTableCell celula : linha.getTableCells()) {
            for (XWPFParagraph paragrafo : celula.getParagraphs()) {
                substituirNoParagrafo(paragrafo, variaveis);
            }
        }
    }

    private void substituirNoParagrafo(XWPFParagraph paragrafo, Map<String, String> variaveis) {
        List<XWPFRun> runs = paragrafo.getRuns();
        if (runs == null || runs.isEmpty())
            return;

        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String t = run.getText(0);
            if (t != null) sb.append(t);
        }
        String full = sb.toString();
        if (!full.contains("${")) return;

        String merged = full;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (merged.contains(e.getKey())) {
                merged = merged.replace(e.getKey(), e.getValue());
            }
        }
        if (merged.equals(full)) return;

        XWPFRun primeiraRun = runs.get(0);
        inserirTextoComQuebras(primeiraRun, textoMesclado);

        for (int i = 1; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            while (run.getCTR().sizeOfTArray() > 0) {
                run.getCTR().removeT(0);
            }
            if (run.getCTR().sizeOfBrArray() > 0) {
                for (int b = run.getCTR().sizeOfBrArray() - 1; b >= 0; b--) {
                    run.getCTR().removeBr(b);
                }
            }
        }
    }

    private boolean rowHasItemPlaceholder(XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                StringBuilder sb = new StringBuilder();
                for (XWPFRun run : p.getRuns()) {
                    for (int i = 0; i < run.getCTR().sizeOfTArray(); i++) {
                        String t = run.getText(i);
                        if (t != null) sb.append(t);
                    }
                }
                if (sb.toString().contains("${itens.")) return true;
            }
        }
        return false;
    }

    private void preservarBordasTabela(XWPFDocument doc) {
        for (XWPFTable tabela : doc.getTables()) {
            if (!isTabelaSemBorda(tabela)) continue;
            for (XWPFTableRow linha : tabela.getRows()) {
                for (XWPFTableCell celula : linha.getTableCells()) {
                    forcarBordasNulasCelula(celula);
                }
            }
        }
    }

    private boolean isTabelaSemBorda(XWPFTable tabela) {
        CTTblPr tblPr = tabela.getCTTbl().getTblPr();
        if (tblPr == null || !tblPr.isSetTblBorders()) return false;
        CTTblBorders b = tblPr.getTblBorders();
        return isBordaNula(b.getTop()) && isBordaNula(b.getBottom())
                && isBordaNula(b.getLeft()) && isBordaNula(b.getRight())
                && isBordaNula(b.getInsideH()) && isBordaNula(b.getInsideV());
    }

    private boolean isBordaNula(CTBorder borda) {
        if (borda == null) return true;
        STBorder.Enum val = borda.getVal();
        if (val == null) return true;
        String v = val.toString();
        return "nil".equals(v) || "none".equals(v);
    }

    private void forcarBordasNulasCelula(XWPFTableCell celula) {
        CTTcPr tcPr = celula.getCTTc().isSetTcPr()
                ? celula.getCTTc().getTcPr()
                : celula.getCTTc().addNewTcPr();
        CTTcBorders tcb = tcPr.isSetTcBorders()
                ? tcPr.getTcBorders()
                : tcPr.addNewTcBorders();
        configurarBordaNula(tcb.isSetTop()     ? tcb.getTop()     : tcb.addNewTop());
        configurarBordaNula(tcb.isSetBottom()  ? tcb.getBottom()  : tcb.addNewBottom());
        configurarBordaNula(tcb.isSetLeft()    ? tcb.getLeft()    : tcb.addNewLeft());
        configurarBordaNula(tcb.isSetRight()   ? tcb.getRight()   : tcb.addNewRight());
        configurarBordaNula(tcb.isSetInsideH() ? tcb.getInsideH() : tcb.addNewInsideH());
        configurarBordaNula(tcb.isSetInsideV() ? tcb.getInsideV() : tcb.addNewInsideV());
    }

    private void configurarBordaNula(CTBorder b) {
        b.setVal(STBorder.NIL);
        b.setSz(BigInteger.ZERO);
        b.setSpace(BigInteger.ZERO);
    }

    private byte[] converterParaPdf(XWPFDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfConverter.getInstance().convert(doc, out, PdfOptions.create());
        doc.close();
        return out.toByteArray();
    }

    private Map<String, String> construirVariaveis(OrcamentoDTO o) {
        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("${nome_cliente}", o.cliente().nome());
        variaveis.put("${numero_orcamento}", o.numeroOrcamento());
        variaveis.put("${data_orcamento}", o.dataOrcamento() != null ? o.dataOrcamento() : "");
        variaveis.put("${valor_subtotal}", formatarValor(o.valorSubtotal()));
        variaveis.put("${desconto_total}", formatarValor(o.valorDesconto()));
        variaveis.put("${valor_total}", formatarValor(o.valorTotal()));
        variaveis.put("${prazo_instalacao}", o.prazoInstalacao() != null ? o.prazoInstalacao() : "");
        variaveis.put("${garantia}", o.garantia() != null ? o.garantia() : "");
        variaveis.put("${forma_pagamento}", o.formaPagamento() != null ? o.formaPagamento() : "");
        variaveis.put("${observacoes}", o.observacoes() != null ? o.observacoes() : "");

        return variaveis;
    }

    private Map<String, String> construirVariaveisItem(OrcamentoItemDTO item) {
        Map<String, String> variaveis = new HashMap<>();
        variaveis.put("${itens.codigo}", "");
        variaveis.put("${itens.quantidade}", formatarQuantidade(item.quantidade()));
        variaveis.put("${itens.produto}", item.descricao());
        variaveis.put("${itens.preco_unitario}", formatarValor(item.precoUnitario()));
        variaveis.put("${itens.valor_total_produto}", formatarValor(item.subtotal()));
        variaveis.put("${itens.desconto_produto}", formatarValor(item.desconto()));
        return variaveis;
    }

    private String formatarValor(Double valor) {
        return valor == null ? "R$ 0,00" : String.format("R$ %.2f", valor).replace(".", ",");
    }

    private String formatarQuantidade(Double quantidade) {
        if (quantidade == null) return "0";
        return quantidade % 1 == 0
                ? String.valueOf(quantidade.intValue())
                : String.valueOf(quantidade);
    }
}