package com.project.extension.infrastructure.adapters;

import com.project.extension.application.ports.PdfStorageService;
import com.project.extension.domain.exception.GeracaoPdfException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("production")
public class S3StorageAdapter implements PdfStorageService {

    private final S3Client s3Client;

    @Value("${AWS_S3_BUCKET:leo-vidros-orcamento-pdfs-us-east-1-prod}")
    private String bucketName;

    public S3StorageAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void salvar(byte[] conteudo, String nomeArquivo) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("orcamentos/" + nomeArquivo)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(conteudo));
            System.out.println("✅ Arquivo enviado com sucesso para o S3: " + bucketName);
        } catch (Exception e) {
            throw new GeracaoPdfException("Erro ao enviar arquivo para o S3", e);
        }
    }
}