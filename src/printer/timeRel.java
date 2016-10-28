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

public class timeRel {
	
	public void timeRel(Connection conn) throws IOException, JSONException, SQLException, InterruptedException, ExecutionException{
		
    	DateFormat logFile = new SimpleDateFormat("MMdd-HH-mms");
    	Date proDate = new Date();
    	PrintWriter writer = new PrintWriter("Timestamp Relevance "+ logFile.format(proDate) +".txt", "UTF-8");

		Statement stmt = (Statement) conn.createStatement();
    	String timeUpdateString = "update dateweb set scoret=? where date=?";
    	PreparedStatement timeUpdate = (PreparedStatement) conn.prepareStatement(timeUpdateString);

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

		int totalN = newsMap.keySet().size();
		System.out.println(totalN);
		rs =  stmt.executeQuery("select date,tjson from dateweb");
		HashMap<String , List<String>> dateMap = new HashMap<String , List<String>>();
		List<Future<String>> listFuture = new ArrayList<Future<String>>();
		ExecutorService executorText = Executors.newFixedThreadPool(5);
		while(rs.next()){
			List<String> news_List = gson.fromJson(rs.getString("tjson"), new TypeToken<List<String>>(){}.getType());
    		Callable<String> termCount = new timeRelvantCount(rs.getString("date") , newsMap, termMap, totalN , news_List);
    		Future<String> futureText = executorText.submit(termCount);
    		listFuture.add(futureText);
			dateMap.put(rs.getString("date"), news_List );
		}

		
		for(Future<String> _f : listFuture){
			String _re = _f.get();
			System.out.println(_re);
			if(_re.contains("NaN")){
				System.out.println(_re);
			}else if(_re.contains("more")){
				String re[] = _re.split("&");
				timeUpdate.setString(1, re[1]);
				timeUpdate.setString(2, re[0]);
				timeUpdate.execute();
			}else if(_re.contains("Null")){
				String re[] = _re.split("&");
			}else{
				try{
					String re[] = _re.split("&");
					timeUpdate.setString(1, re[1]);
					timeUpdate.setString(2, re[0]);
					timeUpdate.execute();
					System.out.println(re[0] + " DONE !! ");
				}catch(Exception E){
					E.printStackTrace();
				}
			}
		}
		writer.close();
	}
	
}





class timeRelvantCount implements Callable<String>{
	
	HashMap<String , JSONArray> termMap;
	HashMap<Integer , Set<String>> newsMap;
	String _date;
	double newsNumber;
	List<String> newsList;
	
    public timeRelvantCount(String d,
			HashMap<Integer, Set<String>> n,
			HashMap<String, JSONArray> h, int totalN,
			List<String> l) {
		// TODO Auto-generated constructor stub

    	_date = d;
    	termMap = h;
		newsMap = n;
		newsNumber = totalN+0.0;
		newsList = l;
	}

	public String call() {
		try{
			int NN=0;
			double t2NN=0.0;
			for(int i=0 ; i< newsList.size() ; i++){
				if(newsMap.containsKey(i)){
					int count=0;
					double t2n=0.0;
					for(String t2 : newsMap.get(i)){
						if(termMap.containsKey(t2)){
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
			return _date+"&"+df.format(t2NN);
		}catch(Exception E){
			E.printStackTrace();
			return _date+"&Null";
		}
    }

	
	
    public double NPMI(String B) throws JSONException{
    	double npmi = 0.0;
    	double PA = newsList.size()/newsNumber;
    	double PB = termMap.get(B).length()/newsNumber;
		Set<String> _as = new HashSet<String>();
		Set<String> _bs = new HashSet<String>();
		for(int i = 0; i < newsList.size(); i++){
			_as.add(newsList.get(i));
		}
		for(int i = 0; i < termMap.get(B).length(); i++){
			_bs.add(String.valueOf(termMap.get(B).get(i)));
		}
		double PAB=0.0;
		
		if( PA > PB){
			int org = newsList.size();
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

	public timeRelvantCount(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
}
