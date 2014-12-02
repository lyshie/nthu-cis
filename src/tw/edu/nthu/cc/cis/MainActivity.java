package tw.edu.nthu.cc.cis;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Document;

import tw.edu.nthu.cc.cis.R;
import tw.edu.nthu.cc.r309.AIS;
import tw.edu.nthu.cc.r309.SimpleHTTPUtils;
import tw.edu.nthu.cc.r309.WifiHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

@SuppressLint("NewApi")
public class MainActivity extends AutoSaveStateActionBarActivity {

	private AIS ais = new AIS();

	private WifiHelper wifiHelper = new WifiHelper(this);

	protected static final int REFRESH_STATUS = 0x00000001;

	protected static final int REFRESH_IMAGE = 0x00000002;

	protected static final int REFRESH_CAPTCHA = 0x00000003;

	protected static final int REFRESH_PHOTO = 0x00000004;

	protected static final int REFRESH_STATUS_AND_DIALOG = 0x00000005;

	protected static final int TOGGLE_LOGIN_BUTTON = 0x00000006;

	protected static final int REFRESH_DIALOG_WAIT = 0x00000007;

	protected static final int REFRESH_TOAST = 0x00000008;

	protected static final long sessionMaxAliveSeconds = 600;

	private ProgressDialog dialogWait;

	@SuppressLint("HandlerLeak")
	protected Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Object result = null;

			result = msg.obj;

