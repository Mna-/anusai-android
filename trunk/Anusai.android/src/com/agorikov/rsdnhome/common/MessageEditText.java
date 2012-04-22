package com.agorikov.rsdnhome.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import com.agorikov.rsdnhome.beans.Binding;
import com.agorikov.rsdnhome.beans.ChangeListener;
import com.agorikov.rsdnhome.beans.Observable;
import com.agorikov.rsdnhome.beans.ObservableValue;
import com.agorikov.rsdnhome.beans.Property;
import com.agorikov.rsdnhome.common.MessageViewFormatter.StyleMetric;

public class MessageEditText extends EditText {

	public MessageEditText(Context context) {
		super(context);
		controlInitialized = true;
	}
	
	public MessageEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		controlInitialized = true;
	}
	
	public MessageEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		controlInitialized = true;
	}

	private void checkInternalState() {
		if (this.textChangeAction == null) {
			initializeInternalState();
		}
	}
	
	private void initializeInternalState() {
		this.spanCache = new WeakHashMap<Object, Boolean>();
		this.selBold = new Property<Boolean>(false);
		this.selEm = new Property<Boolean>(false);
		this.selUnderline = new Property<Boolean>(false);
		this.selStrike = new Property<Boolean>(false);

		this.styleChangeListener = new ChangeListener<Boolean>() {
			@Override
			public void onChange(Observable bean, Boolean oldValue,
					Boolean newValue) {
				if (!inSelectionChangeListener) {
					final int from = getSelectionStart();
					final int to = getSelectionEnd();
					final StyleMetric filter;
					if (bean == selBold)
						filter = StyleMetric.bold;
					else if (bean == selEm)
						filter = StyleMetric.em;
					else if (bean == selUnderline)
						filter = StyleMetric.underline;
					else if (bean == selStrike)
						filter = StyleMetric.strike;
					else return;
					
					if (from <= to)
						updateTextStyle(from, to, filter);
					else
						updateTextStyle(to, from, filter);
				}
			}
		};
		selBold.addChangeListener(styleChangeListener);
		selEm.addChangeListener(styleChangeListener);
		selUnderline.addChangeListener(styleChangeListener);
		selStrike.addChangeListener(styleChangeListener);
		
		this.textChangeAction = HandlerCompositeUtils.wrapPostponedRunnable(new Runnable() {
			@Override
			public void run() {
				final Editable editable = getText();
				final String text = editable.toString();
				
				final int count = text.length();
				{
					final List<Object> cachedSpans = new ArrayList<Object>(spanCache.keySet());
					spanCache.clear();
					for (final Object span : cachedSpans) {
						editable.removeSpan(span);
					}
				}
				
				final Matcher m = MessageViewFormatter.replyPattern.matcher(text);
				for (int i = 0; m.find(i); i = m.end()) {
					final ForegroundColorSpan span = new ForegroundColorSpan(MessageViewFormatter.lineQuoteColor);
					spanCache.put(span, null);
					editable.setSpan(span,
							m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				
				final RadixTreeNode.SubstringMatcher<Integer> emoticonMatcher = new RadixTreeNode.SubstringMatcher<Integer>(MessageViewFormatter.rootEmoticonRadixTreeNode) {
					@Override
					protected void markSubstring(final int start, final int end, final Integer id) {
						final EmoticonSpan span = new EmoticonSpan((AnimationDrawable) EmoticonsManager.getInstance().getCached(id), 
								MessageEditText.this);
						spanCache.put(span, null);
						editable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}};
				
				for (int i = 0; i < count; ++i) {
					emoticonMatcher.putc(text.charAt(i));
				}
				emoticonMatcher.finish();
				markupBinding.invalidate();
			}});	

		this.cursorPosition = new Property<Integer>(0);
		this.cursorPositionListener = HandlerCompositeUtils.wrapPostponedListener(new ChangeListener<Integer> () {
			@Override
			public void onChange(final Observable bean, final Integer oldValue,
					final Integer sel) {
				inSelectionChangeListener = true;
				try {
					boolean bold = false, em = false, underline = false, strike = false;
					for (final CharacterStyle span : getText().getSpans(sel, sel + 1, CharacterStyle.class)) {
						if (span instanceof StyleSpan) {
							switch (((StyleSpan)span).getStyle()) {
							case Typeface.BOLD:
								bold = true;
								break;
							case Typeface.ITALIC:
								em = true;
								break;
							default:
								continue;
							}
						} else if (span instanceof UnderlineSpan) {
							underline = true;
						} else if (span instanceof StrikethroughSpan) {
							strike = true;
						} else continue;
					}
					
					selBold.set(bold);
					selEm.set(em);
					selUnderline.set(underline);
					selStrike.set(strike);
				} finally {
					inSelectionChangeListener = false;
				}
			}});
		cursorPosition.addChangeListener(cursorPositionListener);
		
		this.markupBinding = new Binding<String>() {
			@Override
			protected String calculate() {
				final Editable text = getText();
				final StringBuilder sb = new StringBuilder();

				final LinkedList<MetricMarker> metricMarkers = new LinkedList<MetricMarker>();
				for (final CharacterStyle span : text.getSpans(0, text.length(), CharacterStyle.class)) {
					final StyleMetric metric = MessageViewFormatter.fromCharacterStyle(span);
					if (metric != null) {
						metricMarkers.add(new MetricMarker(text.getSpanStart(span), metric, true));
						metricMarkers.add(new MetricMarker(text.getSpanEnd(span), metric, false));
					}
				}
				Collections.sort(metricMarkers, MetricMarker.positionComparator);
				final char[] buf = new char[1];
				int i = 0;
				for (final MetricMarker marker : metricMarkers) {
					for (; i < marker.position; ++i) {
						text.getChars(i, i + 1, buf, 0);
						sb.append(buf[0]);
					}
					sb.append(marker.getTag());
				}
				for (; i < text.length(); ++i) {
					text.getChars(i, i + 1, buf, 0);
					sb.append(buf[0]);
				}
				return sb.toString();
			}
		};
	}
	
	private static class Interval {
		final int start;
		final int end;
		Interval(final int start, final int end) {
			this.start = start;
			this.end = end;
		}
		
		static final Interval emptyInterval = new Interval(0,0);
	}
	
	private static Interval getSpanInterval(final Editable editable, final List<CharacterStyle> spans, int from, int to) {
		int start = 0;
		int end = 0;
		if (from <= to) {
			start = from;
			end = to;
		} else {
			start = to;
			end = from;
		}
		
		if (spans != null) {
			for (final CharacterStyle span : spans) {
				final int s = editable.getSpanStart(span);
				final int e = editable.getSpanEnd(span);
				if (s <= e) {
					start = Math.min(start, s);
					end = Math.max(end,  e);
				} else {
					start = Math.min(start, e);
					end = Math.max(end,  s);
				}
			}
		}
		if (start < end)
			return new Interval(start, end);
		else
			return Interval.emptyInterval;
	}
	
	
	private void updateTextStyle(final int from, final int to, final StyleMetric filter) {
		try {
			checkInternalState();
			final Editable editable = getText();
			final Map<StyleMetric, List<CharacterStyle>> styleSpanMap = new HashMap<StyleMetric, List<CharacterStyle>>();
			
			final int s, e;
			if (from < to) {
				s = Math.max(0, from - 1);
				e = to;
			} else {
				s = Math.max(0, to - 1);
				e = from;
			}
			
			for (final CharacterStyle span : editable.getSpans(s, e, CharacterStyle.class)) {
				final StyleMetric metric = MessageViewFormatter.fromCharacterStyle(span);
				if (metric == null)
					continue;
				
				if (filter != null && filter != metric)
					continue;
				
				final List<CharacterStyle> list = styleSpanMap.get(metric);
				if (list != null) {
					list.add(span);
				} else {
					final List<CharacterStyle> newList = new ArrayList<CharacterStyle>();
					newList.add(span);
					styleSpanMap.put(metric, newList);
				}
			}
			
			if (filter == null || filter == StyleMetric.bold)
				updateSpans(editable, from, to, StyleMetric.bold, styleSpanMap, selBold.get());
			if (filter == null || filter == StyleMetric.em)
				updateSpans(editable, from, to, StyleMetric.em, styleSpanMap, selEm.get());
			if (filter == null || filter == StyleMetric.underline)
				updateSpans(editable, from, to, StyleMetric.underline, styleSpanMap, selUnderline.get());
			if (filter == null || filter == StyleMetric.strike)
				updateSpans(editable, from, to, StyleMetric.strike, styleSpanMap, selStrike.get());
		} finally {
			markupBinding.invalidate();
		}
	}

	private static void setSpan(final Editable editable, final StyleMetric metric, final int from, final int to) {
		if (from == to) {
		} else if (from < to) {
			editable.setSpan(MessageViewFormatter.fromStyleMetric(metric), 
					from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			editable.setSpan(MessageViewFormatter.fromStyleMetric(metric), 
					to - 1, from, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	static void updateSpans(final Editable editable, final int from, final int to, final StyleMetric metric,
			final Map<StyleMetric, List<CharacterStyle>> styleSpanMap, final boolean enable) {
		final List<CharacterStyle> list = styleSpanMap.get(metric);
		final Interval interval = getSpanInterval(editable, list, from, to);
		if (list != null) {
			for (final CharacterStyle span : list) {
				editable.removeSpan(span);
			}
		}
		if (enable) {
			if (interval != Interval.emptyInterval)
				setSpan(editable, metric, interval.start, interval.end);
			else if (from < to)
				setSpan(editable, metric, from, to);
		} else if (interval != Interval.emptyInterval) {
			if (interval.start < from)
				setSpan(editable, metric, interval.start, from);
			if (to < interval.end)
				setSpan(editable, metric, to, interval.end);
		}
	}
	
	
	
	private Runnable textChangeAction;
	private WeakHashMap<Object, Boolean> spanCache;
	private Property<Boolean> selBold, selEm, selUnderline, selStrike;
	private ChangeListener<Integer> cursorPositionListener;
	private Property<Integer> cursorPosition;
	private boolean inSelectionChangeListener;
	private ChangeListener<Boolean> styleChangeListener;
	private boolean controlInitialized;
	
	
	private static class MetricMarker {
		final int position;
		final StyleMetric metric;
		final boolean start;
		
		MetricMarker(final int position, final StyleMetric metric,
				final boolean start) {
			this.position = position;
			this.metric = metric;
			this.start = start;
		}
		static final Comparator<MetricMarker> positionComparator = new Comparator<MetricMarker>() {
			@Override
			public int compare(MetricMarker o1, MetricMarker o2) {
				return o1.position - o2.position;
			}};
			
		final String getTag() {
			final String tag;
			switch (metric) {
			case bold: tag = "b"; break;
			case em: tag = "i"; break;
			case underline: tag = "u"; break;
			case strike: tag = "s"; break;
			default: throw new RuntimeException();
			}
			return "[" + (start ? "" : "/") + tag + "]";
		}
	}
	
	private Binding<String> markupBinding;
	
	public final Property<Boolean>  boldProperty() {
		return selBold;
	}
	
	public final Property<Boolean>  emProperty() {
		return selEm;
	}
	
	public final Property<Boolean>  underlineProperty() {
		return selUnderline;
	}

	public final Property<Boolean>  strikeProperty() {
		return selStrike;
	}
	

	@Override
	protected void onTextChanged(CharSequence text, int start,
			int lengthBefore, int lengthAfter) {
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		if (!controlInitialized) {
			selBold.set(false);
			selEm.set(false);
			selUnderline.set(false);
			selStrike.set(false);
			cursorPosition.set(0);
			cursorPositionListener.onChange(cursorPosition, null, cursorPosition.get());
		} else {
			checkInternalState();
			updateTextStyle(start, start + lengthAfter, null);
			textChangeAction.run();
		}
		markupBinding.invalidate();
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		super.onSelectionChanged(selStart, selEnd);
		checkInternalState();
		if (selStart <= selEnd)
			cursorPosition.set(Math.max(0, selEnd - 1));
		else
			cursorPosition.set(Math.max(0, selStart));
	}
	
	public final ObservableValue<String> readOnlyMarkup() {
		return markupBinding;
	}
	
	public final void setMarkup(final String markup) {
		controlInitialized = false;
		try {
			setText(MessageViewFormatter.format(markup != null ? markup : "", this));
		} finally {
			controlInitialized = true;
		}
	}
	
}
