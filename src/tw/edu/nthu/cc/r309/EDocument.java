package tw.edu.nthu.cc.r309;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EDocument {
	public static final String urlDashboard = "http://document-doc.vm.nthu.edu.tw/nthopnet/web/odc/odc100.aspx";

	public String url;
	public HashMap<String, String> cookies;
	private ArrayList<ArrayList<ArrayList<String>>> tableContents;

	public EDocument(String url) {
		this.url = url;
	}

	public EDocument login() {
		Response resp = null;

		try {
			resp = SimpleHTTPUtils.httpRequestRaw(this.url,
					SimpleHTTPUtils.METHOD_GET);
		} catch (Exception e) {
			//
		}

		if (resp != null) {
			this.cookies = (HashMap<String, String>) resp.cookies();

			login2();
		}

		return this;
	}

	public void login2() {
		Response resp = null;

		try {
			resp = SimpleHTTPUtils.httpRequestRaw(urlDashboard,
					SimpleHTTPUtils.METHOD_GET, null, this.cookies);
		} catch (Exception e) {
			//
		}

		if (resp != null) {
			Document doc = null;
			try {
				doc = resp.parse();
				Elements tables = doc.select(".TABLE-Content");

				this.tableContents = new ArrayList<ArrayList<ArrayList<String>>>(
						tables.size());

				for (Element table : tables) {
					this.tableContents.add(SimpleDOMUtils
							.tableToArrayList(table));
				}
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}
	}

	public ArrayList<ArrayList<ArrayList<String>>> getDashboard() {
		return this.tableContents;
	}

	public boolean isLogin() {
		return (this.cookies != null);
	}
}
