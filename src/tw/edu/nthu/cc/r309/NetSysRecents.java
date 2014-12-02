package tw.edu.nthu.cc.r309;

import java.util.Comparator;
import java.util.HashMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NetSysRecents {
	public final static String urlBase = "http://net.nthu.edu.tw";
	public final static String urlFeed = urlBase
			+ "/2009/about:latest?do=export_xhtml";

	protected static HashMap<String, HashMap<String, HashMap<String, String>>> internalData = new HashMap<String, HashMap<String, HashMap<String, String>>>();

	public final static Comparator<Object> NUMERIC_COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			Integer t1 = Integer.parseInt((String) o1);
			Integer t2 = Integer.parseInt((String) o2);

			return t1.compareTo(t2);
		}
	};

	public static void init(String... types) {
		for (String type : types) {
			internalData.put(type,
					new HashMap<String, HashMap<String, String>>());
		}
	}

	public static HashMap<String, HashMap<String, String>> getRecents() {
		HashMap<String, HashMap<String, String>> result = new HashMap<String, HashMap<String, String>>();

		Document doc = SimpleHTTPUtils.httpRequest(urlFeed,
				SimpleHTTPUtils.METHOD_GET, null, null, null, true);
		if (doc == null)
			return result;

		Element table = doc.select("div#publish table.inline").first();
		if (table == null)
			return result;

		Elements eles = table.select("tr:gt(0)");
		if (eles == null)
			return result;

		int index = 10000;
		for (Element e : eles) {
			HashMap<String, String> item = new HashMap<String, String>();
			String title = "" + e.select("td:nth-child(3)").text().trim();
			String date = "" + e.select("td:nth-child(1)").text().trim();
			String link = "" + e.select("td:nth-child(3) a").attr("href");

			item.put("subject", title);
			item.put("prettydate", date);
			item.put("article", "netsys/" + link);

			result.put("" + index++, item);
		}

		internalData.put("recents", result);

		return result;
	}
}
