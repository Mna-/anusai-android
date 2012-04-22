package com.agorikov.rsdnhome.model.text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.agorikov.rsdnhome.common.util.Log;

public abstract class MessageParser {
	final static String TAG = "MessageParser";
	
	private static class TagStruct {
		final int start;
		final int length;
		final String args;
		
		public TagStruct(final int start, final int length, final String args) {
			this.start = start;
			this.length = length;
			this.args = args != null && args.length() != 0 ? args : null;
		}
	}
	
	private static class Tag {
		final String name;
		final String args;
		final int start;
		final int length;
		
		public Tag(final String name, final String args, final int start, final int length) {
			this.name = name;
			this.args = args != null && args.length() != 0 ? args : null;
			this.start = start;
			this.length = length;
		}
		@Override
		public String toString() {
			return name + "{" + args +"}: " + start + ":" + length;
		}
	}
	
	public static final Set<String> unaryTags = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"hr", "*")));
	
	
	public final void parse(final String text) throws IOException {
		final InputStream inp = new ByteArrayInputStream(text.getBytes());
		parse(inp);
	}
	
	public void parse(final InputStream inp) throws IOException {
		final StringBuilder buffered;
		if (inp.markSupported()) {
			buffered = null;
			inp.mark(Integer.MAX_VALUE);
		} else {
			buffered = new StringBuilder();
		}
		firstPass(inp, buffered);
		if (buffered == null) {
			inp.reset();
			secondPass(inp);
		} else {
			secondPass(new ByteArrayInputStream(buffered.toString().getBytes()));
		}
		finish();
	}

	protected void finish() {
	}

	private void secondPass(final InputStream inp) throws IOException {
		int skip = 0;
		final ArrayList<Integer> skipStartIndexes = new ArrayList<Integer>();
		final ArrayList<Integer> skipAccum = new ArrayList<Integer>();
		final InputStreamReader rd = new InputStreamReader(inp);
		int counter = 0;
		boolean silenceOutput = false;
		
		for (final Map.Entry<Integer, Integer> skipE : skipMarkers.entrySet()) {
			final int offset = skipE.getKey();
			final int tagLength = skipE.getValue();
			final int end = offset + tagLength;
		
			if (!silenceOutput) {
				for (; counter < offset; ++counter)
					outputText((char) rd.read());
			}
			for (; counter < end; ++counter)
				rd.read();
			skipStartIndexes.add(offset);
			skipAccum.add(skip);
			skip += tagLength;
			
			final Tag tag = tagMap2.get(counter);
			if (tag != null) {
				int indexHigh = Collections.binarySearch(skipStartIndexes, tag.start + tag.length);
				if (indexHigh < 0)
					indexHigh = -(indexHigh + 1);
				final int skipHigh = indexHigh < skipAccum.size() ? skipAccum.get(indexHigh) : 0;

				int indexLow = Collections.binarySearch(skipStartIndexes, tag.start);
				if (indexLow < 0)
					indexLow = -(indexLow + 1);
				final int skipLow = indexLow < skipAccum.size() ? skipAccum.get(indexLow) : 0;
				
				final int length = tag.length - skipHigh + skipLow;
				markTag(tag.name, tag.args, length);
			}
		}
		for (;; ++counter) {
			int c = rd.read();
			if (c == -1) 
				break;
			outputText((char) c);
		}
	}


	protected abstract void outputText(char c);

	protected abstract void markTag(final String name, final String args, final int length);


	private void firstPass(final InputStream inp,
			final StringBuilder buffered) throws IOException {
		boolean inTag = false;
		final InputStreamReader rd = new InputStreamReader(inp);
		
		int counter = 0;
		final StringBuilder tagSB = new StringBuilder();
		int startTagOffset = 0;
		for (;;++counter) {
			final int b = rd.read();
			if (b == -1)
				break;
			final char c = (char)b;
			if (inTag) {
				if (c == ']') {
					inTag = false;
					// close tag
					putTag(tagSB.toString(), startTagOffset, counter - startTagOffset + 1);
					tagSB.delete(0, tagSB.length());
				} else {
					tagSB.append(c);
				}
				
			} else if (c == '[') {
				inTag = true;
				startTagOffset = counter;
			} else {
				// ignore for now
			}
			if (buffered != null)
				buffered.append(c);
		}
	}
	
	private static final Pattern tagPattern = Pattern.compile("(?:\\s*/\\s*([^\\s=]{1,}))|(?:\\s*(?:([^\\s=]{1,})\\s*(=\\s*|)))");
	
	private final Map<String, TagStruct> tagMap = new HashMap<String, TagStruct>();
	private final TreeMap<Integer, Integer> skipMarkers = new TreeMap<Integer, Integer>();
	private final TreeMap<Integer, Tag> tagMap2 = new TreeMap<Integer, Tag>();
	
	
	private void putTag(final String tag, final int start, final int length) {
		final Matcher m = tagPattern.matcher(tag);
		if (!m.find())
			return;
		final String group1 = m.group(1);
		final String group2 = m.group(2);
		if (group1 == null) {
			if (unaryTags.contains(group2)) {
				skipMarkers.put(start, length);
				tagMap2.put(start + length, new Tag(group2, tag.substring(m.end(3)), start, 0));
			} else if (!tagMap.containsKey(group2)) {
				tagMap.put(group2, new TagStruct(start, length, tag.substring(m.end(3))));
			}
		} else {
			final TagStruct openTag = tagMap.remove(group1);
			if (openTag != null) {
				skipMarkers.put(openTag.start, openTag.length);
				skipMarkers.put(start, length);
				tagMap2.put(start + length, new Tag(group1, openTag.args, openTag.start, 
						start - openTag.start));
			}
		}
	}







	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws IOException {
		final StringBuilder outputText = new StringBuilder();

		final MessageParser parser = new MessageParser() {
			@Override
			protected void outputText(char c) {
				outputText.append(c);
			}
			@Override
			protected void markTag(String name, String args, int length) {
				Log.d(TAG, outputText.toString());
				Log.d(TAG, String.format("Mark tag \"%s\" with length %d", name, length));
			}
		};
		final String testString = "[*][url= a]здесь[/url]Started at 2010-02-08 by [b]xBlackCat[/b]\n";//"ab[b]c[s]d[/b]e[/s]f[*]";
		final InputStream inp = new ByteArrayInputStream(testString.getBytes());
		parser.parse(inp);
		
		Log.d(TAG, parser.tagMap2.toString());
		
		
	}

}
