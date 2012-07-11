package com.agorikov.rsdnhome.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class Strings {

	public static String join(final String delimiter, final String... items) {
		return join(delimiter, Arrays.asList(items));
	}

	public static String join(final String delimiter, final Collection<?> items) {
		final StringBuilder sb = new StringBuilder();
		for (final Object item : items) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(item);
		}
		return sb.toString();
	}

	public static String joinFormat(final String delimiter,
			final String format, final String... items) {
		return joinFormat(delimiter, format, Arrays.asList(items));
	}

	public static String joinFormat(final String delimiter,
			final String format, final Collection<String> items) {
		final StringBuilder sb = new StringBuilder();
		for (final String item : items) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(String.format(format, item));
		}
		return sb.toString();
	}

	public static String joinPattern(final String delimiter, final String pat,
			final int number) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < number; ++i) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(pat);
		}
		return sb.toString();
	}

	public static String replyInitialsOld(final String s) {
		if (s != null) {
			final StringBuilder sb = new StringBuilder();
			final String inpStr = s.replaceAll("\\s", "");
			for (int i = 0; i < inpStr.length(); ++i) {
				final char c = inpStr.charAt(i);
				if (Character.isUpperCase(c))
					sb.append(c);
				else if (!Character.isLowerCase(c))
					return s;
			}
			if (sb.length() != 0)
				return sb.toString();
		}
		if (s != null && s.length() > 3) {
			return s.substring(0, 1).toUpperCase();
		}
		return s;
	}

	private final static HashMap<String, String> nickShortcutCache;

	static {
		nickShortcutCache = new HashMap<String, String>();

		// Fill in exceptions
		nickShortcutCache.put("Igor Trofimov", "iT");
		nickShortcutCache.put("_MarlboroMan_", "_MM_");
		nickShortcutCache.put("Hacker_Delphi", "H_D");
	}

	public static String replyInitialsJanus(final String nick) {
		final String shortcut = nickShortcutCache.get(nick);
		if (shortcut != null) {
			return shortcut;
		}

		// Getting shortcuts for nickname
		String shortName = "";
		if (nick.length() <= 3 && !nick.contains(" ")) {
			// Nick is no longer then 3 symbols
			shortName = nick.replaceAll("&", "").replaceAll("<", "")
					.replaceAll(">", "").replaceAll("\"", "")
					.replaceAll("'", "");

		} else {
			// Replace weird symbols with spaces.
			String un = nick.replaceAll("[^A-ZА-Яa-zа-я0-9]+", " ").trim();

			// Remove non-capital characters and digits.
			if (!un.contains(" ")) {
				shortName = un.replaceAll("[a-zа-я0-9]+", "");
				if (shortName.length() > 3)
					shortName = shortName.substring(0, 3);
			}

			// if nick is completely non-capital
			if (shortName.length() == 0) {
				// Split to words
				final String[] sa = un.split("\\s+");

				// Use first symbol only
				for (int i = 0; i < sa.length && i < 3; i++)
					shortName += sa[i].charAt(0);

				// Make upper case.
				shortName = shortName.toUpperCase();
			}
		}

		if (nickShortcutCache.size() < 200) {
			// Cache only first 200 nicks
			nickShortcutCache.put(nick, shortName);
		}
		return shortName;
	}

}
