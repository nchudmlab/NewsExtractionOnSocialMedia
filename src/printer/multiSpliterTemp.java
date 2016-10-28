package printer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class multiSpliterTemp{
	public static int socialStart = 1;
	public static MaxentTagger tagger = new MaxentTagger("models/english-bidirectional-distsim.tagger");
	
	public static void InsertNN(Connection conn, String s) throws SQLException, JSONException, FileNotFoundException, UnsupportedEncodingException, InterruptedException, ExecutionException{
		socialStart = Integer.parseInt(s)*60000;
        String content = "";
        Statement stmt = (Statement) conn.createStatement();
        
        
        HashMap soicalTermsMap = new HashMap<String, Set<Integer>>();
        HashMap newsTermsMap = new HashMap<String, Set<Integer>>();

    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Downcase "+ logFile.format(proDate) +".txt", "UTF-8");
    	
    	Gson gsonReceiver = new Gson();
    	
    	NewsWeb(conn , writer);
    	
    	
    	
    	
    	
//    	soicalTermsMap = SocialHashMapCreate(conn , writer , soicalTermsMap);
//    	newsTermsMap = NewsHashMapCreate(conn , writer , newsTermsMap);
//    	insertTermsQueue(conn , MapMerge(newsTermsMap, soicalTermsMap));
	}
	
	
	public static void NewsWeb(Connection conn , PrintWriter writer) throws SQLException, JSONException{

        Statement stmt = (Statement) conn.createStatement();
        ExecutorService executorText = Executors.newFixedThreadPool(50);
        for(int a=1446;a<2000;a++){
        	int s = 100*a;
	        ResultSet rs = stmt.executeQuery("SELECT body,tid,`title` FROM twitterreweb limit "+s+",100");
	    	String insertTableSQL = "INSERT ignore INTO newstermsweb (tid, njson, done, number) VALUES (?,?,?,?)";
	    	PreparedStatement newsnnPSInsert = (PreparedStatement) conn.prepareStatement(insertTableSQL);
	        int insertC = 0;
	        HashMap<Integer, Future<JSONObject>> mapFuture= new HashMap<Integer, Future<JSONObject>>();
	    	while (rs.next()) {
	        	System.out.println("split : " + rs.getString("title") );
	    		Callable<JSONObject> callableText = new twitterWebSpliter( tagger , rs.getString("body"));
	    		Future<JSONObject> futureText = executorText.submit(callableText);
	    		mapFuture.put(rs.getInt("tid"),futureText);
	        }
	    	
	    	for(int keyID : mapFuture.keySet()){
				try {
		        	JSONObject NNJson = mapFuture.get(keyID).get();
		        	newsnnPSInsert.setInt(1, keyID);
		        	newsnnPSInsert.setString(2, NNJson.toString());
		        	newsnnPSInsert.setInt(3, 0);
		        	newsnnPSInsert.setInt(4, NNJson.length());
		        	newsnnPSInsert.addBatch();
				} catch (ExecutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					writer.println("Web Splite ExecutionException ID : " + keyID);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					writer.println("Web Splite InterruptedException ID : " + keyID);
				}
	    	}
	    	newsnnPSInsert.executeBatch();
	    	System.gc();
	    	System.out.println("NewsWeb A = " +a);
        }
	}

	
	
	public static String toDownCase(String term ,  PrintWriter writer ){
		String down = "";
		try{
			down = term.toLowerCase();
		}catch(Exception e){
			writer.println("Term ID : " + term);
		}
		return down;
	}
	public static JSONObject ContentSplit( String content){
		try{
	        String tagged = tagger.tagString(content);
	        String[] x = tagged.split(" ");
	        ArrayList<String> list = new ArrayList<String>();  
	
	        for(int i=0;i<x.length;i++)
	        {
	            if (x[i].substring(x[i].lastIndexOf("_")+1).startsWith("N")){
	                list.add(x[i].split("_")[0]);
	            }
	        }
	        Map<String, Integer> newsMap = new HashMap<String, Integer>();
	        
	        for(int i=0;i<list.size();i++){
	        	if(newsMap.get(list.get(i)) != null){
	        		newsMap.put(list.get(i), newsMap.get(list.get(i))+1);
	        	}else{
	        		newsMap.put(list.get(i), 1);
	        	}
	        }
			return new JSONObject(newsMap);
		}catch(Exception E){
			System.out.println("Error [" + content + "]");
			return new JSONObject();
		}
    }

	
	
	
	
	
	
	
	
	

	public static HashMap<String, Set<Integer>> NewsHashMapCreate(Connection conn , PrintWriter writer , HashMap<String, Set<Integer>> newsMap) throws SQLException, JSONException{

        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT content,id,`title` FROM scrapynews");

    	String insertTableSQL = "INSERT IGNORE INTO newsterms (newID, nnjson) VALUES (?,?)";
    	PreparedStatement newsnnPSInsert = (PreparedStatement) conn.prepareStatement(insertTableSQL);
        int insertC = 0;
        
    	while (rs.next()) {
        	System.out.println("split : " + rs.getString("title") );
        	JSONObject titleJson = ContentSplit(rs.getString("title"));
        	JSONObject NNJson = ContentSplit(rs.getString("content"));
        	try{
	        	for(String key : JSONObject.getNames(titleJson)){
	        		NNJson.put(key, titleJson.get(key));
	        	}
	        	Iterator<?> keys = NNJson.keys();	
	        	int newId = rs.getInt("id");
	        	while( keys.hasNext() ) {
	        	    String key = toDownCase((String)keys.next() , writer);
	    	    	if(newsMap.containsKey(key)){
	    	    		Set<Integer> a = (Set<Integer>) newsMap.get(key);
	    	    		a.add(newId);
	    	    		newsMap.replace(key, a);
	    	    	}else{
	    	    		Set<Integer> newList = new TreeSet<Integer>();
	    	    		newList.add(newId);
	        	    	newsMap.put(key, newList);
	    	    	}
	        	}
	        	newsnnPSInsert.setInt(1, newId);
	        	newsnnPSInsert.setString(2, NNJson.toString());
	        	newsnnPSInsert.addBatch();
	    		if(insertC % 1000 == 0){
	    			newsnnPSInsert.executeBatch();
	    		}
	    		insertC++;
        	}catch(Exception E){
        		writer.println("# split error , news ID = "+rs.getInt("id"));
        	}
        }
    	newsnnPSInsert.executeBatch();
		return newsMap;
	}
	
	public static HashMap<String, Set<Integer>> SocialHashMapCreate(Connection conn , PrintWriter writer , HashMap<String, Set<Integer>> soicalMap) throws SQLException, InterruptedException, ExecutionException{
		System.out.println("System Social Start! start Number " + socialStart);
		
		Statement stmt = (Statement) conn.createStatement();
		Gson gsonReceiver = new Gson();
//		ResultSet rs = stmt.executeQuery("SELECT N.text,S.tags,S.linkTitle,N.id FROM twitternn as N left join twittersplit as S on N.id=S.id where N.date between '2015-07-01' and '2016-02-01' limit 1,100");

		ResultSet rs = stmt.executeQuery("SELECT N.text,S.tags,S.linkTitle,N.id FROM twitternn as N left join twittersplit as S on N.id=S.id where N.date between '2015-01-01' and '2015-07-01' limit " + socialStart + ",60000");
		int counter = 0;

    	String insertTableSQL = "INSERT IGNORE INTO twitterterms (id, nnjson) VALUES (?,?)";
    	PreparedStatement socialTermsInsert = (PreparedStatement) conn.prepareStatement(insertTableSQL);
        while (rs.next()) {
        	int socailID = rs.getInt("id");
        	Set<String> thisObj = new HashSet<String>();
        	Set<String> obj = gsonReceiver.fromJson(rs.getString("tags"), Set.class);
        	try{
	        	for ( String key : obj){
	        		key = (key.substring(0, 1).equals("#") || key.substring(0, 1).equals("@")) ? key = key.substring(1) : key ;
	        		key = toDownCase(key , writer);
	        		if(key != null){
	            		thisObj.add(key);
	        		}
	        	}
        	} catch (NullPointerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				writer.println("Null Pointer tag set : " + socailID);
			}

    		ExecutorService executorText = Executors.newSingleThreadExecutor();
    		Callable<Set<String>> callableText = new socialSpliter( tagger , rs.getString("text")+". "+rs.getString("linkTitle"));

    		Future<Set<String>> futureText = executorText.submit(callableText);
    		Set<String> TextSet = new HashSet<String>();
        	

			try {
				TextSet = futureText.get();
				thisObj.addAll(TextSet);
				socialTermsInsert.setInt(1 , socailID);
				socialTermsInsert.setString(2,thisObj.toString());
				socialTermsInsert.addBatch();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				writer.println("Splite Error ID : " + socailID);
			}
			
        	if(counter%50 == 0){
        		Date date = new Date();
        		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        		String dateString = sdf.format(date);
        		System.out.println(dateString+" Social Counter : " + counter);
        		socialTermsInsert.executeBatch();
        	}
        	counter++;
        }
        socialTermsInsert.executeBatch();
        return soicalMap;
	}

	
	
	public static void insertTermsQueue(Connection conn , HashMap<String, HashMap<String ,Set<Integer>>> _M) throws SQLException{
    	String insertTableSQL = "INSERT INTO termstemp ( term , term2news , term2social , newsNum , socialNum ) VALUES ( ? , ? , ? , ? , ? )";
    	PreparedStatement insertTerms = (PreparedStatement) conn.prepareStatement(insertTableSQL);
    	int counter = 0;
		for(String A : _M.keySet()){
			String term2news = "[]",term2social="[]";
			int newsNumber = 0, socialNumber = 0;
			if(_M.get(A).containsKey("news")){
				term2news = _M.get(A).get("news").toString();
				newsNumber = _M.get(A).get("news").size();
			}
			if(_M.get(A).containsKey("social")){
				term2social =  _M.get(A).get("social").toString();
				socialNumber = _M.get(A).get("social").size();
			}
			insertTerms.setString(1, A);
			insertTerms.setString(2, term2news);
			insertTerms.setString(3, term2social);
			insertTerms.setInt(4, newsNumber);
			insertTerms.setInt(5, socialNumber);
			insertTerms.addBatch();
			if(counter%1000==0){
				insertTerms.executeBatch();
				System.out.println("Insert Counter :  " + counter);
			}
			counter++;
		}
		insertTerms.executeBatch();
	}
	
	public static HashMap<String, HashMap<String ,Set<Integer>>> MapMerge(HashMap<String, Set<Integer>> n , HashMap<String, Set<Integer>> s){
		HashMap mergeMap = new HashMap<String, HashMap<String ,Set<Integer>>>();
		
		HashMap<String ,Set<Integer>> tempHash = null;
		for (String key : n.keySet()) {
			tempHash = new HashMap<String ,Set<Integer>>();
			tempHash.put("news" , n.get(key));
			if(s.containsKey(key)){
				tempHash.put("social" , s.get(key));
				s.remove(key);
			}
			mergeMap.put(key, tempHash);
		}
		
		for (String key : s.keySet()) {
			tempHash = new HashMap<String ,Set<Integer>>();
			tempHash.put("social" , s.get(key));
			if(n.containsKey(key)){
				System.out.println(key);
			}
			mergeMap.put(key, tempHash);
		}
		return mergeMap;
	}
    
}

class twitterWebSpliter implements Callable<JSONObject>{

	public MaxentTagger tagger;
	public String content;

    public JSONObject call() {
		try{
	        String tagged = tagger.tagString(content.toLowerCase());
	        String[] x = tagged.split(" ");
	        ArrayList<String> list = new ArrayList<String>();  
	
	        for(int i=0;i<x.length;i++)
	        {
	            if (x[i].substring(x[i].lastIndexOf("_")+1).startsWith("N")){
	                list.add(x[i].split("_")[0]);
	            }
	        }
	        Map<String, Integer> newsMap = new HashMap<String, Integer>();
	        
	        for(int i=0;i<list.size();i++){
	        	if(newsMap.get(list.get(i)) != null){
	        		newsMap.put(list.get(i), newsMap.get(list.get(i))+1);
	        	}else{
	        		newsMap.put(list.get(i), 1);
	        	}
	        }
			return new JSONObject(newsMap);
		}catch(Exception E){
			System.out.println("Error [" + content + "]");
			return new JSONObject();
		}
    }
	

	public twitterWebSpliter(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public twitterWebSpliter(MaxentTagger t, String c){
		tagger = t;
		content = c;
	}
}
