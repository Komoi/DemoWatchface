/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ondrejkomarek.meetupwatchface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 */
//REVIEW most of this code was generated
public class MeetupWatchface extends CanvasWatchFaceService {

	public static final String TAG = "MeetupWatchface";

	//complication IDs
	public static final int TOP_COMPLICATION = 0;
	public static final int BOTTOM_COMPLICATION = 1;

	public static final int TOP_COMPLICATION_TYPE = ComplicationData.TYPE_ICON;
	public static final int BOTTOM_COMPLICATION_TYPE = ComplicationData.TYPE_SHORT_TEXT;

	public static final int[] COMPLICATION_IDS = {TOP_COMPLICATION, BOTTOM_COMPLICATION};
	public static final int COMPLICATION_ICON_SIZE = 40;
	/*
	 * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
	private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
	/**
	 * Handler message id for updating the time periodically in interactive mode.
	 */
	private static final int MSG_UPDATE_TIME = 0;
	private ExtendedComplicationData[] mExtendedComplicationData =
			{new ExtendedComplicationData(), new ExtendedComplicationData()};


	@Override
	public Engine onCreateEngine() {
		return new Engine();
	}


	private static class EngineHandler extends Handler {
		private final WeakReference<MeetupWatchface.Engine> mWeakReference;


		public EngineHandler(MeetupWatchface.Engine reference) {
			mWeakReference = new WeakReference<>(reference);
		}


		//handles time updates for ambient and interactive mode
		@Override
		public void handleMessage(Message msg) {
			MeetupWatchface.Engine engine = mWeakReference.get();
			if(engine != null) {
				switch(msg.what) {
					case MSG_UPDATE_TIME:
						engine.handleUpdateTimeMessage();
						break;
				}
			}
		}
	}


	private class Engine extends CanvasWatchFaceService.Engine {
		private static final float HOUR_STROKE_WIDTH = 5f;
		private static final float MINUTE_STROKE_WIDTH = 3f;
		private static final float SECOND_TICK_STROKE_WIDTH = 2f;

