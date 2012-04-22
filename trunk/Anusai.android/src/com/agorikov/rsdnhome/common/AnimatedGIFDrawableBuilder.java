package com.agorikov.rsdnhome.common;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

public class AnimatedGIFDrawableBuilder implements Builder<AnimationDrawable> {

	private GifDecoder decoder;
	private double density = 1.f;
	public static final Handler handler = new Handler();
	
	public final AnimatedGIFDrawableBuilder fromStream(final InputStream is) {
		final GifDecoder decoder = new GifDecoder();
		if (decoder.read(is) != 0) {
			throw new RuntimeException("Decode error");
		}
		this.decoder = decoder;
		return this;
	}
	
	public final AnimatedGIFDrawableBuilder density(final double density) {
		this.density = density;
		return this;
	}
	
	protected void reset() {
		this.decoder = null;
		this.density = 1.0f;
	}
	
	@Override
	public final AnimationDrawable build() {
		try {
			if (decoder == null) {
				throw new RuntimeException("No image");
			}
			final AnimationDrawable animationDrawable = new AnimationDrawable();
			for (int i = 0; i < decoder.getFrameCount(); ++i) {
	            final Bitmap bitmap = decoder.getFrame(i);
	            final BitmapDrawable drawable = new BitmapDrawable(bitmap);
	            // Explicitly set the bounds in order for the frames to display
	            final int width = (int) (bitmap.getWidth() * density + 0.5f);
				final int height = (int) (bitmap.getHeight() * density + 0.5f);
				drawable.setBounds(0, 0, width, height);
	            final int delay = decoder.getDelay(i);
				animationDrawable.addFrame(drawable, delay);
	            if (i == 0) {
	                // Also set the bounds for this container drawable
	            	animationDrawable.setBounds(0, 0, width, height);
	            }
			}
			animationDrawable.setOneShot(false);
			return animationDrawable;
		} finally {
			reset();
		}
	}

}
