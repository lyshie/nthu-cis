package tw.edu.nthu.cc.r309;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class AIS {
	private static final String urlCCXP = "https://www.ccxp.nthu.edu.tw/ccxp/";
	private static final String urlINQUIRE = "https://www.ccxp.nthu.edu.tw/ccxp/INQUIRE/";
	private static final String urlCaptchaDecoder = "http://ccc-r309.vm.nthu.edu.tw/captcha/captcha_decoder.pl";

	public static final String TOKEN_EIP = "線上簽到退";
	public static final String TOKEN_EDOCUMENT = "電子公文線上簽核系統";

	private String acixStore = "";
	private String account = "";
	private String pwdStr = "";

	private HashMap<String, String> menu = null;

	public AIS() {

	}

	public AIS(String account, String acixStore) {
		this.account = account;
		this.acixStore = acixStore;
	}

	public String getNewPwdStr() {
		String result = null;
		Document doc = null;
		doc = SimpleHTTPUtils.httpRequest(urlINQUIRE,
				SimpleHTTPUtils.METHOD_GET);

		if (doc != null) {
			Element ele = doc.select("input ~ img").first();
			result = ele.attr("src") + "";
			result = result.replaceAll("^[^=]*=", "");

			this.pwdStr = result;
		}

		return (result == null) ? "" : result;
	}

	public String getPwdStr() {
		return this.pwdStr;
	}

	public byte[] getCaptchaImage(String pwdstr) {
		byte[] result = null;

		Map<String, String> params = new HashMap<String, String>();
		params.put("pwdstr", pwdstr);

		Response resp = SimpleHTTPUtils.httpRequestRaw(urlINQUIRE
				+ "auth_img.php", SimpleHTTPUtils.METHOD_GET, params);

		if (resp != null)
			result = resp.bodyAsBytes();

		return result;
	}

	public byte[] getPersonalImage(String id) {
		byte[] result = null;

		Response resp = SimpleHTTPUtils.httpRequestRaw(urlCCXP + "PICS/PE/"
				+ id.trim().toUpperCase() + ".jpg", SimpleHTTPUtils.METHOD_GET,
				null, null, urlINQUIRE, true);

		if (resp != null)
			result = resp.bodyAsBytes();

		return result;
	}

	public String decodeCaptchaImage(String pwdstr, String identifier) {
		String result = null;
		Document doc = null;

		identifier = "" + identifier;

		Map<String, String> params = new HashMap<String, String>();
		params.put("pwdstr", pwdstr);

		doc = SimpleHTTPUtils.httpRequest(urlCaptchaDecoder + "?account="
				+ identifier, SimpleHTTPUtils.METHOD_GET, params);

		if (doc != null) {
			result = doc.text().replaceAll("\\D", "");
		}

		return (result == null) ? "" : result;
	}

	public AIS login(String account, String passwd, String passwd2,
			String pwdstr) {
		Document doc = null;

		Map<String, String> params = new HashMap<String, String>();
		params.put("account", account);
		params.put("passwd", passwd);
		params.put("passwd2", passwd2);
		params.put("fnstr", pwdstr);
		params.put("Submit", "登入");

		doc = SimpleHTTPUtils.httpRequest(urlINQUIRE + "/pre_select_entry.php",
				SimpleHTTPUtils.METHOD_POST, params);

		if (doc != null) {
			Element ele = doc.select("meta").first();

			Pattern p = Pattern.compile("ACIXSTORE=([^&]+)");
			Matcher m = p.matcher(ele.attr("content"));

			if (m.find()) {
				this.acixStore = m.group(1);
				this.account = account;

				login2();

				fetchMenu();
			}
		}

		return this;
	}

	private AIS login2() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("ACIXSTORE", this.acixStore);
		params.put("hint", this.account);

		Document doc = SimpleHTTPUtils.httpRequest(urlINQUIRE
				+ "/select_entry.php", SimpleHTTPUtils.METHOD_GET, params);

		if (doc == null || doc.select("frameset").isEmpty()) {
			this.acixStore = "";
		}

		return this;
	}

	public HashMap<String, String> getRelativeMenu() {
		if (this.menu == null)
			fetchMenu();

		return this.menu;
	}

	public HashMap<String, String> getAbsoluteMenu() {
		if (this.menu == null)
			fetchMenu();

		HashMap<String, String> result = new HashMap<String, String>();

		for (Map.Entry<String, String> entry : this.menu.entrySet()) {
			String item = entry.getKey();
			String url = entry.getValue();

			if (!url.matches("(http|https)://.*")) {
				url = urlINQUIRE + url;
			}

			result.put(item, url);
		}

		return result;
	}

	private void fetchMenu() {
		HashMap<String, String> result = new HashMap<String, String>();

		Document doc = null;

		Map<String, String> params = new HashMap<String, String>();
		params.put("ACIXSTORE", this.acixStore);

		doc = SimpleHTTPUtils.httpRequest(urlINQUIRE + "/IN_INQ_STA.php",
				SimpleHTTPUtils.METHOD_GET, params);

		if (doc != null) {
			String[] lines = doc.html().split("[\\n\\r]");

			Pattern p = Pattern
					.compile("insDoc\\(.+\"<font size=2>(.+)</font>\", \"([^\"]+)\"");

			for (int i = 0; i < lines.length; i++) {
				Matcher m = p.matcher(lines[i]);

				if (m.find()) {
					String item = Jsoup.parse(m.group(1)).text();
					String url = m.group(2);

					result.put(item, url);
				}
			}
		}

		this.menu = result;
	}

	public String getSessionKey() {
		return this.acixStore;
	}

	public boolean isLogin() {
		return !this.acixStore.equals("");
	}
}
