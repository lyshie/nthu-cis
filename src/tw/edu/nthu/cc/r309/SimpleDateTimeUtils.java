package tw.edu.nthu.cc.r309;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SimpleDateTimeUtils {
	protected static final String formatOnlyDate = "yyyy/MM/dd";
	protected static final String formatOnlyTime = "HH:mm:ss";
	protected static final String formatOnlyTimeMarker = "a hh:mm:ss";
	protected static final String formatMedium = "yyyy/MM/dd HH:mm";
	protected static final String formatMediumMarker = "yyyy/MM/dd a hh:mm";
	protected static final String formatFull = "yyyy/MM/dd HH:mm:ss";
	protected static final String formatFullMarker = "yyyy/MM/dd a hh:mm:ss";
	protected static final String formatFullTimeZone = "yyyy/MM/dd HH:mm:ss (Z)";
	protected static final String formatFullTimeZoneMarker = "yyyy/MM/dd a hh:mm:ss (Z)";

	public static long parseDateTime(String format, String datetime) {
		long result = 0;

		SimpleDateFormat sdf = new SimpleDateFormat(format);

		Date date = null;

		try {
			date = sdf.parse(datetime);
		} catch (ParseException e) {
			// e.printStackTrace();
		}

		if (date != null) {
			result = date.getTime();
		}

		return result;
	}

	public static String formatDateTime(String format, long datetime) {
		SimpleDateFormat ndf = new SimpleDateFormat(format);

		return ndf.format(new Date(datetime));
	}
	/*
	 * String data = "2014/05/26 下午 02:20:11";
	 * 
	 * SimpleDateFormat sdt = new SimpleDateFormat("y/M/d H:m:s");
	 * SimpleDateFormat ndf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss (Z)");
	 * 
	 * Date mydt = null; try { mydt = sdt.parse(data); } catch (ParseException
	 * e) { System.err.println(e.toString()); }
	 * 
	 * if (mydt != null) { String result = ndt.format(mydt);
	 * System.out.println(result); }
	 */
}