		private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
		private static final int SHADOW_RADIUS = 6;
		private final Rect mPeekCardBounds = new Rect();
		/* Handler to update the time once a second in interactive mode. */
		private final Handler mUpdateTimeHandler = new EngineHandler(this);
		private Calendar mCalendar;
		private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mCalendar.setTimeZone(TimeZone.getDefault());
				invalidate();
			}
		};

		private boolean mRegisteredTimeZoneReceiver = false;
		private boolean mMuteMode;
		private float mCenterX;
		private float mCenterY;
		private float mSecondHandLength;
		private float sMinuteHandLength;
		private float sHourHandLength;
		/* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
		private int mWatchHandColor;
		private int mWatchHandHighlightColor;
		private int mWatchHandShadowColor;
		private Paint mHourPaint;
		private Paint mMinutePaint;
		private Paint mSecondPaint;
		private Paint mTickAndCirclePaint;
		private Paint mBackgroundPaint;
		private Bitmap mBackgroundBitmap;
		private Bitmap mGrayBackgroundBitmap;
		private boolean mAmbient;
		private boolean mLowBitAmbient;
		private boolean mBurnInProtection;

		private Paint mComplicationPaint = new Paint();


		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);

			setWatchFaceStyle(new WatchFaceStyle.Builder(MeetupWatchface.this)
					.setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)// REVIEW deprecated, no peak mode now
					.setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
					.setShowSystemUiTime(false)
					.setAcceptsTapEvents(true)
					.build());

			mBackgroundPaint = new Paint();
			mBackgroundPaint.setColor(Color.BLACK);
			mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

            /* Set defaults for colors */
			mWatchHandColor = Color.WHITE;
			mWatchHandHighlightColor = Color.RED;
			mWatchHandShadowColor = Color.BLACK;

			mHourPaint = new Paint();
			mHourPaint.setColor(mWatchHandColor);
			mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
			mHourPaint.setAntiAlias(true);
			mHourPaint.setStrokeCap(Paint.Cap.ROUND);
			mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

			mMinutePaint = new Paint();
			mMinutePaint.setColor(mWatchHandColor);
			mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
			mMinutePaint.setAntiAlias(true);
			mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
			mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

			mSecondPaint = new Paint();
			mSecondPaint.setColor(mWatchHandHighlightColor);
			mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
			mSecondPaint.setAntiAlias(true);
			mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
			mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

			mTickAndCirclePaint = new Paint();
			mTickAndCirclePaint.setColor(mWatchHandColor);
			mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
			mTickAndCirclePaint.setAntiAlias(true);
			mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
			mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            /* Extract colors from background image to improve watchface style. */
			Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
				@Override
				public void onGenerated(Palette palette) {
					if(palette != null) {
						mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
						mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
						mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
						updateWatchHandStyle();
					}
				}
			});

			mCalendar = Calendar.getInstance();

			setActiveComplications(COMPLICATION_IDS);//REVIEW this starts receiving complication data, tells how many slots

			mComplicationPaint = new Paint();
			mComplicationPaint.setColor(Color.WHITE);
			mComplicationPaint.setTextAlign(Paint.Align.CENTER);
			mComplicationPaint.setTextSize(40);
			mComplicationPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			mComplicationPaint.setAntiAlias(true);
		}


		@Override
		public void onDestroy() {
			mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
			super.onDestroy();
		}


		//REVIEW complication data
		@Override
		public void onComplicationDataUpdate(int watchFaceComplicationId, ComplicationData data) {
			super.onComplicationDataUpdate(watchFaceComplicationId, data);

			long now = System.currentTimeMillis();

			mExtendedComplicationData[watchFaceComplicationId].setComplicationData(data);
			mExtendedComplicationData[watchFaceComplicationId].setComplicationId(watchFaceComplicationId);

			//REVIEW handling data accordingly to its expected type
			switch(watchFaceComplicationId) {
				case TOP_COMPLICATION:
					if(data.getIcon() != null) {//mandatory field - data are not valid if null
						mExtendedComplicationData[watchFaceComplicationId].setComplicationType(TOP_COMPLICATION_TYPE);
						mExtendedComplicationData[watchFaceComplicationId].setHasValidData(true);
					} else {
						mExtendedComplicationData[watchFaceComplicationId].setHasValidData(false);
					}
					break;

				case BOTTOM_COMPLICATION:
					if(data.getShortText() != null) {//mandatory field - data are not valid if null
						mExtendedComplicationData[watchFaceComplicationId].setComplicationType(BOTTOM_COMPLICATION_TYPE);
						mExtendedComplicationData[watchFaceComplicationId].setHasValidData(true);
					} else {
						mExtendedComplicationData[watchFaceComplicationId].setHasValidData(false);
					}
					break;
			}

			//REVIEW also saving to my helper class so it does not need to be extracted in every call of onDraw
			if(data.getIcon() != null) {
				//REVIEW extracting drawable from icon - can be time consuming, better to do just once!
				mExtendedComplicationData[watchFaceComplicationId].setIcon(data.getIcon().loadDrawable(getBaseContext()));
			} else {
				mExtendedComplicationData[watchFaceComplicationId].setIcon(null);
			}

			if(data.getShortText() != null) {
				mExtendedComplicationData[watchFaceComplicationId].setShortText(data.getShortText()
						.getText(getApplicationContext(), now).toString());
			} else {
				mExtendedComplicationData[watchFaceComplicationId].setShortText("");
			}

			if(data.getShortTitle() != null) {
				mExtendedComplicationData[watchFaceComplicationId].setTitle(data.getShortTitle()
						.getText(getApplicationContext(), now).toString());
			} else {
				mExtendedComplicationData[watchFaceComplicationId].setTitle("");
			}
		}


		@Override
		public void onPropertiesChanged(Bundle properties) {
			super.onPropertiesChanged(properties);
			mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
			mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
		}


		@Override
		public void onTimeTick() {
			super.onTimeTick();
			invalidate();
		}


		//REVIEW saving ambient status - we can handle ambient mode as we like
		@Override
		public void onAmbientModeChanged(boolean inAmbientMode) {
			super.onAmbientModeChanged(inAmbientMode);
			mAmbient = inAmbientMode;

			updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
			updateTimer();
		}


		@Override
		public void onInterruptionFilterChanged(int interruptionFilter) {
			super.onInterruptionFilterChanged(interruptionFilter);
			boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
			if(mMuteMode != inMuteMode) {
				mMuteMode = inMuteMode;
				mHourPaint.setAlpha(inMuteMode ? 100 : 255);
				mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
				mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
				invalidate();
			}
		}


		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

            /*
			 * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
			mCenterX = width / 2f;
			mCenterY = height / 2f;

            /*
			 * Calculate lengths of different hands based on watch screen size.
             */
			mSecondHandLength = (float) (mCenterX * 0.875);
			sMinuteHandLength = (float) (mCenterX * 0.75);
			sHourHandLength = (float) (mCenterX * 0.5);


            /* Scale loaded background image (more efficient) if surface dimensions change. */
			float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

			mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
					(int) (mBackgroundBitmap.getWidth() * scale),
					(int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
			 * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
			if(!mBurnInProtection && !mLowBitAmbient) {
				initGrayBackgroundBitmap();
			}
		}


		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);

			if(visible) {
				registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
				mCalendar.setTimeZone(TimeZone.getDefault());
				invalidate();
			} else {
				unregisterReceiver();
			}

            /* Check and trigger whether or not timer should be running (only in active mode). */
			updateTimer();
		}


		@Override
		public void onPeekCardPositionUpdate(Rect rect) {
			super.onPeekCardPositionUpdate(rect);
			mPeekCardBounds.set(rect);
		}


		//REVIEW most of this code was generated, calculations needed to draw watch elements, ticks
		@Override
		public void onDraw(Canvas canvas, Rect bounds) {
			long now = System.currentTimeMillis();
			mCalendar.setTimeInMillis(now);

			if(mAmbient && (mLowBitAmbient || mBurnInProtection)) {
				canvas.drawColor(Color.BLACK);
			} else if(mAmbient) {
				canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
			} else {
				canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
			}

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
			float innerTickRadius = mCenterX - 10;
			float outerTickRadius = mCenterX;
			for(int tickIndex = 0; tickIndex < 12; tickIndex++) {
				float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
				float innerX = (float) Math.sin(tickRot) * innerTickRadius;
				float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
				float outerX = (float) Math.sin(tickRot) * outerTickRadius;
				float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
				canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
						mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
			}

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
			final float seconds =
					(mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
			final float secondsRotation = seconds * 6f;

			final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

			final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
			final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */

			//REVIEW rendering complications - should be before rendering foreground watch elements
			renderComplications(canvas);

			canvas.save();

			//REVIEW rotating whole canvas to make watch hands rendering simpler
			canvas.rotate(hoursRotation, mCenterX, mCenterY);
			canvas.drawLine(
					mCenterX,
					mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
					mCenterX,
					mCenterY - sHourHandLength,
					mHourPaint);

			canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
			canvas.drawLine(
					mCenterX,
					mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
					mCenterX,
					mCenterY - sMinuteHandLength,
					mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
			if(!mAmbient) {
				canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
				canvas.drawLine(
						mCenterX,
						mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
						mCenterX,
						mCenterY - mSecondHandLength,
						mSecondPaint);

			}
			canvas.drawCircle(
					mCenterX,
					mCenterY,
					CENTER_GAP_AND_CIRCLE_RADIUS,
					mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
			canvas.restore();


            /* Draw rectangle behind peek card in ambient mode to improve readability. */
			if(mAmbient) {
				canvas.drawRect(mPeekCardBounds, mBackgroundPaint);
			}
		}


		private void renderComplications(Canvas canvas) {
			if(mExtendedComplicationData[TOP_COMPLICATION] != null &&
					mExtendedComplicationData[TOP_COMPLICATION].isHasValidData()) {
				//REVIEW using already extracted drawable
				mExtendedComplicationData[TOP_COMPLICATION].getIcon()
						.setBounds((int) mCenterX - COMPLICATION_ICON_SIZE / 2, (int) mCenterY / 2 - COMPLICATION_ICON_SIZE / 2,
								(int) mCenterX + COMPLICATION_ICON_SIZE / 2, (int) mCenterY / 2 + COMPLICATION_ICON_SIZE / 2);
				mExtendedComplicationData[TOP_COMPLICATION].getIcon().draw(canvas);
				mExtendedComplicationData[TOP_COMPLICATION]
						.setComplicationBounds(mExtendedComplicationData[TOP_COMPLICATION].getIcon().getBounds());
				//REVIEW saving bounds of icon
			}

			if(mExtendedComplicationData[BOTTOM_COMPLICATION] != null &&
					mExtendedComplicationData[BOTTOM_COMPLICATION].isHasValidData()) {
				if(mExtendedComplicationData[BOTTOM_COMPLICATION].getIcon() != null) {
					mExtendedComplicationData[BOTTOM_COMPLICATION].getIcon().setBounds(
							(int) mCenterX - COMPLICATION_ICON_SIZE / 2, (int) (mCenterY + mCenterY / 4 * 3) - COMPLICATION_ICON_SIZE * 2,
							(int) mCenterX + COMPLICATION_ICON_SIZE / 2, (int) (mCenterY + mCenterY / 4 * 3) - COMPLICATION_ICON_SIZE);
					mExtendedComplicationData[BOTTOM_COMPLICATION].getIcon().draw(canvas);
					mExtendedComplicationData[BOTTOM_COMPLICATION].setComplicationBounds(mExtendedComplicationData[BOTTOM_COMPLICATION]
							.getIcon().getBounds());
				}
				//REVIEW kind of pain - different complication providers shares very different data, different lengths etc.
				String bottomText = mExtendedComplicationData[BOTTOM_COMPLICATION].getShortText();
				if(!mExtendedComplicationData[BOTTOM_COMPLICATION].getTitle().isEmpty()) {
					bottomText = mExtendedComplicationData[BOTTOM_COMPLICATION].getTitle() + " - " + bottomText;
				}
				float textBaseY = mCenterY + mCenterY / 4 * 3;
				float textWidth = mComplicationPaint.measureText(bottomText);
				Rect textBounds = new Rect();
				textBounds.set((int) (mCenterX - textWidth / 2), (int) (textBaseY - mComplicationPaint.getTextSize()),
						(int) (mCenterX + textWidth / 2), (int) (textBaseY + mComplicationPaint.getTextSize()));

				mExtendedComplicationData[BOTTOM_COMPLICATION].setComplicationBounds(textBounds);
				canvas.drawText(bottomText, 0, bottomText.length(), mCenterX, textBaseY, mComplicationPaint);
			}
		}


		/**
		 * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
		 * used for implementing specific logic to handle the gesture.
		 */
		@Override
		public void onTapCommand(int tapType, int x, int y, long eventTime) {
			switch(tapType) {
				case TAP_TYPE_TOUCH:
					// The user has started touching the screen.
					break;
				case TAP_TYPE_TOUCH_CANCEL:
					// The user has started a different gesture or otherwise cancelled the tap.
					break;
				case TAP_TYPE_TAP:
					//REVIEW custom determination, if tap was aimed to some complication
					// The user has completed the tap gesture.
					checkComplicationTap(TOP_COMPLICATION, x, y);
					checkComplicationTap(BOTTOM_COMPLICATION, x, y);

					break;
			}
			invalidate();
		}

		// REVIEW custom tap handling
		private void checkComplicationTap(int complicationId, int x, int y) {
			if(mExtendedComplicationData[complicationId].getComplicationBounds() != null) {

				if(mExtendedComplicationData[complicationId].getComplicationBounds().contains(x, y)) {
					PendingIntent complicationAction = mExtendedComplicationData[complicationId].getComplicationData().getTapAction();
					Toast.makeText(getApplicationContext(), "tapped complication: " + complicationId, Toast.LENGTH_SHORT)
							.show();

					if(complicationAction != null) {
						Log.d(TAG, "Tap cotains id: " + complicationId);
						try {
							//REVIEW LAUNCH INTENT IF NOT NULL - optional field in ComplicationData
							complicationAction.send();
						} catch(PendingIntent.CanceledException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}


		//REVIEW shaadow and more efects when interactive
		private void updateWatchHandStyle() {
			if(mAmbient) {
				mHourPaint.setColor(Color.WHITE);
				mMinutePaint.setColor(Color.WHITE);
				mSecondPaint.setColor(Color.WHITE);
				mTickAndCirclePaint.setColor(Color.WHITE);

				mHourPaint.setAntiAlias(false);
				mMinutePaint.setAntiAlias(false);
				mSecondPaint.setAntiAlias(false);
				mTickAndCirclePaint.setAntiAlias(false);

				mHourPaint.clearShadowLayer();
				mMinutePaint.clearShadowLayer();
				mSecondPaint.clearShadowLayer();
				mTickAndCirclePaint.clearShadowLayer();

			} else {
				mHourPaint.setColor(mWatchHandColor);
				mMinutePaint.setColor(mWatchHandColor);
				mSecondPaint.setColor(mWatchHandHighlightColor);
				mTickAndCirclePaint.setColor(mWatchHandColor);

				mHourPaint.setAntiAlias(true);
				mMinutePaint.setAntiAlias(true);
				mSecondPaint.setAntiAlias(true);
				mTickAndCirclePaint.setAntiAlias(true);

				mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
				mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
				mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
				mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
			}
		}


		private void initGrayBackgroundBitmap() {
			mGrayBackgroundBitmap = Bitmap.createBitmap(
					mBackgroundBitmap.getWidth(),
					mBackgroundBitmap.getHeight(),
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(mGrayBackgroundBitmap);
			Paint grayPaint = new Paint();
			ColorMatrix colorMatrix = new ColorMatrix();
			colorMatrix.setSaturation(0);
			ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
			grayPaint.setColorFilter(filter);
			canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
		}


		private void registerReceiver() {
			if(mRegisteredTimeZoneReceiver) {
				return;
			}
			mRegisteredTimeZoneReceiver = true;
			IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
			MeetupWatchface.this.registerReceiver(mTimeZoneReceiver, filter);
		}


		private void unregisterReceiver() {
			if(!mRegisteredTimeZoneReceiver) {
				return;
			}
			mRegisteredTimeZoneReceiver = false;
			MeetupWatchface.this.unregisterReceiver(mTimeZoneReceiver);
		}


		/**
		 * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
		 */
		private void updateTimer() {
			mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
			if(shouldTimerBeRunning()) {
				mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
			}
		}


		/**
		 * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
		 * should only run in active mode.
		 */
		private boolean shouldTimerBeRunning() {
			return isVisible() && !mAmbient;
		}

		// REVIEW deciding if time needs to be updated or not
		/**
		 * Handle updating the time periodically in interactive mode.
		 */
		private void handleUpdateTimeMessage() {
			invalidate();
			if(shouldTimerBeRunning()) {
				long timeMs = System.currentTimeMillis();
				long delayMs = INTERACTIVE_UPDATE_RATE_MS
						- (timeMs % INTERACTIVE_UPDATE_RATE_MS);
				mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
			}
		}
	}
}
