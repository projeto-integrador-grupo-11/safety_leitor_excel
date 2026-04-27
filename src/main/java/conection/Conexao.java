package conection;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Conexao {

        private static Properties props = new Properties();

        static {
            try (InputStream is = Conexao.class.getClassLoader().getResourceAsStream("config.properties")) {
                props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static Connection getConexao() throws Exception {
            return DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password")
            );
        }
}