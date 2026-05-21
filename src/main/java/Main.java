import conexao.bucket.S3Provider;
import model.IndicadorSeguranca;
import model.Municipio;
import model.OcorrenciaSeguranca;
import model.PopulacaoMunicipio;
import repository.IndicadorSegurancaRepository;
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

        // Credenciais da sessão do AWS Academy
        System.setProperty("aws.accessKeyId", "ASIA3STB4UX4R6RB6URB");
        System.setProperty("aws.secretAccessKey", "LmqKm17RVSxPGFHWRl8mTnGXNht7Ix0CtxdCHdC9");
        System.setProperty("aws.sessionToken", "IQoJb3JpZ2luX2VjEDIaCXVzLXdlc3QtMiJHMEUCIQDZjz96TLMQWmBTMgWT6WfzrQryQaxs3tM2IL8UP0n21gIgbtj2yE9TUWU0wB5DW5NJbEYVOuVvRxxEwAPx8UEGL1wqvAII+///////////ARAAGgw3OTU4NDc5OTI4MjUiDJH+l9LtT5kK6s6QhyqQAvNQD+EZolFjQV88gKA8hJAdDohb543ETlkk2go5+vNb7koj3UZP0n5I0Y5WRmvB48Rl+RC3SQewscorZKirHmtXxr7hcVrqyoA9mZwVuaOL5w6haoPn4KqgCy5e6e4Q8Ehq2nEcF/BcXQjTRJG2PkHH+lObYQeCOej0Xzm20x44AwLzRc2DwvnmdCaZOYqlT7KrOOJtWA5RT18G4O6yRzkULzcKfp1rC/E/5UhakZ4r2+ryYYmgzDuSKQwERLhm6+TtwR6kr/Z6Rw39/APMnjBwOWVoaOfzu9dJRcuDkdfx6ncHW3Ks2NYO7BZLrHEWG1iCqbyZKtYP0S05z3lNA+4SbblepkgR+BH7dmJlnlYyMOPQudAGOp0Ba36Ey9JZV2c5Xgxm76sxvz+wcZQ1BgGkWsNPHGejziXzhPPvu7MqMqMKxOpwPU++ZM0wsTCeZYFlHGmSOmMT853uGthSadOporQiUdLXXXxZuWZO3g5eyss3oCegkEAtOp5XZ4z0va/qockz40/k5kJRQNq9o7C3gUJB+PPFv3k71gM/c62YyA3Pmgiz3v7udEBwO01ibGhag/hR5A==");

        LeitorExcel leitor = new LeitorExcel();
        MunicipioRepository municipioRepo = new MunicipioRepository();
        OcorrenciaSegurancaRepository ocorrenciaRepo = new OcorrenciaSegurancaRepository();
        IndicadorSegurancaRepository indicadorRepo = new IndicadorSegurancaRepository();
        PopulacaoMunicipioRepository populacaoRepo = new PopulacaoMunicipioRepository();

        S3Client s3Client = new S3Provider().getS3Client();
        String bucketName = "17042026-safety";

        listarBuckets(s3Client);
        listarObjetos(s3Client, bucketName);
        baixarObjetos(s3Client, bucketName);

        // 1) IDHM (data_idhm.xlsx) — fluxo original
        System.out.println("\n==> Carregando IDHM (data_idhm.xlsx)");
        List<Municipio> municipios = leitor.ler("data_idhm.xlsx");
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
