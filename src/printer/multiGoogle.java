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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class multiGoogle implements Callable<String>{

	public int socialID;
	public Connection conn;
	public String searchString;

    public String call() throws IOException, JSONException, SQLException {

		String google = "http://www.google.com/search?q=";
		String search = searchString;
//		String search = "3D printing process brings art to blind people | Reuters";
		String charset = "UTF-8";
		String userAgent = "Chrome"; // Change this to your company's name and bot homepage!

		SimpleDateFormat contentFormat = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
		Elements links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g");
		String content="";
		String url="";	
		Date contentDate = null;
		System.out.println(links.size());
		for (Element link : links) {
			String tempUrl = link.select(".r>a").get(0).absUrl("href");
			tempUrl = URLDecoder.decode(tempUrl.substring(tempUrl.indexOf('=') + 1, tempUrl.indexOf('&')), "UTF-8");
			if (!tempUrl.startsWith("http")) {
		        continue;
		    }
			url = url.equals("")?tempUrl:url;
			try {
				String tempContent = link.select(".s>.st").first().text();
				String conArray[] = tempContent.split(" \\.\\.\\.");
				contentDate = contentFormat.parse(conArray[0]);
				content = content.equals("")?conArray[1]:content;
				if(tempUrl.contains("3ders.org") || tempUrl.contains("3dprint.com")){
					contentDate = contentFormat.parse(conArray[0]);
					url = tempUrl;
					content = conArray[1];
					break;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				continue;
			} catch (IndexOutOfBoundsException e){
				// TODO Auto-generated catch block
				continue;
			} catch (NullPointerException e){
				continue;
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if(url.equals("") && content.equals("")){
			return "google faile, ID ="+socialID;
		}else{
			String insertString = "INSERT INTO google4news (tid,content,url,date) value (?,?,?,?)";
			PreparedStatement PSInsert = (PreparedStatement) conn.prepareStatement(insertString);
	    	PSInsert.setInt    (1, socialID);
	    	PSInsert.setString (2, content);
	    	PSInsert.setString (3, url);
	    	PSInsert.setDate(4, new java.sql.Date(contentDate.getTime()));
	    	PSInsert.execute();
			return url;
		}
    }

	  private String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

	  public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
	public String html2text(String html) {
	    return Jsoup.parse(html).text();
	}
	
	public multiGoogle(){
		System.out.println("Error Parameters");
		System.exit(-1);
	}
	public multiGoogle(Connection c , String s , int i){
		searchString = s;
		conn = c;
		socialID = i;
	}

}
