package repository;

import conection.Conexao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Superclasse abstrata de todos os repositórios (Herança).
 *
 * Centraliza o logger, a dependência de {@link LogRepository} e a operação
 * comum {@link #limpar(String)}, eliminando a duplicação entre os repositórios
 * concretos.
 *
 * O relacionamento com {@link LogRepository} ilustra dois conceitos:
 * <ul>
 *   <li><b>Composição</b>: o construtor padrão cria e é dono do próprio
 *       LogRepository — seu ciclo de vida está atrelado ao repositório.</li>
 *   <li><b>Agregação</b>: o construtor que recebe um LogRepository usa uma
 *       instância já existente e compartilhada, cujo ciclo de vida é
 *       independente do repositório.</li>
 * </ul>
 */
public abstract class RepositorioBase {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final LogRepository logRepo;

    /** Composição: o repositório cria e possui o seu próprio LogRepository. */
    protected RepositorioBase() {
        this.logRepo = new LogRepository();
    }

    /** Agregação: o repositório reutiliza um LogRepository fornecido externamente. */
    protected RepositorioBase(LogRepository logRepo) {
        this.logRepo = logRepo;
    }

    /** Operação comum a todos os repositórios: limpar a tabela antes da carga. */
    protected void limpar(String tabela) {
        try (Connection conn = Conexao.getConexao();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + tabela);
            log.info("Tabela {} limpa antes da carga.", tabela);
        } catch (Exception e) {
            log.error("Erro ao limpar {}", tabela, e);
            logRepo.salvar("WARN", "Erro ao limpar " + tabela + ": " + e.getMessage());
        }
    }
}
