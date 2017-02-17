package com.ondrejkomarek.meetupwatchface;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;


/**
 * Created by ondrejkomarek on 15/02/2017.
 */

public class MeetupWatchfaceApplication  extends Application
{
	private static MeetupWatchfaceApplication sInstance;

	public MeetupWatchfaceApplication()
	{
		sInstance = this;
	}

	public static Context getContext()
	{
		return sInstance;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}