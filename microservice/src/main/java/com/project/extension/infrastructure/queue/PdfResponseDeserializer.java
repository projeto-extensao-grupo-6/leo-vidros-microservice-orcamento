package com.project.extension.infrastructure.queue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Base64;

public class PdfResponseDeserializer extends JsonDeserializer<PdfResponse> {

    @Override
    public PdfResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            String pdfBase64 = node.asText();
            byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
            return new PdfResponse("unknown", pdfBytes);
        }

        String numeroOrcamento = node.has("numeroOrcamento") 
            ? node.get("numeroOrcamento").asText() 
            : "unknown";
        
        byte[] pdfBytes = null;
        if (node.has("pdfBytes")) {
            String pdfBase64 = node.get("pdfBytes").asText();
            pdfBytes = Base64.getDecoder().decode(pdfBase64);
        }
        
        PdfResponse response = new PdfResponse(numeroOrcamento, pdfBytes);
        
        if (node.has("tamanho")) {
            response.setTamanho(node.get("tamanho").asLong());
        }
        if (node.has("geradoEm")) {
            response.setGeradoEm(node.get("geradoEm").asLong());
        }
        
        return response;
    }
}
