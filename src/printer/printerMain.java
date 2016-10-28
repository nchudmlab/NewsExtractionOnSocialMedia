package printer;

import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;

import java.io.IOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class printerMain {

	public static void main(String[] args) throws Exception {
		Connection conn = connSQL();
		DateFormat logFile = new SimpleDateFormat("yyyy-MM-dd HH:mm");
 	    Date proDate = new Date();
		System.out.println("System start!!"+logFile.format(proDate));
		
		
		if(args[0].equals("3ders")){
			scrapyNews sc = new scrapyNews();
			sc.toDo3ders(conn);
		}else if(args[0].equals("NLP")){
//			standfordProcess();
		}else if(args[0].equals("splite")){
			multiSpliter sp = new multiSpliter();
			sp.InsertNN(conn);;
		}else if(args[0].equals("spliteTemp")){
			multiSpliterTemp sp = new multiSpliterTemp();
			sp.InsertNN(conn, args[1]);
		}else if(args[0].equals("term2news")){
			Term2Event(conn, "news");
		}else if(args[0].equals("3dprint")){
			scrapyNews sc = new scrapyNews();
			sc.toDO3dprint(conn);
		}else if(args[0].equals("tweetsNews")){
			tweetsProcess tp = new tweetsProcess();
			tp.TweetsToNews(conn);
		}else if(args[0].equals("tweetsToWeb")){
			tweetsProcess tp = new tweetsProcess();
			tp.TweetsToWeb(conn, Integer.valueOf(args[1]));
		}else if(args[0].equals("tweetsUpdate")){
			tweetsProcess tp = new tweetsProcess();
			tp.updateTweetsLinkTitle(conn);
		}else if(args[0].equals("spliteTwitterNews")){
			multiSpliterTemp sp = new multiSpliterTemp();
			sp.InsertNN(conn, args[1]);
		}else if(args[0].equals("UpdateDB")){
			terms ts = new terms();
//			ts.DateUpdate(conn);
			ts.UpdateLang(conn);
			ts.findLang(conn);
			ts.NewsTermsWeb(conn);
			ts.removeOneCharWeb(conn);
		}else if(args[0].equals("PMI")){
			rePMI pm = new rePMI();
			pm.DoDate(conn);
//			pm.NewsTerms(conn);
//			pm.doCompute(conn, Double.parseDouble(args[1]), "newstermsweb");
			//nr.newsRel(conn, "newstermsweb", Integer.parseInt(args[1]));
		}else if(args[0].equals("TermRel")){
			termRel nr = new termRel();
			nr.termRel(conn, "newstermsweb", Integer.parseInt(args[1]));
		}else if(args[0].equals("TimeRel")){
			timeRel nr = new timeRel();
			nr.timeRel(conn);
		}else if(args[0].equals("NewsRel")){
			newsRel nr = new newsRel();
			nr.countNews(conn);
		}else if(args[0].equals("google4News")){
			google4News gn = new google4News();
			gn.googleNews(conn, Integer.parseInt(args[1]));
		}else if(args[0].equals("test")){
			nnDefindWiki();
		}else if(args[0].equals("removeLang")){
			terms ts = new terms();
		}else if(args[0].equals("newsCount")){
			//NewsCount ts = new NewsCount();
			//ts.doCount(conn);
		}else if(args[0].equals("NERDemo")){
			NERDemo ner = new NERDemo();
			ner.testNER();
		}else if(args[0].equals("newsScore")){
			newsScore NS = new newsScore(0.75 ,24);
			NS._Score(conn);
		}else if(args[0].equals("coverageAvg")){
			CoverAll NS = new CoverAll();
			NS.coverageNews(conn);
		}else if(args[0].equals("ground")){
			Ground NS = new Ground();
			NS.GroundDate(conn);
		}else{
			System.out.println("java -jar [jar name] [parameter]");
			System.out.println("Parameter : 3ders 3dprint newsSplite tweetsSplite term2news tweetsNews tweetsUpdate");
		}
		
		
		Date endDate = new Date();
		System.out.println("Close System!!"+logFile.format(endDate));
		System.exit(-1);
		conn.close();
    }
	



	
	private static void nnDefindWiki(){
		
		
	}
	
	
	
	
	private static Connection connSQL(){
		Connection conn = null;
		try {
				String myDriver = "org.gjt.mm.mysql.Driver";
				String myUrl = "jdbc:DB_uri";//DB_uri
				Class.forName(myDriver);
				conn = (Connection) DriverManager.getConnection(myUrl, username , password);// input username  , password
		}catch(ClassNotFoundException e){ 
				System.out.println("DriverClassNotFound :"+e.toString()); 
		}catch(SQLException x) { 
				System.out.println("Exception :"+x.toString()); 
		}
		return conn; 
	}
	
	
	
	public static void Term2Event(Connection conn,String workFor) throws  SQLException, JSONException{
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT newID,nnjson FROM newsnn");
        
		String updateTableSQL = "update terms2 set term2news=? where id=?";
		PreparedStatement updatePS = (PreparedStatement) conn.prepareStatement(updateTableSQL);
    	String insertTableSQL = "INSERT INTO terms2 (term2news, term) VALUES (?,?)";
    	PreparedStatement insertPS = (PreparedStatement) conn.prepareStatement(insertTableSQL);

    	
    	Map<String, List<Integer>> termListMap = new HashMap<String, List<Integer>>();
    	
        while(rs.next()){
        	Map<String, Integer> newsMap= new Gson().fromJson(rs.getString("nnjson") , new TypeToken<HashMap<String, Integer>>() {}.getType());
        	for (Map.Entry<String, Integer> entry : newsMap.entrySet()) {
    			if(!entry.getKey().equals("%") && !entry.getKey().equals("") && !entry.getKey().contains("?")){
            		String _k = entry.getKey().toLowerCase();
	        		if (termListMap.containsKey(_k)) {
	        			termListMap.get(_k).add(rs.getInt("newID"));
	        		}else{
		        		List<Integer> tempList = new ArrayList<Integer>();
		        		tempList.add(rs.getInt("newID"));
		        		termListMap.put(_k, tempList);
	        		}
    			}
        	}
        }
        
        System.out.println("StartInsert");

    	if(workFor.equals("news")){
	    	for (Map.Entry<String, List<Integer>> entry : termListMap.entrySet()) {
	    		try{
		            insertPS.setString(1, entry.getValue().toString());
		            insertPS.setString(2, entry.getKey());
		            insertPS.addBatch();
	    		}catch(Exception e){
	    			System.out.println(entry.getKey());
	    		}
	    	}
    	}else if(workFor.equals("social")){

    	}
    	try{
	    	insertPS.executeBatch();
	//        updatePS.executeBatch();
//	        conn.commit();

		}catch(Exception e){
			
		}
    }
	
	
}