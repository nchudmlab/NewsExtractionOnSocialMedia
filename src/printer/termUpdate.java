package printer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONObject;

import com.mysql.jdbc.Connection;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class termUpdate implements Callable<String>{

	public HashMap<String, Set<Integer>> map;
	public Connection conn;

    public String call() {
		try{
			System.out.println(map.toString());
			System.out.println(map.keySet().toString());
			
			return "SUCCESS";
		}catch(Exception E){
			System.out.println("Update Error !!");
//			return new JSONObject();
			return "FAILE";
		}
    }
	
	
	public termUpdate(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public termUpdate(Connection c , HashMap<String, Set<Integer>> m){
		map = m;
		conn = c;
	}

}
