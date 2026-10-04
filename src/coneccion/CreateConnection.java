package coneccion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class CreateConnection {
    static Properties config = new Properties();
    String hostname = null;
    String port = null;
    String database = null;
    String username = null;
    String password = null; 
    
    
    public CreateConnection (){
        String path = "/home/abdias/NetBeansProjects/ProyectoFInalUMG/src/coneccion/db_config.properties";
        InputStream in = null;
        
            try {
                in = Files.newInputStream(Paths.get(path));  
                config.load(in);
                loadPropertir();
                
            }catch (IOException ex){
                        System.out.println(ex.getMessage());
                        }
    } 
    
    // metodo cargar propieddades 
    public void loadPropertir (){
        this.hostname = config.getProperty("hostname");
        this.port = config.getProperty("port");
        this.database = config.getProperty("database");
        this.username = config.getProperty("username");
        this.password = config.getProperty("password");
        
    }
    public Connection getConection(){
        try {
            Connection conn = null;
            String jdbURL = "jdbc:postgresql://"+this.hostname+":"+
                    this.port + "/" + this.database;
            conn = DriverManager.getConnection(jdbURL, username, password);
            System.out.println("Conexion establecida");
            // retorna la secion abierta de bd
            return conn;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }
}
