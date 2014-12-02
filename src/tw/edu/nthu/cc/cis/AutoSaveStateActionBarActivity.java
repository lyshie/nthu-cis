package tw.edu.nthu.cc.cis;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import tw.edu.nthu.cc.cis.R;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "NewApi", "ValidFragment" })
public class AutoSaveStateActionBarActivity extends ActionBarActivity {

	@SuppressLint("NewApi")
	public static class CustomDialogFragment extends DialogFragment {
		public static CustomDialogFragment newInstance(String title,
				String msg, boolean qrcode) {
			CustomDialogFragment dialog = new CustomDialogFragment(title, msg,
					qrcode);
			return dialog;
		}

		private String title;
		private String msg;
		private boolean qrcode;

		public CustomDialogFragment(String title, String msg, boolean qrcode) {
			this.title = title;
			this.msg = msg;
			this.qrcode = qrcode;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			this.getDialog().setTitle(this.title);

			View v = inflater
					.inflate(R.layout.fragment_about, container, false);

			TextView msgView = (TextView) v.findViewById(R.id.textViewMessage);
			msgView.setText(this.msg);

			if (!qrcode) {
				ImageView imgView = (ImageView) v
						.findViewById(R.id.imageViewQR);
				imgView.setVisibility(View.GONE);
			}

			final Dialog dialog = this.getDialog();
			Button buttonOK = (Button) v.findViewById(R.id.buttonOK);
			buttonOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

			return v;
		}
	}

	final class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			// check if not vertical scrolling
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;

			// right to left swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				// do your code
				overridePendingTransition(R.anim.slide_in_right,
						R.anim.slide_out_left);
				showActivity(nextActivity);

				if (!isMainActivity) {
					overridePendingTransition(R.anim.slide_in_right,
							R.anim.slide_out_left);
					AutoSaveStateActionBarActivity.this.finish();
				}
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				// left to right flip
				overridePendingTransition(R.anim.slide_in_left,
						R.anim.slide_out_right);
				showActivity(previousActivity);

