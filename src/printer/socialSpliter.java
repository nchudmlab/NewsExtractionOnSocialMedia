package printer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONObject;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class socialSpliter implements Callable<Set<String>>{

	public MaxentTagger tagger;
	public String content;

    public Set<String> call() {
		try{
	        String tagged = tagger.tagString(content);
	        String[] x = tagged.split(" ");
	        Set<String> thisObj = new HashSet<String>();
	        for(int i=0;i<x.length;i++)
	        {
	            if (x[i].substring(x[i].lastIndexOf("_")+1).startsWith("N")){
	            	String key = x[i].split("_")[0].toLowerCase();
	            	key = (key.substring(0, 1).equals("#") || key.substring(0, 1).equals("@")) ? key = key.substring(1) : key ;
            		if(key != null && !key.contains("/")){
                		thisObj.add(key);
            		}
	            }
	        }
			return thisObj;
		}catch(Exception E){
			System.out.println("Social Splite Error [" + content + "]");
			return new HashSet<String>();
		}
    }
	

	public socialSpliter(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public socialSpliter(MaxentTagger t, String c){
		tagger = t;
		content = c;
	}
}
