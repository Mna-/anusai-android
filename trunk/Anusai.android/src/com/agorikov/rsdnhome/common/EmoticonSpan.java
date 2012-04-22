package com.agorikov.rsdnhome.common;

import java.lang.ref.WeakReference;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.view.View;

public class EmoticonSpan extends DynamicDrawableSpan {

	private final AnimationDrawable animationDrawable;
	private final WeakReference<View> owner;
	private final int periodDelay;
	private int xOffset, yOffset;
	private int xSize, ySize;
	private boolean subscribed;

	public EmoticonSpan(final AnimationDrawable animationDrawable, final View owner) {
		super(ALIGN_BASELINE);
		this.animationDrawable = animationDrawable;
		this.owner = new WeakReference<View>(owner);
		int periodDelay = 0;
		for (int i = 0; i < animationDrawable.getNumberOfFrames(); ++i) {
			periodDelay += animationDrawable.getDuration(i);
		}
		this.periodDelay = periodDelay;
	}

	private final Runnable delayedRunnable = new Runnable(){
		@Override
		public void run() {
			nextFrame();
		}
	};

	void nextFrame() {
		if (periodDelay == 0)
			return;
		final View view = owner.get();
		if (view != null && view.isShown()) {
	        final int x = xOffset + view.getPaddingLeft();
	        final int y = yOffset + view.getPaddingTop();
			view.postInvalidate(x, y, 
					x + xSize, y + ySize);
		} else {
			subscribed = false;
			AnimationPoller.getInstance().unsubscribe(delayedRunnable);
		}
	}
	
	@Override
	public Drawable getDrawable() {
		int index = 0;
		if (periodDelay != 0) {
			long period = System.currentTimeMillis() % periodDelay;
			while (true) {
				period -= animationDrawable.getDuration(index);
				if (period <= 0) break;
				++index;
			}
		}
		return animationDrawable.getFrame(index);
	}
	
	
    /*
     * Copy-paste of super.getSize(...) but use getDrawable() to get the image/frame to calculate the size,
     * in stead of the cached drawable.
     */
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
        	
            fm.ascent = -rect.bottom; 
            fm.descent = 0; 

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        return rect.right;
    }

    /*
     * Copy-paste of super.draw(...) but use getDrawable() to get the image/frame to draw, in stead of
     * the cached drawable.
     */
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getDrawable();
        canvas.save();

        final Rect bounds = b.getBounds();
		int transY = bottom - bounds.bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        xOffset = (int) x; yOffset = transY;
        xSize = bounds.width(); ySize = bounds.height();
        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
        if (periodDelay != 0 && !subscribed) {
        	AnimationPoller.getInstance().subscribe(delayedRunnable);
        	subscribed = true;
        }
    }

}
