package printer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class google4News {

	public void googleNews(Connection conn, int start) throws IOException, JSONException, SQLException, InterruptedException, ExecutionException{
		
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("googleNews "+ logFile.format(proDate) +".txt", "UTF-8");
		start = start * 50000;
		Statement stmt = (Statement) conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT S.linkTitle,S.id from twittersplit as S left join twitternn as N on S.id = N.id where (not S.linkTitle='[]') and N.date between '2015-06-22' and '2016-01-11' and google=0 limit " + start + " , 50000");
		String reBack = "UPDATE twittersplit set google=? where id=?";
		PreparedStatement reBackUpdate = (PreparedStatement) conn.prepareStatement(reBack);
		int c = 0;
		while(rs.next()){
			try{
				JSONArray ja = new JSONArray(rs.getString("linkTitle"));
				ExecutorService executorText = Executors.newSingleThreadExecutor();
				if(!ja.getString(0).equals("Instagram")){
					writer.println(" ### " + ja.getString(0));
					Callable<String> callableGoogle = new multiGoogle(conn , ja.getString(0) , rs.getInt("id"));
					Future<String> futureText = executorText.submit(callableGoogle);
					reBackUpdate.setInt(1, 1);
					reBackUpdate.setInt(2, rs.getInt("id"));
					try{
						String result = futureText.get();
						if(!result.equals("")){
							System.out.println("result is null");
							System.exit(1);
							reBackUpdate.executeUpdate();
						}
					}catch(Exception e){
						writer.println("result error id = " + rs.getInt("id"));
					}
				}else{
					// it is IG
					reBackUpdate.setInt(1, 3);
					reBackUpdate.setInt(2, rs.getInt("id"));
					reBackUpdate.addBatch();
				}
			}catch(JSONException E){
				// json format error
				reBackUpdate.setInt(1, 2);
				reBackUpdate.setInt(2, rs.getInt("id"));
				reBackUpdate.executeUpdate();
				writer.println("JSON FORMAT ERROR ID = " + rs.getInt("id"));
				System.out.println("JSON FORMAT ERROR ID = " + rs.getInt("id"));
			}
			c++;
			if(c%100==0){
				System.out.println(c);
				Thread.sleep(c*10);
			}
			System.exit(1);
		}
		reBackUpdate.executeBatch();
		writer.close();
	}
	
}
