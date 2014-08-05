/*
 * Copyright (C) 2014 Chang Wentao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.changwentao.widget;

import java.text.DecimalFormat;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.RectF;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cn.changwentao.lemon.R;

/**
 * PercentLemon类继承自{@link View}，以圆环图形的形式显示百分比数值。
 * <p>
 * 在设置百分比值时可以使用方法{@link #setPercent(float)}不包含动画效果，或者方法
 * {@link #animatToPercent(float)}包含过渡动画效果。
 * 
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_heartColor
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_heartClickedColor
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_skinStartColor
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_skinEndColor
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_skinDepth
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_percent
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_duration
 * @attr ref cn.changwentao.lemon.R.styleable#PercentLemon_centerTextColor
 */
public class PercentLemon extends View {
	private static final String TAG = "PercentLemon";
	private static final boolean localLOG = false;

	/** 默认的中心圆颜色 */
	private static final int DEFAULT_HEART_COLOR_NORMAL = 0xFF373737;

	/** 默认的中心圆点击颜色 */
	private static final int DEFAULT_HEART_COLOR_CLICKED = 0XFF33B6EA;

	/** 默认的外圆环起始颜色（百分比为0） */
	private static final int DEFAULT_SKIN_START_COLOR = 0xFFFF0000;

	/** 默认的外圆环结束颜色（百分比为100） */
	private static final int DEFAULT_SKIN_END_COLOR = 0xFF00FF00;

	/** 默认的文本颜色（百分比为0） */
	private static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;

	/** 默认的外圆环宽度在整个半径中所占份额（共100份） */
	private static final int DEFAULT_SKIN_DEPTH = 15;

	/** 默认的外圆环颜色，百分比值之外的正常情况下圆环颜色 */
	private static final int DEFAULT_SKIN_INVALID_COLOR = 0xFFCCCCCC;

	/** 百分比值改变时默认动画播放时长 */
	private static final long DEFAULT_ANIMATION_LENGTH = 1500;

	private static final long DEFAULT_CLICK_ANIMATION_LENGTH = 500;

	private final DecimalFormat mPercentFormat = new DecimalFormat("0.0");

	/** 百分比文本画笔 */
	private final TextPaint mTextPaint;

	/** 中心圆画笔 */
	private final Paint mHeartPaint;

	/** 外围圆环画笔 */
	private final Paint mSkinPaint;

	private ObjectAnimator mArrivePercentAnimator;
	private ValueAnimator mHeartClickAnimator;

	private RectF mBounds;

	private int mTextColor;
	private int mSkinStartColor;
	private int mSkinEndColor;
	private int mHeartColor;
	private int mHeartClickedColor;
	private int mSkinDepth;
	private int mSkinRawDepth;
	private float mPercent;
	private float mTouchPercent = 0f;
	private boolean alwaysKeepInHeart = false;
	private boolean downInHeartFirst = false;

	private OnPercentAnimationEndListener mOnPercentAnimationEndListener;
	private OnHeartClickListener mOnHeartClickListener;

	/**
	 * 构建一个默认样式的{@link PercentLemon}对象。
	 * 
	 * @param context
	 *            Context上下文对象
	 */
	public PercentLemon(Context context) {
		this(context, null);
	}

