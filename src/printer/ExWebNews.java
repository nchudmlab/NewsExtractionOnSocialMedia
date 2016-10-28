package printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class ExWebNews implements Callable<String>{

	public int socialID;
	public Connection conn;
	public String shortLink;

    public String call() throws IOException, JSONException, SQLException {
    	String insertTableSQL = "INSERT ignore INTO twitterreweb (tid, body, hit, url, title) VALUES (?,?,?,?,?)";
    	PreparedStatement PSInsert = (PreparedStatement) conn.prepareStatement(insertTableSQL);
		String content="";
		String url="";
		String title="";
		try{
			org.jsoup.Connection _c = Jsoup.connect(shortLink).followRedirects(true).userAgent("Mozilla").timeout(15*1000);
			Response response = _c.execute();
			url = response.url().toString();
    		Document doc = _c.get();
    		title=doc.title();
			if(url.contains("www.theguardian.com")){
				Elements nodes = doc.select("div.content__article-body.from-content-api.js-article__body");
				content = nodes.text(); 
			}else if(url.contains("3dprintingindustry.com")){
				Elements nodes = doc.select("div.entry_content");
				content = nodes.text(); 
			}else if(url.contains("funtech.com")){
				Elements nodes = doc.select("div.post-body.entry-content");
				content = nodes.text(); 
			}else if(url.contains("designindaba.com")){
				Elements nodes = doc.select("div.field-item");
				content = nodes.text(); 
			}else if(url.contains("inhabitat.com")){
				Elements nodes = doc.select("div.single.post-content");
				content = nodes.text(); 
			}else if(url.contains("mbtmag.com")){
				Elements nodes = doc.select("div.field-item");
				content = nodes.text(); 
			}else if(url.contains("instructables.com")){
				Elements nodes = doc.select("div#instructable-body");
				content = nodes.text(); 
			}else if(url.contains("nerdist.com")){
				Elements nodes = doc.select("div.entry-content");
				content = nodes.text(); 
			}else if(url.contains("nerdist.com")){
				Elements nodes = doc.select("div.entry-content");
				content = nodes.text(); 
			}else if(url.contains("nerdist.com")){
				Elements nodes = doc.select("div.entry-content");
				content = nodes.text(); 
			}else if(url.contains("mashable.com")){
				Elements nodes = doc.select("p");
				content = nodes.text(); 
			}else if(url.contains("inc.com")){
				Elements nodes = doc.select("div.article-body.inc_editable");
				content = nodes.text();
			}else if(url.contains("forbes.com/forbes/welcome/")){
				return "E:::"+socialID+":::4:::Messages : forbes.com/forbes URL: " + shortLink ;
			}else if(url.contains("twitter.com")){
				return "E:::"+socialID+":::4:::Messages : twitter.com URL: " + shortLink ;
			}else if(url.contains("youtube.com")){
				return "E:::"+socialID+":::4:::Messages : youtube.com URL: " + shortLink ;
			}else if(url.contains("3tags.org")){
				return "E:::"+socialID+":::4:::Messages : 3tags.org URL: " + shortLink ;
			}else{
				Elements nodes = doc.select("article");
				if(nodes.size()==0){
					Elements nodes2 = doc.select("p");
					if(nodes2.size()==0){
						throw new NoUsefulException(">"+url);
					}else{
						content = nodes2.text();
					}
				}else{
					content = nodes.text();
				}
			}
		}catch(NoUsefulException e ){
			System.out.println(e.getMessage());
			return "E:::"+socialID+":::5:::Messages : none-process URL: " + shortLink ;
		}catch(HttpStatusException E1){
			return "E:::"+socialID+":::2:::Messages : " + E1.getMessage() + " URL: " + shortLink ;
		}catch(Exception E2){
			return "E:::"+socialID+":::3:::Messages : " + E2.getMessage() + " URL: " + shortLink ;
		}
		
		

    	PSInsert.setInt(1,socialID);
    	PSInsert.setString(2,content);
    	PSInsert.setString(4, url);
		if(shortLink.contains("3ders.org") || shortLink.contains("3dprint.com")){
	    	PSInsert.setInt(3, 1);
		}else{
	    	PSInsert.setInt(3, 0);
		}
		PSInsert.setString(5, title);
		PSInsert.execute();
    	return "O:::"+socialID+":::1";
    }

	
	public ExWebNews(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public ExWebNews(Connection c , String l , int i){
		shortLink = l;
		conn = c;
		socialID = i;
	}

}
