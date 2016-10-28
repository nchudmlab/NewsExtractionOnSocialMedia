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
import java.text.DecimalFormat;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class termRel {
	
	public void termRel(Connection conn , String newTable , int _s) throws IOException, JSONException, SQLException, InterruptedException, ExecutionException{
		
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Terms Relevance "+ logFile.format(proDate) +".txt", "UTF-8");

		Statement stmt = (Statement) conn.createStatement();
    	String termUpdateString = "update terms_web set scoret=?,done=? where term=?";
    	PreparedStatement termUpdate = (PreparedStatement) conn.prepareStatement(termUpdateString);

		Gson gson = new Gson();
    	ResultSet rs =  stmt.executeQuery("select term2news,term from terms_web where num>5");
		HashMap<String , JSONArray> termMap = new HashMap<String , JSONArray>();
		while(rs.next()){
			termMap.put(rs.getString("term"), new JSONArray(rs.getString("term2news")) );
		}
    	rs =  stmt.executeQuery("select tid,njson from newstermsweb where lang='en'");
		HashMap<Integer , Set<String>> newsMap = new HashMap<Integer , Set<String>>();
		while(rs.next()){
			Map<String, Integer> myMap = gson.fromJson(rs.getString("njson"), new TypeToken<Map<String, Integer>>(){}.getType());
			newsMap.put(rs.getInt("tid"), myMap.keySet());
		}
		

		
		
		int stand = 0;
		int totalP = termMap.keySet().size();
		int totalN = newsMap.keySet().size();
		for(int a=0;a<=720;a++){
			if(_s==1){
				rs =  stmt.executeQuery("select term from terms_web where (not done=1) and num>5 and scoret is null order by termid DESC limit 200");
			}else if(_s==0){
				rs =  stmt.executeQuery("select term from terms_web where (not done=1) and num>5 and scoret is null order by termid ASC limit 200");
			}else if(_s==2){
				rs =  stmt.executeQuery("select term from terms_web where scoret='more'");
			}
			
			ExecutorService executorText = Executors.newFixedThreadPool(5);
			List<Future<String>> listFuture = new ArrayList<Future<String>>();
			while(rs.next()){
	    		Callable<String> termCount = new termRelvantCount(rs.getString("term") , newsMap, termMap, totalN);
	    		Future<String> futureText = executorText.submit(termCount);
	    		listFuture.add(futureText);
			}
			
			for(Future<String> _f : listFuture){
				String _re = _f.get();
				System.out.println(_re);
				if(_re.contains("NaN")){
					System.out.println(_re);
				}else if(_re.contains("more")){
					String re[] = _re.split("&");
					termUpdate.setString(1, re[1]);
					termUpdate.setInt(2, 1);
					termUpdate.setString(3, re[0]);
					termUpdate.execute();
				}else if(_re.contains("Null")){
					String re[] = _re.split("&");
					termUpdate.setString(1, re[1]);
					termUpdate.setInt(2, 1);
					termUpdate.setString(3, re[0]);
					termUpdate.execute();
				}else{
					try{
						String re[] = _re.split("&");
						termUpdate.setString(1, re[1]);
						termUpdate.setInt(2, 1);
						termUpdate.setString(3, re[0]);
						termUpdate.execute();
					}catch(Exception E){
						E.printStackTrace();
					}
				}
			}
			System.out.println(" procress a = " + a);
			System.gc();
		}
		writer.close();
	}
	
}





class termRelvantCount implements Callable<String>{
	
	HashMap<String , JSONArray> termMap;
	HashMap<Integer , Set<String>> newsMap;
	String term;
	double newsNumber;
	
    public String call() {
		try{
			JSONArray termsInNews = termMap.get(term);
			int NN=0;
			double t2NN=0.0;
			for(int i=0 ; i< termsInNews.length() ; i++){
				if(newsMap.containsKey(i)){
					int count=0;
					double t2n=0.0;
					for(String t2 : newsMap.get(i)){
						if(!t2.equals(term) && termMap.containsKey(t2)){
							double npmi = NPMI(t2);
							t2n+=npmi;
							count++;
						}
					}
					t2n=t2n/count;
					if(!Double.isNaN(t2n)){
						t2NN+=t2n;
					}
					NN++;
				}
			}
			t2NN=t2NN/NN;
			DecimalFormat df=new DecimalFormat("#.##########");
			return term+"&"+df.format(t2NN);
		}catch(Exception E){
			E.printStackTrace();
			return term+"&Null";
		}
    }

    public double NPMI(String B) throws JSONException{
    	double npmi = 0.0;
    	double PA = termMap.get(term).length()/newsNumber;
    	double PB = termMap.get(B).length()/newsNumber;
		Set<String> _as = new HashSet<String>();
		Set<String> _bs = new HashSet<String>();
		for(int i = 0; i < termMap.get(term).length(); i++){
			_as.add(String.valueOf(termMap.get(term).get(i)));
		}
		for(int i = 0; i < termMap.get(B).length(); i++){
			_bs.add(String.valueOf(termMap.get(B).get(i)));
		}
		double PAB=0.0;
		
		if( PA > PB){
			int org = termMap.get(term).length();
			_as.removeAll(_bs);
			Double diff = (double)org-_as.size();
			PAB = diff/newsNumber;
		}else{
			int org = termMap.get(B).length();
			_bs.removeAll(_as);
			Double diff = (double)org-_bs.size();
			PAB = diff/newsNumber;
		}
		npmi = PAB/((PA * PB));
		npmi = Math.log(npmi);
		npmi = -(npmi/Math.log(PAB));
		if(Double.isNaN(npmi) || npmi<0.0){
			if(PB>1){
				System.out.println(B);
				System.out.println(newsNumber);
				System.out.println(termMap.get(B).length());
				System.exit(0);
			}
			return 0.0;
		}else{
			return npmi;
		}
    }

	public termRelvantCount(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public termRelvantCount(String t, HashMap<Integer , Set<String>> n, HashMap<String , JSONArray> h, int num){
		term = t;
		termMap = h;
		newsMap = n;
		newsNumber = num+0.0;
	}
}
