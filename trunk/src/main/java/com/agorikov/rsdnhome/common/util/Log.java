package com.agorikov.rsdnhome.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.agorikov.rsdnhome.persist.DataModel;

public class Log {
	public interface UtilLog {
		void d(String tag, String msg);
		void d(String tag, String msg, Throwable e);
		void e(String tag, String msg);
		void e(String tag, String msg, Throwable e);
	}

	private boolean enableFileOutput;
	
	private Log() {
	}
	
	private void setEnableFileOutput(boolean enable) {
		if (enable == enableFileOutput) return;
		if (enable) {
			final File logFile = new File(DataModel.SETTINGS_DIR + "/" + "debug.log");
			if (logFile.exists())
				logFile.delete();
			try {
				out = new PrintStream(new FileOutputStream(logFile));
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} else {
			out.close();
			out = System.out;
		}
		enableFileOutput = enable;
	}
	
	private static Log instance = new Log();
	
	
	public static PrintStream out;
	private static final DateFormat timeFmt = DateFormat.getTimeInstance();
	private static final List<UtilLog> androidAppenders = new LinkedList<UtilLog>();
	
	static {
	}

	public static class DefaultUtilLog implements UtilLog {
		@Override
		public void e(String tag, String msg, Throwable e) {
			System.err.print("[" + tag + "] : " + msg);
			e.printStackTrace(System.err);
		}
		
		@Override
		public void e(String tag, String msg) {
			System.err.println("[" + tag + "] : " + msg);
		}
		
		@Override
		public void d(String tag, String msg, Throwable e) {
			System.out.print("[" + tag + "] : " + msg);
			e.printStackTrace(System.out);
		}
		
		@Override
		public void d(String tag, String msg) {
			System.out.println("[" + tag + "] : " + msg);
		}
	}
	
	
	public static void registerUtilLog(final UtilLog uLog) {
		androidAppenders.add(uLog);
	}
	
	public static void enableFileLog(boolean enable) {
		instance.setEnableFileOutput(enable);
	}
	
	public static void d(String tag, String msg) {
		for (final UtilLog uLog : androidAppenders) {
			uLog.d(tag, msg);
		}
		if (instance.enableFileOutput) {
			out.println(String.format("[%s]:%s: %s", tag, timeFmt.format(new Date()), msg));
			out.flush();
		}
	}
	
	public static void d(String tag, String msg, Throwable e) {
		for (final UtilLog uLog : androidAppenders) {
			uLog.d(tag, msg, e);
		}
		if (instance.enableFileOutput) {
			out.print(String.format("[%s]:%s: %s : ", tag, timeFmt.format(new Date()), msg));
			e.printStackTrace(out);
			out.println();
			out.flush();
		}
	}
	
	public static void e(String tag, String msg) {
		for (final UtilLog uLog : androidAppenders) {
			uLog.e(tag, msg);
		}
		if (instance.enableFileOutput) {
			out.println(String.format("[%s]:%s: %s", tag, timeFmt.format(new Date()), msg));
			out.flush();
		}
	}
	
	public static void e(String tag, String msg, Throwable e) {
		for (final UtilLog uLog : androidAppenders) {
			uLog.e(tag, msg, e);
		}
		if (instance.enableFileOutput) {
			out.print(String.format("[%s]:%s: %s : ", tag, timeFmt.format(new Date()), msg));
			e.printStackTrace(out);
			out.println();
			out.flush();
		}
	}
	
	
}
