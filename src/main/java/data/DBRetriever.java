package data;
import java.sql.*;
import java.util.*;
import java.lang.*;
import org.postgis.*;
import org.postgresql.util.PGobject;

/**
 * Created by petrovalexey on 28.05.16.
 */
public class DBRetriever {

    public static void main(String[] args){
        DataCreator.isUpToDate("modis",1);
//        getGeom();
    }

    public static void getGeom(){
        String dburl="jdbc:postgresql://localhost/postgres";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dburl, "postgres", "postgres");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT wkb_geometry FROM modisfire");
            rs.next();
            PGobject result = (PGobject) rs.getObject(1);
            System.out.println(result);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
