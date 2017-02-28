package com.ondrejkomarek.meetupwatchface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.ondrejkomarek.meetupwatchface.MeetupWatchface.BOTTOM_COMPLICATION;
import static com.ondrejkomarek.meetupwatchface.MeetupWatchface.BOTTOM_COMPLICATION_TYPE;
import static com.ondrejkomarek.meetupwatchface.MeetupWatchface.TOP_COMPLICATION;
import static com.ondrejkomarek.meetupwatchface.MeetupWatchface.TOP_COMPLICATION_TYPE;


/**
 * Created by ondrejkomarek on 29/05/16.
 */


/**
 * The watch-side config activity for {@link MeetupWatchface}, which allows various settings.
 */
public class AnalogWatchFaceWearableConfigActivity extends Activity {
	private static final String TAG = "AnalogWatchFaceConfig";

	private TextView mHeader;
	private Button mTopComplicationButton;
	private Button mBottomComplicationButton;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_analog_config);

		mHeader = (TextView) findViewById(R.id.header);
		mTopComplicationButton = (Button) findViewById(R.id.top_complication);
		mBottomComplicationButton = (Button) findViewById(R.id.bottom_complication);

		if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			mTopComplicationButton.setVisibility(View.INVISIBLE); //we need to hide complications settings button, because pressing it will cause nothing on lower APIs
			mBottomComplicationButton.setVisibility(View.INVISIBLE);
		}

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pickComplicationAndRequestData(v.equals(mBottomComplicationButton) ? BOTTOM_COMPLICATION : TOP_COMPLICATION, v.equals(mBottomComplicationButton) ? BOTTOM_COMPLICATION_TYPE : TOP_COMPLICATION_TYPE);
			}
		};

		mTopComplicationButton.setOnClickListener(onClickListener);
		mBottomComplicationButton.setOnClickListener(onClickListener);


	}


	// REVIEW button click handling, launching "ComplicationHelperActivity" for specific slot
	private void pickComplicationAndRequestData(int complicationID, int complicationDataType) {
		ComponentName componentName = new ComponentName(
				getApplicationContext(),
				MeetupWatchface.class);

		Intent intent = ComplicationHelperActivity.createProviderChooserHelperIntent(getApplicationContext(), componentName, complicationID, complicationDataType);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
}

