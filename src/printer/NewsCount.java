package printer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class NewsCount {

	public static void doCount(Connection conn) throws SQLException, IOException{
		System.out.println("Find Lang");
        Statement stmt = (Statement) conn.createStatement();
    	ResultSet rs =  stmt.executeQuery("select term2news,term from terms_web where num>5");
		HashMap<String , Double> termMap = new HashMap<String , Double>();
        rs = stmt.executeQuery("select tid,njson from newstermsweb where lang='en'");

    	String DeleteSQL = "delete from terms_web where termid=?";
    	PreparedStatement psDelete = (PreparedStatement) conn.prepareStatement(DeleteSQL);
    	int a=0;
        Pattern pattern = Pattern.compile(".*[a-zA-Z]+.*");

        while (rs.next()) {
            Matcher matcher = pattern.matcher(rs.getString("term"));
            if(!matcher.matches()){
        		psDelete.setInt(1, rs.getInt("termid"));
        		psDelete.addBatch();
            }
        	if(rs.getString("term").length()==1){
        		psDelete.setInt(1, rs.getInt("termid"));
        		psDelete.addBatch();
        	}
        }
		psDelete.executeBatch();
        System.exit(0);
    }
}
