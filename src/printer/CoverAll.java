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




public class CoverAll {
	
	public static void coverageNews(Connection conn) throws SQLException, FileNotFoundException, UnsupportedEncodingException{

		Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select njson,tid,number from newstermsweb where done=4");
        
        Gson gson = new Gson();
		HashMap<Integer , Set<String>> newsMap = new HashMap<Integer , Set<String>>();

		DecimalFormat df = new DecimalFormat("0.000");
		Double MaxC=0.0;
		HashMap<Integer , HashMap<Integer , Double>> CSVData = new HashMap<Integer , HashMap<Integer , Double>>(); 
		
		double count=0.0,avg=0.0;
		
		List<String> totalTerm = new ArrayList();
		while(rs.next()){
			int tid = rs.getInt("tid");
			Map<String, Integer> myMap = gson.fromJson(rs.getString("njson"), new TypeToken<Map<String, Integer>>(){}.getType());
			
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
			}
			totalTerm.addAll(myMap.keySet());
			CSVData.put(tid, _tData);
			newsMap.put(tid, myMap.keySet());
		}
		System.out.println("Finish Counting , And Output Result");
		System.out.println(df.format((avg/count)));
		System.out.println("--------------------");
	}
	

}