				if (!isMainActivity) {
					overridePendingTransition(R.anim.slide_in_left,
							R.anim.slide_out_right);
					AutoSaveStateActionBarActivity.this.finish();
				}
			}

			return false;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	@SuppressLint("ValidFragment")
	public class PlaceholderFragment extends Fragment {
		int resource;

		public PlaceholderFragment(int resource) {
			this.resource = resource;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(this.resource, container, false);

			return rootView;
		}
	}

	private static final int SWIPE_MIN_DISTANCE = 60;
	private static final int SWIPE_MAX_OFF_PATH = 250;

	private static final int SWIPE_THRESHOLD_VELOCITY = 50;

	protected static int[] averageColor(Bitmap orig) {
		int cs[] = new int[orig.getHeight()];

		// the left-most vertical line
		for (int y = 0; y < orig.getHeight(); y++) {
			cs[y] = orig.getPixel(0, y);
		}

		// red, green, blue, threshold
		int[] colors = new int[4];

		// red
		colors[0] = 0;
		for (int y = 0; y < orig.getHeight(); y++) {
			colors[0] += Color.red(cs[y]);
		}
		colors[0] = colors[0] / orig.getHeight();

		// green
		colors[1] = 0;
		for (int y = 0; y < orig.getHeight(); y++) {
			colors[1] += Color.green(cs[y]);
		}
		colors[1] = colors[1] / orig.getHeight();

		// blue
		colors[2] = 0;
		for (int y = 0; y < orig.getHeight(); y++) {
			colors[2] += Color.blue(cs[y]);
		}
		colors[2] = colors[2] / orig.getHeight();

		// threshold
		int th_r = Math.abs(Color.red(cs[0])
				- Color.red(cs[orig.getHeight() - 1])) / 2;
		int th_g = Math.abs(Color.green(cs[0])
				- Color.green(cs[orig.getHeight() - 1])) / 2;
		int th_b = Math.abs(Color.blue(cs[0])
				- Color.blue(cs[orig.getHeight() - 1])) / 2;

		colors[3] = th_r;

		if (th_g > colors[3])
			colors[3] = th_g;

		if (th_b > colors[3])
			colors[3] = th_b;

		return colors;
	}

	protected static Bitmap removeBackground(Bitmap orig) {
		// mutable
		Bitmap dest = null;
		dest = orig.copy(orig.getConfig(), true);

		if (dest == null) {
			return orig;
		}

		int[] avg = averageColor(orig);
		double factor = 1.414;
		int threshold = (int) (avg[3] / factor);

		// left-to-right
		for (int y = 0; y < orig.getHeight(); y++) {
			for (int x = 0; x < orig.getWidth() / 2; x++) {
				int c = orig.getPixel(x, y);

				if (Math.abs((Color.red(c) - avg[0])) < threshold
						&& Math.abs((Color.green(c) - avg[1])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else if (Math.abs((Color.green(c) - avg[1])) < threshold
						&& Math.abs((Color.blue(c) - avg[2])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else if (Math.abs((Color.red(c) - avg[0])) < threshold
						&& Math.abs((Color.blue(c) - avg[2])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else {
					// more white
					int r = (int) (Color.red(c) * factor);
					int g = (int) (Color.green(c) * factor);
					int b = (int) (Color.blue(c) * factor);

					r = r > 255 ? 255 : r;
					g = g > 255 ? 255 : g;
					b = b > 255 ? 255 : b;

					dest.setPixel(x, y, Color.rgb(r, g, b));

					break;
				}
			}
		}

		// right-to-left
		for (int y = 0; y < orig.getHeight(); y++) {
			for (int x = orig.getWidth() - 1; x >= orig.getWidth() / 2; x--) {
				int c = orig.getPixel(x, y);

				if (Math.abs((Color.red(c) - avg[0])) < threshold
						&& Math.abs((Color.green(c) - avg[1])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else if (Math.abs((Color.green(c) - avg[1])) < threshold
						&& Math.abs((Color.blue(c) - avg[2])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else if (Math.abs((Color.red(c) - avg[0])) < threshold
						&& Math.abs((Color.blue(c) - avg[2])) < threshold) {
					dest.setPixel(x, y, Color.rgb(255, 255, 255));
				} else {
					// more white
					int r = (int) (Color.red(c) * factor);
					int g = (int) (Color.green(c) * factor);
					int b = (int) (Color.blue(c) * factor);

					r = r > 255 ? 255 : r;
					g = g > 255 ? 255 : g;
					b = b > 255 ? 255 : b;

					dest.setPixel(x, y, Color.rgb(r, g, b));

					break;
				}
			}
		}

		return dest;
	}

	public Class<?> previousActivity;

	public Class<?> nextActivity;

	public boolean isMainActivity;

	private NotificationCompat.Builder mBuilder;

	private NotificationCompat.InboxStyle inboxStyle;

	protected void clearPreference(String... keys) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		if (keys != null) {
			for (int i = 0; i < keys.length; i++) {
				prefs.edit().remove(keys[i]);
			}
			prefs.edit().commit();
		} else {
			prefs.edit().clear().commit();
		}
	}

	protected void displayNotification(String title, String text, Bitmap icon) {
		if (mBuilder == null) {
			mBuilder = new NotificationCompat.Builder(this);
		}

		if (inboxStyle == null && mBuilder != null) {
			inboxStyle = new NotificationCompat.InboxStyle(mBuilder);
		}

		if (mBuilder == null || inboxStyle == null)
			return;

		Intent resultIntent = new Intent(this, this.getClass());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(this.getClass());
		stackBuilder.addNextIntent(resultIntent);

		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_ONE_SHOT);

		mBuilder.setSmallIcon(R.drawable.ic_launcher).setContentTitle(title)
				.setContentText(text).setContentIntent(resultPendingIntent)
				.setAutoCancel(true);

		if (icon != null)
			mBuilder.setLargeIcon(icon);

		inboxStyle.setBigContentTitle(title).addLine(text);
		mBuilder.setStyle(inboxStyle);

		int notifyID = 0;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(notifyID, inboxStyle.build());
	}

	protected byte[] getImageByView(ImageView img) {
		byte[] result = null;

		Bitmap bm = null;
		Drawable drawable = img.getDrawable();

		if (drawable instanceof BitmapDrawable) {
			bm = ((BitmapDrawable) drawable).getBitmap();

			if (bm != null) {
				ByteArrayOutputStream b = new ByteArrayOutputStream();
				bm.compress(Bitmap.CompressFormat.PNG, 100, b);
				result = b.toByteArray();
			}
		}

		return result;
	}

	protected byte[] getPhotoFile(String filename) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		FileInputStream in = null;

		try {
			in = openFileInput(filename);
			int n;
			while ((n = in.read(buffer)) >= 0) {
				out.write(buffer, 0, n);
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return out.toByteArray();
	}

	protected String getPreferenceKey(String name) {
		return getPreferenceKey(name, "[" + name + "]");
	}

	protected String getPreferenceKey(String name, String value) {
		String result;

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		result = prefs.getString(name, value);

		return result;
	}

	protected void notifyStatus(Handler handler, int what, Object obj) {
		handler.obtainMessage(what, obj).sendToTarget();
	}

	@Override
	protected void onPause() {
		super.onPause();

		ViewGroup vg = (ViewGroup) getWindow().getDecorView();

		saveChildViews(vg);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		ViewGroup vg = (ViewGroup) getWindow().getDecorView();

		restoreChildViews(vg);

		// gesture (fling) on ActionBar
		GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();
		final GestureDetector gd = new GestureDetector(
				AutoSaveStateActionBarActivity.this, gestureListener);

		View.OnTouchListener vListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				gd.onTouchEvent(event);
				return false;
			}
		};

		setViewOnTouchListener(vg, vListener);
	}

	protected void restoreChildViews(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);

			if (v instanceof ViewGroup) {
				restoreChildViews((ViewGroup) v);
			} else if (v instanceof TextView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					if (tag.contains(":html")) {
						((TextView) v).setText(Html.fromHtml(getPreferenceKey(
								tag, "")));
					} else {
						String value = getPreferenceKey(tag, "");
						((TextView) v).setText(value);
					}
				}
			} else if (v instanceof ImageView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					String data = getPreferenceKey(tag, "");

					if (!data.equals("")) {
						setImageByView((ImageView) v,
								Base64.decode(data, Base64.DEFAULT));
					}
				}
			}
		}
	}

	protected void saveChildViews(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);

			if (v instanceof ViewGroup) {
				saveChildViews((ViewGroup) v);
			} else if (v instanceof TextView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					if (tag.contains(":html")) {
						setPreferenceKey(tag,
								Html.toHtml((Spanned) ((TextView) v).getText()));
					} else {
						setPreferenceKey(tag, ((TextView) v).getText()
								.toString());
					}
				}
			} else if (v instanceof ImageView) {
				String tag = (String) v.getTag();

				if (tag != null && !tag.equals("")) {
					byte[] data = getImageByView((ImageView) v);

					if (data != null) {
						setPreferenceKey(tag,
								Base64.encodeToString(data, Base64.DEFAULT));
					}
				}
			}
		}
	}

	protected void setImageByView(ImageView img, byte[] bytes) {
		Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		img.setImageBitmap(bm);
	}

	protected void setPhotoFile(String filename, byte[] data) {
		FileOutputStream out = null;

		try {
			out = openFileOutput(filename, Context.MODE_PRIVATE);
			out.write(data, 0, data.length);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected boolean setPreferenceKey(String key, String value) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.edit().putString(key, value).commit();
	}

	protected void setTextById(int id, Spanned text) {
		if (text != null) {
			TextView v = (TextView) findViewById(id);
			if (v != null)
				v.setText(text);
		}
	}

	protected void setTextById(int id, String text) {
		TextView v = (TextView) findViewById(id);
		if (v != null)
			v.setText(text + "");
	}

	protected void setViewOnTouchListener(ViewGroup vg, View.OnTouchListener vl) {
		if (vg instanceof View && vg.getClass().getName().contains("ActionBar")) {
			vg.setOnTouchListener(vl);
		}

		for (int i = 0; i < vg.getChildCount(); i++) {
			View v = vg.getChildAt(i);

			if (v instanceof ViewGroup) {
				setViewOnTouchListener((ViewGroup) v, vl);
			}
		}
	}

	public void showActivity(Class<?> c) {
		Intent intent = new Intent(this, c);
		startActivity(intent);
	}

	protected void showDialog(String title, String msg) {
		showDialog(title, msg, false);
	}

	protected void showDialog(String title, String msg, boolean qrcode) {
		CustomDialogFragment dialog = CustomDialogFragment.newInstance(title,
				msg, qrcode);

		try {
			dialog.show(getFragmentManager(), "dialog");
		} catch (Exception e) {
			// TODO
		}
	}

	protected void toastNotify(String text) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
}
