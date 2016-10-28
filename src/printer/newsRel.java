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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import printer.newsRelevanceCount.Term;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class newsRel {

	public static void countNews(Connection conn) throws SQLException{
        Statement stmt = (Statement) conn.createStatement();
    	ResultSet rs =  stmt.executeQuery("select term,scoret from terms_web where num>5 and (not scoret is null) and not term in ('3d','3dprinting','printing','printer','3dprinter')");
		HashMap<String , Double> termMap = new HashMap<String , Double>();
		
		while(rs.next()){
			try{
				if(!rs.getString("scoret").isEmpty() && !rs.getString("scoret").equals("")){
	//				Double.parseDouble(rs.getString("scoret"));
					termMap.put(rs.getString("term"), Double.parseDouble(rs.getString("scoret")));
				}
			}catch(NumberFormatException e){
				System.out.println(rs.getString("term") + " cant do " +rs.getString("scoret"));
				e.printStackTrace();
			}
		}
		HashMap<String , String>  dateMap = new HashMap<String, String>();
		rs = stmt.executeQuery("select date, scoret from dateweb");
		while(rs.next()){
			dateMap.put(rs.getString("date") , rs.getString("scoret"));
		}
		
		
		Gson g = new Gson();

    	String newsE2E = "update newstermsweb set e2e2=?,e2d2=?,done=4 where tid=?";
    	PreparedStatement newsE2EUpdate = (PreparedStatement) conn.prepareStatement(newsE2E);
		for(int a = 0;a < 500 ; a++){
	        rs = stmt.executeQuery("select tid,njson,date from newstermsweb where lang='en' and done=3 limit 100");
	        while(rs.next()){
	        	int avg=0;
	        	double val=0.0;
	        	Map<String, Integer> myMap = g.fromJson(rs.getString("njson"), new TypeToken<Map<String, Integer>>(){}.getType());
	        	for(String t : myMap.keySet()){
	        		if(termMap.containsKey(t)){
	        			avg++;
	        			val+=termMap.get(t);
	        		}
	        	}
	        	val=val/avg;
	        	newsE2EUpdate.setString(1, String.valueOf(val));
	        	newsE2EUpdate.setString(2, dateMap.get(rs.getString("date")));
	        	newsE2EUpdate.setInt(3, rs.getInt("tid"));
	        	newsE2EUpdate.executeUpdate();
	        	System.out.println(rs.getInt("tid"));
	        }
	        System.out.println(" process : "+ a);
		}
	}
	
	public void newsRelOld(Connection conn , String newTable , int _s) throws IOException, JSONException, SQLException, InterruptedException, ExecutionException{
		
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("News Relevance "+ logFile.format(proDate) +".txt", "UTF-8");

		Statement stmt = (Statement) conn.createStatement();
		
    	int newsNumber = 0;
		ResultSet rs = stmt.executeQuery("SELECT count(*) as C from "+newTable);
		while(rs.next()){
			newsNumber = rs.getInt("C");
		}
    	System.out.println("/************************************/");
    	System.out.println("newsNumber : " + newsNumber);
    	System.out.println("/************************************/");
    	
    	
    	String reBack = "update newstermsweb set e2e=?,e2d=?,done=? where tid=?";
    	PreparedStatement newsReBack = (PreparedStatement) conn.prepareStatement(reBack);
    	
    	
    	
    	HashMap<String , List<String>> dateN = new HashMap<String , List<String>>();
    	
    	rs = stmt.executeQuery("select * from dateweb");
    	
    	while(rs.next()){
    		List<String> _a = new ArrayList<String>();
    		_a.add(rs.getString("tjson"));
    		_a.add(rs.getString("number"));
    		dateN.put(rs.getString("date"), _a);
    	}

    	for(int a=0;a<=900;a++){

    		ExecutorService executorText = Executors.newFixedThreadPool(10);
    		if(_s==0){
            	rs = stmt.executeQuery("select njson,tid,date from newstermsweb where done=2 and lang='en' limit 200");
    		}else{
            	rs = stmt.executeQuery("select njson,tid,date from newstermsweb where done=2 and lang='en' order by tid DESC limit 200");
    		}
            List<Future<String>> listFuture = new ArrayList<Future<String>>();
			Gson gson = new Gson();
    		while(rs.next()){
    			Map<String, String> myMap = gson.fromJson(rs.getString("njson"), new TypeToken<Map<String, Integer>>(){}.getType());
    	        Callable<String> newsCount = new newsRelevanceCount(conn , myMap.keySet() , newsNumber+0.0 , dateN.get(rs.getString("date")), rs.getInt("tid"));
    	        System.out.println("PROCESS :: " + rs.getInt("tid"));
    			Future<String> futureText = executorText.submit(newsCount);
    			listFuture.add(futureText);
    			myMap=null;
    		}

    		for(Future<String> _f : listFuture){
    			try{
    				String result = _f.get();
    				if(!result.contains("Error")){
    					String re[]=result.split("&");
    					System.out.println(re[2]+" & E2E: "+re[0]+" & E2D: "+re[1]);
    					writer.println(re[2]+" & E2E: "+re[0]+" & E2D: "+re[1]);
    					newsReBack.setString(1, re[0]);
    					newsReBack.setString(2, re[1]);
    					newsReBack.setInt(3, 3);
    					newsReBack.setInt(4, Integer.parseInt(re[2]));
        				newsReBack.execute();
    				}else{
    					String re[]=result.split("&");
    					newsReBack.setString(1, null);
    					newsReBack.setString(2, null);
    					newsReBack.setInt(3, 4);
    					newsReBack.setInt(4, Integer.valueOf(re[1]));
        				newsReBack.execute();
    				}
    			}catch(Exception e){
    				e.printStackTrace();
    			}finally{
//					newsReBack.executeBatch();
    				_f=null;
    			}
    		}
    		gson=null;
    		listFuture.clear();
    		listFuture=null;
        	executorText.shutdownNow();
    		System.gc();
    	}
		
		writer.close();
	}
	
}





