package repository;


import conection.Conexao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class LogRepository {
    private static final Logger log = LogManager.getLogger(MunicipioRepository.class);
    public void salvar(String nivel, String mensagem) {


        String sql = "INSERT INTO log_sistema (nivel, mensagem, data_hora) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nivel);
            stmt.setString(2, mensagem);
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

        } catch (Exception e) {
            log.error("Erro ao salvar no banco", e);
        }
    }
}