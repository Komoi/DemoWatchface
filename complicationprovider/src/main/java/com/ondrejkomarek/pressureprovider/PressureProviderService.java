package com.ondrejkomarek.pressureprovider;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;


/**
 * Created by ondrejkomarek on 17/02/2017.
 */

public class PressureProviderService extends ComplicationProviderService implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mPressure;
	private String mPressureString = "";

	private static final String TAG = "PressureProvider";

	@Override
	public void onComplicationActivated(int complicationId, int dataType, ComplicationManager complicationManager) {
		initPressureReading();

	}

	private void initPressureReading(){
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
		ComplicationData complicationData = null;
		Log.d(TAG, "data update: " + mPressureString);

		switch (dataType) {
			case ComplicationData.TYPE_SHORT_TEXT:
				complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
						.setShortText(ComplicationText.plainText(mPressureString))
						.build();
				break;
		}

		if (complicationData != null) {
			complicationManager.updateComplicationData(complicationId, complicationData);
		}
	}

	@Override
	public void onComplicationDeactivated(int complicationId) {
		mSensorManager.unregisterListener(this);

	}


	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			mPressureString = String.valueOf(Math.round(event.values[0])) + " hPa";
		}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		mSensorManager.unregisterListener(this);
	}


	@Override
	public void onCreate() {
		super.onCreate();
		if(mSensorManager == null) {
			initPressureReading();
		}
	}

}