package cn.uway.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 网络工具类
 * 
 * @author MikeYang
 * @Date 2012-10-29
 * @version 3.0
 * @since 1.0
 */
public final class NetUtil {

	private static String hostname;

	/**
	 * 获取本地计算机名
	 */
	public synchronized static String getHostName() {
		if (hostname != null && !hostname.trim().isEmpty())
			return hostname;

		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = (e != null && e.getMessage() != null ? e.getMessage() : e.getMessage().trim());
			try {
				hostname = hostname.split(":")[0].trim();
			} catch (Exception exx) {
			}
		}

		return hostname;
	}
}
