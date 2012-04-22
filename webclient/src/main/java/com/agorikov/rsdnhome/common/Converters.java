package com.agorikov.rsdnhome.common;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ksoap2.serialization.SoapObject;

import com.agorikov.rsdnhome.common.util.Log;

public class Converters {
	
	final static String TAG = "Converters";
	
	public static String asString(final Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof SoapObject) {
			final SoapObject obj = (SoapObject) o;
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < obj.getPropertyCount(); ++i) {
				sb.append(asString(obj.getProperty(i)));
			}
			return sb.toString();
		}
		return o.toString();
	}
	
	
	public static Long asLong(final Object obj) {
		final String asString = asString(obj);
		return !asString.equals("") ? Long.parseLong(asString) : null;
	}
	
	
	private final static TimeZone inputTZ;
	private final static Pattern timeStampPat = Pattern.compile("^\\s*(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,3})|())");
	static {
		String tzId = null;
		for (final String timezoneId : TimeZone.getAvailableIDs()) {
			final String tz = timezoneId.toLowerCase();
			if (tz.contains("europe") && tz.contains("moscow")) {
				tzId = timezoneId;
				break;
			}
		}
		if (tzId != null)
			inputTZ = TimeZone.getTimeZone(tzId);
		else
			inputTZ = TimeZone.getDefault();
	}

	public static Date asDate(final Object obj) {
		final String asString = asString(obj);
		if (!asString.equals("")) {
			try {
				final Matcher m = timeStampPat.matcher(asString);
				if (m == null || !m.find() || m.groupCount() != 8)
					throw new ParseException(asString, 0);
				final Calendar cal = Calendar.getInstance(inputTZ);
				final int year = Integer.parseInt(m.group(1));
				final int month = Integer.parseInt(m.group(2));
				final int day = Integer.parseInt(m.group(3));
				final int hour = Integer.parseInt(m.group(4));
				final int minute = Integer.parseInt(m.group(5));
				final int sec = Integer.parseInt(m.group(6));
				final String msecStr = m.group(7);
				final long msec = (msecStr != null && !msecStr.equals("")) 
						? Long.parseLong(msecStr) : 0;
				cal.set(year, month - 1, day, hour, minute, sec);
				final Date dt = new Timestamp(cal.getTime().getTime() + msec);
				//Log.d(TAG, "Parse date " + asString + " to " + dt);
				return dt;
			} catch (final ParseException e) {
				Log.e(TAG, "Error in asDate", e);
			}
		}
		return null;
	}

	public static String safeFormatDate(final DateFormat df, final Date dt) {
		if (dt != null)
			return df.format(dt);
		else
			return null;
	}
	
	public static <E> ArrayList<E> asArrayList(final Iterable<E> it) {
		final ArrayList<E> out = new ArrayList<E>();
		for (final E e : it) {
			out.add(e);
		}
		return out;
	}


	public static String nonNullStr(final String s) {
		return s != null ? s : "";
	}
	
}
