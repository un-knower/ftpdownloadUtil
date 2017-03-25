package cn.uway.util;

/**
 * NumberUtil
 * 
 * @author dell 2012-12-7
 */
public class NumberUtil {

	/**
	 * string转换为int 如果为空或者非法格式 返回0
	 * 
	 * @param string
	 * @return
	 */
	public static int parseInt(String string, Integer defaultValue) {
		try {
			return Integer.parseInt(string);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * string转换为long 如果为空或者非法格式 返回0
	 * 
	 * @param string
	 * @return
	 */
	public static long parseLong(String string, Long defaultValue) {
		try {
			return Long.parseLong(string);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * string转换为long 如果为空或者非法格式 返回0
	 * 
	 * @param string
	 * @return 字符串转换后的double数字 转换失败则返回0.0
	 */
	public static double parseDouble(String string, Double defaultValue) {
		try {
			return Double.parseDouble(string);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * float转换Util
	 * 
	 * @param string
	 * @return float number
	 */
	public static float parseFloat(String string, Float defaultValue) {
		try {
			return Float.parseFloat(string);
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
