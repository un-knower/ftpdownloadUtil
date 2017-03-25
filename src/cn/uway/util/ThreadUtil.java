package cn.uway.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 增加线程工具类<br>
 * 比如一些常见的线程休眠不用再捕获异常
 * 
 * @author chenrongqiang @ 2013-4-21
 */
public final class ThreadUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadUtil.class); // 日志

	/**
	 * 线程休眠类
	 * 
	 * @param millis 休眠毫秒数
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.error("Thread={}线程休眠异常", Thread.currentThread().getName(),e);
		}
	}
	
	/**
	 * 线程休眠类
	 * 
	 * @param millis
	 */
	public static void await(long millis) {
		try {
			Thread.currentThread().wait(millis);
		} catch (InterruptedException e) {
			LOGGER.error("Thread={}线程挂起异常", Thread.currentThread().getName(),e);
		}
	}

}
