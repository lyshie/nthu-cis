package tw.edu.nthu.cc.r309;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.support.v4.util.LruCache;

public class SimpleHTTPUtils {
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	public static final int TIMEOUT_MILLIS = 5000;

	private static int cacheSize = 1 * 1024 * 1024; // 1 MiB

	private static LruCache<String, Document> httpCache = new LruCache<String, Document>(
			cacheSize) {
		@Override
		protected int sizeOf(String key, Document value) {
			return value.outerHtml().getBytes().length;
		}
	};

	private static LruCache<String, Response> httpRawCache = new LruCache<String, Response>(
			cacheSize) {
		@Override
		protected int sizeOf(String key, Response value) {
			return value.bodyAsBytes().length;
		}
	};

	private static String getReferrer(String url) {
		String referrer = "";

		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
		}

		if (u == null)
			return "";

		referrer = u.getProtocol() + "://" + u.getHost();

		if (u.getPort() > 0) {
			referrer += ":" + u.getPort();
		}

		referrer += "/";

		return referrer;
	}

	public static Document httpRequest(String url) {
		return httpRequest(url, METHOD_GET, null, null, null);
	}

	public static Document httpRequest(String url, int method) {
		return httpRequest(url, method, null, null, null);
	}

	public static Document httpRequest(String url, int method,
			Map<String, String> params) {
		return httpRequest(url, method, params, null, null);
	}

	public static Document httpRequest(String url, int method,
			Map<String, String> params, Map<String, String> cookies) {
		return httpRequest(url, method, params, cookies, null);
	}

	public static Document httpRequest(String url, int method,
			Map<String, String> params, Map<String, String> cookies,
			String referrer) {
		return httpRequest(url, method, params, cookies, referrer, false);
	}

	public static Document httpRequest(String url, int method,
			Map<String, String> params, Map<String, String> cookies,
			String referrer, boolean enableCache) {

		// deal with cache read
		String key = null;
		if (enableCache) {
			key = uniqueKey(url, method, params, cookies, referrer);
			Document cache = httpCache.get(key);
			if (cache != null) {
				return cache;
			}
		}

		Connection conn = Jsoup.connect(url).timeout(TIMEOUT_MILLIS)
				.referrer((referrer == null) ? getReferrer(url) : referrer)
				.followRedirects(true);

		if (params != null) {
			conn = conn.data(params);
		}

		if (cookies != null) {
			conn = conn.cookies(cookies);
		}

		Document result = null;

		if (method == METHOD_GET) {
			try {
				result = conn.get();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		} else if (method == METHOD_POST) {
			try {
				result = conn.post();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}

		// deal with cache write
		if (enableCache) {
			if (result != null) {
				synchronized (httpCache) {
					if (key != null && httpCache.get(key) == null) {
						httpCache.put(key, result);
					}
				}
			}
		}

		return result;
	}

	public static Response httpRequestRaw(String url) {
		return httpRequestRaw(url, METHOD_GET, null, null, null);
	}

	public static Response httpRequestRaw(String url, int method) {
		return httpRequestRaw(url, method, null, null, null);
	}

	public static Response httpRequestRaw(String url, int method,
			Map<String, String> params) {
		return httpRequestRaw(url, method, params, null, null);
	}

	public static Response httpRequestRaw(String url, int method,
			Map<String, String> params, Map<String, String> cookies) {
		return httpRequestRaw(url, method, params, cookies, null);
	}

	public static Response httpRequestRaw(String url, int method,
			Map<String, String> params, Map<String, String> cookies,
			String referrer) {
		return httpRequestRaw(url, method, params, cookies, referrer, false);
	}

	public static Response httpRequestRaw(String url, int method,
			Map<String, String> params, Map<String, String> cookies,
			String referrer, boolean enableCache) {

		// deal with cache read
		String key = null;
		if (enableCache) {
			key = uniqueKey(url, method, params, cookies, referrer);
			Response cache = httpRawCache.get(key);
			if (cache != null) {
				return cache;
			}
		}

		Response result = null;

		Connection conn = Jsoup.connect(url).timeout(TIMEOUT_MILLIS)
				.referrer((referrer == null) ? getReferrer(url) : referrer)
				.ignoreContentType(true);

		if (params != null) {
			conn = conn.data(params);
		}

		if (cookies != null) {
			conn = conn.cookies(cookies);
		}

		if (method == METHOD_GET) {
			try {
				result = conn.method(Method.GET).execute();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		} else if (method == METHOD_POST) {
			try {
				result = conn.method(Method.POST).execute();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}

		// deal with cache write
		if (enableCache) {
			if (result != null) {
				synchronized (httpRawCache) {
					if (key != null && httpRawCache.get(key) == null) {
						httpRawCache.put(key, result);
					}
				}
			}
		}

		return result;
	}

	public static void doParallelTasks(Runnable... runnables) {
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
		ThreadPoolExecutor tpe = new ThreadPoolExecutor(2, 2, 10,
				TimeUnit.SECONDS, queue);

		for (Runnable runnable : runnables) {
			tpe.execute(runnable);
		}

		tpe.shutdown();

		try {
			tpe.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO
		}
	}

	public static String uniqueKey(String url, int method,
			Map<String, String> params, Map<String, String> cookies,
			String referrer) {
		String plain = null;

		plain = url + "\0";
		plain += method + "\0";
		plain += referrer + "\0";

		if (params != null && params.keySet() != null) {
			Object[] ps = params.keySet().toArray();
			Arrays.sort(ps);
			for (Object k : ps) {
				plain += k + "\t" + params.get(k) + "\0";
			}
		}

		if (cookies != null && cookies.keySet() != null) {
			Object[] cs = cookies.keySet().toArray();
			Arrays.sort(cs);
			for (Object k : cs) {
				plain += k + "\t" + cookies.get(k) + "\0";
			}
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}

		if (md != null) {
			md.update(plain.getBytes());

			StringBuffer sb = new StringBuffer();
			for (byte b : md.digest()) {
				sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString();
		} else {
			return null;
		}
	}
}
