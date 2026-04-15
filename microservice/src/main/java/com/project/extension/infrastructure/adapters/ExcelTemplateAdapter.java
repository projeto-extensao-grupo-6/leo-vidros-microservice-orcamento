package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.PdfGenerator;
import com.project.extension.domain.dto.OrcamentoDTO;
import com.project.extension.domain.dto.OrcamentoItemDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest; // CERTIFIQUE-SE QUE É java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers; // IMPORTANTE PARA O POST
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class ExcelTemplateAdapter implements PdfGenerator {

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${app.orcamento.template-path}")
    private String templatePath;

    @Override
    public byte[] generateFromOrcamento(OrcamentoDTO orcamento) {
        Resource resource = resourceLoader.getResource(templatePath);

        try {
            byte[] excelBytes = preencherExcel(orcamento);
            return converterViaApiExterna(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("Falha na geração via Excel/API: " + e.getMessage() + " Mensagem: " + e.getCause(), e);
        }
    }

    private byte[] preencherExcel(OrcamentoDTO dto) throws IOException {
        Resource resource = resourceLoader.getResource(templatePath);
        try (InputStream is = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            preencherCabecalho(sheet, dto);
            preencherItens(sheet, dto.itens());

            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] converterViaApiExterna(byte[] excelBytes) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .build();

        String base64Excel = java.util.Base64.getEncoder().encodeToString(excelBytes);

        String jsonBody = """
            {
                "Parameters": [
                    {
                        "Name": "File",
                        "FileValue": {
                            "Name": "orcamento.xlsx",
                            "Data": "%s"
                        }
                    }
                ]
            }
            """.formatted(base64Excel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://v2.convertapi.com/convert/xlsx/to/pdf"))
                .header("Authorization", "Bearer YsF8uQpMH1k5dRSxVhaQ2IT0OJsHbidJ")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erro na API: " + response.body());
        }

        String body = response.body();
        String busca = "\"FileData\":\"";
        int inicio = body.indexOf(busca) + busca.length();
        int fim = body.indexOf("\"", inicio);
        String base64Pdf = body.substring(inicio, fim);

        return java.util.Base64.getDecoder().decode(base64Pdf);
    }

    private void preencherCabecalho(Sheet sheet, OrcamentoDTO dto) {
        getCell(sheet, 1, 5).setCellValue(dto.dataOrcamento());
        getCell(sheet, 11, 1).setCellValue(dto.cliente().nome());
        getCell(sheet, 11, 5).setCellValue(dto.numeroOrcamento());
    }

    private void preencherItens(Sheet sheet, List<OrcamentoItemDTO> itens) {
        int linhaInicial = 18;
        for (int i = 0; i < itens.size() && i < 14; i++) {
            Row row = getRow(sheet, linhaInicial + i);
            OrcamentoItemDTO item = itens.get(i);
            getCell(sheet, linhaInicial + i, 1).setCellValue(item.quantidade());
            getCell(sheet, linhaInicial + i, 2).setCellValue(item.descricao());
            getCell(sheet, linhaInicial + i, 3).setCellValue(item.precoUnitario());
        }
    }

    private Row getRow(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        return (row == null) ? sheet.createRow(rowIdx) : row;
    }

    private Cell getCell(Sheet sheet, int row, int col) {
        Row r = getRow(sheet, row);
        Cell c = r.getCell(col);
        return (c == null) ? r.createCell(col) : c;
    }
}