package printer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;

public class tweetsProcess {
	
	public static void GetHashTag(Connection conn) throws FileNotFoundException, UnsupportedEncodingException, SQLException{
		DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
 	    Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Update Twitter "+ logFile.format(proDate) +".txt", "UTF-8");
    	
		String tempA = " ";
    	String updateTableSQL = "update twitterSplit set  link=?, linkTitle=? where id=?";
    	PreparedStatement PSUpdate = (PreparedStatement) conn.prepareStatement(updateTableSQL);
		
	}
	
	public static void twitterReWeb(Connection conn , int start) throws FileNotFoundException, UnsupportedEncodingException, SQLException{

		DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
 	    Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Re Twitter "+ logFile.format(proDate) +".txt", "UTF-8");
    	
    	start = start * 50000;
		Statement stmt = (Statement) conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT S.linkTitle,S.id from twittersplit as S left join twitternn as N on S.id = N.id where (not S.linkTitle='[]') and N.date between '2015-10-01' and '2016-02-01' limit " + start + " , 50000");
		
	}

	public static void updateTweetsLinkTitle(Connection conn) throws IOException, SQLException{
		DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
 	    Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Update Twitter "+ logFile.format(proDate) +".txt", "UTF-8");
    	
		String tempA = " ";
    	String updateTableSQL = "update twitterSplit set  link=?, linkTitle=? where id=?";
    	PreparedStatement PSUpdate = (PreparedStatement) conn.prepareStatement(updateTableSQL);

        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id,link FROM twittersplit where link like '%"+tempA+"%'");
        int _count = 0;
        while (rs.next()){
        	try{
        		String newLink = rs.getString("link").replace(tempA, "");
				Document doc = Jsoup.connect(newLink).followRedirects(true).userAgent("Mozilla").timeout(15*1000).get();
				String newTitle = doc.title();
				PSUpdate.setString(1, newLink);
				PSUpdate.setString(2, newTitle);
				PSUpdate.setInt(3, rs.getInt("id"));
				PSUpdate.addBatch();
        	}catch(Exception E){
        		String newLink = rs.getString("link").replace(tempA, "");
				System.out.println(E.getMessage() + " URL: " +  rs.getString("link"));
				writer.println("SQL ID : " + rs.getInt("id"));
				writer.println("---------------------------------");
				PSUpdate.setString(1, newLink);
				PSUpdate.setString(2, "");
				PSUpdate.setInt(3, rs.getInt("id"));
				PSUpdate.addBatch();
        	}
        	if(_count % 100==0){
        		System.out.println("Update Version #1");
                PSUpdate.executeBatch();
        	}
        	_count++;
        }
        writer.close();
	}

	public static void TweetsToWeb(Connection conn , int start) throws IOException, SQLException, ParseException{
		DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
 	    Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Twitter to Web File "+ logFile.format(proDate) +".txt", "UTF-8");
        Statement stmt = (Statement) conn.createStatement();
        start = start * 40000;
        ResultSet rs = stmt.executeQuery("SELECT S.link,S.id from twittersplit as S left join twitternn as N on S.id = N.id where S.web=0 and N.date between '2015-02-01' and '2015-10-01' and (not S.link='') limit "+start+",40000");

    	String UpdateSQL = "update twitterSplit set web=? where id=?";
    	PreparedStatement PSUp = (PreparedStatement) conn.prepareStatement(UpdateSQL);
    	System.out.println("TweetsToNews Execute !!");
    	int _count = 0;
        while (rs.next()){
        	int sqlId = rs.getInt("id");
        	String _l = rs.getString("link").replace(" ", "").replace("…", "");
			ExecutorService executorText = Executors.newSingleThreadExecutor();
			try {
	        	_l = URLDecoder.decode(_l, "UTF-8");

				Callable<String> callableEx = new ExWebNews(conn , _l , rs.getInt("id"));
				Future<String> futureText = executorText.submit(callableEx);
				String result;
				result = futureText.get();
				String re[] = result.split(":::");
				if(re[0].equals("E")){
					writer.println(re[1]+" -> "+re[2]+" -> "+re[3]);
				}
				PSUp.setInt(1, Integer.parseInt(re[2]));
				PSUp.setInt(2, Integer.parseInt(re[1]));
				PSUp.addBatch();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e){
				System.out.println("Error");
				PSUp.executeBatch();
			}
        	_count++;
        	if(_count%1000==0){
        		System.out.println("----------------------------");
        		System.out.println("Process :: "+_count);
                PSUp.executeBatch();
        		System.out.println("----------------------------");
        	}
        }
        PSUp.executeBatch();
    	writer.close();
	}
	
	
	public static void TweetsToNews(Connection conn) throws IOException, SQLException, ParseException{

		DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
 	    Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Twitter File "+ logFile.format(proDate) +".txt", "UTF-8");
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id,`text` FROM twitternn where id > 601000");

    	String insertTableSQL = "INSERT IGNORE INTO twitterSplit (id, link, tags, peoples, linkTitle) VALUES (?,?,?,?,?)";
    	PreparedStatement PSInsert = (PreparedStatement) conn.prepareStatement(insertTableSQL);
    	int _count = 601000;
    	System.out.println("TweetsToNews Execute !!");
        while (rs.next()) {
        	int sqlId = rs.getInt("id");
        	String _s = rs.getString("text");
        	String _sArray[] = _s.split(" ");
        	String link="";
        	List<String> linkTitle = new ArrayList<String>();
        	List<String> hashTag = new ArrayList<String>();
        	List<String> authorTag = new ArrayList<String>();
        	for(String _a : _sArray){
        		try{
	        		if(_a.contains("https://") || _a.contains("http://")){
	        			try{
		        			link = _a;
		            		Document doc = Jsoup.connect(_a).followRedirects(true).userAgent("Mozilla").timeout(15*1000).get();
		            		linkTitle.add(doc.title());
	        			}catch(HttpStatusException E){
	        				System.out.println(E.getMessage() + " URL: " +  _a);
	        				writer.println("SQL ID : " + sqlId);
	        				writer.println("HttpStatusException :" + _a);
	        				writer.println("---------------------------------");
	        			}catch(Exception E){
	        				System.out.println(E.getMessage() + " URL: " +  _a);
	        				writer.println("SQL ID : " + sqlId);
	        				writer.println("IllegalArgumentException : " + _a);
	        				writer.println("---------------------------------");
	        			}
	        		}else if(_a.substring(0, 1).equals("#")){
	        			hashTag.add(_a);
	        		}else if(_a.substring(0, 1).equals("@")){
	        			authorTag.add(_a);
	        		}
        		}catch(Exception E){
    				System.out.println(E.getMessage() + " URL: " +  _a);
    				writer.println("SQL ID : " + sqlId);
    				writer.println("StringIndexOutOfBoundsException : " + _a);
    				writer.println("---------------------------------");
        		}
        	}

        	PSInsert.setInt    (1, sqlId);
        	PSInsert.setString (2, link);
        	PSInsert.setString (3, new Gson().toJson(hashTag));
        	PSInsert.setString (4, new Gson().toJson(authorTag));
        	PSInsert.setString (5, new Gson().toJson(linkTitle));
        	PSInsert.addBatch();
        	_count++;
        	if(_count%1000==0){
        		System.out.println("----------------------------");
        		System.out.println("Process :: "+_count);
                PSInsert.executeBatch();
        		System.out.println("----------------------------");
        	}
        }
        PSInsert.executeBatch();
    	writer.close();
	}
	
	
	
}
