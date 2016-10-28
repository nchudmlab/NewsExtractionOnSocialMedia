package printer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.TwitterException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class spliter {

	public static MaxentTagger tagger = new MaxentTagger("models/english-bidirectional-distsim.tagger");
	

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
	
	public static HashMap<String, Set<Integer>> SocialHashMapCreate(Connection conn , PrintWriter writer , HashMap<String, Set<Integer>> soicalMap) throws SQLException{
		System.out.println("System Social Start!");
		
		Statement stmt = (Statement) conn.createStatement();
		Gson gsonReceiver = new Gson();
		ResultSet rs = stmt.executeQuery("SELECT N.text,S.tags,S.linkTitle,N.id FROM twitternn as N left join twittersplit as S on N.id=S.id");
		int counter = 0;
        while (rs.next()) {
        	int socailID = rs.getInt("id");
        	Set<String> obj = gsonReceiver.fromJson(rs.getString("tags"), Set.class);
        	for ( String key : obj){
        		key = (key.substring(0, 1).equals("#") || key.substring(0, 1).equals("@")) ? key = key.substring(1) : key ;
        		key = toDownCase(key , writer);
    	    	if(soicalMap.containsKey(key)){
    	    		Set<Integer> a = (Set<Integer>) soicalMap.get(key);
		    		a.add(socailID);
		    		soicalMap.replace(key, a);
		    	}else{
		    		Set<Integer> socialSet = new TreeSet<Integer>();
		    		socialSet.add(socailID);
		    		soicalMap.put(key, socialSet);
		    	}
        	}
        	JSONObject TitleJson = ContentSplit(rs.getString("linkTitle"));
        	Iterator<?> keys = TitleJson.keys();
        	while( keys.hasNext() ) {
        		String key = toDownCase((String)keys.next() , writer);
        		try{
	        		key = (key.substring(0, 1).equals("#") || key.substring(0, 1).equals("@")) ? key = key.substring(1) : key ;
	    	    	if(soicalMap.containsKey(key)){
			    		Set<Integer> a = (Set<Integer>) soicalMap.get(key);
			    		a.add(socailID);
			    		soicalMap.replace(key, a);
			    	}else{
			    		Set<Integer> socialSet = new TreeSet<Integer>();
			    		socialSet.add(socailID);
			    		soicalMap.put(key, socialSet);
			    	}
        		}catch(Exception e){
        			System.out.println("Error Key : "+ key );
        			writer.println("Error Key : "+ key );
        		}
        	}
        	JSONObject TextJson = ContentSplit(rs.getString("text"));
        	keys = TitleJson.keys();
        	while( keys.hasNext() ) {
        		String key = toDownCase((String)keys.next() , writer);
        		key = (key.substring(0, 1).equals("#") || key.substring(0, 1).equals("@")) ? key.substring(1) : key ;
    	    	if(soicalMap.containsKey(key)){
    	    		Set<Integer> a = (Set<Integer>) soicalMap.get(key);
		    		a.add(socailID);
		    		soicalMap.replace(key, a);
		    	}else{
		    		Set<Integer> socialSet = new TreeSet<Integer>();
		    		socialSet.add(socailID);
		    		soicalMap.put(key, socialSet);
		    	}
        	}
        	
        	if(counter%1000 == 0){System.out.println("Social Counter : " + counter);}
        	counter++;
        	System.out.println(counter);
        }
        return soicalMap;
	}

	
	public static void InsertNN(Connection conn) throws SQLException, JSONException, FileNotFoundException, UnsupportedEncodingException{
        String content = "";
        Statement stmt = (Statement) conn.createStatement();
    	//String selectTerm = "select id,term,term2news,term2social from terms2";

        HashMap soicalTermsMap = new HashMap<String, Set<Integer>>();
        HashMap newsTermsMap = new HashMap<String, Set<Integer>>();
    	ResultSet rsTerm = stmt.executeQuery("select id,term,term2news,term2social from terms2");

    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mm");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Downcase "+ logFile.format(proDate) +".txt", "UTF-8");
    	
    	Gson gsonReceiver = new Gson();
    	while(rsTerm.next()){
    		soicalTermsMap.put(rsTerm.getString("term"), gsonReceiver.fromJson(rsTerm.getString("term2social"),new TypeToken<HashMap<String,Integer>>(){}.getType()));
    		newsTermsMap.put(rsTerm.getString("term"), gsonReceiver.fromJson(rsTerm.getString("term2news"),new TypeToken<HashMap<String,Integer>>(){}.getType()));
    	}
    	

//    	newsTermsMap = NewsHashMapCreate(conn , writer , newsTermsMap);
    	soicalTermsMap = SocialHashMapCreate(conn , writer , soicalTermsMap);
    	insertTermsQueue(conn , MapMerge(newsTermsMap, soicalTermsMap));	
	}
	
	public static void insertTermsQueue(Connection conn , HashMap<String, HashMap<String ,Set<Integer>>> _M) throws SQLException{
    	String insertTableSQL = "INSERT INTO terms2 ( term , term2news , term2social , newsNum , socialNum ) VALUES ( ? , ? , ? , ? , ? )";
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
    
    
    public static void term2News(Connection conn,String workFor) throws  SQLException, JSONException{
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT newID,nnjson FROM newsnn limit 5");
        
		String updateTableSQL = "update terms2 set term2news=? where id=?";
		PreparedStatement updatePS = (PreparedStatement) conn.prepareStatement(updateTableSQL);
    	String insertTableSQL = "INSERT INTO terms2 (term2news, term) VALUES (?,?)";
    	PreparedStatement insertPS = (PreparedStatement) conn.prepareStatement(insertTableSQL);

    	
    	Map<String, List<Integer>> termListMap = new HashMap<String, List<Integer>>();
    	
        while(rs.next()){
        	System.out.println(rs.getInt("newID"));
        	Map<String, Integer> newsMap= new Gson().fromJson(rs.getString("nnjson") , new TypeToken<HashMap<String, Integer>>() {}.getType());
        	for (Map.Entry<String, Integer> entry : newsMap.entrySet()) {
        		System.out.println(entry.getKey());
        		if (termListMap.containsKey(entry.getKey())) {
        			termListMap.get(entry.getKey()).add(rs.getInt("newID"));
        		}else{
        			List<Integer> tempA= new ArrayList<Integer>();
        			tempA.add(rs.getInt("newID"));
        			termListMap.put(entry.getKey(), tempA);
        		}
        	}
        }
        
        System.out.println(termListMap.toString());
    	
    	for (Map.Entry<String, List<Integer>> entry : termListMap.entrySet()) {
    		System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue().toString());

        	if(workFor.equals("news")){
            		insertPS.setString(1, entry.getValue().toString());
            		insertPS.setString(2, entry.getKey());
            		insertPS.execute();
        	}else if(workFor.equals("social")){

        	}
    	}
        insertPS.executeBatch();
        updatePS.executeBatch();
        conn.commit();
    }

    
    public static void news2Terms(Connection conn) throws SQLException{

        String content = "";
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT content,id FROM scrapynews limit 50");
        
        while (rs.next()) {
        	String jsonString = ContentSplit2(rs.getString("content"));
        	int newId = rs.getInt("id");

            Statement stmtSub = (Statement) conn.createStatement();
        	ResultSet rsSub = stmtSub.executeQuery("SELECT newID FROM newsnn where newID='"+newId+"'");
        	if(rsSub.next()){
            	String insertTableSQL = "update newsnn set newID=?, nnjson=? where newID=?";
            	PreparedStatement preparedStatement = (PreparedStatement) conn.prepareStatement(insertTableSQL);
            	preparedStatement.setInt(1, newId);
            	preparedStatement.setInt(3, newId);
            	preparedStatement.setString(2, jsonString);
            	preparedStatement.executeUpdate();
        		
        	}else{
            	String insertTableSQL = "INSERT INTO newsnn (newID, nnjson) VALUES (?,?)";
            	PreparedStatement preparedStatement = (PreparedStatement) conn.prepareStatement(insertTableSQL);
            	preparedStatement.setInt(1, newId);
            	preparedStatement.setString(2, jsonString);
            	preparedStatement.executeUpdate();
        	}
        }
    }
    
    public static String ContentSplit2( String content){
        // Initialize the tagger
        MaxentTagger tagger = new MaxentTagger("models/english-bidirectional-distsim.tagger");
 
        // The tagged string
        String tagged = tagger.tagString(content);
 
        // Output the result
        System.out.println(tagged);
        String[] x = tagged.split(" ");
        ArrayList<String> list = new ArrayList<String>();  

        for(int i=0;i<x.length;i++)
        {
            if (x[i].substring(x[i].lastIndexOf("_")+1).startsWith("N"))
            {
                list.add(x[i].split("_")[0]);
            }
        }
        
        Map<String, Integer> newsMap = new HashMap<String, Integer>();
        
        
        for(int i=0;i<list.size();i++)
        {
        	if(newsMap.get(list.get(i)) != null){
        		newsMap.put(list.get(i), newsMap.get(list.get(i))+1);
        	}else{
        		newsMap.put(list.get(i), 1);
        	}
        }

		return new JSONObject(newsMap).toString();
    }
    
	
}
