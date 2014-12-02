package tw.edu.nthu.cc.cis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Collator;

import tw.edu.nthu.cc.cis.R;
import tw.edu.nthu.cc.r309.FindNTHU;
import tw.edu.nthu.cc.r309.SimpleDOMUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

@SuppressLint("NewApi")
public class FindNTHUActivity extends AutoSaveStateActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_findnthu);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container,
							new PlaceholderFragment(R.layout.fragment_findnthu))
					.commit();
		}

		this.previousActivity = MailingListActivity.class;
		this.nextActivity = EIPActivity.class;
	}

	protected void createViewPagerAndTabHost() {
		// ViewPager
		final ViewPager pager = (ViewPager) findViewById(R.id.viewPagerQuery);

		// layout from XML
		final ArrayList<View> listView = new ArrayList<View>();
		listView.add(getLayoutInflater().inflate(R.layout.layout_contact_query,
				null));
		listView.add(getLayoutInflater()
				.inflate(R.layout.layout_ip_query, null));

		// TabHost
		final TabHost tabHost = createTabs();

		if (tabHost != null) {
			tabHost.setOnTabChangedListener(new OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					if (pager == null)
						return;

					if ("contact_query".equals(tabId)) {
						pager.setCurrentItem(0);
					}

					if ("ip_query".equals(tabId)) {
						pager.setCurrentItem(1);
					}
				}
			});
		}

		// PagerAdapter
		pager.setAdapter(new PagerAdapter() {
			@Override
			public void destroyItem(ViewGroup view, int position, Object arg2) {
				ViewPager pViewPager = (ViewPager) view;
				pViewPager.removeView(listView.get(position));

				saveChildViews(view);
			}

			@Override
			public Object instantiateItem(ViewGroup view, int position) {
				ViewPager pViewPager = (ViewPager) view;
				pViewPager.addView(listView.get(position));

				if (position == 1) {
					EditText edit = (EditText) pViewPager
							.findViewById(R.id.editTextIP);
					strictEditText(edit);
				}

				restoreChildViews(view);

				return listView.get(position);
			}

			@Override
			public int getCount() {
				return listView.size();
			}

			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return (arg0 == arg1);
			}

			@Override
			public Parcelable saveState() {
				return null;
			}

			@Override
			public void restoreState(Parcelable arg0, ClassLoader arg1) {
			}

			@Override
			public void startUpdate(View arg0) {
			}

			@Override
			public void finishUpdate(View arg0) {
			}
		});

		pager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int arg0) {
				if (tabHost != null) {
					tabHost.setCurrentTab(arg0);
				}
			}
		});

		// bad code here
		pager.setOffscreenPageLimit(listView.size());
		pager.setCurrentItem(1);
		pager.setCurrentItem(0);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		createViewPagerAndTabHost();
	}

	private void addTab(TabHost tabHost, String spec, int res_string, int viewId) {
		addTab(tabHost, spec, getResources().getString(res_string), viewId);
	}

	private void addTab(TabHost tabHost, String spec, String indicator,
			int viewId) {
		TabSpec tabSpec = tabHost.newTabSpec(spec);
		if (tabSpec == null)
			return;

		tabSpec.setIndicator(indicator);
		// always show ViewPager
		tabSpec.setContent(R.id.viewPagerQuery);

		tabHost.addTab(tabSpec);
	}

	private TabHost createTabs() {
		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		if (tabHost == null)
			return null;

		tabHost.setup();

		addTab(tabHost, "contact_query", R.string.label_keyword,
				R.id.tabContactQuery);
		addTab(tabHost, "ip_query", R.string.label_ip, R.id.tabIPQuery);

		TabWidget tabWidget = tabHost.getTabWidget();
		if (tabWidget == null)
			return tabHost;

		for (int i = 0; i < tabWidget.getChildCount(); i++) {
			tabWidget.getChildAt(i).getLayoutParams().height = 60;
		}

		return tabHost;
	}

	public static class IPv4TextWatcher implements TextWatcher {
		// prevent stack overflow (recursively called)
		private boolean mSelfChange = false;

		@Override
		public synchronized void afterTextChanged(Editable s) {
			if (mSelfChange)
				return;

			mSelfChange = true; // synchronized: only one run into this
								// session

			String str = s.toString();
			str = str.replaceAll("[^0-9\\.]", "");

			String tokens[] = str.split("\\.");

			ArrayList<String> ips = new ArrayList<String>();

			for (int i = 0; i < tokens.length; i++) {
				try {
					int num = Integer.parseInt(tokens[i]);

					if (num >= 0 && num <= 255) {
						ips.add(num + "");
					} else {
						ips.add(((num / 10) % 256) + "");
						ips.add((num % 10) + "");
					}
				} catch (Exception e) {
					// TODO
				}
			}

			s.replace(
					0,
					s.length(),
					SimpleDOMUtils.joinString(".", ips, 0, 1, 2, 3)
							+ (str.endsWith(".") ? "." : ""));

			mSelfChange = false;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			if (mSelfChange)
				return;
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (mSelfChange)
				return;
		}

	};

	private void strictEditText(final EditText edit) {
		if (edit != null) {
			edit.addTextChangedListener(new IPv4TextWatcher());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.find_nthu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == android.R.id.home) {
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected static final int REFRESH_RESULT = 0x00000001;
	protected static final int REFRESH_DIALOG = 0x00000002;
	protected static final int TOGGLE_QUERY_BUTTON = 0x00000003;
	protected static final int TOGGLE_IP_QUERY_BUTTON = 0x00000004;
	protected static final int REFRESH_DIALOG_WAIT = 0x00000005;
	protected static final int REFRESH_WEBVIEW = 0x00000006;

	private ProgressDialog dialogWait;

	@SuppressLint({ "HandlerLeak", "SetJavaScriptEnabled" })
	protected Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Object result = msg.obj;

			switch (msg.what) {
			case REFRESH_DIALOG_WAIT:
				dialogWait = ProgressDialog.show(FindNTHUActivity.this, "",
						getResources().getString(R.string.msg_loading), false,
						true);
				break;
			case TOGGLE_QUERY_BUTTON:
				Button queryButton = (Button) findViewById(R.id.buttonQuery);
				if (queryButton != null)
					queryButton.setEnabled((boolean) result);
				break;
			case TOGGLE_IP_QUERY_BUTTON:
				Button ipQueryButton = (Button) findViewById(R.id.buttonIPQuery);
				if (ipQueryButton != null)
					ipQueryButton.setEnabled((boolean) result);
				break;
			case REFRESH_DIALOG:
				if (dialogWait != null)
					dialogWait.dismiss();

				String message = (String) result;
				showDialog(
						getResources().getString(
								R.string.title_activity_find_nthu), message);
				break;
			case REFRESH_RESULT:
				if (dialogWait != null)
					dialogWait.dismiss();

				// push into ArrayList
				if (result instanceof ArrayList<?>) {
					@SuppressWarnings("unchecked")
					ArrayList<ArrayList<String>> rows = (ArrayList<ArrayList<String>>) result;

					ArrayList<Spanned> arr = new ArrayList<Spanned>();

					// padding for first element
					String tmp = "";
					for (ArrayList<String> row : rows) {
						if (row.get(0).equals(""))
							row.set(0, tmp);

						tmp = row.get(0);
					}

					// chinese (Taiwan) sort
					final Collator twCollator = Collator
							.getInstance(Locale.TAIWAN);

					Collections.sort(rows, new Comparator<ArrayList<String>>() {
						@Override
						public int compare(ArrayList<String> lhs,
								ArrayList<String> rhs) {
							return twCollator.compare(lhs.get(0), rhs.get(0));
						}
					});

					for (ArrayList<String> row : rows) {
						if (row.size() > 1) {
							row.set(0, "<big><b>" + row.get(0) + "</b></big>");
							row.set(1,
									"<font color=\"#0000ff\"><small>"
											+ row.get(1) + "</small></font>");
							for (int i = 2; i < row.size(); i++) {
								row.set(i, "<small>" + row.get(i) + "</small>");
							}
							arr.add(Html.fromHtml(SimpleDOMUtils.joinString(
									" ", row, 0, 1, 2, 3, 4)));
						}
					}

					// set view and adapter
					ListView lv = (ListView) findViewById(R.id.listViewResult);

					if (lv != null) {

						ArrayAdapter<Spanned> adapter = new ArrayAdapter<Spanned>(
								lv.getContext(),
								android.R.layout.simple_list_item_1, arr);

						lv.setAdapter(adapter);

						// set event listener
						lv.setOnItemClickListener(new OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> parent,
									View view, int position, long id) {

								String line = ((TextView) view).getText()
										.toString();
								String email = extractEmail(line);

								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri.parse("mailto:" + email));

								try {
									startActivity(intent);
								} catch (Exception e) {
									notifyStatus(
											handler,
											REFRESH_DIALOG,
											getResources().getString(
													R.string.msg_failed));
								}
							}
						});
					}
				}

				break;
			case REFRESH_WEBVIEW:
				String url = (String) result + "";

				if (!url.equals("")) {
					WebView wv = (WebView) findViewById(R.id.webViewIP);
					if (wv != null) {
						wv.setWebViewClient(mWebViewClient);
						wv.getSettings().setJavaScriptEnabled(true);
						wv.getSettings().setBuiltInZoomControls(true);
						wv.getSettings().setDisplayZoomControls(true);
						//wv.getSettings().setLoadWithOverviewMode(true);
						wv.getSettings().setUseWideViewPort(true);

						wv.loadUrl((String) result);
					}
				}
				break;
			default:
			}
		}
	};

	private WebViewClient mWebViewClient = new WebViewClient() {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (dialogWait != null)
				dialogWait.dismiss();

			dialogWait = ProgressDialog
					.show(FindNTHUActivity.this, "",
							getResources().getString(R.string.msg_loading),
							false, true);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (dialogWait != null)
				dialogWait.dismiss();
		}
	};

	public void onButtonClick(View v) {
		switch (v.getId()) {
		case R.id.buttonIPQuery:
			EditText ip = (EditText) findViewById(R.id.editTextIP);
			String ipv4 = ip.getText().toString().trim();
			queryIP(ipv4);
			break;
		case R.id.buttonQuery:
			EditText keyword = (EditText) findViewById(R.id.editTextKeyword);
			String data = keyword.getText().toString().trim();
			findNTHU(data);
			break;
		default:
		}
	}

	protected static String extractEmail(String line) {
		String result = "";

		Pattern p = Pattern.compile("(\\S+@\\S+)");
		Matcher m = p.matcher(line);

		if (m.find())
			result = m.group(1);

		return result;
	}

	protected void findNTHU(final String keyword) {
		final FindNTHUActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, TOGGLE_QUERY_BUTTON, false);
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				ArrayList<ArrayList<String>> rows = null;

				rows = FindNTHU.query(keyword);

				if (rows != null) {
					if (rows.isEmpty()) {
						notifyStatus(self.handler, REFRESH_DIALOG,
								getResources().getString(R.string.msg_no_data));
					} else {
						notifyStatus(self.handler, REFRESH_RESULT, rows);
					}
				} else {
					notifyStatus(self.handler, REFRESH_DIALOG, getResources()
							.getString(R.string.msg_failed));
				}

				notifyStatus(self.handler, TOGGLE_QUERY_BUTTON, true);
			}
		});

		background.start();
	}

	protected void queryIP(final String ipv4) {
		final FindNTHUActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, TOGGLE_IP_QUERY_BUTTON, false);

				String ips[] = ipv4.split("\\D+");

				if (ips.length > 1) {
					int i = ips.length - 2;
					int j = ips.length - 1;
					String url = "";
					try {
						url = "http://service.oz.nthu.edu.tw/cgi-bin/ipquery.pl?ip3="
								+ URLEncoder.encode(ips[i], "UTF-8")
								+ "&ip4="
								+ URLEncoder.encode(ips[j], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						//
					}

					notifyStatus(self.handler, REFRESH_WEBVIEW, url);
				}

				notifyStatus(self.handler, TOGGLE_IP_QUERY_BUTTON, true);
			}
		});

		background.start();
	}
}
