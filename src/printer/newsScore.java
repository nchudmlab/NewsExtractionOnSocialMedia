package printer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;




public class newsScore {
	static double PickNum;
	double lamda;
	public newsScore(double l , double P){
		lamda= l;
		PickNum = P;
	}

	public void _Score(Connection conn) throws IOException, ClassNotFoundException, SQLException, ParseException{
		
        Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select `e2e2`,`e2d2`,`tid`,`date` from newstermsweb where (not (e2e2 is null or e2d2 is null or e2e2='NaN')) and lang='en' and number between 60 and 800");
        HashMap<Integer , newsObj> newsMap = new HashMap<Integer , newsObj>();
        HashMap<Integer ,Double> _so = new HashMap<Integer ,Double>();
        Double xD=0.0,mD=9999.0,xE=0.0,mE=9999.0;
        HashMap<String , Integer> D2News = new HashMap<String , Integer>(); 
		while(rs.next()){
			double e2e = Double.parseDouble(rs.getString("e2e2"));
			double e2d = Double.parseDouble(rs.getString("e2d2"));
			xD=e2d>xD?e2d:xD;
			xE=e2e>xE?e2e:xE;

			mD=e2d<mD?e2d:mD;
			mE=e2e<mE?e2e:mE;

			int tid = rs.getInt("tid");

			newsObj _tNO = new newsObj(e2e,e2d,rs.getString("date"),lamda);
			newsMap.put(tid , _tNO);
			D2News.put(rs.getString("date"),0);
		}
		
		Double pD=xD-mD,pE=xE-mE;
		for(int tid : newsMap.keySet()){
			newsMap.get(tid).FormalizedSocre(mE,pE,mD,pD);
			_so.put(tid, newsMap.get(tid).score);
		}
		
		
		HashMap<String , String> reS = FindNews(_so,newsMap,D2News,stmt);
		//ChromeNews(conn,"tid in ("+reS.get("GreedyTID").substring(1)+")");
		
		DateFormat logFile = new SimpleDateFormat("MM-dd-HH");
 	    Date resultDate = new Date();
 	    String dirPath= "C:/Users/bird/workspace/MainPrinter/result/"+logFile.format(resultDate)+"-"+String.valueOf(PickNum)+"-"+String.valueOf(lamda).substring(2);

		File f = new File(dirPath);
		f.mkdir();
		PrintWriter writer = new PrintWriter(dirPath+"/TotalResult.csv", "UTF-8");
		writer.print(reS.get("CSV"));
		writer.flush();
		writer.close();
//		coverageNews(conn,"tid in ("+reS.get("TIDS").substring(1)+")  order by tid", dirPath+"/Coverage.csv");
		creartCSV(conn, newsMap, reS.get("GreedyTID"),  dirPath+"/GreedyResult.csv");
		creartTSV(conn, newsMap, reS.get("GreedyTID"),  dirPath+"/GreedyResult.tsv");
//		coverageNews(conn,"tid in ("+reS.get("GreedyTID").substring(1)+")  order by tid", dirPath+"/GreedCoverage.csv");
		
		conn.close();
	}
	
	

	public static void creartTSV(Connection conn, HashMap<Integer, newsObj> newsMap , String idString, String filePath) throws SQLException, FileNotFoundException, UnsupportedEncodingException, ParseException {
		String dt = "2015-10-21";  // Start date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance(); 
		c.setTime(sdf.parse(dt));
		dt = sdf.format(c.getTime());
		Statement stmt = (Statement) conn.createStatement();
		String sql = "select w.tid,n.date,w.url,w.title from twitterreweb as w left join twitternn as n on w.tid=n.id where";
		
		ResultSet rs = stmt.executeQuery(sql+" w.tid in ("+idString.substring(1)+")");
		HashMap<String , String> GR = new HashMap<String , String>();
		DecimalFormat df = new DecimalFormat("0.00000");
		String CSVFileString="Date\tTitle\tPosition\n";

		while(rs.next()){
			GR.put(rs.getString("date"), rs.getString("title"));
		}
		int p=1;
		for(int i=0; i<=90; i++){
			c.add(Calendar.DATE, 1);
			String d = sdf.format(c.getTime());
			if(GR.keySet().contains(d)){
				CSVFileString+=d+"\t"+GR.get(d)+"\t"+p+"\n";
				p=p==1?2:1;
			}else{
				CSVFileString+=d+"\t \t0\n";
			}
		}
		

		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		writer.print(CSVFileString);
		writer.flush();
		writer.close();
	}


