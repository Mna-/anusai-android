package com.agorikov.rsdnhome.common;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.text.MessageParser;

public final class MessageViewFormatter extends MessageParser {
	final static String TAG = "MessageViewFormatter";
	
	static final RadixTreeNode<Integer> rootEmoticonRadixTreeNode;
	static {
		rootEmoticonRadixTreeNode = RadixTreeNode.create();
		for (final Map.Entry<String, Integer> e : EmoticonsManager.emoticonIdsMap.entrySet()) {
			rootEmoticonRadixTreeNode.insert(e.getKey()).setValue(e.getValue());
		}
	}
	
	private final SpannableStringBuilder sb;
	private final RadixTreeNode.SubstringMatcher<Integer> emoticonMatcher;
	public static final Pattern replyPattern = Pattern.compile("^(\\S{1,}?)\\s{0,1}(>{1,})(?:.*?)$", Pattern.MULTILINE);
	private static final Pattern urlPattern = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	static int lineQuoteColor = 0xff338822;
	final static int taglineColor = 0xffa52a2a;
	
	protected MessageViewFormatter(final SpannableStringBuilder sb, final TextView owner) {
		this.sb = sb;
		this.emoticonMatcher = new RadixTreeNode.SubstringMatcher<Integer>(rootEmoticonRadixTreeNode) {
			@Override
			protected void markSubstring(final int start, final int end, final Integer id) {
				sb.setSpan(new EmoticonSpan((AnimationDrawable) EmoticonsManager.getInstance().getCached(id), 
				owner), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}};
	}
	
	public static SpannableStringBuilder format(final String text, final TextView owner) {
		final SpannableStringBuilder sb = new SpannableStringBuilder();
		try {
			if (text != null)
				new MessageViewFormatter(sb, owner).parse(text);
		} catch (IOException e) {
			Log.e(TAG, "Error in MessageViewFormatter.parse", e);
		}
		return sb;
	}
	
	@Override
	protected void outputText(final char c) {
		sb.append(c);
		emoticonMatcher.putc(c);
	}
	
	enum StyleMetric {
		bold, em, underline, strike;
	};
	
	static CharacterStyle fromStyleMetric(final StyleMetric metric) {
		switch (metric) {
		case bold: return new StyleSpan(Typeface.BOLD);
		case em: return new StyleSpan(Typeface.ITALIC);
		case underline: return new UnderlineSpan();
		case strike: return new StrikethroughSpan();
		default: throw new RuntimeException(String.valueOf(metric));
		}
	}

	static StyleMetric fromCharacterStyle(final CharacterStyle span) {
		final StyleMetric metric;
		if (span instanceof StyleSpan) {
			switch (((StyleSpan)span).getStyle()) {
			case Typeface.BOLD:
				metric = StyleMetric.bold;
				break;
			case Typeface.ITALIC:
				metric = StyleMetric.em;
				break;
			default:
				return null;
			}
		} else if (span instanceof UnderlineSpan) {
			metric = StyleMetric.underline;
		} else if (span instanceof StrikethroughSpan) {
			metric = StyleMetric.strike;
		} else return null;
		return metric;
	}

	
	
	@Override
	protected void markTag(final String name, final String args, final int length) {
		final int start = sb.length() - length;
		final int end = start + length;
		if (name.equals("b")) {
			sb.setSpan(fromStyleMetric(StyleMetric.bold), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (name.equals("i")) {
			sb.setSpan(fromStyleMetric(StyleMetric.em), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (name.equals("s")) {
			sb.setSpan(fromStyleMetric(StyleMetric.strike), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (name.equals("u")) {
			sb.setSpan(fromStyleMetric(StyleMetric.underline), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (name.equals("tagline")) {
			sb.setSpan(new LeadingMarginSpan.Standard(20), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			sb.setSpan(new ForegroundColorSpan(taglineColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (name.equals("url")) {
			if (args != null) {
				final String url = args;
				final Matcher m = urlPattern.matcher(url);
				if (m.find() && m.start() == 0)
					sb.setSpan(new URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		} else if (name.equals("*")) {
			sb.setSpan(new BulletSpan(), start, end, Spanned.SPAN_MARK_MARK);
		} else {
			return;
		}
	}
	
	@Override
	protected void finish() {
		super.finish();
		emoticonMatcher.finish();
		markQuotes();
		markUrls();
	}

	private void markQuotes() {
		final Matcher m = replyPattern.matcher(sb);
		for (int i = 0; m.find(i); i = m.end()) {
			sb.setSpan(new ForegroundColorSpan(lineQuoteColor),
					m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private void markUrls() {
		final Matcher m = urlPattern.matcher(sb);
		for (int i = 0; m.find(i); i = m.end()) {
			final String url = m.group(0);
			sb.setSpan(new URLSpan(url),
					m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
	
	
	
	
	
}