			switch (msg.what) {
			case REFRESH_TOAST:
				toastNotify((String) result);
				Bitmap icon = null;
				ImageView image = (ImageView) findViewById(R.id.imageViewPhoto);
				if (image instanceof ImageView) {
					Drawable drawable = image.getDrawable();
					if (drawable instanceof BitmapDrawable) {
						icon = ((BitmapDrawable) drawable).getBitmap();

						if (icon != null) {
							icon = Bitmap
									.createScaledBitmap(
											icon,
											(int) getResources()
													.getDimension(
															android.R.dimen.notification_large_icon_width),
											(int) getResources()
													.getDimension(
															android.R.dimen.notification_large_icon_height),
											false);
						}
					}
				}

				if (icon != null) {
					displayNotification(
							getResources().getString(R.string.app_name),
							(String) result, icon);
				}
				break;
			case REFRESH_DIALOG_WAIT:
				dialogWait = ProgressDialog.show(MainActivity.this, "",
						getResources().getString(R.string.msg_loading), false,
						true);
				break;
			case TOGGLE_LOGIN_BUTTON:
				if (dialogWait != null)
					dialogWait.dismiss();

				Button loginButton = (Button) findViewById(R.id.buttonLogin);
				if (loginButton != null && result != null)
					loginButton.setEnabled((boolean) result);
				break;
			case REFRESH_STATUS:
				setTextById(R.id.textViewStatus, (String) result);
				break;
			case REFRESH_STATUS_AND_DIALOG:
				if (dialogWait != null)
					dialogWait.dismiss();

				setTextById(R.id.textViewStatus, (String) result);
				showDialog(getResources().getString(R.string.label_status),
						(String) result);
				break;
			case REFRESH_IMAGE:
				if (result != null)
					setImageById(R.id.imageViewCaptcha, (byte[]) result);
				break;
			case REFRESH_CAPTCHA:
				setTextById(R.id.editTextCaptcha, (String) result);
				break;
			case REFRESH_PHOTO:
				if (result != null)
					setImageById(R.id.imageViewPhoto, (byte[]) result);
				break;
			default:
			}
		}
	};

	protected void cleanUpChildViews(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);

			if (v instanceof ViewGroup) {
				cleanUpChildViews((ViewGroup) v);
			} else if (v instanceof TextView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					((TextView) v).setText("");
				}
			} else if (v instanceof ImageView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					setImageByView((ImageView) v, new byte[0]);
				}
			}
		}
	}

	@SuppressLint("DefaultLocale")
	protected void fetchCaptcha() {
		final MainActivity self = this;
		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				final EditText account = (EditText) findViewById(R.id.editTextAccount);

				SimpleHTTPUtils.doParallelTasks(new Runnable() {
					@Override
					public void run() {
						// get user photo and display
						byte[] imgPhoto = ais.getPersonalImage(account
								.getText().toString());
						notifyStatus(self.handler, REFRESH_PHOTO, imgPhoto);
					}
				}, new Runnable() {
					@Override
					public void run() {
						// get pwdstr code
						String pwdstr = ais.getNewPwdStr();
						notifyStatus(
								self.handler,
								REFRESH_STATUS,
								pwdstr.equals("") ? getResources().getString(
										R.string.msg_no_data) : pwdstr);

						// fetch captcha image and display
						byte[] imgCaptcha = ais.getCaptchaImage(pwdstr);
						notifyStatus(self.handler, REFRESH_IMAGE, imgCaptcha);

						// get the answer of captcha image
						String answer = ais.decodeCaptchaImage(pwdstr, account
								.getText().toString());
						notifyStatus(self.handler, REFRESH_CAPTCHA, answer);

						if (imgCaptcha != null)
							notifyStatus(self.handler, TOGGLE_LOGIN_BUTTON,
									true);
						else
							notifyStatus(self.handler,
									REFRESH_STATUS_AND_DIALOG, getResources()
											.getString(R.string.msg_no_data));
					}
				});
			}
		});

		background.start();
	}

	@SuppressLint("DefaultLocale")
	protected void loginCCXP() {
		final MainActivity self = this;

		Thread background = new Thread(new Runnable() {
			@Override
			public void run() {
				// login CCXP
				notifyStatus(self.handler, TOGGLE_LOGIN_BUTTON, false);
				notifyStatus(self.handler, REFRESH_DIALOG_WAIT, null);

				ais.login(((EditText) findViewById(R.id.editTextAccount))
						.getText().toString(),
						((EditText) findViewById(R.id.editTextPassword))
								.getText().toString(),
						((EditText) findViewById(R.id.editTextCaptcha))
								.getText().toString(), ais.getPwdStr());

				if (ais.isLogin()) {
					// get session key
					String session = ais.getSessionKey();
					self.setPreferenceKey("session", session);
					notifyStatus(self.handler, REFRESH_STATUS_AND_DIALOG,
							getResources().getString(R.string.msg_logged_in));
				} else {
					notifyStatus(self.handler, REFRESH_STATUS_AND_DIALOG,
							getResources().getString(R.string.msg_failed));
				}
			}
		});

		background.start();
	}

	public void onButtonClick(View v) {
		switch (v.getId()) {
		case R.id.buttonFetch:
			fetchCaptcha();
			break;
		case R.id.buttonLogin:
			loginCCXP();
			break;
		default:
		}
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null)
			getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.container,
							new PlaceholderFragment(R.layout.fragment_main))
					.commit();

		// should I apply autologin
		int autologin = Integer.parseInt(getPreferenceKey("autologin_list",
				"-1"));
		if (autologin > 0) {
			wifiHelper.onCreateRegister();
			wifiHelper.setOnConnectedListener(new Runnable() {
				@Override
				public void run() {
					Thread background = new Thread(new Runnable() {
						@Override
						public void run() {
							// show connected notification
							notifyStatus(handler, REFRESH_TOAST, String.format(
									getResources().getString(
											R.string.msg_wifi_connected),
									wifiHelper.wifiAPName));

							// auto submit account and password to login
							String account = getPreferenceKey("account_text")
									+ "";
							String password = getPreferenceKey("wifi_password_text")
									+ "";

							HashMap<String, String> params = new HashMap<String, String>();
							params.put("user", account.toLowerCase());
							params.put("password", password);
							params.put("Login", "Login");
							Document doc = SimpleHTTPUtils
									.httpRequest(
											"https://securelogin.arubanetworks.com/auth/index.html/u",
											SimpleHTTPUtils.METHOD_POST, params);

							if (doc != null)
								notifyStatus(handler,
										REFRESH_STATUS_AND_DIALOG, doc.text());
						}
					});
					background.start();
				}
			});
		}

		this.isMainActivity = true;
		this.previousActivity = EDocActivity.class;
		this.nextActivity = MailingListActivity.class;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			showActivity(SettingsActivity.class);
			return true;
		} else if (id == R.id.action_wifi) {
			wifiHelper.wifiAutoLogin();
			return true;
		} else if (id == R.id.action_find_nthu) {
			showActivity(FindNTHUActivity.class);
			return true;
		} else if (id == R.id.action_eip) {
			showActivity(EIPActivity.class);
			return true;
		} else if (id == R.id.action_edoc) {
			showActivity(EDocActivity.class);
			return true;
		} else if (id == R.id.action_mailing_list) {
			showActivity(MailingListActivity.class);
			return true;
		} else if (id == R.id.action_about) {
			showDialog(getResources().getString(R.string.title_activity_about),
					getResources().getString(R.string.label_author_info), true);
			return true;
		} else if (id == R.id.action_update) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(getResources().getString(
					R.string.apk_download_url)));
			startActivity(intent);
			return true;
		} else if (id == R.id.action_cleanup) {
			// clean up child views
			cleanUpChildViews((ViewGroup) getWindow().getDecorView());

			// clean up preferences
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			prefs.edit().clear().commit();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();

		int autologin = Integer.parseInt(getPreferenceKey("autologin_list",
				"-1"));
		if (autologin > 0)
			wifiHelper.onPauseRegister();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setScrollingMovement();

		restoreSession();

		EditText edit = (EditText) findViewById(R.id.editTextAccount);
		strictEditText(edit);
	}

	@Override
	protected void onResume() {
		super.onResume();

		int autologin = Integer.parseInt(getPreferenceKey("autologin_list",
				"-1"));
		if (autologin > 0)
			wifiHelper.onResumeRegister();
	}

	protected void restoreSession() {
		if (!getPreferenceKey("session", "").equals("")) {
			long current = System.currentTimeMillis();
			long session_time = Long.parseLong(getPreferenceKey("session-time",
					"0"));

			if (TimeUnit.SECONDS.toSeconds(current - session_time) < sessionMaxAliveSeconds) {
				notifyStatus(handler, REFRESH_STATUS,
						getResources().getString(R.string.msg_logged_in));
			}
		}
	}

	protected void setImageById(int id, byte[] bytes) {
		ImageView img = (ImageView) findViewById(id);
		Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

		if (img != null)
			img.setImageBitmap(bm);
	}

	@Override
	protected boolean setPreferenceKey(String key, String value) {
		if (key.equals("session")) {
			long current = System.currentTimeMillis();
			super.setPreferenceKey("session-time", current + "");
		}

		return super.setPreferenceKey(key, value);
	}

	protected void setScrollingMovement() {
		TextView status = (TextView) findViewById(R.id.textViewStatus);
		status.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	protected void setTextById(int id, String text) {
		TextView v = (TextView) findViewById(id);
		v.setText(text);
	}

	public static class AlphaNumericTextWatcher implements TextWatcher {
		// prevent stack overflow (recursively called)
		private boolean mSelfChange = false;

		@Override
		public synchronized void afterTextChanged(Editable s) {
			if (mSelfChange)
				return;

			mSelfChange = true; // synchronized: only one run into this
								// session

			String str = s.toString();
			str = str.replaceAll("[^0-9A-Za-z]", "");
			s.replace(0, s.length(), str);

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
			edit.addTextChangedListener(new AlphaNumericTextWatcher());
		}
	}
}