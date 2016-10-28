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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;


class getConn implements Callable<String>{
	Connection conn;
	String url;
	int id;
	public getConn(Connection conn2, String string , int i) {
		// TODO Auto-generated constructor stub
		conn = conn2;
		url = string;
		id = i;
	}
	void getConn(){
		System.out.println("Error");
	}
	@Override
	public String call(){
		// TODO Auto-generated method stub
		try{
			org.jsoup.Connection _c = Jsoup.connect(url).followRedirects(true).userAgent("Mozilla").timeout(15*1000);
			Response response = _c.execute();
			Document doc = _c.get();
			Element taglang = doc.select("html").first();
			String lang=taglang.attr("lang").length()>0?taglang.attr("lang"):"no";
			return id+"&"+lang;
		}catch(Exception E){
			return id+"&no";
		}
	}
}

public class terms {

	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    return pattern.matcher(nfdNormalizedString).replaceAll("");
	}

	public static void DateUpdate(Connection conn) throws SQLException, IOException{
		System.out.println("Update Date");
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select n.tid,t.date from newstermsweb as n left join twitternn as t on n.tid=t.id");

    	String UpdateSQL = "update newstermsweb set date=? where tid=?";
    	PreparedStatement psUpdate = (PreparedStatement) conn.prepareStatement(UpdateSQL);
    	int a=0;
        while (rs.next()) {
        	psUpdate.setDate(1, rs.getDate("date"));
        	psUpdate.setInt(2, rs.getInt("tid"));
        	psUpdate.addBatch();
        	if(++a%1000==0){
        		psUpdate.executeBatch();
        	}
        }
        psUpdate.executeBatch();
    }

    public static void UpdateLang(Connection conn) throws SQLException, IOException{
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT url,tid FROM twitterreweb");
        ExecutorService executorText = Executors.newFixedThreadPool(30);
        List<Future<String>> listFuture = new ArrayList<Future<String>>();
        

    	String updateSQL = "update newstermsweb set lang=? where tid=?";
    	PreparedStatement psUpdate = (PreparedStatement) conn.prepareStatement(updateSQL);
        while (rs.next()) {
        	Callable<String> findLang = new getConn(conn, rs.getString("url"), rs.getInt("tid"));
        	Future<String> ft = executorText.submit(findLang);
        	listFuture.add(ft);
        }
        System.out.println("finish part one");
        int i=0;
        for(Future<String> _f : listFuture){
        	try {
				String re = _f.get();
				String r[] = re.split("&");
				System.out.println(r[0]);
				if(r[1].contains("en")){
					psUpdate.setString(1, "en");
					psUpdate.setInt(2, Integer.parseInt(r[0]));
					psUpdate.addBatch();
				}else{
					psUpdate.setString(1, "no");
					psUpdate.setInt(2, Integer.parseInt(r[0]));
					psUpdate.addBatch();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	if(++i%1000==0){
        		System.out.println("Submit!! i = "+ i);
        		psUpdate.executeBatch();
        	}
        }
        psUpdate.executeBatch();
        System.out.println("finish part two");
        executorText.shutdown();
        listFuture=null;
    }
    
	public static void removeOneCharWeb(Connection conn) throws SQLException, IOException{
		System.out.println("Remove One Char form Web");
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT term,termid from terms_web");

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
    }
	
	public static void findLang(Connection conn) throws SQLException, IOException{
		System.out.println("Find Lang from table[newstermsweb] ");
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT w.body,w.tid FROM twitterreweb as w left join newstermsweb as n on w.tid=n.tid where n.lang='un'");

    	String updateSQL = "update newstermsweb set lang=? where tid=?";
    	PreparedStatement psUpdate = (PreparedStatement) conn.prepareStatement(updateSQL);
    	int a=0;
        while (rs.next()) {
        	String tempString = rs.getString("body").replace("café", "cafe");
        	String normalized = Normalizer.normalize(tempString, Form.NFD);
        	if(!tempString.equals(normalized)){
    			psUpdate.setString(1, "no");
    			psUpdate.setInt(2, rs.getInt("tid"));
    			psUpdate.addBatch();
        	}
        	if(++a%1000==0){
        		System.out.println("Process : " + a);
        		psUpdate.executeBatch();
        	}
        }
		psUpdate.executeBatch();
    }

	public void NewsTermsWeb(Connection conn) throws SQLException, FileNotFoundException, UnsupportedEncodingException{
		System.out.println("PROCESS NewsTerms for Web");
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("news terms web"+ logFile.format(proDate) +".txt", "UTF-8");
    	
		Statement stmt = (Statement) conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * from newstermsweb where lang='en'");
//		ResultSet rs = stmt.executeQuery("SELECT * from newstermsweb where not done=1");
    	String insertOneLine = "INSERT INTO terms_web (term,term2news,Num,done) VALUES (?,?,?,0)";
    	PreparedStatement newsTermsInsert = (PreparedStatement) conn.prepareStatement(insertOneLine);
    	Gson gsonReceiver = new Gson();
    	int count = 0;
    	
    	HashMap<String, Set<Integer>> mainMap = new HashMap<String, Set<Integer>>();
		while (rs.next()) {
        	int twitterID = rs.getInt("tid");
        	HashMap<String, Integer> newsMap = gsonReceiver.fromJson(rs.getString("njson"),new TypeToken<HashMap<String,Integer>>(){}.getType());
        	Set<String> _items = newsMap.keySet();
    		for (String s : _items) {
    			s = deAccent(s).toLowerCase();
    			if(!s.contains("/") && !s.contains("@") && !s.contains(">") && !s.contains("<") && !s.contains("#") && !s.contains(":") && !s.contains("?") && !s.contains("\"") && !s.contains("\'") && !s.equals("")){
	    			if(mainMap.containsKey(s)){
	    				Set<Integer> a = mainMap.get(s);
	    				a.add(twitterID);
	    				mainMap.put(s, a);
	    			}else{
	    				Set<Integer> a = new HashSet<Integer>();
	    				a.add(twitterID);
	    				mainMap.put(s, a);
	    			}
    			}
    		}
    		count++;
    		if(count%100==0){
    			System.out.println("Process :: "+twitterID);
    		}
        }
//		reBackUpdate.executeBatch();
		/*
		 * 
		 * Update news and how much the term
		 * 
		 * */
		
		
		
		
		Set<String> _items = mainMap.keySet();
		count = 0;
		for (String s : _items) {
			JSONArray _j = new JSONArray(mainMap.get(s));
			String tempS = mainMap.get(s).toString();
			tempS = _j.toString();
			newsTermsInsert.setString(1, s);
			newsTermsInsert.setString(2, tempS);
			newsTermsInsert.setInt(3, mainMap.get(s).size());
			try{
				newsTermsInsert.execute();
			}catch(Exception E){
				writer.println(s+":+:"+tempS+":+:"+mainMap.get(s).size());
				E.printStackTrace();
			}
			
			count++;
    		if(count%1000==0){
    			System.out.println("Process :: " + count + "/" + _items.size());
    		}
		}
		System.out.println("Over!!");
		writer.close();
	}

	
	
}
