package repository;

import conection.Conexao;
import model.IndicadorSeguranca;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

public class IndicadorSegurancaRepository {

    private static final Logger log = LogManager.getLogger(IndicadorSegurancaRepository.class);

    private final LogRepository logRepo = new LogRepository();

    public void limpar() {
        try (Connection conn = Conexao.getConexao();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM indicador_seguranca");
            log.info("Tabela indicador_seguranca limpa antes da carga.");
        } catch (Exception e) {
            log.error("Erro ao limpar indicador_seguranca", e);
            logRepo.salvar("WARN", "Erro ao limpar indicador_seguranca: " + e.getMessage());
        }
    }

    public void salvarLista(List<IndicadorSeguranca> indicadores) {
        if (indicadores == null || indicadores.isEmpty()) {
            log.info("Nenhum indicador de segurança para salvar.");
            return;
        }

        String sql = "INSERT INTO indicador_seguranca (uf, tipo, ano, quantidade) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                log.info("Iniciando inserção de indicadores de segurança. Registros: {}", indicadores.size());

                for (IndicadorSeguranca i : indicadores) {
                    stmt.setString(1, i.getUf());
                    stmt.setString(2, i.getTipo());
                    stmt.setInt(3, i.getAno());
                    stmt.setInt(4, i.getQuantidade());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();

                log.info("Indicadores de segurança inseridos. Total: {}", indicadores.size());
                logRepo.salvar("INFO", "Indicadores de segurança inseridos: " + indicadores.size());
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Erro ao salvar indicadores de segurança", e);
            logRepo.salvar("WARN", "Erro ao salvar indicadores de segurança: " + e.getMessage());
        }
    }
}
