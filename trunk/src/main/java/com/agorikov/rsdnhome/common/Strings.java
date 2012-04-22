package com.agorikov.rsdnhome.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {
	
	public static String join(final String delimiter, final String ...items) {
		return join(delimiter, Arrays.asList(items));
	}
	
	public static String join(final String delimiter, final Collection<String> items) {
		final StringBuilder sb = new StringBuilder();
		for (final String item : items) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(item);
		}
		return sb.toString();
	}
	
	public static String joinFormat(final String delimiter, final String format, final String ...items) {
		return joinFormat(delimiter, format, Arrays.asList(items));
	}
	
	public static String joinFormat(final String delimiter, final String format, final Collection<String> items) {
		final StringBuilder sb = new StringBuilder();
		for (final String item : items) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(String.format(format, item));
		}
		return sb.toString();
	}

	public static String joinPattern(final String delimiter, final String pat, final int number) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < number; ++i) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(pat);
		}
		return sb.toString();
	}

	public static String splitCamelCase(final String s) {
	   return s.replaceAll(
	      String.format("%s|%s|%s",
	         "(?<=[A-ZА-Я])(?=[A-ZА-Я][a-zа-я])",
	         "(?<=[^A-ZА-Я])(?=[A-ZА-Я])",
	         "(?<=[A-Za-zА-яа-я])(?=[^A-Za-zА-Яа-я])"
	      ), " ");
	}
	private static final Pattern firstWPat = Pattern.compile("\\w");
	
	public static String initials(final String s) {
		final StringBuilder sb = new StringBuilder();
		final String splitCamelCase = splitCamelCase(s);
		for (final String word : splitCamelCase.split("[\\s\\-_]{1,}")) {
			if (word.length() != 0) {
				final Matcher m = firstWPat.matcher(word);
				if (m.find())
					sb.append(m.group().toUpperCase());
				else
					sb.append(word.substring(0, 1).toUpperCase());
			}
		}
		if (sb.length() > 1)
			return sb.toString();
		else
			return s.replace(" \t\r\n", "");
	}
	
	
	
}
