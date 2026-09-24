/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package conexion;
import java.sql.*;
/**
 *
 * @author adrir
 */
public class Coneccion {
    
    private static String URL="jdbc:postgresql://ep-lingering-night-aukh27yr-pooler.c-10.us-east-1.aws.neon.tech:5432/neondb?sslmode=require";
    private static String user="neondb_owner";
    private static String password="npg_zUgWJZp17wHk";
    
    public static Connection CreateConnection(){
    
        try{
        
            Connection con = DriverManager.getConnection(URL, user, password);
            System.out.println("se conecto");
            return con;
            
        }catch(SQLException e){
        e.printStackTrace();
        return null;
        }
    }
}
