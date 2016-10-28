package printer;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;

public class scrapyNews {

	public static void toDO3dprint(Connection conn) throws IOException, ParseException, SQLException, InterruptedException{
		String Categories [] = {"3d-printers","3d-printing","3d-printing-materials","3d-scanners",
				"3d-software","aerospace-3d-printing","business-2","editorials-opinions","exclusive-interviews",
				"featured-stories","health-3d-printing","robotics","virtual-reality","3d-design","3d-printed-art","3d-printed-eyewear",
				"3d-printed-guns-2"};
		for(String _s : Categories){
			Scrapy3dprint("http://3dprint.com/category/"+_s+"/" , conn);
		}
	}
	
	public static void toDo3ders(Connection conn) throws IOException, ParseException, SQLException{
		Scrapy3ders("http://3ders.org/" , conn);
		for(int a = 261; a > 0 ; a -- ){
			System.out.println("http://3ders.org/home/page"+a+".html");
			Scrapy3ders("http://3ders.org/home/page"+a+".html" , conn);
		}
	}
	
	public static void Scrapy3ders(String pageLink, Connection conn) throws IOException, ParseException, SQLException{
		Document doc = Jsoup.connect(pageLink).timeout(15*1000).get();
		Elements links = doc.select("div.art-layout-cell a.art-button");
		for (Element src : links) {
			if(src.attr("href").contains("articles")){
				try {
					Statement stmt = (Statement) conn.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT link FROM scrapynews WHERE link = 'http://3ders.org/"+src.attr("href").substring(2).replace("'", "\\'")+"'");
					if(!rs.next()){
						Thread.sleep(5000);
						Doc3ders("http://3ders.org/"+src.attr("href").substring(2), conn);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (MySQLSyntaxErrorException e){
					System.out.println(src.attr("href").substring(2));
				}
			}
        }
		
		
	}

	public static void Doc3dprint(String sUrl, Connection conn, PreparedStatement insertNews) throws IOException, SQLException, ParseException{
		try{
			Document doc = Jsoup.connect(sUrl).timeout(15*1000).get();
			try{
				
				String dateText = doc.select("p.post-meta span.published").text();
				SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
				Date dt = sdf.parse(dateText);
				String title=doc.select("div.et_post_meta_wrapper h1.entry-title").text();
				String thisContext = doc.select("div.entry-content").text().replace("Share on Tumblr", "");
				
				
				/**SQL INSERT**/
				
				insertNews.setString (1, title);
				insertNews.setString (2, thisContext);
				insertNews.setString (3, "3dprint");
				insertNews.setDate   (4, new java.sql.Date(dt.getTime()) );
				insertNews.setString (5, sUrl);
				insertNews.addBatch();
	
			}catch(Exception e){
				System.out.println("Date cant paser :: "+sUrl);
			}
		}catch(Exception e){
			System.out.println("Url cant open :: "+sUrl);
		}
	}

	public static void Scrapy3dprint(String pageLink, Connection conn) throws IOException, ParseException, SQLException, InterruptedException{
 	    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
 	    Date date = new Date();
		System.out.println("Time : " + dateFormat.format(date) + " URL : " + pageLink);
		Document doc = Jsoup.connect(pageLink).timeout(15*1000).get();
		Elements links = doc.select("h2.entry-title a");
		String query = " insert IGNORE into scrapynews (title, content, source, date, link) values (?, ?, ?, ?, ?)";
		PreparedStatement insertNews = (PreparedStatement) conn.prepareStatement(query);
		for (Element src : links) {
			Doc3dprint(src.attr("href"), conn, insertNews);
			Thread.sleep(4000);
        }
		insertNews.executeBatch();
		Elements nextPage = doc.select("div.alignright a");
		if(!nextPage.isEmpty()){
			Scrapy3dprint(nextPage.attr("href") , conn);
		}
	}

	public static void Doc3ders(String sUrl, Connection conn) throws IOException, SQLException, ParseException{
		try{
			Document doc = Jsoup.connect(sUrl).timeout(15*1000).get();
			
			try{
	
				String dateText = sUrl.split("articles/")[1].split("-")[0];
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				Date dt = sdf.parse(dateText);
				String title="";
				Elements Etis = doc.select("h2.art-postheader");
				for (Element Eti : Etis) {
					if(Eti.tagName().equals("h2")){
						title = Eti.text();
					}
		        }
				
				System.out.println(dateText+" - "+title);
		
				String thisContext = "";
				Elements contexts = doc.select("div.art-layout-cell p");
				for (Element src : contexts) {
					if(src.attr("style")==""){
						if(src.text().contains("Posted in")){
							break;
						}else if(!src.html().equals("&nbsp;")){
							thisContext+=src.text()+"\n";
						}
					}
		        }
				
				
				String query = " insert into scrapynews (title, content, source, date, link) values (?, ?, ?, ?, ?)";
		
			    PreparedStatement preparedStmt = (PreparedStatement) conn.prepareStatement(query);
			    preparedStmt.setString (1, title);
			    preparedStmt.setString (2, thisContext);
			    preparedStmt.setString (3, "3ders");
			    preparedStmt.setDate   (4, new java.sql.Date(dt.getTime()) );
			    preparedStmt.setString (5, sUrl);
			    preparedStmt.execute();
	
			}catch(Exception e){
				
				System.out.println("Date cant paser :: "+sUrl);
			}
		}catch(Exception e){
			System.out.println("Url cant open :: "+sUrl);
		}
	}
}
