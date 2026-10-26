package conexion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/** Conexión heredada opcional; el cliente JavaFX normal utiliza la API REST. */
public class Coneccion {
    public static Connection CreateConnection() {
        String url=System.getenv("LEGACY_JDBC_URL");
        String user=System.getenv("LEGACY_JDBC_USER");
        String password=System.getenv("LEGACY_JDBC_PASSWORD");
        if(url==null || user==null || password==null)
            throw new IllegalStateException("La conexión JDBC heredada no está configurada. Usa el cliente JavaFX con la API.");
        try { return DriverManager.getConnection(url,user,password); }
        catch(SQLException ex) { throw new IllegalStateException("No se pudo abrir la conexión JDBC heredada.",ex); }
    }
}
