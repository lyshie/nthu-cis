package tw.edu.nthu.cc.r309;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EIP {
	private String url;
	private ArrayList<ArrayList<String>> signTime;
	private ArrayList<ArrayList<String>> thisWeekSignTime;
	private ArrayList<ArrayList<String>> vocation;
	private HashMap<String, String> cookies = new HashMap<String, String>();

	public EIP(String url) {
		this.url = url;
	}

	public EIP login() {
		Document doc = null;
		Response response = null;

		try {
			response = SimpleHTTPUtils.httpRequestRaw(this.url,
					SimpleHTTPUtils.METHOD_GET);

			if (response != null) {
				doc = response.parse();
				this.cookies.putAll(response.cookies());
			}
		} catch (Exception e) {
			//
		}

		if (doc != null) {
			try {
				// all sign time
				Element table = doc.select(
						"table#ctl00_ContentPlaceHolder1_datagrid_cardlog")
						.first();
				table.select("tr:lt(1)").remove();

				this.signTime = SimpleDOMUtils.tableToArrayList(table);
			} catch (NullPointerException e) {
				// e.printStackTrace();
			}

			try {
				// this week sign time
				Element table_week = doc
						.select("table#ctl00_ContentPlaceHolder1_PersonalDutyNoWeekly_DataGrid")
						.first();
				table_week.select("tr:lt(2)").remove();

				this.thisWeekSignTime = SimpleDOMUtils
						.tableToArrayList(table_week);
			} catch (NullPointerException e) {
				// e.printStackTrace();
			}
		}

		return this;
	}

	public ArrayList<ArrayList<String>> getSignTime() {
		return this.signTime;
	}

	public ArrayList<ArrayList<String>> getThisWeekSignTime() {
		return this.thisWeekSignTime;
	}

	public ArrayList<ArrayList<String>> getVacation() {
		String url = "http://140.114.60.89/EIP/humanly/reporting/personal_vacation_hum_note.aspx";

		Document doc = SimpleHTTPUtils.httpRequest(url,
				SimpleHTTPUtils.METHOD_GET, null, this.cookies, null, true);

		if (doc != null) {
			Elements inputs = doc.select("input");

			if (inputs != null) {
				// all input fields (name, value) to params for HTTP POST
				HashMap<String, String> params = new HashMap<String, String>();
				for (Element input : inputs) {
					String key = input.attr("name") + "";
					String value = input.attr("value") + "";
					String id = input.attr("id") + "";

					if (key.equals("ctl00$ContentPlaceHolder1$SEDate$dateselector_sdate$txt_Date"))
						value = value.replaceAll("\\d\\d\\-\\d\\d$", "01-01");

					if (id.contains("Verify_checkbox")) {
						Element label = doc.select("label[for=" + id + "]")
								.first();
						if (label != null) {
							if (label.text().contains("核准")) {
								params.put(key, value);
							}
						}
					} else {
						params.put(key, value);
					}
				}

				Document d = SimpleHTTPUtils.httpRequest(url,
						SimpleHTTPUtils.METHOD_POST, params, this.cookies);

				if (d != null) {
					Element table = d
							.select("table#ctl00_ContentPlaceHolder1_datagrid_humnt_total")
							.first();

					if (table != null) {
						table.select("tr:lt(1)").remove();
						this.vocation = SimpleDOMUtils.tableToArrayList(table);
					}
				}
			}
		}

		return this.vocation;
	}

	public boolean isLogin() {
		return (this.signTime != null) || (this.thisWeekSignTime != null);
	}
}
