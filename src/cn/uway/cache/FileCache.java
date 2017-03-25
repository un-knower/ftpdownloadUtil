package cn.uway.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.config.SystemConfig;
import cn.uway.file.FileGenerator;
import cn.uway.util.FileUtil;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;

/**
 * 文件缓存有两个：一是ok文件目录（当目录做完后，加上.ok的后缀，标记作用)，二是log文件
 * 
 * @author yuy @ 29 May, 2014
 */
public class FileCache extends AbstractCache {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public static String path = "./cache";

	// dataTime的日志，正在记录所有文件
	public static String tmpSmallExdName = ".log.tmp";

	// dataTime的日志，已记录完所有文件
	public static String smallExdName = ".log";

	// 每天的日志目录，表示已完成
	public static String exdName = ".ok";

	// 每天的日志记录，表示正在生成
	public static String tmpExdName = ".tmp";

	// 每次加载几个时间点
	public static int times = 5;

	public FileGenerator fileWriter;

	public List<String> fileList;

	public Date startTime;

	public Date endTime;

	public static int filePeriod = Integer.parseInt(SystemConfig.getInstance().getFileGeneratePeriod());

	public FileCache(Date startTime, Date endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public FileCache(String dateString) {
		fileWriter = new FileGenerator(path + "/" + dateString.substring(0, 8) + tmpExdName, dateString + tmpSmallExdName);
	}

	// 增加一条文件记录
	public void add(String filename) {
		fileWriter.write((filename + "\n").getBytes());
	}

	public void addByBatch(List<String> list) {
		if (list == null || list.size() == 0)
			return;
		for (String str : list) {
			fileWriter.write((str + "\n").getBytes());
		}
	}

	// 获取最近的dataTime(时间仓促，有点乱，以后慢慢优化吧)
	public synchronized List<Date> handleDataTime(List<String> fileList, Date dateTime_) throws Exception {
		if (fileList == null || fileList.size() == 0)
			return null;

		List<Date> dateTimeList = new ArrayList<Date>();

		if (dateTime_ != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateTime_);
			dateTimeList.add(calendar.getTime());
			for (int n = 0; n < times; n++) {
				calendar.add(Calendar.MINUTE, filePeriod);
				// 如果超过当前时间，打住
				if (calendar.getTime().getTime() >= new Date().getTime())
					break;
				dateTimeList.add(calendar.getTime());
			}
			return dateTimeList;
		}
		// 先排序 增序
		sortDirs();
		// 后去遍历
		for (String file : fileList) {
			// 检查下是否超过7天，是，删除。趁着这个机会把活干了
			if (file.endsWith(exdName)) {
				String name = file.substring(0, file.indexOf("."));
				Date time = TimeUtil.getyyyyMMddHHmmssDate(name);
				int days = (int) (new Date().getTime() - time.getTime()) / 1000 / 60 / 60 / 24;
				if (days >= 7)
					new File(file).delete();
			}
			// 判断是否存在tmp文件夹
			if (file.endsWith(tmpExdName)) {
				String patternTime = StringUtil.getPattern(file, "\\d{8}");
				Date currentDataTime = TimeUtil.getyyyyMMddDate(patternTime);
				// 与任务开始时间比较
				String dataStr = TimeUtil.getDateString_yyyyMMdd(startTime);
				if (currentDataTime.getTime() < TimeUtil.getyyyyMMddDate(dataStr).getTime()) {
					continue;
				}
				if ((endTime != null && currentDataTime.getTime() > endTime.getTime())) {
					continue;
				}
				// String dateStr = file.substring(0, file.lastIndexOf("."));
				List<String> smalllist = FileUtil.getFileNames(file, "*" + smallExdName + "*");
				if (smalllist.size() == 0)
					continue;
				// 倒序
				sortSmallFiles(smalllist);
				for (String f : smalllist) {
					// 判断small文件中是否存在tmp文件
					if (f.endsWith(tmpSmallExdName)) {
						String name = f.substring(FileUtil.getLastSeparatorIndex(f) + 1);
						name = name.substring(0, name.indexOf("."));
						dateTimeList.add(TimeUtil.getyyyyMMddHHmmssDate(name));
						// 加载到内存中
						BufferedReader reader = null;
						try {
							reader = new BufferedReader(new FileReader(f));
							String line = null;
							while ((line = reader.readLine()) != null) {
								MemoryCache.hasDoneFileSet.add(line);
							}
						} catch (Exception e) {
							LOGGER.debug("读取文件" + f + "出错", e);
						} finally {
							reader.close();
						}
					}
				}
				int c = 24 * 60 / filePeriod;
				String lastSmallFile = smalllist.get(smalllist.size() - 1);
				// 既存在.tmp，最后一个small文件也是.log，add该文件的下个时间点
				if (dateTimeList.size() > 0 && lastSmallFile.endsWith(smallExdName)) {
					String name = lastSmallFile.substring(FileUtil.getLastSeparatorIndex(lastSmallFile) + 1);
					name = name.substring(0, name.indexOf("."));
					Date date = TimeUtil.getyyyyMMddHHmmssDate(name);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					calendar.add(Calendar.MINUTE, filePeriod);
					// 同一天
					if (calendar.get(Calendar.DAY_OF_MONTH) != day) {
						continue;
					}
					// 如果超过当前时间，打住
					if (calendar.getTime().getTime() >= new Date().getTime())
						continue;
					dateTimeList.add(calendar.getTime());
				}

				// 不存在.tmp，但是还没做完，找到最近一个文件的时间，加filePeriod
				if (dateTimeList.size() == 0 && smalllist.size() < c) {
					String lastFile = smalllist.get(smalllist.size() - 1);
					String name = lastFile.substring(FileUtil.getLastSeparatorIndex(lastFile) + 1);
					name = name.substring(0, name.indexOf("."));
					Date dateTime = TimeUtil.getyyyyMMddHHmmssDate(name);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(dateTime);
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					// 加filePeriod
					calendar.add(Calendar.MINUTE, filePeriod);
					// 同一天
					if (calendar.get(Calendar.DAY_OF_MONTH) != day) {
						continue;
					}
					// 如果超过当前时间，打住
					if (calendar.getTime().getTime() >= new Date().getTime())
						continue;
					dateTimeList.add(calendar.getTime());
					for (int n = 0; n < times; n++) {
						calendar.add(Calendar.MINUTE, filePeriod);
						if (calendar.get(Calendar.DAY_OF_MONTH) != day) {
							break;
						}
						// 如果超过当前时间，打住
						if (calendar.getTime().getTime() >= new Date().getTime())
							break;
						dateTimeList.add(calendar.getTime());
					}
				}
				// 发现都是log的，且数量为24*60/filePeriod，则表明做完了，把父目录改为ok。趁着这个机会把活干了
				if (dateTimeList.size() == 0 && smalllist.size() == c) {
					File parentFile = new File(path + "/" + file);
					parentFile.renameTo(new File(path + "/" + file.replace(tmpExdName, exdName)));
				}
				// 如果已经有了dataTime，先退出，做完再说
				if (dateTimeList.size() > 0) {
					break;
				}
			}
		}
		// 全部ok文件夹，找到最近一个
		if (dateTimeList.size() == 0) {
			String lastFile = fileList.get(fileList.size() - 1);
			String name = lastFile.substring(FileUtil.getLastSeparatorIndex(lastFile) + 1);
			name = name.substring(0, name.indexOf("."));
			Date dateTime = TimeUtil.getyyyyMMddDate(name);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateTime);

			List<String> smalllist = FileUtil.getFileNames(lastFile, "*" + smallExdName + "*");
			// 不是空目录 加1天
			if (smalllist.size() > 0) {
				calendar.add(Calendar.DAY_OF_MONTH, 1);
			}
			while (calendar.getTime().getTime() < startTime.getTime()) {
				calendar.add(Calendar.MINUTE, filePeriod);
			}
			if (calendar.getTime().getTime() >= new Date().getTime())
				return dateTimeList;
			dateTimeList.add(calendar.getTime());
			for (int n = 0; n < times; n++) {
				calendar.add(Calendar.MINUTE, filePeriod);
				// 如果超过当前时间，打住
				if (calendar.getTime().getTime() >= new Date().getTime())
					break;
				dateTimeList.add(calendar.getTime());
			}
		}

