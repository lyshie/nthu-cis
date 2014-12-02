package tw.edu.nthu.cc.r309;

import java.util.List;

import tw.edu.nthu.cc.cis.AutoSaveStateActionBarActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;

public class WifiHelper {
	private boolean isReceiverRegistered;
	private boolean isConnectivityReceiverRegistered;
	private AutoSaveStateActionBarActivity who;
	private Runnable onConnectedListener;
	private Runnable onEnabledListener;
	private Runnable onWifiAutoScanListener;
	private Runnable onWifiScanResultListener;
	public String wifiAPName = "nthu-cc";

	private BroadcastReceiver WifiStateChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int extraWifiState = intent.getIntExtra(
					WifiManager.EXTRA_WIFI_STATE,
					WifiManager.WIFI_STATE_UNKNOWN);

			if (extraWifiState == WifiManager.WIFI_STATE_ENABLED
					|| extraWifiState == WifiManager.WIFI_STATE_ENABLING) {
				if (onEnabledListener != null)
					onEnabledListener.run();
				wifiAutoScan();
			}
		}
	};

	private BroadcastReceiver WifiScanResultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			WifiManager wfm = (WifiManager) who
					.getSystemService(Context.WIFI_SERVICE);
			if (wfm == null)
				return;

			List<ScanResult> results = wfm.getScanResults();
			if (results == null)
				return;

			if (onWifiScanResultListener != null)
				onWifiScanResultListener.run();

			for (ScanResult result : results) {
				if (wifiAPName.equals(result.SSID)) {
					WifiConfiguration wfc = new WifiConfiguration();
					wfc.SSID = "\"" + result.SSID + "\"";
					wfc.allowedKeyManagement.set(KeyMgmt.NONE);
					wfc.status = WifiConfiguration.Status.ENABLED;

					int netId = wfm.addNetwork(wfc);
					wfm.saveConfiguration();
					wfm.enableNetwork(netId, true);
					wfm.reconnect();

					safeConnectivityReceiverRegister();
					break;
				}
			}
		}
	};

	private BroadcastReceiver ConnectivityReceiver = new BroadcastReceiver() {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras() != null) {
				NetworkInfo ni = (NetworkInfo) intent.getExtras().get(
						ConnectivityManager.EXTRA_NETWORK_INFO);
				if (ni != null
						&& (ni.isConnected() || ni.isConnectedOrConnecting())) {
					// run callback function after connected
					if (onConnectedListener != null) {
						onConnectedListener.run();
					}

					safeConnectivityReceiverUnregister();
				}
			}
		}
	};

	public WifiHelper(AutoSaveStateActionBarActivity who) {
		this.who = who;
	}

	public void onCreateRegister() {
		safeReceiverRegister();
	}

	public void onPauseRegister() {
		safeReceiverUnregister();
		safeConnectivityReceiverUnregister();
	}

	public void onResumeRegister() {
		safeReceiverRegister();
	}

	private void safeConnectivityReceiverRegister() {
		if (!isConnectivityReceiverRegistered) {
			who.registerReceiver(ConnectivityReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));

			isConnectivityReceiverRegistered = true;
		}
	}

	private void safeConnectivityReceiverUnregister() {
		if (isConnectivityReceiverRegistered) {
			try {
				who.unregisterReceiver(ConnectivityReceiver);
			} catch (Exception e) {
				// TODO
			}

			isConnectivityReceiverRegistered = false;
		}
	}

	private void safeReceiverRegister() {
		if (!isReceiverRegistered) {
			who.registerReceiver(WifiStateChangedReceiver, new IntentFilter(
					WifiManager.WIFI_STATE_CHANGED_ACTION));
			who.registerReceiver(WifiScanResultReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

			isReceiverRegistered = true;
		}
	}

	private void safeReceiverUnregister() {
		if (isReceiverRegistered) {
			try {
				who.unregisterReceiver(WifiStateChangedReceiver);
				who.unregisterReceiver(WifiScanResultReceiver);
			} catch (Exception e) {
				// TODO
			}

			isReceiverRegistered = false;
		}
	}

	public void setOnConnectedListener(Runnable onConnectedListener) {
		this.onConnectedListener = onConnectedListener;
	}

	public void setOnEnabledListener(Runnable onEnabledListener) {
		this.onEnabledListener = onEnabledListener;
	}

	public void setOnWifiAutoScanListener(Runnable onWifiAutoScanListener) {
		this.onWifiAutoScanListener = onWifiAutoScanListener;
	}

	public void setOnWifiScanResultListener(Runnable onWifiScanResultListener) {
		this.onWifiScanResultListener = onWifiScanResultListener;
	}

	public void wifiAutoLogin() {
		ConnectivityManager conMgr = (ConnectivityManager) who
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wfm = (WifiManager) who
				.getSystemService(Context.WIFI_SERVICE);

		if (conMgr == null || wfm == null)
			return;

		if ((conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
				|| (conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.getState() == NetworkInfo.State.CONNECTING)) { // wifi
			// connected
			safeConnectivityReceiverRegister();
		} else {
			if (!wfm.isWifiEnabled()) {
				if ((wfm.getWifiState() != WifiManager.WIFI_STATE_ENABLED)
						|| (wfm.getWifiState() != WifiManager.WIFI_STATE_ENABLING))
					wfm.setWifiEnabled(true);
			} else {
				wifiAutoScan();
			}
		}
	}

	private void wifiAutoScan() {
		ConnectivityManager conMgr = (ConnectivityManager) who
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wfm = (WifiManager) who
				.getSystemService(Context.WIFI_SERVICE);

		if (conMgr == null || wfm == null)
			return;

		if (conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.DISCONNECTED
				|| conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.getState() == NetworkInfo.State.DISCONNECTING) { // wifi
			// disconnected

			// wifi scan
			if (onWifiAutoScanListener != null)
				onWifiAutoScanListener.run();

			wfm.startScan();
		}
	}

	/*
	 * private void wifiShowConnectionInfo() { WifiManager wfm = (WifiManager)
	 * who .getSystemService(Context.WIFI_SERVICE);
	 * 
	 * if (wfm != null) { WifiInfo wfi = wfm.getConnectionInfo();
	 * 
	 * if (wfi != null) { who.showDialog("Wifi", wfi.getSSID()); } } }
	 */
}
