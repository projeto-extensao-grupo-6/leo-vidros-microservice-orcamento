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

    private final ResourceLoader resourceLoader;

    @Value("${app.orcamento.template-path}")
    private String templatePath;

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

            Map<String, String> vars = buildVars(orcamento);

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, vars);
            }

            for (XWPFTable table : doc.getTables()) {
                processTable(table, orcamento, vars);
            }

            preserveTableBorders(doc);
            return toPdf(doc);

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

        XWPFTableRow templateRow = table.getRow(templateRowIndex);
        List<OrcamentoItemDTO> itens = orcamento.itens();

        for (int i = itens.size() - 1; i >= 0; i--) {
            CTRow cloned = (CTRow) templateRow.getCtRow().copy();
            table.addRow(new XWPFTableRow(cloned, table), templateRowIndex);
            replaceInRow(table.getRow(templateRowIndex), buildItemVars(itens.get(i)));
        }

        table.removeRow(templateRowIndex + itens.size());
    }

    private void replaceInRow(XWPFTableRow row, Map<String, String> vars) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                replaceInParagraph(p, vars);
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph para, Map<String, String> vars) {
        List<XWPFRun> runs = para.getRuns();
        if (runs == null || runs.isEmpty()) return;

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

        XWPFRun first = runs.get(0);
        while (first.getCTR().sizeOfTArray() > 0) first.getCTR().removeT(0);
        first.getCTR().addNewT().setStringValue(merged);
        for (int i = 1; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            while (run.getCTR().sizeOfTArray() > 0) run.getCTR().removeT(0);
        }
    }

    private boolean rowHasItemPlaceholder(XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                    String t = run.getText(0);
                    if (t != null && t.contains("${itens.")) return true;
                }
            }
        }
        return false;
    }

    private void preserveTableBorders(XWPFDocument doc) {
        for (XWPFTable table : doc.getTables()) {
            if (!isTableBorderless(table)) continue;
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    enforceCellNilBorders(cell);
                }
            }
        }
    }

    private boolean isTableBorderless(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null || !tblPr.isSetTblBorders()) return false;
        CTTblBorders b = tblPr.getTblBorders();
        return isNil(b.getTop()) && isNil(b.getBottom())
            && isNil(b.getLeft()) && isNil(b.getRight())
            && isNil(b.getInsideH()) && isNil(b.getInsideV());
    }

    private boolean isNil(CTBorder border) {
        if (border == null) return true;
        STBorder.Enum val = border.getVal();
        if (val == null) return true;
        String v = val.toString();
        return "nil".equals(v) || "none".equals(v);
    }

    private void enforceCellNilBorders(XWPFTableCell cell) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                ? cell.getCTTc().getTcPr()
                : cell.getCTTc().addNewTcPr();
        CTTcBorders tcb = tcPr.isSetTcBorders()
                ? tcPr.getTcBorders()
                : tcPr.addNewTcBorders();
        nilBorder(tcb.isSetTop()     ? tcb.getTop()     : tcb.addNewTop());
        nilBorder(tcb.isSetBottom()  ? tcb.getBottom()  : tcb.addNewBottom());
        nilBorder(tcb.isSetLeft()    ? tcb.getLeft()    : tcb.addNewLeft());
        nilBorder(tcb.isSetRight()   ? tcb.getRight()   : tcb.addNewRight());
        nilBorder(tcb.isSetInsideH() ? tcb.getInsideH() : tcb.addNewInsideH());
        nilBorder(tcb.isSetInsideV() ? tcb.getInsideV() : tcb.addNewInsideV());
    }

    private void nilBorder(CTBorder b) {
        b.setVal(STBorder.NIL);
        b.setSz(BigInteger.ZERO);
        b.setSpace(BigInteger.ZERO);
    }

    private byte[] toPdf(XWPFDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfConverter.getInstance().convert(doc, out, PdfOptions.create());
        doc.close();
        return out.toByteArray();
    }

    private Map<String, String> buildVars(OrcamentoDTO o) {
        Map<String, String> vars = new HashMap<>();
        vars.put("${nome_cliente}", o.cliente().nome());
        vars.put("${numero_orcamento}", o.numeroOrcamento());
        vars.put("${data_orcamento}", o.dataOrcamento() != null ? o.dataOrcamento() : "");
        vars.put("${valor_subtotal}", formatarValor(o.valorSubtotal()));
        vars.put("${desconto_total}", formatarValor(o.valorDesconto()));
        vars.put("${valor_total}", formatarValor(o.valorTotal()));
        vars.put("${prazo_instalacao}", o.prazoInstalacao() != null ? o.prazoInstalacao() : "");
        vars.put("${garantia}", o.garantia() != null ? o.garantia() : "");
        vars.put("${forma_pagamento}", o.formaPagamento() != null ? o.formaPagamento() : "");
        vars.put("${observacoes}", o.observacoes() != null ? o.observacoes() : "");
        return vars;
    }

    private Map<String, String> buildItemVars(OrcamentoItemDTO item) {
        Map<String, String> vars = new HashMap<>();
        vars.put("${itens.codigo}", "");
        vars.put("${itens.quantidade}", formatarQuantidade(item.quantidade()));
        vars.put("${itens.produto}", item.descricao());
        vars.put("${itens.preco_unitario}", formatarValor(item.precoUnitario()));
        vars.put("${itens.valor_total_produto}", formatarValor(item.subtotal()));
        vars.put("${itens.desconto_produto}", formatarValor(item.desconto()));
        return vars;
    }

    private String formatarValor(Double valor) {
        if (valor == null) return "R$ 0,00";
        return String.format("R$ %.2f", valor).replace(".", ",");
    }

    private String formatarQuantidade(Double quantidade) {
        if (quantidade == null) return "0";
        return quantidade % 1 == 0
                ? String.valueOf(quantidade.intValue())
                : String.valueOf(quantidade);
    }
}