class newsRelevanceCount implements Callable<String>{
	
	class Term {
		public Double P;
		public Set<String> S;
		public Term(double _p, Set<String> _s) {
			P=_p;
			S=_s;
		}
	}
	
	Connection conn;
	Set<String> entities;
	Double newsNumber;
	List<String> dateItem;
	int tid;
    public String call() {
		Double e2e=0.0, e2d=0.0;

		HashSet<String> termSet = new HashSet<String>();
		HashMap<String , Term> Terms = new HashMap<String , Term>();
		try{
			String rooList="\""+entities.toString().substring(1, entities.toString().length()-1).replace(", ", "\", \"")+"\"";
			Statement stmt = (Statement) conn.createStatement();
//			DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//			Date _read = new Date();  
//			System.out.println("read : " + sdf.format(_read));
			ResultSet rs = stmt.executeQuery("SELECT term,term2news,Num from terms_web where term in ("+rooList+")");
//			Date _sql = new Date();
//			System.out.println("sql : " + sdf.format(_sql));
			Gson gson = new Gson();
			Set<String> dateNews = gson.fromJson(dateItem.get(0),new TypeToken<Set<String>>(){}.getType());
			Double dateNumber = Double.parseDouble(dateItem.get(1));
			double dateP = dateNumber/newsNumber;
			
			int RTn=0;
			while(rs.next()){
				RTn++;
				String thisTerm = rs.getString("term");
				Set<String> mySet = gson.fromJson(rs.getString("term2news"),new TypeToken<Set<String>>(){}.getType());
				Double thisNumber = rs.getDouble("Num");
				Term _t = new Term( thisNumber/newsNumber , mySet);getClass();
				Terms.put(thisTerm, _t);
				Double _pd=0.0;
				for(String s : termSet){
					Double _p=0.0;
					double _pt1t2=0.0;
					if( Terms.get(thisTerm).P > Terms.get(s).P){
						Set<String> _o = new HashSet<String>(Terms.get(thisTerm).S);
						Set<String> _d = new HashSet<String>(Terms.get(s).S);
						int org = _o.size();
						_o.removeAll(_d);
						Double diff = (double)org-_o.size();
						_pt1t2 = diff/newsNumber;
					}else{
						Set<String> _o = new HashSet<String>(Terms.get(s).S);
						Set<String> _d = new HashSet<String>(Terms.get(thisTerm).S);
						int org = _o.size();
						_o.removeAll(_d);
						Double diff = (double)org-_o.size();
						_pt1t2 = diff/newsNumber;
					}
					_p = _pt1t2/((Terms.get(s).P * Terms.get(thisTerm).P));
					_p = Math.log(_p);
					_p = -(_p/Math.log(_pt1t2));
					e2e += _p;
				}
				double _pdt=0.0;
				if( dateP > Terms.get(thisTerm).P){
					Set<String> _o = new HashSet<String>(dateNews);
					Set<String> _d = new HashSet<String>(Terms.get(thisTerm).S);
					int org = _o.size();
					_o.removeAll(_d);
					if(org==_o.size()){
						System.out.println(thisTerm);
					}
					Double diff = (double)org-_o.size();
					_pdt = diff/newsNumber;
				}else{
					Set<String> _o = new HashSet<String>(Terms.get(thisTerm).S);
					Set<String> _d = new HashSet<String>(dateNews);
					int org = _o.size();
					_o.removeAll(_d);
					if(org==_o.size()){
						System.out.println(thisTerm);
					}
					Double diff = org-_o.size()+0.0;
					_pdt = diff/newsNumber;
				}
				_pd = _pdt/((dateP * Terms.get(thisTerm).P));
				_pd = Math.log(_pd);
				_pd = -(_pd/Math.log(_pdt));
				e2d+=_pd;
				termSet.add(thisTerm);
				RTn++;
			}

//			Date _end = new Date();
//			System.out.println("end : " + sdf.format(_sql));
			e2e=e2e/(RTn-1.0);
			e2d=e2d/(RTn-1.0);
			termSet = null;
			Terms = null;
			System.gc();
			return e2e+"&"+e2d+"&"+tid;
		}catch(Exception E){
//			E.printStackTrace();
			termSet = null;
			Terms = null;
			System.gc();
			return "Error&"+tid+"&"+E.getMessage();
		}
    }
	

	public newsRelevanceCount(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public newsRelevanceCount(Connection c, Set<String> e, Double n , List<String> d, int t){
		conn = c;
		entities = e;
		newsNumber = n;
		dateItem = d;
		tid = t;
	}
}
