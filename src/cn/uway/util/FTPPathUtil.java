package cn.uway.util;

import java.io.UnsupportedEncodingException;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;

/**
 * 实现采集路径时间通配符等处理
 * 
 * @ClassName: FTPPathUtil
 * @author Niow
 * @date: 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public class FTPPathUtil{

	protected static Logger logger = LogMgr.getInstance().getSystemLogger();

	public static final String BENGIN_TAG = "{d";

	public static final String END_TAG = "}";

	public static SimpleDateFormat format = new TimeSimpleFormat();

	/**
	 * 替换采集路径中的所有时间
	 * 
	 * @param gatherPath
	 *            例如:/ftp/lte/telecom/test_nokia/pm/neexport_{dyyyyMMdd}/A{
	 *            dyyyyMMdd.HHmm}*.xml.gz
	 * @param date
	 * @return
	 */
	public static String getReplacedPath(String gatherPath, Date date){
		if(gatherPath == null || gatherPath.length() == 0){
			return "";
		}
		int pos = 0;
		StringBuilder path = new StringBuilder(gatherPath);
		while(pos > -1){
			int bPos = path.indexOf(BENGIN_TAG);
			pos = bPos;
			if(bPos < 0){
				return path.toString();
			}
			int ePos = path.indexOf(END_TAG, bPos);
			pos = ePos;
			if(ePos < 0){
				return path.toString();
			}
			String pattern = path.substring(bPos + BENGIN_TAG.length(), ePos);
			format.applyPattern(pattern);
			String strDate = format.format(date);
			path.replace(bPos, ePos + 1, strDate);
		}
		return path.toString();
	}

	/**
	 * 从采集路径中提取所有时间格式
	 * 
	 * @param gatherPath
	 * @return
	 */
	public static List<String> getDatePatterns(String gatherPath){
		List<String> patterns = new ArrayList<String>();
		if(StringUtil.isEmpty(gatherPath)){
			return patterns;
		}
		StringBuilder path = new StringBuilder(gatherPath);
		String pattern = extractPattern(path);
		while(pattern != ""){
			patterns.add(pattern);
			pattern = extractPattern(path);
		}
		return patterns;
	}

	/**
	 * 从一段字符中提取第一个时间格式
	 * 
	 * @param startIndex
	 * @param path 会对path做截取动作，因此path需要copy一份
	 * @return
	 */
	public static String extractPattern(StringBuilder path){
		if(path == null || path.length() == 0){
			return "";
		}
		int bPos = path.indexOf(BENGIN_TAG);
		if(bPos < 0){
			return "";
		}
		int ePos = path.indexOf(END_TAG, bPos);
		if(ePos < 0){
			return "";
		}
		String pattern = path.substring(bPos + BENGIN_TAG.length(), ePos);
		path.delete(0, ePos);
		return pattern;
	}

	/**
	 * 计算路径的最大层级数
	 * 
	 * @param path
	 * @return
	 */
	public static int getPathMaxLevel(String gatherPath){
		if(gatherPath.endsWith("/")){
			gatherPath = gatherPath.substring(0, gatherPath.length() - 1);
		}
		if(!gatherPath.startsWith("/")){
			gatherPath = "/" + gatherPath;
		}
		String [] split = gatherPath.split("/");
		return split.length - 1;
	}

	/**
	 * 编码一条FTP路径
	 * 
	 * @param ftpPath FTP路径
	 * @return 编码后的路径
	 */
	public String encodeFTPPath(String ftpPath, String charset){
		try{
			String str = StringUtil.isNotEmpty(charset) ? new String(ftpPath.getBytes(charset), "iso_8859_1") : ftpPath;
			return str;
		}catch(UnsupportedEncodingException e){
			logger.error("设置的编码不正确:" + charset, e);
		}
		return ftpPath;
	}

	/**
	 * 解码一条FTP路径
	 * 
	 * @param ftpPath FTP路径
	 * @return 解码后的路径
	 */
	public String decodeFTPPath(String ftpPath, String charset){
		try{
			String str = StringUtil.isNotEmpty(charset) ? new String(ftpPath.getBytes("iso_8859_1"), charset) : ftpPath;
			return str;
		}catch(UnsupportedEncodingException e){
			logger.error("设置的编码不正确:" + charset, e);
		}
		return ftpPath;
	}

	/**
	 * 对周的时间做转换
	 * 
	 * @ClassName: TimeSimpleFormat
	 * @author Niow
	 * @date: 2014-7-17
	 */
	static class TimeSimpleFormat extends SimpleDateFormat{
		
		int weekDays[] = {7,1,2,3,4,5,6};
		
		/**  */
		private static final long serialVersionUID = -5563073568524624529L;

		public TimeSimpleFormat(String pattern){
			super(pattern);
		}
		
		public TimeSimpleFormat(){
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.text.SimpleDateFormat#format(java.util.Date,
		 * java.lang.StringBuffer, java.text.FieldPosition)
		 */
		@Override
		public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos){
			String pattern = super.toPattern();
			if(pattern == null || pattern.equals("")){
				return super.format(date, toAppendTo, pos);
			}
			calendar.setTime(date);
			int dayOfWeek = weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
			int ePos = pattern.indexOf('E');
			while(ePos > -1){
				pattern = pattern.replaceAll("E", String.valueOf(dayOfWeek));
				ePos = pattern.indexOf('E');
			}
			super.applyPattern(pattern);
			return super.format(date, toAppendTo, pos);
		}
	}

	public static void main(String [] args) throws ParseException{
//		String path = "/ftp/lte/telecom/test_nokia/pm/neexport{dE}/A20140526.0000+0800-0015+0800_FBJ900001.xml.gz/";
					String path2 = "/ftp/lte/telecom/test_nokia/pm{dE}/neexport_{dyyyyMMdd}{dyyyy-MM-dd}/*/A{dyyyyMMdd.HHmm}+0800-0015+0800_FBJ900001.xml.gz";
		//			List<String> datePatterns = getDatePatterns(path2);
		//			for(String p : datePatterns){
		//				System.out.println(p);
		//			}
		//			System.out.println(getReplacedPath(path2, new Date()));
		Date a = TimeUtil.getDate("2014-07-17 10:11:17");
		Calendar c = Calendar.getInstance();
		c.setTime(a);
		//		System.out.println(format.getDateInstance(SimpleDateFormat.DAY_OF_WEEK_FIELD).format(a));
		System.out.println(getReplacedPath(path2, a));
	}

}
