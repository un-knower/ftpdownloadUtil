package cn.uway.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * IO工具类。
 * 
 * @author ChenSijiang 2012-10-28
 */
public final class IoUtil {

//	private static final Logger log = LoggerFactory.getLogger(IoUtil.class);

	/**
	 * 针对<code>java.io.Closeable</code>接口的关闭，忽略IO异常。
	 * 
	 * @param io
	 *            <code>java.io.Closeable</code>接口。
	 */
	public static void closeQuietly(Closeable io) {
		if (io == null)
			return;
		try {
			io.close();
		} catch (IOException e) {
//			log.warn("关闭IO时发生了异常。", e);
		}
	}

	/**
	 * 将流的数据全部读完。
	 * 
	 * @param in
	 *            流。
	 * @return 是否出现了异常。
	 * @throws NullPointerException
	 *             流为<code>null</code>时。
	 */
	public static boolean readFinish(InputStream in) {
		if (in == null)
			throw new NullPointerException("in");
		try {
			byte[] buff = new byte[1024];
			while (in.read(buff) > -1) {
			}
		} catch (Exception e) {
//			log.warn("[readFinish]读取InputStream异常。", e);
			return false;
		}
		return true;
	}

	private IoUtil() {
	}
}