	public static void creartCSV(Connection conn, HashMap<Integer, newsObj> newsMap , String idString, String filePath) throws SQLException, FileNotFoundException, UnsupportedEncodingException {
		
		Statement stmt = (Statement) conn.createStatement();
		String sql = "select w.tid,n.date,w.url,w.title from twitterreweb as w left join twitternn as n on w.tid=n.id where";
		
		ResultSet rs = stmt.executeQuery(sql+" w.tid in ("+idString.substring(1)+")");

		DecimalFormat df = new DecimalFormat("0.00000");
		String CSVFileString="tid, date, Score, E2E, E2D, Title, Url \n";
		while(rs.next()){
			int tid=rs.getInt("tid");
			String thisTitle = rs.getString("title").replaceAll("[^A-Za-z0-9]", "");
			CSVFileString+= rs.getInt("tid")+","
					+rs.getString("date")+","
					+df.format(newsMap.get(tid).score)+","
					+df.format(newsMap.get(tid).e2e)+","
					+df.format(newsMap.get(tid).e2d)+","
					+rs.getString("title")+","
					+rs.getString("url")+"\n";
		}
		

		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		writer.print(CSVFileString);
		writer.flush();
		writer.close();
	}





	private static void ChromeNews(Connection conn, String TidIN) throws SQLException, IOException{
		Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select url from twitterreweb where "+TidIN);
        while(rs.next()){
        	Runtime.getRuntime().exec(new String[] { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", rs.getString("url")});   
        }
	}
	
	private static HashMap<String, String> FindNews(
			HashMap<Integer, Double> _so, HashMap<Integer, newsObj> newsMap,
			HashMap<String, Integer> d2News, Statement stmt) throws SQLException {
		// TODO Auto-generated method stub

		
		String CSVFileString="tid, date, Score, E2E, E2D, Title, Url \n";
		String TIDS="";
		int pre=_so.size();
		int count=0;
		Set<String> checkSet = new HashSet<String>();

		HashMap<Date, Double> daMap = new HashMap<Date , Double>();
		HashMap<Double, newsStur> MapS = new HashMap<Double , newsStur>();
		List<Double> scoreList = new ArrayList<Double>();
		
		while(count!=d2News.keySet().size()){
			Map sortedMap = sortByValue(_so);
			Set<Integer> _m = sortedMap.keySet();
			DecimalFormat df = new DecimalFormat("0.00000");
			String idString = "";
			for(int _this : _m){
				if(d2News.get(newsMap.get(_this).date)==0){
					idString+=","+_this;
					d2News.put(newsMap.get(_this).date, _this);
				}
			}
	
	
			String sqlString = "select w.tid,n.date,w.url,w.title from twitterreweb as w left join twitternn as n on w.tid=n.id where";
			
			ResultSet rs = stmt.executeQuery(sqlString+" w.tid in ("+idString.substring(1)+")");
			while(rs.next()){
				int tid=rs.getInt("tid");
				String thisTitle = rs.getString("title").replaceAll("[^A-Za-z0-9]", "");
				if(!checkSet.contains(rs.getString("url")) && !checkSet.contains(thisTitle) ){
					CSVFileString+= rs.getInt("tid")+","
							+rs.getString("date")+","
							+df.format(newsMap.get(tid).score)+","
							+df.format(newsMap.get(tid).e2e)+","
							+df.format(newsMap.get(tid).e2d)+","
							+rs.getString("title")+","
							+rs.getString("url")+"\n";
					checkSet.add(rs.getString("url"));
					checkSet.add(thisTitle);
					TIDS+=","+tid;
					_so.remove(tid);
					count++;
					Date d = rs.getDate("date");
					double s = newsMap.get(tid).score;
			    	daMap.put(d, s);
			    	MapS.put(s, new newsStur(tid , d, s));
			    	scoreList.add(s);
				}else{
					d2News.put(rs.getString("date"), 0);
					_so.remove(tid);
				}
			}
		}
		HashMap<String , String> reS = new HashMap<String , String>();
		reS.put("GreedyTID", GreedyMainProcess(daMap, MapS, scoreList));
		reS.put("TIDS", TIDS);
		reS.put("CSV", CSVFileString);
		return reS;
	}


	public static String GreedyMainProcess(HashMap<Date, Double> daMap ,HashMap<Double, newsStur> MapS, List<Double> scoreList){
//		CSVData
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		
		/**
		 * Sort Score
		 * Score to newsStur
		 */
		double totalDay = 89;
		double t = Math.floor(totalDay/Math.ceil(PickNum/2))/2-1;
		List<Date> obDate = GreedySubProcess(MapS , scoreList , (int)t);
		System.out.println("Get Number : " +obDate.size());
		String TIDS="";
		for(Date d : obDate){
			TIDS+=","+MapS.get(daMap.get(d)).tid;
		}
		return TIDS;
	}
	
	public static void coverageNews(Connection conn, String TidIN, String dirPath) throws SQLException, FileNotFoundException, UnsupportedEncodingException{

		Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select njson,tid,number from newstermsweb where "+TidIN);
        
        Gson gson = new Gson();
		HashMap<Integer , Set<String>> newsMap = new HashMap<Integer , Set<String>>();
		String number = "";
		DecimalFormat df = new DecimalFormat("0.000");
		String MaxCoverageString = "";
		Double MaxC=0.0;
		HashMap<Integer , HashMap<Integer , Double>> CSVData = new HashMap<Integer , HashMap<Integer , Double>>(); 
		
		String CSVRow=" ";
		double count=0.0,avg=0.0;
		
		List<String> totalTerm = new ArrayList();
		while(rs.next()){
			int tid = rs.getInt("tid");
			Map<String, Integer> myMap = gson.fromJson(rs.getString("njson"), new TypeToken<Map<String, Integer>>(){}.getType());
			
			number+=tid+" : "+ rs.getInt("number")+"\t";
			Set<String> _as = new HashSet<String>();
			_as.addAll(myMap.keySet());
			HashMap<Integer , Double> _tData = new HashMap<Integer , Double>();
			for(int b : newsMap.keySet()){
				Set<String> _bs = new HashSet<String>();
				_bs.addAll(newsMap.get(b));
				/**
				 * AS and BS of Intersection
				 * 
				 * Coverage = Union/Intersection
				 * 
				 * 
				 * */
				List<String> ins_total = new ArrayList<String>();
				ins_total.addAll(_as);
				ins_total.addAll(_bs);
				int org_size = _bs.size();
				_bs.removeAll(_as);
				double coverage = (org_size-_bs.size()+0.0)/(ins_total.size()+0.0);
				avg+=coverage;
				count++;
				_tData.put(b , coverage);
				if(coverage>MaxC){
					MaxC=coverage;
					MaxCoverageString=b+","+rs.getInt("tid");
				}
			}
			totalTerm.addAll(myMap.keySet());
			CSVData.put(tid, _tData);
			newsMap.put(tid, myMap.keySet());
			CSVRow+=","+tid;
		}
		rs = stmt.executeQuery("select count(*) as C from terms_web where Num>5");
		Double _t = 0.0;
		while(rs.next()){
			_t=rs.getDouble("C");
		}
		CSVRow+="\n";
		SortedSet<Integer> keys = new TreeSet<Integer>(CSVData.keySet());
		for(int tid : keys){
			CSVRow+=tid+",";
			for(int tid2 : keys){
				if(tid==tid2 || !CSVData.get(tid).containsKey(tid2)){
					CSVRow+=",";
				}else{
					CSVRow+=CSVData.get(tid).get(tid2)+",";
				}
			}
			CSVRow+="\n";
		}
		
		CSVRow+="\n\nMax ,"+MaxC+","+MaxCoverageString+"\nAVG ,"+df.format((avg/count))+"\nTotal Coverage ,"+df.format(((totalTerm.size()+0.0)/_t));
		System.out.println("Finish Counting , And Output Result");
		PrintWriter writer = new PrintWriter(dirPath, "UTF-8");
		writer.print(CSVRow);
		writer.flush();
		writer.close();
		System.out.println("--------------------");
	}
	

	public static List<Date> GreedySubProcess(HashMap<Double, newsStur> MapS, List<Double> scoreList, int t){
		List<Date> blockDate = new ArrayList<Date>();
		List<Date> obDate = new ArrayList<Date>(); 
		Collections.sort(scoreList);
		Collections.reverse(scoreList);
		for(Double d : scoreList){
			newsStur thisNews = MapS.get(d);
			if(!blockDate.contains(thisNews.d)){
				blockDate.add(thisNews.d);
				obDate.add(thisNews.d);
				blockDate = BlockDateProcess(blockDate, thisNews.d, t);
			}
		}
		return obDate;
	}
	public static List<Date> BlockDateProcess(List<Date> bdp, Date d, int t){
		Calendar c_add = Calendar.getInstance();
		Calendar c_diff = Calendar.getInstance();
		c_add.setTime(d);
		c_diff.setTime(d);
		for(int i=0;i<t;i++){
			c_add.add(Calendar.DATE, 1);
			bdp.add(c_add.getTime());
			c_diff.add(Calendar.DATE, -1);
			bdp.add(c_diff.getTime());
		}
		return bdp;
	}
	
	public static Map sortByValue(Map unsortedMap) {
		Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}
}


class newsObj{
	String date,title,url;
	Double score,e2e,e2d;
	int tid;
	double lamda=1.0;
	public newsObj(double e, double d, String D, double l) {
		// TODO Auto-generated constructor stub
		e2e=e;
		e2d=d;
		date=D;
		lamda=l;
	}
	public void FormalizedSocre(Double mE, Double pE, Double mD, Double pD) {
		// TODO Auto-generated method stub
		e2e = (e2e-mE)/pE;
		e2d = (e2d-mD)/pD;
		score = (((1-lamda)*e2e) + (lamda*e2d));
	}
}

class newsStur{
	int tid;
	Date d;
	double s;
	public newsStur(int _t, Date _d, double _s) {
		// TODO Auto-generated constructor stub
		tid=_t;
		d=_d;
		s=_s;
	}
}

class ValueComparator implements Comparator {
	Map map;
	public ValueComparator(Map map) {
		this.map = map;
	}
	public int compare(Object keyA, Object keyB) {
		Comparable valueA = (Comparable) map.get(keyA);
		Comparable valueB = (Comparable) map.get(keyB);
		return valueB.compareTo(valueA);
	}
}