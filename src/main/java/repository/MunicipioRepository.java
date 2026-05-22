package repository;

import conection.Conexao;
import model.Municipio;
import model.Pessoa;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

public class MunicipioRepository {
    private LogRepository logRepo = new LogRepository();
    private static final Logger log = LogManager.getLogger(MunicipioRepository.class);

    public void limpar() {
        try (Connection conn = Conexao.getConexao();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM municipio");
            log.info("Tabela municipio limpa antes da carga.");
        } catch (Exception e) {
            log.error("Erro ao limpar municipio", e);
            logRepo.salvar("WARN", "Erro ao limpar municipio: " + e.getMessage());
        }
    }

    public void salvarLista(List<Municipio> municipios) {

        if (municipios == null || municipios.isEmpty()) {
            log.info("Nenhum município para salvar — lista vazia.");
            return;
        }

        String sql = "INSERT INTO municipio (uf, nome, idhm_geral, renda, educacao, longevidade) \n" +
                "VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Municipio p : municipios) {

                stmt.setString(1, p.getUf());
                stmt.setString(2, p.getNome());
                stmt.setDouble(3,p.getIdhmGeral());
                stmt.setDouble(4,p.getRenda());
                stmt.setDouble(5,p.getEducacao());
                stmt.setDouble(6,p.getLongevidade());

                stmt.executeUpdate();

                log.info("Municipio salvo: {}", p);
            }

            log.info("Todos os municipios foram inseridos no banco! Total: {}", municipios.size());

        } catch (Exception e) {
            log.error("Erro ao salvar no banco", e);
            logRepo.salvar("WARM", "Erro ao salvar no banco");
        }
    }
}