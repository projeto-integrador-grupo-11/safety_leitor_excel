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
        System.setProperty("aws.accessKeyId", "ASIA3STB4UX45XUSGYZ7");
        System.setProperty("aws.secretAccessKey", "tAOrc9g2Irq+fjd2AxMg41qSG2qT2ewf8XWdvPug");
        System.setProperty("aws.sessionToken", "IQoJb3JpZ2luX2VjEKr//////////wEaCXVzLXdlc3QtMiJHMEUCIQC77mYi4w1UM/+IU3sPtmAb/GjbyrXCW8GAx6K47/Sv1AIgQGIXIKlGTjWss8qa2uGEWHF3siiX042hs3U9Pp42fI8qswIIcxAAGgw3OTU4NDc5OTI4MjUiDOhYsr8YcSiVfifGjiqQAg6DpA9sRKBYW7OSZf1sqwiunYoOKMv7jqXhtR7OQ3VS6i6XR+RT6tnViiIUzwaHJMYveZrkFBCLJRkURJpmGCPEzk578TrqmjJ7gasnaHNRzIIyAh13HujlQbTOZHYB/ODf9uICXMSxfgDh2vtD4CTqxuiXzshhjcfb8J3cI7FoK2pXE19E2aczZbtOgAK1e0/DqcrkcHMWo8HA3fTwZn0Y3puskFHVWvb737rNNfslgfSP1cfFhlQdhYsr3vtuMii4nsV6TZQIuJKKHO+FbKY5/mb41fl6XgGs4z5/koiSZK/xZPnZpFFn6HYeDczCKY9LZYQQg7O9t+ypxg9ZuIC0NEOJEXyCT/jFRtFuAgXgMJj609AGOp0BT+UVMIQBxC2eHjRgE3xwRQ0vwziiTi9A+lbU60VBmywyVluis2q8E84hEoWmlioMIk6NY+0lGOOn7HIBd/2m78+qnjDO5DB825Yfb4VMrgk3f7OSv5gJ7wjjmXIZVBi2YPUBGjS8XAolREgzzZmRYBWLYIZ2z9uIUhoOL1wfVGqSe8XVOX5qzJt3qzA3NofB995DDfaqdaEjEzrV/w==");

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
