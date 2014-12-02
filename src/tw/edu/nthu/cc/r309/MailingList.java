package tw.edu.nthu.cc.r309;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Response;

public class MailingList {
	public final static String urlBase = "http://lists.net.nthu.edu.tw/";
	public final static String urlProfiles = urlBase + "api/public/profile";
	public final static String urlRecents = urlBase
			+ "api/public/article/_ALL_/recent/1209600";

	protected static HashMap<String, HashMap<String, HashMap<String, String>>> internalData = new HashMap<String, HashMap<String, HashMap<String, String>>>();

	public final static Comparator<Object> NUMERIC_COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			Integer t1 = Integer.parseInt(((String) o1).replaceAll("\\D", ""));
			Integer t2 = Integer.parseInt(((String) o2).replaceAll("\\D", ""));

			return t1.compareTo(t2);
		}
	};

	public static void init(String... types) {
		for (String type : types) {
			internalData.put(type,
					new HashMap<String, HashMap<String, String>>());
		}
	}

	public static HashMap<String, HashMap<String, String>> jsonToHashMap(
			JSONObject json) {
		HashMap<String, HashMap<String, String>> result = new HashMap<String, HashMap<String, String>>();

		if (json != null) {
			// layer-1
			Iterator<?> keys = json.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				Object obj = null;
				try {
					obj = json.get(key);
				} catch (JSONException e) {
				}

				if (obj instanceof JSONObject) {
					HashMap<String, String> value = new HashMap<String, String>();
					// layer-2
					Iterator<?> sub_keys = ((JSONObject) obj).keys();
					while (sub_keys.hasNext()) {
						String sub_key = (String) sub_keys.next();
						String sub_value = null;
						try {
							sub_value = ((JSONObject) obj).getString(sub_key)
									+ "";
						} catch (JSONException e) {
							e.printStackTrace();
						}

						if (sub_key != null)
							value.put(sub_key, sub_value);
					}

					result.put(key, value);
				}
			}
		}

		return result;
	}

	public static JSONObject parseJSON(String data) {
		JSONObject json = new JSONObject();

		try {
			json = new JSONObject(data);
		} catch (JSONException obj_e) {
			JSONArray array = new JSONArray();

			try {
				array = new JSONArray(data);
			} catch (JSONException arr_e) {
			}

			JSONArray index = new JSONArray();
			for (int i = 0; i < array.length(); i++) {
				try {
					index.put(i, Integer.toString(i));
				} catch (JSONException e) {
				}
			}

			try {
				json = array.toJSONObject(index);
			} catch (JSONException e) {
			}
		}

		return json;
	}

	public static HashMap<String, HashMap<String, String>> getJSONMap(
			String url, String type) {
		return getJSONMap(url, type, false);
	}

	public static HashMap<String, HashMap<String, String>> getJSONMap(
			String url, String type, boolean forceUpdate) {
		HashMap<String, HashMap<String, String>> result = new HashMap<String, HashMap<String, String>>();

		if (internalData.containsKey(type)) {
			if (forceUpdate)
				fetchJSON(url, type);
			else if (internalData.get(type).isEmpty())
				fetchJSON(url, type);

			result = internalData.get(type);
		}

		return result;
	}

	private static void fetchJSON(String url, String type) {
		Response response = SimpleHTTPUtils.httpRequestRaw(url);

		if (response != null) {
			String data = response.body() + "";
			JSONObject json = parseJSON(data);

			if (json != null)
				internalData.put(type, jsonToHashMap(json));
		}
	}
}
