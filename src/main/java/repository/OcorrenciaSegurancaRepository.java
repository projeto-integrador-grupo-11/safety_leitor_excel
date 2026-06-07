package repository;

import conection.Conexao;
import model.OcorrenciaSeguranca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class OcorrenciaSegurancaRepository extends RepositorioBase {

    private static final int TAMANHO_LOTE = 1000;

    public OcorrenciaSegurancaRepository() {
        super();
    }

    public OcorrenciaSegurancaRepository(LogRepository logRepo) {
        super(logRepo);
    }

    public void limpar() {
        limpar("ocorrencia_seguranca");
    }

    public void salvarLista(List<OcorrenciaSeguranca> ocorrencias) {
        if (ocorrencias == null || ocorrencias.isEmpty()) {
            log.info("Nenhuma ocorrência de segurança para salvar.");
            return;
        }

        String sql = "INSERT INTO ocorrencia_seguranca " +
                "(uf, nome_municipio, evento, ano_ref, mes_ref, qtd_vitimas) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int contador = 0;
                int totalInserido = 0;

                for (OcorrenciaSeguranca o : ocorrencias) {
                    stmt.setString(1, o.getUf());
                    stmt.setString(2, o.getNomeMunicipio());
                    stmt.setString(3, o.getEvento());
                    stmt.setInt(4, o.getAnoRef());
                    stmt.setInt(5, o.getMesRef());
                    stmt.setInt(6, o.getQtdVitimas());
                    stmt.addBatch();
                    contador++;

                    if (contador % TAMANHO_LOTE == 0) {
                        stmt.executeBatch();
                        totalInserido += TAMANHO_LOTE;
                        log.info("Ocorrências salvas: {}", totalInserido);
                    }
                }

                stmt.executeBatch();
                conn.commit();

                log.info("Todas as ocorrências foram inseridas no banco! Total: {}", contador);
                logRepo.salvar("INFO", "Ocorrências de segurança inseridas: " + contador);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Erro ao salvar ocorrências de segurança", e);
            logRepo.salvar("WARN", "Erro ao salvar ocorrências de segurança: " + e.getMessage());
        }
    }
}
