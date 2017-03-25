package cn.uway.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.apache.mina.util.ConcurrentHashSet;

import cn.uway.config.LogMgr;
import cn.uway.util.FileUtil;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;

public class MemoryCache extends AbstractCache {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public static Set<String> hasDoneFileSet = new ConcurrentHashSet<String>();

	public FileCache fileCache;

	public MemoryCache() {
	}

	// 内存/本地同步缓存
	public void add(String file) {
		hasDoneFileSet.add(file);
		fileCache.add(file);
	}

	// 移除过期的文件
	public static void removeOutDate(int retentionTime) {
		try {
			Date now = new Date();
			for (String str : hasDoneFileSet) {
				String name = str.substring(FileUtil.getLastSeparatorIndex(str) + 1);
				String patternTime = StringUtil.getPattern(name, "\\d{8}");
				Date time = TimeUtil.getyyyyMMddDate(patternTime);
				if ((now.getTime() - time.getTime()) / 1000 / 60 / 60 / 24 >= retentionTime)
					hasDoneFileSet.remove(str);
			}
		} catch (ParseException e) {
			LOGGER.debug("清除内存报错");
		}
	}

	public void addByBatch(List<String> fileList) {
		if (fileList == null || fileList.size() == 0)
			return;
		for (String str : fileList) {
			add(str);
		}
	}

	public void load() throws Exception {
		File file = fileCache.fileWriter.file;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = reader.readLine()) != null) {
			hasDoneFileSet.add(line);
		}
	}

	public static boolean isHave(String file) {
		if (hasDoneFileSet.contains(file))
			return true;
		return false;
	}

	public FileCache getFileCache() {
		return fileCache;
	}

	public void setFileCache(FileCache fileCache) {
		this.fileCache = fileCache;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		final Set<String> hasDoneFileSet = new ConcurrentHashSet<String>();
		Thread thread1 = new Thread(new Runnable() {

			@Override
			public void run() {
				int count = 100000;
				while (count-- > 0) {
					hasDoneFileSet.add("123" + count);
				}
				System.out.println("add over");
			}
		});
		thread1.start();

		Thread.sleep(1000);

		Thread thread2 = new Thread(new Runnable() {

			@Override
			public void run() {
				int count = 0;
				for (String str : hasDoneFileSet) {
					hasDoneFileSet.contains(str);
					count++;
				}
				System.out.println("remove over:" + count);
			}
		});
		thread2.start();
	}
}
