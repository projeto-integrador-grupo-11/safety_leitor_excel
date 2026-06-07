package repository;

import conection.Conexao;
import model.PopulacaoMunicipio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class PopulacaoMunicipioRepository extends RepositorioBase {

    private static final int TAMANHO_LOTE = 1000;

    public PopulacaoMunicipioRepository() {
        super();
    }

    public PopulacaoMunicipioRepository(LogRepository logRepo) {
        super(logRepo);
    }

    public void limpar() {
        limpar("populacao_municipio");
    }

    public void salvarLista(List<PopulacaoMunicipio> populacoes) {
        if (populacoes == null || populacoes.isEmpty()) {
            log.info("Nenhuma população municipal para salvar.");
            return;
        }

        String sql = "INSERT INTO populacao_municipio (uf, nome_municipio, populacao, ano) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int contador = 0;
                int totalInserido = 0;
                log.info("Iniciando inserção de população municipal. Registros: {}", populacoes.size());

                for (PopulacaoMunicipio p : populacoes) {
                    stmt.setString(1, p.getUf());
                    stmt.setString(2, p.getNomeMunicipio());
                    stmt.setInt(3, p.getPopulacao());
                    stmt.setInt(4, p.getAno());
                    stmt.addBatch();
                    contador++;

                    if (contador % TAMANHO_LOTE == 0) {
                        stmt.executeBatch();
                        totalInserido += TAMANHO_LOTE;
                        log.info("População municipal inserida (parcial): {}", totalInserido);
                    }
                }

                stmt.executeBatch();
                conn.commit();

                log.info("População municipal inserida. Total: {}", contador);
                logRepo.salvar("INFO", "População municipal inserida: " + contador);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Erro ao salvar população municipal", e);
            logRepo.salvar("WARN", "Erro ao salvar população municipal: " + e.getMessage());
        }
    }
}
