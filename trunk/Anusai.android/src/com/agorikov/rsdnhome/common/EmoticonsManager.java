package com.agorikov.rsdnhome.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;

import com.agorikov.rsdnhome.app.R;
import com.agorikov.rsdnhome.app.RSDNApplication;
import com.agorikov.rsdnhome.common.util.Log;

public class EmoticonsManager implements ImageGetter {
	static final String TAG = "EmoticonsManager";

	public static Map<String, Integer> emoticonIdsMap;
	
	static {
		final Map<String, Integer> m = new HashMap<String, Integer>();
		m.put(":)", R.raw.smile);
		m.put(":-)", R.raw.smile);
		m.put(":(", R.raw.frown);
		m.put(":-(", R.raw.frown);
		m.put(";)", R.raw.wink);
		m.put(";-)", R.raw.wink);
		m.put(":))", R.raw.biggrin);
		m.put(":-))", R.raw.biggrin);
		m.put(":)))", R.raw.lol);
		m.put(":-)))", R.raw.lol);
		m.put(":-\\", R.raw.smirk);
		m.put(":???:", R.raw.confused);
		m.put(":no:", R.raw.no);
		m.put(":up:", R.raw.sup);
		m.put(":down:", R.raw.down);
		m.put(":super:", R.raw.super1);
		m.put(":shuffle:", R.raw.shuffle);
		m.put(":wow:", R.raw.wow);
		m.put(":crash:", R.raw.crash);
		m.put(":user:", R.raw.user);
		m.put(":maniac:", R.raw.maniac);
		m.put(":xz:", R.raw.xz);
		m.put(":beer:", R.raw.beer);
		
		emoticonIdsMap = Collections.unmodifiableMap(m);
	}
	
	private final AnimatedGIFDrawableBuilder animatedGIFBuilder = new AnimatedGIFDrawableBuilder();
	private final Map<Integer, Drawable> cache = new HashMap<Integer, Drawable>();
	
	private static EmoticonsManager instance_ = new EmoticonsManager();
	
	public static EmoticonsManager getInstance() {
		return instance_;
	}
	
	@Override
	public final Drawable getDrawable(final String source) {
		return getCached(emoticonIdsMap.get(source));
	}

	public Drawable getCached(int resId) {
		final Drawable img = cache.get(resId);
		if (img != null)
			return img;

		final InputStream is = RSDNApplication.getInstance().getResources().openRawResource(resId);
		final AnimationDrawable animatedGIF;
		try {
			animatedGIF = animatedGIFBuilder.density(
						RSDNApplication.getInstance().getResources().getDisplayMetrics().density)
						.fromStream(is).build();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.e(TAG, "Error in getCached", e);
			}
		}
		cache.put(resId, animatedGIF);
		return animatedGIF;
	}

}
