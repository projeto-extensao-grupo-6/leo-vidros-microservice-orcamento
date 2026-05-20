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

import java.math.BigInteger;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocxTemplateAdapter implements PdfGenerator {

    private final ResourceLoader carregadorRecursos;

    @Value("${app.orcamento.template-path}")
    private String caminhoTemplate;

    public DocxTemplateAdapter(ResourceLoader carregadorRecursos) {
        this.carregadorRecursos = carregadorRecursos;
    }

    @Override
    public byte[] generateFromOrcamento(OrcamentoDTO orcamento) {
        try (InputStream is = carregadorRecursos.getResource(caminhoTemplate).getInputStream()) {
            XWPFDocument documento = new XWPFDocument(is);

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

    private void processarTabela(XWPFTable tabela, OrcamentoDTO orcamento, Map<String, String> variaveis) {
        int indiceLinhaTemplate = -1;
        for (int i = 0; i < tabela.getRows().size(); i++) {
            if (obterTextoLinha(tabela.getRow(i)).contains("${itens.")) {
                indiceLinhaTemplate = i;
                break;
            }
        }

        if (indiceLinhaTemplate < 0) {
            for (XWPFTableRow linha : tabela.getRows()) {
                substituirNaLinha(linha, variaveis);
                corrigirLayoutFormaPagamento(linha, variaveis);
            }
            return;
        }

        for (int i = 0; i < tabela.getRows().size(); i++) {
            if (i != indiceLinhaTemplate) {
                substituirNaLinha(tabela.getRow(i), variaveis);
                corrigirLayoutFormaPagamento(tabela.getRow(i), variaveis);
            }
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

        for (XWPFRun run : runs) {
            String texto = run.getText(0);
            if (texto == null) continue;
            String textoSubstituido = texto;
            boolean alterado = false;
            for (Map.Entry<String, String> entrada : variaveis.entrySet()) {
                if (textoSubstituido.contains(entrada.getKey())) {
                    textoSubstituido = textoSubstituido.replace(entrada.getKey(), entrada.getValue() != null ? entrada.getValue() : "");
                    alterado = true;
                }
            }
            if (alterado)
                inserirTextoComQuebras(run, textoSubstituido);
        }

        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : runs) {
            String t = run.getText(0);
            if (t != null) sb.append(t);
        }
        String textoCompleto = sb.toString();
        boolean precisaMesclar = variaveis.keySet().stream().anyMatch(textoCompleto::contains);
        if (!needsMerge(textoCompleto, variaveis)) return;

        String textoMesclado = textoCompleto;
        for (Map.Entry<String, String> entrada : variaveis.entrySet()) {
            textoMesclado = textoMesclado.replace(entrada.getKey(), entrada.getValue() != null ? entrada.getValue() : "");
        }

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

    private boolean needsMerge(String textoCompleto, Map<String, String> variaveis) {
        return variaveis.keySet().stream().anyMatch(textoCompleto::contains);
    }

    private void inserirTextoComQuebras(XWPFRun run, String textoCompleto) {
        while (run.getCTR().sizeOfTArray() > 0) {
            run.getCTR().removeT(0);
        }

        if (textoCompleto == null) return;

        String[] linhas = textoCompleto.split("\n", -1);

        for (int i = 0; i < linhas.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(linhas[i]);
        }
    }

    /**
     * Aplica espaçamentos calculados para descolar o texto da borda inferior e dar recuo à esquerda.
     */
    private void corrigirLayoutFormaPagamento(XWPFTableRow linha, Map<String, String> variaveis) {
        String valorFormaPagamento = variaveis.get("${forma_pagamento}");
        if (valorFormaPagamento == null || valorFormaPagamento.isEmpty()) return;

        for (XWPFTableCell celula : linha.getTableCells()) {
            if (celula.getText().contains(valorFormaPagamento)) {

                // Força alinhamento central vertical na célula
                celula.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

                // Reseta a restrição de altura rígida da linha se houver
                if (linha.getCtRow().isSetTrPr() && linha.getCtRow().getTrPr().sizeOfTrHeightArray() > 0) {
                    linha.getCtRow().getTrPr().setTrHeightArray(new CTHeight[]{});
                }

                for (XWPFParagraph paragrafo : celula.getParagraphs()) {
                    // Força um recuo de 120 dxa (aproximadamente 3mm a 4mm à esquerda para afastar da margem)
                    paragrafo.setIndentationLeft(120);

                    // Define um espaçamento sutil antes do texto (60 dxa) para desencavalar do teto
                    paragrafo.setSpacingBefore(60);
                    paragrafo.setSpacingAfter(60);
                    paragrafo.setSpacingLineRule(LineSpacingRule.AUTO);
                    paragrafo.setAlignment(ParagraphAlignment.LEFT);
                }
            }
        }
    }

    private String obterTextoLinha(XWPFTableRow linha) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell celula : linha.getTableCells()) {
            for (XWPFParagraph paragrafo : celula.getParagraphs()) {
                for (XWPFRun run : paragrafo.getRuns()) {
                    for (int i = 0; i < run.getCTR().sizeOfTArray(); i++) {
                        String t = run.getText(i);
                        if (t != null) sb.append(t);
                    }
                }
            }
        }
        return sb.toString();
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