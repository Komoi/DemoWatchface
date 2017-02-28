package com.ondrejkomarek.pressureprovider;

import android.content.ComponentName;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;


/**
 * Created by ondrejkomarek on 17/02/2017.
 */

public class PressureProviderService extends ComplicationProviderService implements SensorEventListener {
	private static final String TAG = "PressureProvider";
	private static boolean handlerSetUp = false;
	private SensorManager mSensorManager;
	private Sensor mPressure;
	private String mPressureString = "";
	private ComponentName mComponentName;

	private ProviderUpdateRequester mProviderUpdateRequester;
	private Handler mComplicationUpdateHandler;


	@Override
	public void onCreate() {
		super.onCreate();
		if(mSensorManager == null) {
			initPressureReading();
		}

		// REVIEW This enables to send complication info much more often, then system usually allows.
		// REVIEW  Do not use it unless really necessary - this is for quick changes while demonstrating.
		if(mProviderUpdateRequester == null && !handlerSetUp) {
			mComponentName = new ComponentName(getApplicationContext(), PressureProviderService.class);
			mProviderUpdateRequester = new ProviderUpdateRequester(getBaseContext(), mComponentName);

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if(mProviderUpdateRequester != null) {
						mProviderUpdateRequester.requestUpdateAll(); // REVIEW this forces data update
					}
					mComplicationUpdateHandler.postDelayed(this, 1000 * 10);
				}
			};

			mComplicationUpdateHandler = new Handler();
			mComplicationUpdateHandler.postDelayed(runnable, 1000 * 10);   // 10 sec update interval
			handlerSetUp = true;
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		mSensorManager.unregisterListener(this);
	}


	@Override
	public void onComplicationActivated(int complicationId, int dataType, ComplicationManager complicationManager) {
		initPressureReading();
	}


	//REVIEW this method is called when data update is requested.
	@Override
	public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
		ComplicationData complicationData = null;
		Log.d(TAG, "data update: " + mPressureString);

		switch(dataType) {
			case ComplicationData.TYPE_SHORT_TEXT:
				//REVIEW fill with data of relevant type
				complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
						.setShortText(ComplicationText.plainText(mPressureString))
						.build();
				break;
		}

		//REVIEW send updated data
		if(complicationData != null) {
			complicationManager.updateComplicationData(complicationId, complicationData);
		}
	}


	@Override
	public void onComplicationDeactivated(int complicationId) {
		mSensorManager.unregisterListener(this);

	}


	//REVIEW saving current value in a string with unit
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			mPressureString = String.valueOf(Math.round(event.values[0])) + " hPa";
		}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	//REVIEW start receiving sensor data
	private void initPressureReading() {
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_UI);
	}

}