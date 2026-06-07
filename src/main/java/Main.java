import conexao.bucket.S3Provider;
import model.IndicadorSeguranca;
import model.Municipio;
import model.OcorrenciaSeguranca;
import model.PopulacaoMunicipio;
import repository.IndicadorSegurancaRepository;
import repository.LogRepository;
import repository.MunicipioRepository;
import repository.OcorrenciaSegurancaRepository;
import repository.PopulacaoMunicipioRepository;
import service.LeitorExcel;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class Main {

    private static final int[] ANOS_SEGURANCA = {2021, 2022, 2023, 2024, 2025};
    private static final int ANO_POPULACAO = 2025;

    public static void main(String[] args) {

        // Credenciais da sessão do AWS Academy.
        // Defina as variáveis de ambiente AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
        // e AWS_SESSION_TOKEN antes de executar. NUNCA versione credenciais no código.
        System.setProperty("aws.accessKeyId", System.getenv("AWS_ACCESS_KEY_ID"));
        System.setProperty("aws.secretAccessKey", System.getenv("AWS_SECRET_ACCESS_KEY"));
        System.setProperty("aws.sessionToken", System.getenv("AWS_SESSION_TOKEN"));

        // Agregação: um único LogRepository compartilhado entre o leitor e todos
        // os repositórios (ciclo de vida independente de cada um deles).
        LogRepository logRepo = new LogRepository();

        LeitorExcel leitor = new LeitorExcel(logRepo);
        MunicipioRepository municipioRepo = new MunicipioRepository(logRepo);
        OcorrenciaSegurancaRepository ocorrenciaRepo = new OcorrenciaSegurancaRepository(logRepo);
        IndicadorSegurancaRepository indicadorRepo = new IndicadorSegurancaRepository(logRepo);
        PopulacaoMunicipioRepository populacaoRepo = new PopulacaoMunicipioRepository(logRepo);

        S3Client s3Client = new S3Provider().getS3Client();
        String bucketName = "17042026-safety";

        listarBuckets(s3Client);
        listarObjetos(s3Client, bucketName);
        baixarObjetos(s3Client, bucketName);

        // 1) IDHM municipal por UF (idhm_municipios.xlsx)
        System.out.println("\n==> Carregando IDHM municipal (idhm_municipios.xlsx)");
        List<Municipio> municipios = leitor.lerIdhmMunicipios("idhm_municipios.xlsx");
        if (municipios.isEmpty()) {
            System.out.println("  [fallback] Tentando data_idhm.xlsx (legado SP)…");
            municipios = leitor.ler("data_idhm.xlsx");
        }
        if (!municipios.isEmpty()) {
            municipioRepo.limpar();
            municipioRepo.salvarLista(municipios);
        }

        // 2) População por município (XLS antigo)
        System.out.println("\n==> Carregando população municipal (populacao_municipios_2025.xls)");
        List<PopulacaoMunicipio> populacoes = leitor.lerPopulacaoMunicipios(
                "populacao_municipios_2025.xls", ANO_POPULACAO
        );
        if (!populacoes.isEmpty()) {
            populacaoRepo.limpar();
            populacaoRepo.salvarLista(populacoes);
        }

        // 3) Indicadores agregados (roubo/furto de veículo por UF/ano)
        System.out.println("\n==> Carregando indicadores (indicadores_seguranca_publica.xlsx)");
        List<IndicadorSeguranca> indicadores = leitor.lerIndicadores(
                "indicadores_seguranca_publica.xlsx"
        );
        if (!indicadores.isEmpty()) {
            indicadorRepo.limpar();
            indicadorRepo.salvarLista(indicadores);
        }

        // 4) Ocorrências mensais (latrocínio + homicídio) para todos os anos disponíveis
        System.out.println("\n==> Carregando ocorrências de segurança (banco_seguranca_YYYY.xlsx)");
        System.out.println("    Memória máxima JVM: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        ocorrenciaRepo.limpar();
        for (int ano : ANOS_SEGURANCA) {
            String arquivo = "banco_seguranca_" + ano + ".xlsx";
            File f = new File(arquivo);
            if (!f.exists()) {
                System.out.println("  [skip] " + arquivo + " não encontrado.");
                continue;
            }
            List<OcorrenciaSeguranca> ocorrencias = leitor.lerSeguranca(arquivo, ano);
            if (!ocorrencias.isEmpty()) {
                ocorrenciaRepo.salvarLista(ocorrencias);
            }
            ocorrencias = null;
            System.gc();
        }

        System.out.println("\nProcesso finalizado!");
    }

    private static void listarBuckets(S3Client s3Client) {
        try {
            List<Bucket> buckets = s3Client.listBuckets().buckets();
            System.out.println("Lista de buckets:");
            for (Bucket bucket : buckets) {
                System.out.println("- " + bucket.name());
            }
        } catch (S3Exception e) {
            System.err.println("Erro ao listar buckets: " + e.getMessage());
        }
    }

    private static void listarObjetos(S3Client s3Client, String bucketName) {
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
    }

    private static void baixarObjetos(S3Client s3Client, String bucketName) {
        try {
            ListObjectsRequest requisicao = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();
            List<S3Object> objects = s3Client.listObjects(requisicao).contents();
            for (S3Object object : objects) {
                File destino = new File(object.key());
                if (destino.exists()) {
                    System.out.println("Aviso: arquivo já existente: " + object.key());
                    continue;
                }
                try {
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object.key())
                            .build();

                    InputStream inputStream = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
                    Files.copy(inputStream, destino.toPath());
                    System.out.println("Arquivo baixado: " + object.key());
                } catch (IOException | S3Exception e) {
                    System.err.println("Erro ao baixar " + object.key() + ": " + e.getMessage());
                }
            }
        } catch (S3Exception e) {
            System.err.println("Erro ao listar objetos para download: " + e.getMessage());
        }
    }
}