	/**
	 * 根据xml文件提供的属性值构建一个{@link PercentLemon}对象。
	 * 
	 * @param context
	 *            Context上下文对象
	 */
	@SuppressLint("NewApi")
	public PercentLemon(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayerToSW(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mHeartClickAnimator = ObjectAnimator.ofFloat(PercentLemon.this,
					"BreathHeartPercent", 0f);
			mHeartClickAnimator.setDuration(DEFAULT_CLICK_ANIMATION_LENGTH);
			mArrivePercentAnimator = ObjectAnimator.ofFloat(PercentLemon.this,
					"Percent", 0f);
			mArrivePercentAnimator.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					PercentLemon.this.setLayerToSW(PercentLemon.this);
					if (mOnPercentAnimationEndListener != null) {
						mOnPercentAnimationEndListener
								.onPercentAnimationEnd(PercentLemon.this);
					}
				}
			});
		}

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.PercentLemon, 0, 0);
		try {
			mTextColor = a.getColor(R.styleable.PercentLemon_centerTextColor,
					DEFAULT_TEXT_COLOR);
			mHeartColor = a.getColor(R.styleable.PercentLemon_heartColor,
					DEFAULT_HEART_COLOR_NORMAL);
			mHeartClickedColor = a.getColor(
					R.styleable.PercentLemon_heartClickedColor,
					DEFAULT_HEART_COLOR_CLICKED);
			setAnimationDuration(a.getInt(R.styleable.PercentLemon_duration,
					(int) DEFAULT_ANIMATION_LENGTH));
			if (!a.hasValue(R.styleable.PercentLemon_skinStartColor)
					^ a.hasValue(R.styleable.PercentLemon_skinEndColor)) {
				mSkinStartColor = a.getColor(
						R.styleable.PercentLemon_skinStartColor,
						DEFAULT_SKIN_START_COLOR);
				mSkinEndColor = a.getColor(
						R.styleable.PercentLemon_skinEndColor,
						DEFAULT_SKIN_END_COLOR);
			} else if (a.hasValue(R.styleable.PercentLemon_skinStartColor)) {
				mSkinEndColor = mSkinStartColor = a.getColor(
						R.styleable.PercentLemon_skinStartColor,
						DEFAULT_SKIN_START_COLOR);
			} else {
				mSkinStartColor = mSkinEndColor = a.getColor(
						R.styleable.PercentLemon_skinEndColor,
						DEFAULT_SKIN_END_COLOR);
			}
			mSkinDepth = a.getInt(R.styleable.PercentLemon_skinDepth,
					DEFAULT_SKIN_DEPTH);
			mPercent = a.getFloat(R.styleable.PercentLemon_percent, 0f);
			if (Float.compare(mPercent, 100.0f) > 0
					|| Float.compare(mPercent, 0.0f) < 0) {
				throw new IllegalArgumentException("百分比值必须在0到100之间");
			}
		} finally {
			a.recycle();
		}

		mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(mTextColor);
		mHeartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mHeartPaint.setColor(mHeartColor);
		mHeartPaint.setStyle(Paint.Style.FILL);
		mSkinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSkinPaint.setStyle(Paint.Style.STROKE);

		if (this.isInEditMode()) {
			setPercent(66.66f);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int mXCenter = (getWidth() + getPaddingLeft() - getPaddingRight()) / 2;
		int mYCenter = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2;
		int mHeartRadius = Math.min(
				mXCenter - getPaddingLeft() - mSkinRawDepth, mYCenter
						- getPaddingTop() - mSkinRawDepth);
		canvas.drawCircle(mXCenter, mYCenter, mHeartRadius, mHeartPaint);

		if (Float.compare(mPercent, 0) >= 0) {
			float radianAngle = (mPercent / 100) * 360;
			mSkinPaint.setColor(getCurrentColor(mSkinStartColor, mSkinEndColor,
					mPercent));
			canvas.drawArc(mBounds, -90, radianAngle, false, mSkinPaint);
			mSkinPaint.setColor(DEFAULT_SKIN_INVALID_COLOR);
			canvas.drawArc(mBounds, -90 + radianAngle, 360 - radianAngle,
					false, mSkinPaint);
			String percentText = mPercentFormat.format(mPercent);
			setRawTextSize(mHeartRadius / 2);
			float mTxtWidth = mTextPaint.measureText(percentText, 0,
					percentText.length());
			FontMetrics fm = mTextPaint.getFontMetrics();
			float mTxtHeight = (int) Math.ceil(fm.descent - fm.ascent);
			setRawTextSize(mHeartRadius / 4);
			float mTxtWidth2 = mTextPaint.measureText("%", 0, 1);
			float mTxtHeight2 = (int) Math.ceil(fm.descent - fm.ascent);
			setRawTextSize(mHeartRadius / 2);
			canvas.drawText(percentText, mXCenter - (mTxtWidth + mTxtWidth2)
					/ 2, mYCenter + mTxtHeight / 4, mTextPaint);
			setRawTextSize(mHeartRadius / 4);
			canvas.drawText("%", mXCenter - (mTxtWidth2 - mTxtWidth) / 2,
					mYCenter + mTxtHeight / 4 + (mTxtHeight - mTxtHeight2) / 2,
					mTextPaint);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mHeartColor != mHeartClickedColor) {
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (pointInLemonHeart(x, y)) {
					alwaysKeepInHeart = true;
					downInHeartFirst = true;
					startTouchDownAnimation();
					return true;
				} else {
					alwaysKeepInHeart = false;
					downInHeartFirst = false;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (downInHeartFirst) {
					if (alwaysKeepInHeart && !pointInLemonHeart(x, y)) {
						alwaysKeepInHeart = false;
						startTouchUpAnimation(false);
					}
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				if (downInHeartFirst) {
					if (alwaysKeepInHeart && pointInLemonHeart(x, y)) {
						startTouchUpAnimation(true);
						if (mOnHeartClickListener != null) {
							mOnHeartClickListener
									.onHeartClick(PercentLemon.this);
						}
					}
					alwaysKeepInHeart = false;
					return true;
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				if (downInHeartFirst) {
					alwaysKeepInHeart = false;
					startTouchUpAnimation(false);
					return true;
				}
				break;
			}
		}
		return super.onTouchEvent(event);
	}

	// 判断点击点是否落在圆环中心园内，如果落在中心圆内则返回true否则返回false
	private Boolean pointInLemonHeart(float x, float y) {
		float xx, yy;
		if (Float.compare(x, mBounds.centerX()) > 0) {
			xx = x - mBounds.centerX();
		} else {
			xx = mBounds.centerX() - x;
		}
		if (Float.compare(y, mBounds.centerY()) > 0) {
			yy = y - mBounds.centerY();
		} else {
			yy = mBounds.centerY() - y;
		}
		xx *= xx;
		yy *= yy;

		float r = Math.min(mBounds.width() - mSkinRawDepth, mBounds.height()
				- mSkinRawDepth) / 2;

		if (Float.compare((float) Math.sqrt(xx + yy), r) < 0) {
			return true;
		}
		return false;
	}

	// 将中心圆的颜色渐变到点击颜色
	@SuppressLint("NewApi")
	private void startTouchDownAnimation() {
		long localLength = 300;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mHeartClickAnimator.setFloatValues(mTouchPercent, 100f);
			mHeartClickAnimator.setDuration((long) (localLength - mTouchPercent
					* localLength / 100));
			mHeartClickAnimator.start();
		} else {
			mTouchPercent = 100f;
			mHeartPaint.setColor(mHeartClickedColor);
			invalidate();
		}
	}

	// 将中心圆的颜色渐变到正常颜色
	@SuppressLint("NewApi")
	private void startTouchUpAnimation(boolean playToEdn) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (playToEdn && Float.compare(mTouchPercent, 100) < 0) {
				mHeartClickAnimator.setFloatValues(mTouchPercent, 100f, 0f);
				mHeartClickAnimator
						.setDuration((long) (DEFAULT_CLICK_ANIMATION_LENGTH * 2 - mTouchPercent
								* DEFAULT_CLICK_ANIMATION_LENGTH / 100));
			} else {
				mHeartClickAnimator.setFloatValues(mTouchPercent, 0f);
				mHeartClickAnimator.setDuration((long) (mTouchPercent
						* DEFAULT_CLICK_ANIMATION_LENGTH / 100));
			}
			mHeartClickAnimator.start();
		} else {
			mTouchPercent = 0f;
			mHeartPaint.setColor(mHeartColor);
			invalidate();
		}
	}

	/**
	 * 定义当百分比动画播放结束时回调的接口
	 */
	public interface OnPercentAnimationEndListener {
		/**
		 * 当百分比动画播放结束时被调用
		 * 
		 * @param lemon
		 *            PercentLemon对象
		 */
		void onPercentAnimationEnd(PercentLemon lemon);
	}

	/**
	 * 中心圆环被点击时回调的接口
	 */
	public interface OnHeartClickListener {
		/**
		 * 中心圆环被点击时被调用
		 * 
		 * @param lemon
		 *            PercentLemon对象
		 */
		void onHeartClick(PercentLemon lemon);
	}

	/**
	 * 注册一个当百分比动画结束时调用的回调
	 * 
	 * @param l
	 *            设定的回调
	 */
	public void setOnPercentAnimationEndListener(OnPercentAnimationEndListener l) {
		mOnPercentAnimationEndListener = l;
	}

	/**
	 * 注册一个当中心圆环被点击时调用的回调
	 * 
	 * @param l
	 *            设定的回调
	 */
	public void setOnHeartClickListener(OnHeartClickListener l) {
		mOnHeartClickListener = l;
	}

	/**
	 * 返回是否有绑定OnPercentAnimationEndListener回调。如果有返回true否则返回false。
	 */
	public boolean hasOnPercentAnimationEndListener() {
		return (mOnPercentAnimationEndListener != null);
	}

	/**
	 * 返回是否有绑定OnHeartClickListener回调。如果有返回true否则返回false。
	 */
	public boolean hasOnHeartClickListener() {
		return (mOnHeartClickListener != null);
	}

	/**
	 * 设置百分比动画播放时间长度。默认长度是1500毫秒。
	 * 
	 * @param duration
	 *            动画时间长度，单位是毫秒。
	 */
	@SuppressLint("NewApi")
	public void setAnimationDuration(long duration) {
		if (mArrivePercentAnimator != null) {
			mArrivePercentAnimator.setDuration(duration);
		}
	}

	// 根据给定的起始、结束颜色和百分比值计算当由起始颜色渐变到结束颜色经历所给百分比值时刻当前颜色
	private int getCurrentColor(int startColor, int endColor, float percent) {
		if (Float.compare(percent, 100.0f) > 0)
			percent = 100.0f;
		if (Float.compare(percent, 0.0f) < 0)
			percent = 0.0f;

		int startA = (startColor >> 24) & 0xff;
		int startR = (startColor >> 16) & 0xff;
		int startG = (startColor >> 8) & 0xff;
		int startB = startColor & 0xff;

		int endA = (endColor >> 24) & 0xff;
		int endR = (endColor >> 16) & 0xff;
		int endG = (endColor >> 8) & 0xff;
		int endB = endColor & 0xff;

		return (int) ((startA + (int) (percent * (endA - startA) / 100)) << 24)
				| (int) ((startR + (int) (percent * (endR - startR) / 100)) << 16)
				| (int) ((startG + (int) (percent * (endG - startG) / 100)) << 8)
				| (int) ((startB + (int) (percent * (endB - startB) / 100)));
	}

	@SuppressLint("NewApi")
	@Override
	protected Parcelable onSaveInstanceState() {
		// TODO:
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.percent = mPercent;
		ss.textColor = mTextColor;
		ss.skinStartColor = mSkinStartColor;
		ss.skinEndColor = mSkinEndColor;
		ss.heartColor = mHeartColor;
		ss.heartClickedColor = mHeartClickedColor;
		ss.skinDepth = mSkinDepth;
		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		// TODO:
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		setTextColor(ss.textColor);
		setSkinColor(ss.skinStartColor, ss.skinEndColor);
		setHeartColor(ss.heartColor);
		setHeartClickedColor(ss.heartClickedColor);
		setSkinDepth(ss.skinDepth);
		animatToPercent(ss.percent);
	}

	/**
	 * 用来保存PercentLemon的用户界面状态 {@link View#onSaveInstanceState}.
	 */
	static class SavedState extends BaseSavedState {
		public float percent;
		private int textColor;
		private int skinStartColor;
		private int skinEndColor;
		private int heartColor;
		private int heartClickedColor;
		private int skinDepth;

		SavedState(Parcelable superState) {
			super(superState);
		}

		public SavedState(Parcel source) {
			super(source);
			percent = source.readFloat();
			textColor = source.readInt();
			skinStartColor = source.readInt();
			skinEndColor = source.readInt();
			heartColor = source.readInt();
			heartClickedColor = source.readInt();
			skinDepth = source.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeFloat(percent);
			dest.writeInt(textColor);
			dest.writeInt(skinStartColor);
			dest.writeInt(skinEndColor);
			dest.writeInt(heartColor);
			dest.writeInt(heartClickedColor);
			dest.writeInt(skinDepth);
		}

		@Override
		public String toString() {
			return "PercentLemon.SavedState{"
					+ Integer.toHexString(System.identityHashCode(this))
					+ " percent=" + percent + "}";
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		// 默认宽度100像素
		return 100;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		// 默认高度100像素
		return 100;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec, heightMeasureSpec),
				measureHeight(widthMeasureSpec, heightMeasureSpec));
	}

	/**
	 * 计算PercentLemon的宽度应该为多少像素
	 * 
	 * @return 宽度
	 */
	private int measureWidth(int width, int height) {
		float result;
		int widthMode = MeasureSpec.getMode(width);
		int widthSize = MeasureSpec.getSize(width);

		int heightMode = MeasureSpec.getMode(height);
		int contentHeightSize = MeasureSpec.getSize(height) - getPaddingTop()
				- getPaddingBottom();
		if (contentHeightSize < 0) {
			contentHeightSize = 0;
		}

		if (widthMode == MeasureSpec.EXACTLY) {
			result = widthSize;
		} else {
			if (heightMode == MeasureSpec.EXACTLY) {
				result = getPaddingLeft() + getPaddingRight()
						+ contentHeightSize;
			} else {
				result = getPaddingLeft() + getPaddingRight()
						+ getSuggestedMinimumWidth();
			}
			if (widthMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, widthSize);
			}
		}
		if (localLOG) {
			Log.i(TAG, "宽度：" + result);
		}
		return (int) Math.ceil(result);
	}

	/**
	 * 计算PercentLemon的高度应该为多少像素
	 * 
	 * @return 高度
	 */
	private int measureHeight(int width, int height) {
		float result;
		int heightMode = MeasureSpec.getMode(height);
		int heightSize = MeasureSpec.getSize(height);

		int widthMode = MeasureSpec.getMode(width);
		int contentWidthSize = MeasureSpec.getSize(width) - getPaddingLeft()
				- getPaddingRight();
		if (contentWidthSize < 0) {
			contentWidthSize = 0;
		}

		if (heightMode == MeasureSpec.EXACTLY) {
			result = heightSize;
		} else {
			if (widthMode == MeasureSpec.EXACTLY) {
				result = getPaddingTop() + getPaddingBottom()
						+ contentWidthSize;
			} else {
				result = getPaddingTop() + getPaddingBottom()
						+ getSuggestedMinimumHeight();
			}
			if (heightMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, heightSize);
			}
		}
		if (localLOG) {
			Log.i(TAG, "高度：" + result);
		}
		return (int) Math.ceil(result);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int mXCenter = (w + getPaddingLeft() - getPaddingRight()) / 2;
		int mYCenter = (h + getPaddingTop() - getPaddingBottom()) / 2;
		int mRadius = Math.min(mXCenter - getPaddingLeft(), mYCenter
				- getPaddingTop());
		mSkinRawDepth = (int) (mSkinDepth / 100.0 * mRadius);
		mSkinPaint.setStrokeWidth(mSkinRawDepth);
		mBounds = new RectF(mXCenter - mRadius + mSkinRawDepth / 2, mYCenter
				- mRadius + mSkinRawDepth / 2, mXCenter + mRadius
				- mSkinRawDepth / 2, mYCenter + mRadius - mSkinRawDepth / 2);
	}

	/**
	 * 设置中心百分比文字颜色值。
	 * 
	 * @param color
	 *            文字颜色
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_centerTextColor
	 */
	public void setTextColor(int color) {
		if (mTextPaint.getColor() != color) {
			mTextColor = color;
			mTextPaint.setColor(mTextColor);
			invalidate();
		}
	}

	private void setRawTextSize(float size) {
		if (Float.compare(size, mTextPaint.getTextSize()) != 0) {
			mTextPaint.setTextSize(size);
		}
	}

	/**
	 * 设置中心圆的颜色值
	 * 
	 * @param heartColor
	 *            中心圆颜色
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_heartColor
	 */
	public void setHeartColor(int heartColor) {
		if (heartColor != mHeartColor) {
			mHeartColor = heartColor;
			mHeartPaint.setColor(mHeartColor);
			invalidate();
		}
	}

	/**
	 * 设置中心圆点击状态的颜色值
	 * 
	 * @param heartClickedColor
	 *            中心圆点击颜色
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_heartClickedColor
	 */
	public void setHeartClickedColor(int heartClickedColor) {
		if (heartClickedColor != mHeartClickedColor) {
			mHeartClickedColor = heartClickedColor;
		}
	}

	@SuppressWarnings("unused")
	private void setBreathHeartPercent(float percent) {
		mTouchPercent = percent;
		mHeartPaint.setColor(getCurrentColor(mHeartColor, mHeartClickedColor,
				percent));
		invalidate();
	}

	@SuppressWarnings("unused")
	private float getBreathHeartPercent() {
		return mTouchPercent;
	}

	/**
	 * 返回中心圆的颜色
	 * 
	 * @return 中心圆的颜色
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_heartColor
	 */
	public int getHeartColor() {
		return mHeartColor;
	}

	/**
	 * 设置外围百分比圆环颜色过渡范围，起始颜色为百分值为0时的颜色，结束颜色为百分值为100时的颜色，其他百分值外环颜色在起始与结束颜色之间。
	 * 
	 * @param startColor
	 *            起始颜色
	 * @param endColor
	 *            结束颜色
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_skinStartColor
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_skinEndColor
	 */
	public void setSkinColor(int startColor, int endColor) {
		if (startColor != mSkinStartColor || endColor != mSkinEndColor) {
			mSkinStartColor = startColor;
			mSkinEndColor = endColor;
			invalidate();
		}
	}

	/**
	 * 设置外围百分比圆环的宽度，数值在1到50之间，为外围圆环在整个View的半径中宽度相对于100所占比重。
	 * 
	 * @param skinDepth
	 *            外围圆环像宽度比重
	 * @throws IllegalArgumentException
	 *             如果传递的参数值不在1到50之间
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_skinDepth
	 */
	public void setSkinDepth(int skinDepth) {
		if (skinDepth != mSkinDepth) {
			if (skinDepth > 50 || skinDepth < 1) {
				throw new IllegalArgumentException("宽度比重必须在1到50之间");
			}
			mSkinDepth = skinDepth;
			invalidate();
		}
	}

	/**
	 * 设置百分比值，不包含动画过渡。如果需要动画过渡，请调用{@link #animatToPercent(float)}。
	 * 
	 * @param percent
	 *            百分比值0到100之间
	 * @throws IllegalArgumentException
	 *             如果传递的参数值不在0到100之间
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_percent
	 */
	@SuppressLint("NewApi")
	public void setPercent(float percent) {
		if (Float.compare(percent, mPercent) != 0) {
			if (Float.compare(percent, 100) > 0
					|| Float.compare(percent, 0) < 0) {
				throw new IllegalArgumentException("百分比值必须在0到100之间");
			}
			mPercent = percent;
			invalidate();
		}
	}

	/**
	 * 设置百分比值，包含动画过渡。如果不需要动画过渡，请调用{@link #setPercent(float)}。
	 * 
	 * @param toValue
	 *            百分比值0到100之间
	 * @throws IllegalArgumentException
	 *             如果传递的参数值不在0到100之间
	 */
	@SuppressLint("NewApi")
	public void animatToPercent(float toValue) {
		if (Float.compare(toValue, 100) > 0 || Float.compare(toValue, 0) < 0) {
			throw new IllegalArgumentException("百分比值必须在0到100之间");
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerToHW(this);
			stopChangingPercent();
			if (Float.compare(toValue, mPercent) != 0) {
				mArrivePercentAnimator.setFloatValues(toValue);
			} else {
				mArrivePercentAnimator.setFloatValues(0.0f, toValue);
			}
			mArrivePercentAnimator.start();
		} else {
			setPercent(toValue);
		}
	}

	/**
	 * 返回PercentLemon当前显示的百分值。
	 * 
	 * @return 百分比
	 * @attr ref cn.changwentao.lemon.R#PercentLemon_percent
	 */
	public float getPercent() {
		return mPercent;
	}

	@SuppressLint("NewApi")
	private void stopChangingPercent() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (mArrivePercentAnimator.isRunning())
				mArrivePercentAnimator.cancel();
		}
	}

	@SuppressLint("NewApi")
	private void setLayerToSW(View v) {
		if (!v.isInEditMode()
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}

	@SuppressLint("NewApi")
	private void setLayerToHW(View v) {
		if (!v.isInEditMode()
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}
	}
}