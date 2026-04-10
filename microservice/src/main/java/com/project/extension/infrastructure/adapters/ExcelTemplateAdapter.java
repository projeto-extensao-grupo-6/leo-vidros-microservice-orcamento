package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.dto.OrcamentoItemDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.poi.ss.util.SheetUtil.getCell;

@Component
public class ExcelTemplateAdapter implements PdfGenerator {
    @Value("${app.orcamento.template-path}")
    private String templatePath;

    @Override
    public byte[] generateFromOrcamento(OrcamentoDTO orcamento)
    {
        try (InputStream is = new FileInputStream(templatePath);
             Workbook workbook = new XSSFWorkbook(is))
        {
            Sheet sheet = workbook.getSheetAt(0);
            preencherCabecalho(sheet, orcamento);
            preencherItens(sheet, orcamento.itens());

            // Força a atualização de fórmulas para recalcular os totais
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar template Excel", e);
        }
    }

    private void preencherCabecalho(Sheet sheet, OrcamentoDTO dto)
    {
        getCell(sheet, 1, 5).setCellValue(dto.dataOrcamento()); // F2
        getCell(sheet, 11, 1).setCellValue(dto.cliente().nome()); // B12
        getCell(sheet, 11, 5).setCellValue(dto.numeroOrcamento()); // F12
    }

    private void preencherItens(Sheet sheet, List<OrcamentoItemDTO> itens) {
        int linhaInicial = 18; // Linha 19 no Excel
        for (int i = 0; i < itens.size() && i < 14; i++) { // Limite de 14 linhas do template
            Row row = sheet.getRow(linhaInicial + i);
            OrcamentoItemDTO item = itens.get(i);

            row.getCell(1).setCellValue(item.quantidade());      // B
            row.getCell(2).setCellValue(item.descricao());       // C
            row.getCell(3).setCellValue(item.precoUnitario());    // D
            // Se as colunas E e F forem fórmulas, o evaluateAll() resolve.
        }
    }

    private Cell getCell(Sheet sheet, int row, int col) {
        Row row01 = sheet.getRow(row);
        if (row01 == null) row01 = sheet.createRow(row);

        Cell cell = row01.getCell(col);
        if (cell == null)
            cell = row01.createCell(col);
        return cell;
    }
}