		return dateTimeList;
	}

	// 目录排序 增序
	public void sortDirs() throws ParseException {
		int n = 0;
		while (true) {
			int count = 0;
			for (int m = n + 1; m < fileList.size(); n++, m++) {
				String file = fileList.get(n);
				String name = file.substring(FileUtil.getLastSeparatorIndex(file) + 1);
				name = name.substring(0, name.indexOf("."));
				Date date = TimeUtil.getyyyyMMddDate(name);
				String file_ = fileList.get(m);
				String name_ = file_.substring(FileUtil.getLastSeparatorIndex(file_) + 1);
				name_ = name_.substring(0, name_.indexOf("."));
				Date date_ = TimeUtil.getyyyyMMddDate(name_);
				if (date.after(date_)) {
					fileList.set(n, file_);
					fileList.set(m, file);
					count++;
				}
			}
			if (count == 0)
				break;
			n = 0;
		}
	}

	/**
	 * log文件排序 增序
	 * 
	 * @param fileList
	 * @throws ParseException
	 */
	public static void sortSmallFiles(List<String> fileList) throws ParseException {
		int n = 0;
		while (true) {
			int count = 0;
			for (int m = n + 1; m < fileList.size(); n++, m++) {
				String file = fileList.get(n);
				String name = file.substring(FileUtil.getLastSeparatorIndex(file) + 1);
				name = name.substring(0, name.indexOf("."));
				Date date = TimeUtil.getyyyyMMddHHmmssDate(name);
				String file_ = fileList.get(m);
				String name_ = file_.substring(FileUtil.getLastSeparatorIndex(file_) + 1);
				name_ = name_.substring(0, name_.indexOf("."));
				Date date_ = TimeUtil.getyyyyMMddHHmmssDate(name_);
				if (date.after(date_)) {
					fileList.set(n, file_);
					fileList.set(m, file);
					count++;
				}
			}
			if (count == 0)
				break;
			n = 0;
		}
		LOGGER.debug("排序后的日志文件");
		for (String str : fileList) {
			LOGGER.debug("日志文件：" + str);
		}
	}

	// 下载后修改后缀名
	public void reName() {
		fileWriter.reFileName(tmpSmallExdName, smallExdName);
	}

	public List<String> listFile() {
		fileList = new ArrayList<String>();
		fileList.addAll(ListFile(tmpExdName));
		fileList.addAll(ListFile(exdName));
		return fileList;
	}

	public static List<String> ListFile(String filter) {
		return FileUtil.getDirNames(path, filter);
	}

	public void close() {
		try {
			fileWriter.close();
		} catch (IOException e) {
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File dir = new File("/home/yuy/my/cache");
		// File[] list = dir.listFiles();
		// for (File file : list) {
		// System.out.println(file.getName());
		// }
		List<String> lis = FileUtil.getDirNames(dir.getAbsolutePath(), "*.tmp");
		for (String file : lis) {
			System.out.println(file);
		}
	}

}
