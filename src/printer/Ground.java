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




public class Ground {
	
	public static void GroundDate(Connection conn) throws SQLException, FileNotFoundException, UnsupportedEncodingException{

		Statement stmt = (Statement) conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) as C from newstermsweb as n left join twitterreweb as w on n.tid=w.tid group by w.url order by C DESC");
        
	}
	

}

