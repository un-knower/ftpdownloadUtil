package cn.uway.cleaner;

import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;

import cn.uway.cache.MemoryCache;
import cn.uway.config.LogMgr;

public class MemoryCleaner extends Thread {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	// 内存中文件保留时间（单位：天）
	protected static int retentionTime = 1;

	// 任务时间周期
	protected static final long period = 24 * 60 * 60 * 1000;

	/** 加载定时器 **/
	private static Timer timer;

	public MemoryCleaner() {
		super("内存清理线程");
	}

	@Override
	public void run() {
		long min = period / 1000 / 60;
		LOGGER.debug("内存清理线程" + min + "分钟后开始执行，执行周期为" + min + "分钟");
		timer = new Timer("内存清理线程");
		timer.schedule(new ReloadTimerTask(), period, period);
	}

	/**
	 * 重载定时器任务
	 */
	class ReloadTimerTask extends TimerTask {

		public void run() {
			load();
		}
	}

	/**
	 * 从数据库中加载网元信息
	 * 
	 * @throws ParseException
	 */
	public void load() {
		LOGGER.debug("开始启动内存清理线程.");
		MemoryCache.removeOutDate(retentionTime);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
