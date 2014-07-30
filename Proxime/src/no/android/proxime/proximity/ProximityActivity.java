/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.android.proxime.proximity;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import no.android.proxime.profile.BleProfileService;
import no.android.proxime.profile.BleProfileServiceReadyActivity;
import no.android.proxime.proximity.ProximityManager;
import no.android.proxime.utility.DebugLogger;
import no.nordicsemi.android.log.Logger;
import no.android.proxime.R;
import android.app.FragmentManager;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ProximityActivity extends BleProfileServiceReadyActivity<ProximityService.ProximityBinder> {
	private static final String TAG = "ProximityActivity";

	public static final String PREFS_GATT_SERVER_ENABLED = "prefs_gatt_server_enabled";
	private static final String IMMEDIATE_ALERT_STATUS = "immediate_alert_status";
	private boolean isImmediateAlertOn = false;
	private Handler mHandler;

	private Button mFindMeButton;
	private Button mMonitor;
	public TextView mRSSI;
	private ImageView mLockImage;
	private CheckBox mGattServerSwitch;
	private ProximityManager mble;
	public int rssilevel = 0;
	public int monitorStop = 0;
	public int monitorvis = 1;
	public int errorcheck = 0;
	private ScheduledExecutorService mScheduleExecutorService;
	private ScheduledFuture mScheduleFuture;


	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_proximity);
		setGUI();
	}

	private void setGUI() {
		mFindMeButton = (Button) findViewById(R.id.action_findme);
		mMonitor = (Button) findViewById(R.id.monitor);
		mLockImage = (ImageView) findViewById(R.id.imageLock);
		mGattServerSwitch = (CheckBox) findViewById(R.id.option);
		mRSSI = (TextView) findViewById(R.id.textrssi);
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ProximityActivity.this);
		mGattServerSwitch.setChecked(preferences.getBoolean(PREFS_GATT_SERVER_ENABLED, true));
		mGattServerSwitch.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				preferences.edit().putBoolean(PREFS_GATT_SERVER_ENABLED, isChecked).commit();
			}
		});
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IMMEDIATE_ALERT_STATUS, isImmediateAlertOn);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		isImmediateAlertOn = savedInstanceState.getBoolean(IMMEDIATE_ALERT_STATUS);
		if (isDeviceConnected()) {
			showOpenLock();

			if (isImmediateAlertOn) {
				showSilentMeOnButton();
			}
		}
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.proximity_feature_title;
	}

	@Override
	protected void onServiceBinded(final ProximityService.ProximityBinder binder) {
		// you may get the binder instance here 
	}

	@Override
	protected void onServiceUnbinded() {
		// you may release the binder instance here
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return ProximityService.class;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.proximity_about_text;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.proximity_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return ProximityManager.LINKLOSS_SERVICE_UUID;
	}

	/**
	 * Callback of FindMe button on ProximityActivity
	 */
	public void onFindMeClicked(final View view) {
		if (isBLEEnabled()) {
			if (!isDeviceConnected()) {
				// do nothing
			} else if (!isImmediateAlertOn) {
				showSilentMeOnButton();
				((ProximityService.ProximityBinder) getService()).startImmediateAlert();
				((ProximityService.ProximityBinder) getService()).stopAlarm();
				((ProximityService.ProximityBinder) getService()).getRssi();
				showOpenLock();
				rssilevel = ((ProximityService.ProximityBinder) getService()).getRssiValue();
				isImmediateAlertOn = true;
				updateRssi(rssilevel);
			} else {
				showFindMeOnButton();
				((ProximityService.ProximityBinder) getService()).stopImmediateAlert();
				((ProximityService.ProximityBinder) getService()).stopAlarm();
				((ProximityService.ProximityBinder) getService()).getRssi();
				showOpenLock();
				rssilevel = ((ProximityService.ProximityBinder) getService()).getRssiValue();
				isImmediateAlertOn = false;
				updateRssi(rssilevel);
			}
		} else {
			showBLEDialog();
		}
	}
	
	public void onMonitorClick(final View view){
		if (isBLEEnabled()) {
			if (!isDeviceConnected()) {
					// do nothing
			} else if (monitorvis == 0) {
				showMonitor();
				DebugLogger.v(TAG, "show monitor");
				//monitorStop = 4;
				Thread.interrupted();
				shutdownExecutor();
					
			} else if (monitorvis == 1) {
				hideMonitor();
				DebugLogger.v(TAG, "hide monitor");
				monitorStop = 0;
				runThread();

			}
		} 
		else {
			showBLEDialog();
		}
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	


	private void runThread() {

	    new Thread() {
	    	int i;
	        public void run() {
	            while (monitorStop != 3) {
	                try {
	                    runOnUiThread(new Runnable() {

	                        @Override
	                        public void run() {
	                        	int x = ((ProximityService.ProximityBinder) getService()).checkDrop();
	                        		if ( x == 1) { showDropDialog("Device"); ((ProximityService.ProximityBinder) getService()).stopDrop(); }
	            				((ProximityService.ProximityBinder) getService()).getRssi();
	            				rssilevel = ((ProximityService.ProximityBinder) getService()).getRssiValue();
	            				mRSSI.setText(String.valueOf(rssilevel) + "dB");
	            				detectRange(rssilevel);
	                        }
	                    });
	                    Thread.sleep(750);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	    }.start();
	}


	private void shutdownExecutor() {
	    if (mScheduleExecutorService != null && !mScheduleExecutorService.isShutdown()) {
	        mScheduleExecutorService.shutdownNow();
	    }
	}
	
	public void detectRange(int rssi){
		mRSSI.setText(String.valueOf(rssi) + "dB");
		if ( (rssi-5) <= errorcheck ) {
			if (rssi+5 >= errorcheck) {
			
					if (rssi < -92) {
						((ProximityService.ProximityBinder) getService()).startImmediateAlert();
						((ProximityService.ProximityBinder) getService()).playAlarm();
						showAlarm();
						errorcheck = rssi;
						monitorStop = 3;
									}
					else if (rssi < -82) {
						((ProximityService.ProximityBinder) getService()).startImmediateWarning();
						((ProximityService.ProximityBinder) getService()).playNotification();
						showSilentMeOnButton();
						errorcheck = rssi;
						monitorStop = 2;
											}
					
					else {
						monitorStop = 0;
						errorcheck = rssi;
						//((ProximityService.ProximityBinder) getService()).stopImmediateAlert();
						((ProximityService.ProximityBinder) getService()).stopAlarm();
						}
									}
			else {
				errorcheck = rssi;
			}
		}
		else {
			errorcheck = rssi;
		}
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	@Override
	protected void setDefaultUI() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mFindMeButton.setText(R.string.proximity_action_findme);
				mMonitor.setText("Monitor");
				mLockImage.setImageResource(R.drawable.proximity_lock_closed);
			}
		});
	}

	@Override
	public void onServicesDiscovered(boolean optionalServicesFound) {
	}
	
	
	public void updateRssi(int rssi)	{
		//final int rssiPercent = (int) (100.0f * (127.0f + rssi) / 127.0f + 20.0f);
		mRSSI.setText(String.valueOf(rssi) + "dB");
		if (rssi > 60){
			DebugLogger.v(TAG, "inside print");
			mRSSI.setText(String.valueOf(rssi) + "dB");
		} else {
			monitorStop = 1;
		}
	}
	
	@Override
	public void onDeviceConnected() {
		super.onDeviceConnected();
		showOpenLock();
		mGattServerSwitch.setEnabled(false); 
	}

	@Override
	public void onDeviceDisconnected() {
		super.onDeviceDisconnected();
		showClosedLock();
		mGattServerSwitch.setEnabled(true);
	}

	@Override
	public void onBondingRequired() {
		showClosedLock();
	}

	@Override
	public void onBonded() {
		showOpenLock();
	}

	@Override
	public void onLinklossOccur() {
		super.onLinklossOccur();
		showClosedLock();
		resetForLinkloss();

		DebugLogger.w(TAG, "Linkloss occur");

		String deviceName = getDeviceName();
		if (deviceName == null) {
			deviceName = getString(R.string.proximity_default_name);
		}

		showLinklossDialog(deviceName);
	}
	
	@Override
	public void onDrop() {
		super.onDrop();
		DebugLogger.w(TAG, "dropped item occur");
		
		String deviceName = getDeviceName();
		if (deviceName == null) {
			deviceName = getString(R.string.proximity_default_name);
		}

		showDropDialog(deviceName);

	}
	
	private void resetForLinkloss() {
		isImmediateAlertOn = false;
		setDefaultUI();
	}

	private void showFindMeOnButton() {
		mFindMeButton.setText(R.string.proximity_action_findme);
	}

	private void showSilentMeOnButton() {
		mFindMeButton.setText(R.string.proximity_action_silentme);
	}
	
	private void showMonitor() {
		mMonitor.setText("Monitor");
		monitorvis = 1;
	}

	private void hideMonitor() {
		mMonitor.setText("Stop");
		monitorvis = 0;
	}

	private void showOpenLock() {
		mFindMeButton.setEnabled(true);
		mMonitor.setEnabled(true);
		mLockImage.setImageResource(R.drawable.proximity_lock_open);
	}

	private void showClosedLock() {
		mFindMeButton.setEnabled(false);
		mMonitor.setEnabled(false);
		mLockImage.setImageResource(R.drawable.proximity_lock_closed);
	}
	
	private void showAlarm() {
		mFindMeButton.setEnabled(true);
		mMonitor.setEnabled(false);
		mLockImage.setImageResource(R.drawable.proximity_lock_closed);
	}

	private void showLinklossDialog(final String name) {
		try {
			FragmentManager fm = getFragmentManager();
			LinklossFragment dialog = LinklossFragment.getInstance(name);
			dialog.show(fm, "scan_fragment");
		} catch (final Exception e) {
			// the activity must have been destroyed
		}
	}
	
	private void showDropDialog(final String name) {
		try {
			FragmentManager fm = getFragmentManager();
			DropFragment dialog = DropFragment.getInstance(name);
			dialog.show(fm, "scan_fragment");
		} catch (final Exception e) {
			// the activity must have been destroyed
		}
	}
}
