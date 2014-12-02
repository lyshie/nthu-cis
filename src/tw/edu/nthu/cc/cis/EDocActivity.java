package tw.edu.nthu.cc.cis;

import java.util.ArrayList;

import tw.edu.nthu.cc.cis.R;
import tw.edu.nthu.cc.r309.AIS;
import tw.edu.nthu.cc.r309.EDocument;
import tw.edu.nthu.cc.r309.SimpleDOMUtils;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

@SuppressLint("NewApi")
public class EDocActivity extends AutoSaveStateActionBarActivity {

	private EDocument edoc = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edoc);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container,
							new PlaceholderFragment(R.layout.fragment_edoc))
					.commit();
		}

		this.previousActivity = EIPActivity.class;
		this.nextActivity = MainActivity.class;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setScrollingMovement();
		getDocuments();
	}

	protected void setScrollingMovement() {
		TextView status = (TextView) findViewById(R.id.textViewDocuments);
		status.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edoc, menu);
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

	protected static final int REFRESH_DOCUMENTS = 0x00000001;
	protected static final int REFRESH_DIALOG_WAIT = 0x00000002;
	protected static final int REFRESH_DIALOG = 0x00000003;
	protected static final int REFRESH_WEBVIEW = 0x00000004;
	protected static final int REFRESH_DOCUMENTS_AND_TOAST = 0x00000005;

	private ProgressDialog dialogWait;

	@SuppressLint({ "HandlerLeak", "SetJavaScriptEnabled" })
	protected Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Object result = msg.obj;

			switch (msg.what) {
			case REFRESH_DIALOG_WAIT:
				dialogWait = ProgressDialog.show(EDocActivity.this, "",
						getResources().getString(R.string.msg_loading), false,
						true);
				break;
			case REFRESH_DOCUMENTS:
				if (dialogWait != null)
					dialogWait.dismiss();

				setTextById(R.id.textViewDocuments,
						Html.fromHtml((String) result));
				break;
			case REFRESH_DOCUMENTS_AND_TOAST:
				if (dialogWait != null)
					dialogWait.dismiss();

				setTextById(R.id.textViewDocuments,
						Html.fromHtml((String) result));
				toastNotify(getResources().getString(
						R.string.click_to_open_edoc));
				break;
			case REFRESH_DIALOG:
				String message = (String) result;
				showDialog(
						getResources().getString(R.string.title_activity_edoc),
						message);
				break;
			case REFRESH_WEBVIEW:
				String url = (String) result + "";

				if (!url.equals("")) {
					WebView wv = (WebView) findViewById(R.id.webViewEDOC);

					if (wv != null) {
						// cookies (CookieSyncManager, CookieManager)
						if (edoc != null && edoc.cookies != null) {
							CookieSyncManager csm = CookieSyncManager
									.createInstance(wv.getContext());
							csm.sync();

							CookieManager cm = CookieManager.getInstance();
							cm.setAcceptCookie(true);
							cm.removeSessionCookie();

							// small hack
							SystemClock.sleep(1000);

							String cookie = "";
							for (String key : edoc.cookies.keySet()) {
								cookie += String.format("%s=%s;", key,
										edoc.cookies.get(key));
							}

							cm.setCookie("http://document-doc.vm.nthu.edu.tw",
									cookie);

							CookieSyncManager.getInstance().sync();
						}

						// WebView
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

						View layout_wv = findViewById(R.id.layoutWebViewEDOC);
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
					.show(EDocActivity.this, "",
							getResources().getString(R.string.msg_loading),
							false, true);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (dialogWait != null)
				dialogWait.dismiss();
		}
	};

	protected void getDocuments() {
		final EDocActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				String account = self.getPreferenceKey("account_text", "");
				String session = self.getPreferenceKey("session", "");

				// get AIS and create new EDocument
				if (!account.equals("") && !session.equals("")) {
					AIS ais = new AIS(account, session);
					if (self.edoc == null) {
						self.edoc = new EDocument(ais.getRelativeMenu().get(
								AIS.TOKEN_EDOCUMENT));
					}
				}

				// login and fetch
				if (self.edoc != null) {
					self.edoc.login();

					if (self.edoc.isLogin()) {
						String msg = "";

						ArrayList<ArrayList<ArrayList<String>>> dashboard = self.edoc
								.getDashboard();

						if (dashboard != null) {
							for (ArrayList<String> row : dashboard.get(0)) {
								for (String item : row) {
									item = SimpleDOMUtils.normalizeNumber(item
											.replaceAll("\\s", ""));
									item = item
											.replaceAll("(\\d+)",
													"<font color=\"#ff0000\">$1</font>");
									msg += item;
								}
								msg += "<br>";
							}
						}

						notifyStatus(self.handler, REFRESH_DOCUMENTS_AND_TOAST,
								msg);
					} else {
						notifyStatus(self.handler, REFRESH_DOCUMENTS,
								getResources().getString(R.string.msg_failed));
					}
				} else {
					notifyStatus(self.handler, REFRESH_DOCUMENTS,
							getResources().getString(R.string.msg_failed));
				}
			}
		});

		background.start();
	}

	@Override
	public void onBackPressed() {
		View layout_wv = findViewById(R.id.layoutWebViewEDOC);

		if (layout_wv != null) {
			if (layout_wv.getVisibility() != View.VISIBLE)
				finish();
			else {
				WebView wv = (WebView) findViewById(R.id.webViewEDOC);

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

	public void onButtonClick(View v) {
		final EDocActivity self = this;

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					String url = "" + self.edoc.url;
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					try {
						startActivity(i);
					} catch (Exception e) {
						// TODO
					}
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					notifyStatus(handler, REFRESH_WEBVIEW,
							EDocument.urlDashboard);
					break;
				}
			}
		};

		switch (v.getId()) {
		case R.id.textViewDocuments:
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);

			if (dialog != null) {
				dialog.setMessage(
						getResources().getString(
								R.string.msg_open_external_browser))
						.setPositiveButton(
								getResources().getString(R.string.msg_yes),
								listener)
						.setNegativeButton(
								getResources().getString(R.string.msg_no),
								listener).show();
			}
			break;
		default:
		}
	}
}
