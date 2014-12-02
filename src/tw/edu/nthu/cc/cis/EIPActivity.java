package tw.edu.nthu.cc.cis;

import java.util.ArrayList;

import tw.edu.nthu.cc.cis.R;
import tw.edu.nthu.cc.r309.AIS;
import tw.edu.nthu.cc.r309.EIP;
import tw.edu.nthu.cc.r309.SimpleDOMUtils;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class EIPActivity extends AutoSaveStateActionBarActivity {

	private EIP eip = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_eip);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container,
							new PlaceholderFragment(R.layout.fragment_eip))
					.commit();
		}

		this.previousActivity = FindNTHUActivity.class;
		this.nextActivity = EDocActivity.class;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setScrollingMovement();
		getSignTime();
	}

	protected void setScrollingMovement() {
		TextView status = (TextView) findViewById(R.id.textViewSignTime);
		status.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.eip, menu);
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

	protected static final int REFRESH_SIGNTIME = 0x00000001;
	protected static final int REFRESH_WEBVIEW = 0x00000002;
	protected static final int REFRESH_DIALOG_WAIT = 0x00000003;

	private ProgressDialog dialogWait;

	@SuppressLint("HandlerLeak")
	protected Handler handler = new Handler() {
		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			Object result = msg.obj;

			switch (msg.what) {
			case REFRESH_DIALOG_WAIT:
				dialogWait = ProgressDialog.show(EIPActivity.this, "",
						getResources().getString(R.string.msg_loading), false,
						true);
				break;
			case REFRESH_SIGNTIME:
				if (dialogWait != null)
					dialogWait.dismiss();

				setTextById(R.id.textViewSignTime,
						Html.fromHtml((String) result));
				break;
			case REFRESH_WEBVIEW:
				String url = (String) result + "";

				if (!url.equals("")) {
					WebView wv = (WebView) findViewById(R.id.webViewEIP);
					if (wv != null) {
						wv.getSettings().setBuiltInZoomControls(true);
						wv.getSettings().setDisplayZoomControls(true);
						// wv.getSettings().setSupportZoom(true);
						wv.setWebViewClient(mWebViewClient);
						wv.loadUrl((String) result);
					}
				}
				break;
			default:
			}
		}
	};

	WebViewClient mWebViewClient = new WebViewClient() {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	};

	protected void getSignTime() {
		final EIPActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				String account = self.getPreferenceKey("account_text", "");
				String session = self.getPreferenceKey("session", "");
				String url = null;

				// get AIS and create new EIP
				if (!account.equals("") && !session.equals("")) {
					AIS ais = new AIS(account, session);
					if (self.eip == null) {
						self.eip = new EIP(ais.getRelativeMenu().get(
								AIS.TOKEN_EIP));

						url = ais.getRelativeMenu().get(AIS.TOKEN_EIP);
					}
				}

				// login and fetch
				if (self.eip != null) {
					self.eip.login();

					if (self.eip.isLogin()) {
						String msg = "";

						ArrayList<ArrayList<String>> signtime = self.eip
								.getSignTime();
						ArrayList<ArrayList<String>> thisweek_signtime = self.eip
								.getThisWeekSignTime();
						ArrayList<ArrayList<String>> vacation = self.eip
								.getVacation();

						if (signtime != null) {
							msg += "<small><b>簽到時間</b><br>";
							for (ArrayList<String> row : signtime) {
								msg += SimpleDOMUtils.joinString(" ", row, 0)
										+ "<br>\n";
							}
							msg += "</small><br>";
						}

						if (thisweek_signtime != null) {
							msg += "<small><b>當週出勤紀錄</b><br>";
							for (ArrayList<String> row : thisweek_signtime) {
								if (row.get(2).equals(""))
									continue;

								row.set(1,
										"<font color=\"#ff0000\"><b>"
												+ row.get(1) + "</font></b>");
								msg += SimpleDOMUtils.joinString("<br>", row,
										1, 2, 3) + "<br><br>";
							}
							msg += "</small>";
						}

						if (vacation != null) {
							msg += "<small><b>休假統計</b><br>";
							for (ArrayList<String> row : vacation) {
								msg += SimpleDOMUtils
										.joinString(" ", row, 0, 1)
										.replaceAll("\\D+/\\D+", " 日 ")
										.replaceAll("(\\d+)",
												"<font color=\"red\">$1</font>")
										+ " 時" + "<br>\n";
							}
							msg += "</small>";
						}

						notifyStatus(self.handler, REFRESH_SIGNTIME, msg);
						notifyStatus(self.handler, REFRESH_WEBVIEW, url);
					} else {
						notifyStatus(self.handler, REFRESH_SIGNTIME,
								getResources().getString(R.string.msg_failed));
					}
				} else {
					notifyStatus(self.handler, REFRESH_SIGNTIME, getResources()
							.getString(R.string.msg_failed));
				}
			}
		});

		background.start();
	}
}
