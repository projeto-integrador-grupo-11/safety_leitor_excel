package repository;

import conection.Conexao;
import model.Municipio;
import model.Pessoa;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class MunicipioRepository {
    private LogRepository logRepo = new LogRepository();
    private static final Logger log = LogManager.getLogger(MunicipioRepository.class);

    public void salvarLista(List<Municipio> municipios) {

        String sql = "INSERT INTO municipio (nome, idhm_geral, renda, educacao, longevidade) \n" +
                "VALUES (?, ?, ?, ?, ?);";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Municipio p : municipios) {

                stmt.setString(1, p.getNome());
                stmt.setDouble(2,p.getIdhmGeral());
                stmt.setDouble(3,p.getRenda());
                stmt.setDouble(4,p.getEducacao());
                stmt.setDouble(5,p.getLongevidade());

                stmt.executeUpdate();

                log.info("Municipio salvo: {}", p);
            }

            log.info("Todas os municipios foram inseridos no banco!");

        } catch (Exception e) {
            log.error("Erro ao salvar no banco", e);
            logRepo.salvar("WARM", "Erro ao salvar no banco");
        }
    }
}