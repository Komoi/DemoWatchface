package com.ondrejkomarek.meetupwatchface;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.wearable.complications.ComplicationData;


/**
 * Created by ondrejkomarek on 17/02/2017.
 */

public class ExtendedComplicationData {//REVIEW ComplicationData can not be actually extended due to private constructors


	private ComplicationData mComplicationData;
	private boolean mHasValidData;
	private Rect mComplicationBounds;
	private int mComplicationId;
	private int mComplicationType;

	private String mShortText = "";
	private String mTitle = "";
	private Drawable mIcon;


	public ComplicationData getComplicationData() {
		return mComplicationData;
	}


	public void setComplicationData(ComplicationData complicationData) {
		mComplicationData = complicationData;
	}


	public boolean isHasValidData() {
		return mHasValidData;
	}


	public void setHasValidData(boolean hasValidData) {
		mHasValidData = hasValidData;
	}


	public Rect getComplicationBounds() {
		return mComplicationBounds;
	}


	public void setComplicationBounds(Rect complicationBounds) {
		mComplicationBounds = complicationBounds;
	}


	public int getComplicationId() {
		return mComplicationId;
	}


	public void setComplicationId(int complicationId) {
		mComplicationId = complicationId;
	}


	public int getComplicationType() {
		return mComplicationType;
	}


	public void setComplicationType(int complicationType) {
		mComplicationType = complicationType;
	}


	public String getShortText() {
		return mShortText;
	}


	public void setShortText(String shortText) {
		mShortText = shortText;
	}


	public String getTitle() {
		return mTitle;
	}


	public void setTitle(String title) {
		mTitle = title;
	}


	public Drawable getIcon() {
		return mIcon;
	}


	public void setIcon(Drawable icon) {
		mIcon = icon;
	}
}
