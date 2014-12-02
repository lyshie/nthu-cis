package tw.edu.nthu.cc.cis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import tw.edu.nthu.cc.cis.R;
import tw.edu.nthu.cc.r309.MailingList;
import tw.edu.nthu.cc.r309.NetSysRecents;
import tw.edu.nthu.cc.r309.SimpleHTTPUtils;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MailingListActivity extends AutoSaveStateActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mailing_list);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container,
							new PlaceholderFragment(
									R.layout.fragment_mailing_list)).commit();
		}

		this.nextActivity = FindNTHUActivity.class;
		this.previousActivity = MainActivity.class;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		getMailingList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mailing_list, menu);
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

	protected static final int REFRESH_MAILING_LIST = 0x00000001;
	protected static final int REFRESH_WEBVIEW = 0x00000002;
	protected static final int REFRESH_DIALOG = 0x00000003;
	protected static final int REFRESH_DIALOG_WAIT = 0x00000004;
	private static final String FORMAT_PUBLISH_ARTICLE = MailingList.urlBase
			+ "api/public/article/%s/publish/%s/html";
	private static ArrayList<ArrayList<String>> recents_list;

	private ProgressDialog dialogWait;

	@SuppressLint({ "HandlerLeak", "SetJavaScriptEnabled" })
	protected Handler handler = new Handler() {
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			Object result = msg.obj;

			switch (msg.what) {
			case REFRESH_DIALOG_WAIT:
				dialogWait = ProgressDialog.show(MailingListActivity.this, "",
						getResources().getString(R.string.msg_loading), false,
						true);
				break;
			case REFRESH_DIALOG:
				String message = (String) result;
				showDialog(
						getResources().getString(
								R.string.title_activity_mailing_list), message);
				break;
			case REFRESH_MAILING_LIST:
				if (dialogWait != null)
					dialogWait.dismiss();

				if (result instanceof ArrayList<?>) {
					@SuppressWarnings("unchecked")
					ArrayList<ArrayList<String>> rows = (ArrayList<ArrayList<String>>) result;

					// no data
					if (rows.size() > 0) {

						ArrayList<Spanned> arr = new ArrayList<Spanned>();

						for (ArrayList<String> row : rows) {
							Spanned subject = Html.fromHtml(String.format(
									"<b>%s</b>", row.get(0)));
							Spanned right = Html
									.fromHtml(String
											.format("<br><font color=\"blue\"><small>%s</small></font>"
													+ "<br><font color=\"green\"><small>%s</small></font>",
													row.get(1), row.get(2)));
							SpannableString right_span = new SpannableString(
									right);

							right_span
									.setSpan(new AlignmentSpan.Standard(
											Alignment.ALIGN_OPPOSITE), 0, right
											.length(),
											Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

							arr.add((Spanned) TextUtils.concat(subject,
									right_span));
						}

						// set view and adapter
						ListView lv = (ListView) findViewById(R.id.listViewMailingList);

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

									ArrayList<String> row = recents_list
											.get(position);

									if (row != null) {
										String article = row.get(3);
										String[] a = article.split("/", 2);

										if (a != null && (a.length > 1)) {
											String url;
											if (a[1].startsWith("/")) {
												url = String
														.format(NetSysRecents.urlBase
																+ "%s?do=export_xhtml",
																a[1]);
											} else {
												url = String.format(
														FORMAT_PUBLISH_ARTICLE,
														a[0], a[1]);
											}

											notifyStatus(handler,
													REFRESH_WEBVIEW, url);
										}
									}
								}
							});
						}
					} else {
						notifyStatus(handler, REFRESH_DIALOG, getResources()
								.getString(R.string.msg_no_data));
					}
				}
				break;
			case REFRESH_WEBVIEW:
				String url = (String) result + "";

				if (!url.equals("")) {
					WebView wv = (WebView) findViewById(R.id.webViewMailingList);

					if (wv != null) {
						wv.setWebViewClient(mWebViewClient);
						wv.getSettings().setJavaScriptEnabled(true);
						wv.getSettings().setBuiltInZoomControls(true);
						wv.getSettings().setDisplayZoomControls(true);
						// wv.getSettings().setLoadWithOverviewMode(true);
						wv.getSettings().setUseWideViewPort(true);

						// download file and open external
						wv.setDownloadListener(new DownloadListener() {
							@Override
							public void onDownloadStart(String url,
									String userAgent,
									String contentDisposition, String mimetype,
									long contentLength) {

								Uri uri = Uri.parse(url);
								Intent intent = new Intent(Intent.ACTION_VIEW,
										uri);

								// failed-safe
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

						wv.loadUrl((String) result);

						View layout_wv = findViewById(R.id.layoutWebView);
						if (layout_wv != null)
							layout_wv.setVisibility(View.VISIBLE);
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
					.show(MailingListActivity.this, "", getResources()
							.getString(R.string.msg_loading), false, true);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (dialogWait != null)
				dialogWait.dismiss();
		}
	};

	protected void getMailingList() {
		final MailingListActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				NetSysRecents.init("recents");
				MailingList.init("recents", "profiles");

				final HashMap<String, HashMap<String, String>> recents = new HashMap<String, HashMap<String, String>>();
				final HashMap<String, HashMap<String, String>> profiles = new HashMap<String, HashMap<String, String>>();

				SimpleHTTPUtils.doParallelTasks(new Runnable() {
					@Override
					public void run() {
						recents.putAll(NetSysRecents.getRecents());
					}
				}, new Runnable() {
					@Override
					public void run() {
						recents.putAll(MailingList.getJSONMap(
								MailingList.urlRecents, "recents"));
					}
				}, new Runnable() {
					@Override
					public void run() {
						profiles.putAll(MailingList.getJSONMap(
								MailingList.urlProfiles, "profiles"));

					}
				});

				ArrayList<String> indexes = new ArrayList<String>();
				indexes.addAll(recents.keySet());
				Collections.sort(indexes, new Comparator<Object>() {
					@Override
					public int compare(Object o1, Object o2) {
						String t1 = recents.get(o1).get("prettydate")
								.replaceAll("\\D", "");
						String t2 = recents.get(o2).get("prettydate")
								.replaceAll("\\D", "");

						// if both have same date string, compare with its
						// integer index
						if (t2.equals(t1)) {
							Integer i1 = Integer.parseInt(((String) o1)
									.replaceAll("\\D", ""));
							Integer i2 = Integer.parseInt(((String) o2)
									.replaceAll("\\D", ""));

							return i1.compareTo(i2);
						}

						return t2.compareTo(t1);
					}
				});

				ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();

				// recents and profiles must not be empty
				if (!profiles.isEmpty()) {
					for (String index : indexes) {
						ArrayList<String> arrs = new ArrayList<String>();

						String name = recents.get(index).get("article")
								.replaceAll("/.*$", "");
						String listname = profiles.get(name).get("description");
						String subject = recents.get(index).get("subject");
						String prettydate = recents.get(index)
								.get("prettydate");

						arrs.add(0, subject);
						arrs.add(1, prettydate);
						arrs.add(2, listname);
						arrs.add(3, recents.get(index).get("article"));

						rows.add(arrs);
					}
				}

				recents_list = rows;
				notifyStatus(self.handler, REFRESH_MAILING_LIST, rows);
			}

		});

		background.start();
	}

	@Override
	public void onBackPressed() {
		View layout_wv = findViewById(R.id.layoutWebView);

		if (layout_wv != null) {
			if (layout_wv.getVisibility() != View.VISIBLE)
				finish();
			else {
				WebView wv = (WebView) findViewById(R.id.webViewMailingList);

				if (wv != null) {
					WebBackForwardList bf = wv.copyBackForwardList();
					String back_url = "about:blank";

					if (bf != null & bf.getCurrentIndex() > 0)
						back_url = bf.getItemAtIndex(bf.getCurrentIndex() - 1)
								.getUrl();

					if (wv.canGoBack() && !back_url.equals("about:blank")) {
						wv.goBack();
					} else {
						layout_wv.setVisibility(View.INVISIBLE);
						wv.loadUrl("about:blank");
					}
				}
			}
		} else {
			finish();
		}
	}
}
