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
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.MysqlDataTruncation;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class rePMI {
	class Term {
		public Double P;
		public Set<String> S;
		public Term(double _p, Set<String> _s) {
			P=_p;
			S=_s;
		}
	}
	

	public void NewsTerms(Connection conn) throws SQLException, FileNotFoundException, UnsupportedEncodingException{
		System.out.println("PROCESS NewsTerms");
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("news terms "+ logFile.format(proDate) +".txt", "UTF-8");
    	
		Statement stmt = (Statement) conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * from newstermsweb where (not done=1)");
    	String updateOneLine = "UPDATE terms_web as T1 INNER JOIN (SELECT concat(term2news, ?) as T2S , CAST(newsNum+1 as INT) as T2N FROM terms_web where term = ?) as T2 set T1.term2news = T2.T2S , T1.newsNum = T2.T2N  where T1.term = ?";
    	String insertOneLine = "INSERT INTO terms_web (term,term2news,newsNum,socialNum) VALUES (?,?,?,?)";
    	String reBack = "update newstermsweb set done=1,number=? where newID=?";
    	String updateOneLine2 = "UPDATE terms_web set term2news=?, newsNum=? where term=?";
    	PreparedStatement newsTermsUpdate = (PreparedStatement) conn.prepareStatement(updateOneLine2);
    	PreparedStatement newsTermsInsert = (PreparedStatement) conn.prepareStatement(insertOneLine);
    	PreparedStatement reBackUpdate = (PreparedStatement) conn.prepareStatement(reBack);
    	Gson gsonReceiver = new Gson();
    	int count = 0;
    	
    	HashMap<String, Set<Integer>> mainMap = new HashMap<String, Set<Integer>>();
		while (rs.next()) {
        	int socialID = rs.getInt("newID");
        	HashMap<String, Integer> newsMap = gsonReceiver.fromJson(rs.getString("njson"),new TypeToken<HashMap<String,Integer>>(){}.getType());
        	Set<String> _items = newsMap.keySet();
    		for (String s : _items) {
    			s = s.toLowerCase();
    			if(mainMap.containsKey(s)){
    				Set<Integer> a = mainMap.get(s);
    				a.add(socialID);
    				mainMap.put(s, a);
    			}else{
    				Set<Integer> a = new HashSet<Integer>();
    				a.add(socialID);
    				mainMap.put(s, a);
    			}
    		}
    		reBackUpdate.setInt(1, _items.size());
    		reBackUpdate.setInt(2, socialID);
    		reBackUpdate.addBatch();
    		count++;
    		if(count%100==0){
    			System.out.println("Process :: "+socialID);
    			reBackUpdate.executeBatch();
    		}
        }
		reBackUpdate.executeBatch();
		
		Set<String> _items = mainMap.keySet();
		count = 0;
		for (String s : _items) {
			String tempS = mainMap.get(s).toString();
			tempS = tempS.substring(1, tempS.length()-1).replace(", ", ";");		
			try{
				newsTermsUpdate.setString(1, tempS);
				newsTermsUpdate.setInt(2, mainMap.get(s).size());
				newsTermsUpdate.setString(3, s);
				if(newsTermsUpdate.executeUpdate()==0){
					throw new SQLException();
				}
			}catch(SQLException e){
				newsTermsInsert.setString(1, s);
				newsTermsInsert.setString(2, tempS);
				newsTermsInsert.setInt(3, 0);
				newsTermsInsert.setInt(4, mainMap.get(s).size());
				try{
					newsTermsInsert.execute();
				}catch(MySQLIntegrityConstraintViolationException e2){
					System.out.println("news write error : " + s + "\n\t"+tempS);
					writer.println("news write error");
					writer.println(s+"\t"+tempS);
				}catch(MysqlDataTruncation e3){
					System.out.println("news write error : " + s + "\n\t"+tempS);
					writer.println("Data truncation: Data too long");
					writer.println(s+"\t"+tempS);
				}
			}
			count++;
    		if(count%1000==0){
    			System.out.println("Process :: " + count + "/" + _items.size());
    		}
		}
		System.out.println("Over!!");
		writer.close();
	}
	public void DoDate(Connection conn) throws SQLException, FileNotFoundException, UnsupportedEncodingException{
		System.out.println("PROCESS Date");
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Date Parser "+ logFile.format(proDate) +".txt", "UTF-8");
    	
		Statement stmt = (Statement) conn.createStatement();
//		ResultSet rs = stmt.executeQuery("select n.date,w.tid from twitterreweb as w left join twitternn as n on w.tid=n.id");
		ResultSet rs = stmt.executeQuery("select date,tid from newstermsweb where lang='en'");	
    	String insertOneLine = "INSERT INTO dateWeb (date, tjson, number) VALUES (?,?,?)";
    	PreparedStatement newsTermsInsert = (PreparedStatement) conn.prepareStatement(insertOneLine);
    	int count = 0;
    	
    	HashMap<String, Set<Integer>> mainMap = new HashMap<String, Set<Integer>>();
		while (rs.next()) {
        	int socialID = rs.getInt("tid");
        	String _date = rs.getString("date");
        	if(mainMap.containsKey(_date)){
				Set<Integer> a = mainMap.get(_date);
				a.add(socialID);
				mainMap.put(_date, a);
        	}else{
				Set<Integer> a = new HashSet<Integer>();
				a.add(socialID);
				mainMap.put(_date, a);
        	}
        }
		
		Set<String> _items = mainMap.keySet();
		count = 0;
		for (String s : _items) {
			String tempS = mainMap.get(s).toString();
			newsTermsInsert.setString(1, s);
			newsTermsInsert.setString(2, tempS);
			newsTermsInsert.setInt(3, mainMap.get(s).size());
			newsTermsInsert.addBatch();
		}
		newsTermsInsert.executeBatch();
		System.out.println("Over!!");
		writer.close();
	}
	
	public void doCompute(Connection conn , double threshold , String newTable) throws IOException, JSONException, SQLException, InterruptedException, ExecutionException{
		int newsNumber = 1;
		
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mmss");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("PMI-"+threshold+"-"+ logFile.format(proDate) +".txt", "UTF-8");
    	
		Statement stmt = (Statement) conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT count(*) as C from "+newTable);
		while(rs.next()){
			newsNumber = rs.getInt("C");
		}
		

    	System.out.println("/************************************/");
    	System.out.println("newsNumber : " + newsNumber);
    	System.out.println("/************************************/");
		
		// Set Number more than 10
		rs = stmt.executeQuery("SELECT term,term2news,newsNum from terms_web");
		// Set Number more than 10
		HashMap<String , HashMap<String , Double>> PMI = new HashMap<String , HashMap<String , Double>>();
		HashMap<String , Term> Terms = new HashMap<String , Term>();
		int a = 0;
		while(rs.next()){
			String thisTerm = rs.getString("term");
			Set<String> mySet = new HashSet<String>(Arrays.asList(rs.getString("term2news").split(";")));
			
			Double thisNumber = rs.getDouble("newsNum");
			Term _t = new Term( thisNumber/newsNumber , mySet);
			HashMap<String , Double> thisPMI = new HashMap<String , Double>();
			Terms.put(thisTerm, _t);
			for(String s : PMI.keySet()){
				Double _p=0.0;
				if( Terms.get(thisTerm).P > Terms.get(s).P){
					Set<String> _o = Terms.get(thisTerm).S;
					Set<String> _d = Terms.get(s).S;
					int org = _o.size();
					_o.removeAll(_d);
					Double diff = (double)org-_o.size();
					_p = diff/newsNumber;
					_p = _p/((Terms.get(s).P*Terms.get(thisTerm).P));
				}else{
					Set<String> _o = Terms.get(s).S;
					Set<String> _d = Terms.get(thisTerm).S;
					int org = _o.size();
					_o.removeAll(_d);
					Double diff = (double)org-_o.size();
					_p = diff/newsNumber;
					_p = _p/((Terms.get(s).P*Terms.get(thisTerm).P));
				}
				_p = Math.log(_p);
				thisPMI.put(s, _p);
			}
			PMI.put(thisTerm, thisPMI);
			if(a++%1000 == 0){
				System.out.println("PROCESS:: "+ a +" / "+ newsNumber);
			}
			
		}
	    JSONObject json = new JSONObject(PMI);
	    writer.println(json.toString());
		writer.close();
	}

	
}
