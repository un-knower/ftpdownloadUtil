package cn.uway.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

	/** 把 yyyy-MM-dd HH:mm:ss形式的字符串 转换成 时间 */
	public static Date getDate(String str) throws ParseException {
		String pattern = "yyyy-MM-dd HH:mm:ss";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMdd形式的字符串 转换成 时间 */
	public static Date getyyyyMMddDate(String str) throws ParseException {
		String pattern = "yyyyMMdd";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMddHH形式的字符串 转换成 时间 */
	public static Date getyyyyMMddHHDate(String str) throws ParseException {
		String pattern = "yyyyMMddHH";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMddHHmm形式的字符串 转换成 时间 */
	public static Date getyyyyMMddHHmmDate(String str) throws ParseException {
		String pattern = "yyyyMMddHHmm";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMddHHmmss形式的字符串 转换成 时间 */
	public static Date getyyyyMMddHHmmssDate(String str) throws ParseException {
		String pattern = "yyyyMMddHHmmss";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMdd_HHmm形式的字符串 转换成 时间 */
	public static Date getyyyyMMdd_HHmmDate(String str) throws ParseException {
		String pattern = "yyyyMMdd_HHmm";

		return getDate(str, pattern);
	}

	/** 把 yyyyMMdd_HHmmss形式的字符串 转换成 时间 */
	public static Date getyyyyMMdd_HHmmssDate(String str) throws ParseException {
		String pattern = "yyyyMMdd_HHmmss";

		return getDate(str, pattern);
	}

	/** 把 字符串 转换成 时间 */
	public static Date getDate(String str, String pattern) throws ParseException {
		SimpleDateFormat f = new SimpleDateFormat(pattern);
		Date d = f.parse(str);

		return d;
	}

	/** 转换Date为字符串格式 yyyy-MM-dd HH:mm:ss */
	public static String getDateString(Date date) {
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat f = new SimpleDateFormat(pattern);

		return f.format(date);
	}

	/** 转换Date为字符串格式 yyyyMMddHHmmss */
	public static String getyyyyMMddHHmmssDateString(Date date) {
		String pattern = "yyyyMMddHHmmss";
		SimpleDateFormat f = new SimpleDateFormat(pattern);

		return f.format(date);
	}

	/** 转换Date为字符串格式 yyyyMMdd */
	public static String getyyyyMMddDateString(Date date) {
		String pattern = "yyyyMMdd";
		SimpleDateFormat f = new SimpleDateFormat(pattern);

		return f.format(date);
	}

	/** 转换Timestamp为字符串格式 yyyy-MM-dd HH:mm:ss */
	public static String getDateString(Timestamp tsp) {
		Date date = new Date(tsp.getTime());

		return getDateString(date);
	}

	/** 转换时间为字符串格式 yyyyMMddHHmmss */
	public static String getDateString_yyyyMMddHHmmss(Timestamp tsp) {
		Date date = new Date(tsp.getTime());

		return getDateString_yyyyMMddHHmmss(date);
	}

	/** 转换时间为字符串格式 yyyyMMddHHmmss */
	public static String getDateString_yyyyMMddHHmmss(Date date) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
		String strTime = f.format(date);
		return strTime;
	}

	/** 转换时间为字符串格式 yyyyMMddHH */
	public static String getDateString_yyyyMMddHH(Date date) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
		String strTime = f.format(date);
		return strTime;
	}

	/** 转换时间为字符串格式 yyyyMMdd.HHmm */
	public static String getDateString_yyyyMMddHHmm(Date date) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd.HHmm");
		String strTime = f.format(date);
		return strTime;
	}

	/** 转换时间为字符串格式 yyyyMMddHHmm */
	public static String getDateStringyyyyMMddHHmm(Date date) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHHmm");
		String strTime = f.format(date);
		return strTime;
	}

	/** 转换时间为字符串格式 yyyyMMdd */
	public static String getDateString_yyyyMMdd(Date date) {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		String strTime = f.format(date);
		return strTime;
	}

	/**
	 * 判断是否最后一天
	 * 
	 * @param date
	 * @return true or false
	 */
	public static boolean isEndOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (day == getDaysOfMonth(date))
			return true;
		return false;
	}

	/**
	 * 判断是否最后一天
	 * 
	 * @param dateStr
	 *            （format:yyyy-MM-dd HH:mm:ss）
	 * @return
	 * @throws ParseException
	 */
	public static boolean isEndOfMonth(String dateStr) throws ParseException {
		return isEndOfMonth(getDate(dateStr));
	}

	/**
	 * 获取当前月份的总天数
	 * 
	 * @param date
	 * @return days of the month
	 */
	public static int getDaysOfMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.getActualMaximum(Calendar.DATE);
	}

	/**
	 * 按月判断是否在一个时间段内
	 * 
	 * @param date
	 * @param beginTime
	 *            开始时间：23:40
	 * @param endTime
	 *            截止时间：00:20
	 * @return boolean
	 * @throws Exception
	 */
	public static boolean isBetweenTimeByMonth(Date date, String beginTime, String endTime) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// 当前月的最后一天
		int lastDay = getDaysOfMonth(date);
		int thisDay = calendar.get(Calendar.DATE);
		// 当前月的最后一天或者第一天
		if (thisDay == lastDay || thisDay == 1) {
			return isBetweenTimeByDay(date, beginTime, endTime);
		}
		return false;
	}

	/**
	 * 按月判断是否在一个时间段内
	 * 
	 * @param dateStr
	 * @param beginTime
	 *            开始时间：23:40
	 * @param endTime
	 *            截止时间：00:20
	 * @return boolean
	 * @throws Exception
	 */
	public static boolean isBetweenTimeByMonth(String dateStr, String beginTime, String endTime) throws Exception {
		return isBetweenTimeByMonth(getDate(dateStr), beginTime, endTime);
	}

	/**
	 * 按天判断是否在一个时间段内
	 * 
	 * @param date
	 * @param beginTime
	 *            开始时间：23:40
	 * @param endTime
	 *            截止时间：00:20
	 * @return boolean
	 * @throws Exception
	 */
	public static boolean isBetweenTimeByDay(Date date, String beginTime, String endTime) throws Exception {
		// 开始时间
		Calendar beginCalendar = Calendar.getInstance();
		beginCalendar.setTime(date);
		setTime(beginTime, beginCalendar);

		// 截止时间
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(date);
		setTime(endTime, endCalendar);

		Date beginDate = beginCalendar.getTime();
		Date endDate = endCalendar.getTime();

		// 开始时间 = 截止时间
		if (beginDate.getTime() == endDate.getTime()) {
			return true;
		}

		// 开始时间 = date || 截止时间 = date
		if (date.getTime() == beginDate.getTime() || date.getTime() == endDate.getTime()) {
			return true;
		}

		// 00:20 < 23:40 || 23:00 = 23:00
		if (endDate.before(beginDate)) {
			// 23:40 - 00:01
			Calendar eCalendar = Calendar.getInstance();
			eCalendar.setTime(endDate);
			eCalendar.set(Calendar.HOUR_OF_DAY, 0);
			eCalendar.set(Calendar.MINUTE, 1);
			eCalendar.set(Calendar.DATE, eCalendar.get(Calendar.DATE) + 1);
			boolean resu1 = date.after(beginDate) && date.before(eCalendar.getTime());

			// 00:00 - 00:20
			Calendar bCalendar = Calendar.getInstance();
			bCalendar.setTime(beginDate);
			bCalendar.set(Calendar.HOUR_OF_DAY, 0);
			bCalendar.set(Calendar.MINUTE, 0);
			boolean resu2 = date.after(bCalendar.getTime()) && date.before(endCalendar.getTime());

			return resu1 || resu2;
		}

		// 23:59 > 23:40
		if (endDate.after(beginDate)) {
			return date.after(beginDate) && date.before(endDate);
		}

		return false;
	}

	/**
	 * 按天判断是否在一个时间段内
	 * 
	 * @param dateStr
	 * @param beginTime
	 *            开始时间：23:40
	 * @param endTime
	 *            截止时间：00:20
	 * @return boolean
	 * @throws Exception
	 */
	public static boolean isBetweenTimeByDay(String dateStr, String beginTime, String endTime) throws Exception {
		return isBetweenTimeByDay(getDate(dateStr), beginTime, endTime);
	}

	/**
	 * 设置时分秒
	 * 
	 * @param time
	 * @param calendar
	 * @return Hour
	 * @throws Exception
	 */
	public static void setTime(String time, Calendar calendar) throws Exception {
		int hour = Integer.parseInt(time.substring(0, time.indexOf(":")));
		int minu = Integer.parseInt(time.substring(time.indexOf(":") + 1));
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minu);
		calendar.set(Calendar.SECOND, 0);
	}

	/**
	 * 获取下个时间点
	 * 
	 * @param date
	 * @param periodMinutes
	 * @return
	 */
	public static Date nextTime(Date date, int periodMinutes) {
		long timeStamp = periodMinutes * 60 * 1000;
		return new Date(date.getTime() + timeStamp);
	}

	public static void main(String[] args) throws Exception {
		// System.out.println(isEndOfMonth("2014-1-31 1:00:00"));
		String fromTime = "22:50";
		String endTime = "22:50";
		System.out.println(isBetweenTimeByDay(new Date(), fromTime, endTime));
		System.out.println(isBetweenTimeByMonth(new Date(), fromTime, endTime));
		String dateStr1 = "2014-12-31 22:50:00";
		System.out.println(isBetweenTimeByDay(dateStr1, fromTime, endTime));
		System.out.println(isBetweenTimeByMonth(dateStr1, fromTime, endTime));
		String dateStr2 = "2008-12-1 22:50:50";
		System.out.println(isBetweenTimeByDay(dateStr2, fromTime, endTime));
		System.out.println(isBetweenTimeByMonth(dateStr2, fromTime, endTime));
	}
}
