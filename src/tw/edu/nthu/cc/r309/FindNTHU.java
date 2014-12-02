package tw.edu.nthu.cc.r309;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class FindNTHU {
	private static final String urlQuery = "http://ccc-nthudss.vm.nthu.edu.tw/nthusearch/search.php";

	public static ArrayList<ArrayList<String>> query(String keyword) {
		ArrayList<ArrayList<String>> result = null;

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "0");
		params.put("q", keyword);

		Document doc = SimpleHTTPUtils.httpRequest(urlQuery,
				SimpleHTTPUtils.METHOD_GET, params, null, null, true);

		if (doc != null) {
			result = new ArrayList<ArrayList<String>>();
			for (Element table : doc.select("div.story table tr:gt(0)")) {
				ArrayList<ArrayList<String>> a = null;
				a = SimpleDOMUtils.tableToArrayList(table);
				if (a != null)
					result.addAll(a);
			}
		}

		return result;
	}
}
