package repository;

import conection.Conexao;
import model.Municipio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

public class MunicipioRepository extends RepositorioBase {

    public MunicipioRepository() {
        super();
    }

    public MunicipioRepository(LogRepository logRepo) {
        super(logRepo);
    }

    public void limpar() {
        limpar("municipio");
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
                setDoubleOrNull(stmt, 3, p.getIdhmGeral());
                setDoubleOrNull(stmt, 4, p.getRenda());
                setDoubleOrNull(stmt, 5, p.getEducacao());
                setDoubleOrNull(stmt, 6, p.getLongevidade());

                stmt.executeUpdate();

                log.info("Municipio salvo: {}", p);
            }

            log.info("Todos os municipios foram inseridos no banco! Total: {}", municipios.size());

        } catch (Exception e) {
            log.error("Erro ao salvar no banco", e);
            logRepo.salvar("WARM", "Erro ao salvar no banco");
        }
    }

    private static void setDoubleOrNull(PreparedStatement stmt, int index, Double value) throws Exception {
        if (value == null) {
            stmt.setNull(index, Types.DOUBLE);
        } else {
            stmt.setDouble(index, value);
        }
    }
}