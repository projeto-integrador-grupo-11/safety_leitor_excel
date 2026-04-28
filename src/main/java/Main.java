import conexao.bucket.S3Provider;
import model.Municipio;
import repository.MunicipioRepository;
import service.LeitorExcel;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {

        //Credenciais da sessão do AWS Academy
        System.setProperty("aws.accessKeyId", "ASIA3STB4UX4UIZHPITB");
        System.setProperty("aws.secretAccessKey", "i1wuNQOitG6VYJxprJyc+Twa8y+5ZV9kayh76Og5");
        System.setProperty("aws.sessionToken", "IQoJb3JpZ2luX2VjEAoaCXVzLXdlc3QtMiJIMEYCIQDFy1E4JT85YzcMLqtrWAqe4eGpAfiBIQ4k/uPaxAQq8QIhALdur7Tq9KURppl+7SiMYsTNkcaeeUEFJICWrmKhVIxRKrwCCNP//////////wEQABoMNzk1ODQ3OTkyODI1IgwMezda99kApu22+IUqkAJTw1UzqdjQVd6RWNIlrAqu/VZhytdFmjBhq59MmvDcvMS52ZdtaT+CwDTSptD7YBk20EEW7vplsTBGJFaGvNZ0E/0IRRSexNPq9oekukhMOZ7FNP/sFSvIRIHI5a6SLxe+PiUD+gou51ZWizy8AJpr+mJJ1Z7tjZh52A+s337GqVOaX2oKlUyzJCrTCZ7hykgVB1aT3Dsts9ODbOMQ6GdCcGY5Z31X276xu8KCCt9G3lJOfQ9fdpCF2saIoMCcjR6kRN9I9tbVXYw5VfTsJhzmsz+EN4ONKwS5yLh+3FCrhw2MtlqlwfKQqr9rUJwLhRGJX8T6eiBrlwtVPkhrU19bR8D09m5zzPToGGad+h2JUzC4qcDPBjqcAdVOLjB2tUUwcJWr0LMu866fD02UBRX3X9of+S0UuhmZyQh9KIZ4pguTAKPcz4OFCVs6mbtj5cdytNAdSRyqS7D8zGKNAFZF5IX2WscGOwvmtm06D4J4JNJXo86oFVr2Ru4b4AzlaoVuB/nEYNM1oy1HOeQcb2RJhVioXAgZ0meNMs0UpMDx9VRYLPcZ6qoHGHkqvAmR7Y2P17rKWA==");

        LeitorExcel leitor = new LeitorExcel();
        MunicipioRepository repo = new MunicipioRepository();

        // Instanciando o cliente S3 via S3Provider
        S3Client s3Client = new S3Provider().getS3Client();
        String bucketName = "17042026-safety";

        // Listando todos os buckets
        try {
            List<Bucket> buckets = s3Client.listBuckets().buckets();
            System.out.println("Lista de buckets:");
            for (Bucket bucket : buckets) {
                System.out.println("- " + bucket.name());
            }
        } catch (S3Exception e) {
            System.err.println("Erro ao listar buckets: " + e.getMessage());
        }

        // Listando objetos do bucket
        try {
            ListObjectsRequest requisicao = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();

            List<S3Object> objects = s3Client.listObjects(requisicao).contents();
            System.out.println("Objetos no bucket " + bucketName + ":");
            for (S3Object object : objects) {
                System.out.println("- " + object.key());
            }
        } catch (S3Exception e) {
            System.err.println("Erro ao listar objetos no bucket: " + e.getMessage());
        }

        // Fazendo upload de arquivo
        /*try {
            String uniqueFileName = UUID.randomUUID().toString();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            File file = new File("identificador-do-objeto");
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            System.out.println("Arquivo '" + file.getName() + "' enviado com sucesso com o nome: " + uniqueFileName);
        } catch (S3Exception e) {
            System.err.println("Erro ao fazer upload do arquivo: " + e.getMessage());
        }*/


        // Fazendo download de arquivos
        try {
            ListObjectsRequest requisicao = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();
            List<S3Object> objects = s3Client.listObjects(requisicao).contents();
            for (S3Object object : objects) {

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(object.key())
                        .build();

                InputStream inputStream = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
                Files.copy(inputStream, new File(object.key()).toPath());
                System.out.println("Arquivo baixado: " + object.key());
            }
        } catch (FileAlreadyExistsException e) {
            System.err.println("Aviso: arquivo já existente: " + e.getMessage());
            System.out.println(e.fillInStackTrace());
        } catch (IOException | S3Exception e) {
            System.err.println("Erro ao fazer download dos arquivos: " + e.getMessage());
        }


        // Deletando um objeto do bucket
        /*try {
            String objectKeyToDelete = "identificador-do-objeto";
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKeyToDelete)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);

            System.out.println("Objeto deletado com sucesso: " + objectKeyToDelete);
        } catch (S3Exception e) {
            System.err.println("Erro ao deletar objeto: " + e.getMessage());
        }*/


        List<Municipio> municipios = leitor.ler("data_idhm.xlsx");
        repo.salvarLista(municipios);
        System.out.println("Processo finalizado!");
    }
}