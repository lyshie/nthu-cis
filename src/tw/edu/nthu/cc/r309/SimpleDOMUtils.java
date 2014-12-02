package tw.edu.nthu.cc.r309;

import java.util.ArrayList;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SimpleDOMUtils {
	public static ArrayList<ArrayList<String>> tableToArrayList(Element table) {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

		Elements trs = table.select("tr");

		for (Element tr : trs) {
			ArrayList<String> row = new ArrayList<String>();

			Elements tds = tr.select("td,th");

			for (Element td : tds) {
				// properly deal with No-Break white space '\u00A0'
				Elements links = td.select("a");

				String text = td.text().replaceAll(
						"(^\\p{Whitespace}+|\\p{Whitespace}+$)", "");

				if (text != null && !text.equals("")) {
					row.add(text);
				} else {
					if (!links.isEmpty()) {
						String link_url = "";

						for (Element link : links) {
							String href = link.attr("href") + "";

							if (href.startsWith("mailto:"))
								link_url += href.replaceAll("^mailto:", "")
										+ " ";
						}

						row.add(link_url.trim());
					} else {
						row.add("");
					}
				}
			}

			result.add(row);
		}

		return result;
	}

	public static String joinString(String delimiter, ArrayList<String> items,
			int... index) {
		String result = "";

		for (int i = 0; i < index.length; i++) {
			if (index[i] < items.size()) {
				result += items.get(index[i]) + delimiter;
			}
		}

		if (!result.equals("")) {
			result = result.replaceFirst(".$", "");
		}

		return result;
	}

	public static String normalizeNumber(String data) {
		String result = "";

		result = data.replaceAll("\\s*(\\d+)\\s*", " $1 ");

		return result;
	}
}
