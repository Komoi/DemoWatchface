package com.batterywatchface.ondrejkomarek.meetupwatchface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.batterywatchface.ondrejkomarek.meetupwatchface.MeetupWatchface.BOTTOM_COMPLICATION;
import static com.batterywatchface.ondrejkomarek.meetupwatchface.MeetupWatchface.COMPLICATION_IDS;


/**
 * Created by ondrejkomarek on 29/05/16.
 */


/**
 * The watch-side config activity for {@link MeetupWatchface}, which allows various settings.
 */
public class AnalogWatchFaceWearableConfigActivity extends Activity {
	private static final String TAG = "AnalogWatchFaceConfig";

	private TextView mHeader;
	private Button mComplicationsButton;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_analog_config);

		mHeader = (TextView) findViewById(R.id.header);
		mComplicationsButton = (Button) findViewById(R.id.complications_button);

		if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
			mComplicationsButton.setVisibility(View.INVISIBLE); //we need to hide complications settings button, because pressing it will cause nothing or lower APIs
		}

		mComplicationsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pickComplicationAndRequestData();
			}
		});


	}


	private void pickComplicationAndRequestData(){
		ComponentName componentName = new ComponentName(
				getApplicationContext(),
				MeetupWatchface.class);

		Intent intent = ComplicationHelperActivity.createProviderChooserHelperIntent(getApplicationContext(), componentName, BOTTOM_COMPLICATION, ComplicationData.TYPE_SHORT_TEXT);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
}

