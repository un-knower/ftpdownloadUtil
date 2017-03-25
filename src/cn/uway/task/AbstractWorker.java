package cn.uway.task;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

import cn.uway.cache.RecordFileCache;
import cn.uway.cache.RecordMemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.pool.FTPPoolManager;
import cn.uway.pool.PoolManager;
import cn.uway.pool.SFTPClient;
import cn.uway.pool.SFTPPoolManager;
import cn.uway.task.job.AbstractJob;
import cn.uway.task.job.JobFuture;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.FTPTool;
import cn.uway.util.FTPUtil;
import cn.uway.util.StringUtil;
import cn.uway.util.TarUtil;
import cn.uway.util.TimeUtil;
import cn.uway.util.ZipUtil;

/**
 * 抽象工人类
 * 
 * @ClassName: AbstractWorker
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public abstract class AbstractWorker implements Callable<TaskFuture> {

	/** Job执行线程池 */
	protected CompletionService<JobFuture> jobPool;

	protected static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	protected Task task;

	protected TaskTrigger taskTrigger;

	protected FTPTool ftpTool;

	protected RecordMemoryCache memoryCache;

	protected RecordFileCache fileCache;

	protected PoolManager ftpPool;

	protected Map<String, Set<String>> groupbyTimeFileMap;

	/**
	 * 遍历文件对比缓存，若连续N次没有增加则满足打包
	 * 
	 * @param checkTimes
	 *            检测次数
	 * @param gapTimeSec
	 *            每次检测间隔时间
	 * @param pathList
	 *            检测路径列表
	 * @param fileName
	 *            检测文件名
	 * @return 返回变化的文件路径列表
	 */
	protected List<String> compressCheck(int checkTimes, int gapTimeSec, List<String> pathList, String fileName) {
		LOGGER.debug("开始检测文件完整性，检测次数:" + checkTimes + ",每次检测间隔时间:" + gapTimeSec + "秒,检测文件：" + fileName);
		try {
			List<String> additionFileList = new ArrayList<String>();
			int jobNum = task.getDownLoadJobNum();
			if (jobNum <= 0) {
				jobNum = 1;
			}
			for (int i = 1; i <= checkTimes; i++) {
				LOGGER.debug("第" + i + "检测次扫描开始");
				// createJobAndSubmit(AbstractJob.JOB_SCAN, pathList, jobNum);
				createJobAndSubmit(Task.WORKER_TYPE_SFTP_SYN.equalsIgnoreCase(task.getWorkerType())
						? AbstractJob.JOB_SYN_SCAN_DOWN_SFTP
						: AbstractJob.JOB_SCAN, pathList, jobNum);
				for (int j = 0; j < jobNum; j++) {
					try {
						Future<JobFuture> take = jobPool.take();
						if (take == null) {
							LOGGER.error("job重新执行结果提取为null");
							continue;
						}
						additionFileList.addAll(take.get().getFilePathList());
					} catch (Exception e) {
						LOGGER.error("job重新执行结果提取出错", e);
					}
				}
				if (additionFileList.isEmpty()) {
					LOGGER.debug("第" + i + "检测次扫描结束，总共扫描到文件:" + additionFileList.size() + "个，没有文件变化,进行下一次扫描");
					if (i < checkTimes) {
						LOGGER.debug("进入检测间歇期:" + gapTimeSec + "秒");
						try {
							Thread.sleep(gapTimeSec * 1000);
						} catch (Exception e) {
						}
					}
				} else {
					LOGGER.debug("第" + i + "检测次扫描结束,总共扫描到文件:" + additionFileList.size() + "个，存在变化文件：" + additionFileList.size() + "个");
					return additionFileList;
				}
			}
			return additionFileList;
		} catch (Exception e) {
			LOGGER.debug("compressCheck出错", e);
			return null;
		} finally {
		}
	}

	/**
	 * 遍历文件对比缓存，若连续N次没有增加则满足打包
	 * 
	 * @param checkTimes
	 *            检测次数
	 * @param gapTimeSec
	 *            每次检测间隔时间
	 * @param checkRegion
	 *            检测时间范围
	 * @param pathList
	 *            检测路径列表
	 * @param fileName
	 *            检测文件名
	 * @return 返回变化的文件路径列表
	 */
	protected List<String> compressCheck(int checkTimes, int gapTimeSec, int checkRegion, List<String> pathList, String fileName) {
		long nowTime = System.currentTimeMillis();
		long execTime = task.getExecTime().getTime();
		long regionTime = nowTime - checkRegion * 60 * 1000;
		if (checkRegion <= 0) {
			LOGGER.debug("采集时间点不在文件完整性检测时间范围设置为0，检测所有时间点文件完整性");
		} else if (execTime < regionTime) {
			LOGGER.debug("采集时间点不在文件完整性检测时间范围内，采集时间点:" + TimeUtil.getDateString(task.getExecTime()) + "，检测时间范围:"
					+ TimeUtil.getDateString(new Date(regionTime)));
			return new ArrayList<String>();
		}
		return compressCheck(checkTimes, gapTimeSec, pathList, fileName);
	}

	/**
	 * 创建任务，并且提交执行
	 * 
	 * @param filePathList
	 * @param jobNum
	 */
	protected void createJobAndSubmit(int jobType, List<String> filePathList, int jobNum) {
		LOGGER.debug("实际单任务并发JOB个数（创建线程数）：" + jobNum);
		LOGGER.debug("Job线程池创建");
		int perJobCount = filePathList.size() / jobNum;
		for (int i = 1; i <= jobNum; i++) {
			List<String> subList = filePathList.subList((i - 1) * perJobCount, i * perJobCount);
			if (i == jobNum) {
				subList = filePathList.subList((i - 1) * perJobCount, filePathList.size());
			}
			AbstractJob job = JobCreatFactory.createJob(jobType, task, subList, task.getTimeZoneDate(), memoryCache, ftpPool);
			job.setId(i);
			jobPool.submit(job);
		}
	}

	/**
	 * @return the ftpPool
	 */
	public PoolManager getFtpPool() {
		return ftpPool;
	}

	/**
	 * @param ftpPool
	 *            the ftpPool to set
	 */
	public void setFtpPool(PoolManager ftpPool) {
		this.ftpPool = ftpPool;
	}

	public AbstractWorker(Task task) {
		this.task = task;
	}

	public AbstractWorker(Task task, FTPPoolManager ftpPool) {
		this.task = task;
		this.ftpPool = ftpPool;
	}

	/**
	 * 加载缓存文件到内存中
	 */
	protected void loadCache() {
		memoryCache = new RecordMemoryCache();
		fileCache = new RecordFileCache(task);
		memoryCache.addBatchRecord(fileCache.getAllRecords());
	}

	/**
	 * 重命名线程
	 */
	protected void renameThread() {
		String execTime = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		Thread.currentThread().setName(task.getTaskName() + "-" + this.getClass().getSimpleName() + execTime);
	}

	/**
	 * 登陆FTP，并自动设置FTP编码
	 * 
	 * @param ftpInfo
	 * @return
	 */
	protected boolean loginFTP() {
		FtpInfo ftpInfo = task.getFtpInfo();
		ftpTool = new FTPTool(ftpInfo);
		LOGGER.debug(task.getTaskName() + ": 开始FTP登陆.");
		try {
			// login
			boolean bOK = ftpTool.login((FTPPoolManager) ftpPool);
			if (!bOK) {
				LOGGER.error(task.getTaskName() + ": FTP多次尝试登陆失败，任务退出.");
				// 结束ftp连接
				if (ftpTool != null) {
					ftpTool.disconnect();
					ftpTool = null;
				}
				return false;
			}
			LOGGER.debug(task.getTaskName() + ": FTP登陆成功.");
			ftpTool.setFtpPool((FTPPoolManager) ftpPool);
			if (StringUtil.isEmpty(ftpInfo.getCharset())) {
				String charset = FTPUtil.autoSetCharset(ftpTool.getFtp());
				LOGGER.debug("FTP编码未设置，执行自动判断设置编码：" + charset);
				ftpInfo.setCharset(charset);
			}
		} catch (Exception e) {
			LOGGER.error(task.getTaskName() + ": FTP采集异常.", e);
			return false;
		}
		return true;
	}

	/**
	 * 登陆FTP，并自动设置FTP编码
	 * 
	 * @param ftpInfo
	 * @return
	 */
	protected boolean loginSFTP() {
		FtpInfo ftpInfo = task.getFtpInfo();
		LOGGER.debug(task.getTaskName() + ": 开始SFTP登陆.");
		try {
			// login
			SFTPClient sFtpClient = ((SFTPPoolManager) ftpPool).login(ftpInfo);
			if (sFtpClient == null) {
				LOGGER.error(task.getTaskName() + ": FTP多次尝试登陆失败，任务退出.");
				return false;
			}
			// 归还sftp连接
			((SFTPPoolManager) ftpPool).getPool(ftpInfo).returnSftpChannel(sFtpClient);
			LOGGER.debug(task.getTaskName() + ": SFTP登陆成功.");
			// if(StringUtil.isEmpty(ftpInfo.getCharset())){
			// String charset = FTPUtil.autoSetCharset(ftpTool.getFtp());
			// LOGGER.debug("FTP编码未设置，执行自动判断设置编码：" + charset);
			// ftpInfo.setCharset(charset);
			// }
		} catch (Exception e) {
			LOGGER.error(task.getTaskName() + ": FTP采集异常.", e);
			return false;
		}
		return true;
	}

	/**
	 * @return the task
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * @param task
	 *            the task to set
	 */
	public void setTask(Task task) {
		this.task = task;
	}

	/**
	 * @return the taskTrigger
	 */
	public TaskTrigger getTaskTrigger() {
		return taskTrigger;
	}

	/**
	 * @param taskTrigger
	 *            the taskTrigger to set
	 */
	public void setTaskTrigger(TaskTrigger taskTrigger) {
		this.taskTrigger = taskTrigger;
	};

	/**
	 * 通过压缩任务的压缩格式来判断打包类型
	 * 
	 * @param compressPattern
	 */
	protected boolean compress(String compressPattern) {
		int compressFailedTimes = 0;
		while (compressFailedTimes < 3) {
			boolean result = false;
			if (groupbyTimeFileMap != null) {
				result = groupTar();
			} else if (compressPattern.toUpperCase().endsWith(".ZIP")) {
				result = zip();
			} else if (compressPattern.toUpperCase().endsWith(".TAR")) {
				result = tar();
			} else if (compressPattern.toUpperCase().endsWith(".TAR.GZ")) {
				result = tarGz();
			}
			if (result) {
				return true;
			} else {
				compressFailedTimes++;
			}
			try {
				LOGGER.debug("第" + (compressFailedTimes + 1) + "次打包失败，等待5秒钟后重试");
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

	/**
	 * 打包Tar
	 * 
	 */
	public boolean tar() {
		String fileName = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		String targetName = task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + task.getCompressPattern();
		File target = new File(targetName);
		File targetTemp = new File(targetName + ".temp");
		if (target.exists()) {
			LOGGER.debug("tar包已存在：" + target.getAbsolutePath());
		}
		String downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
		File source = new File(downLoadPath);
		if (source == null || !source.exists()) {
			LOGGER.debug("压缩源目录不存在:" + downLoadPath + "，跳过打包动作，进入下一个采集点");
			return true;
		}
		LOGGER.debug("准备tar包，源目录：" + source + "，目标文件：" + target);
		if (TarUtil.tarDir(source, targetTemp, null, false)) {
			LOGGER.debug("tar打包成功：" + target);
			targetTemp.renameTo(target);
			targetTemp.delete();
			// 删除文件及目录
			File[] fileArray = source.listFiles();
			for (File f : fileArray) {
				f.delete();
			}
			source.delete();
			return true;
		} else {
			LOGGER.error("tar打包失败：" + target);
			return false;
		}
	}
	
	public boolean tarGz(){
		String fileName = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		String targetName = task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + task.getCompressPattern();
		//tar文件
		String targetTemptar = targetName.substring(0, targetName.lastIndexOf("."));
		File target = new File(targetName);
		File targettar = new File(targetTemptar);
		if (target.exists()) {
			LOGGER.debug("tar.gz包已存在：" + target.getAbsolutePath());
		}
		String downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
		File source = new File(downLoadPath);
		if (source == null || !source.exists()) {
			LOGGER.debug("压缩源目录不存在:" + downLoadPath + "，跳过打包动作，进入下一个采集点");
			return true;
		}
		LOGGER.debug("准备tar.gz包，源目录：" + source + "，目标文件：" + target);
		if (TarUtil.tarDir(source, targettar, null, false)) {
			if(ZipUtil.gzipFile(targettar)){
				LOGGER.debug("tar.gz打包成功：" + target);
				targettar.delete();
				File[] fileArray = source.listFiles();
				for (File f : fileArray) {
					f.delete();
				}
				source.delete();
				return true;
			}else{
				LOGGER.error("tar.gz打包失败：" + target);
				return false;
			}
		} else {
			LOGGER.error("tar.gz打包失败：" + target);
			return false;
		}
	}

	/**
	 * 分组打包Tar
	 * 
	 */
	public boolean groupTar() {
		Iterator<String> it = groupbyTimeFileMap.keySet().iterator();
		String downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
		File source = new File(downLoadPath);
		while (it.hasNext()) {
			String timeString = it.next();
			Date timeDate = null;
			try {
				timeDate = TimeUtil.getyyyyMMddHHDate(timeString);
			} catch (ParseException e) {
			}
			String fileName = TimeUtil.getDateString_yyyyMMddHHmm(timeDate);
			String targetName = task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + task.getCompressPattern();
			File target = new File(targetName);
			File targetTemp = new File(targetName + ".temp");
			int n = 0;
			while (targetTemp.exists() || target.exists()) {
				LOGGER.debug("tar包已存在：" + target.getAbsolutePath());
				targetName = task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + "_" + n + task.getCompressPattern();
				target = new File(targetName);
				targetTemp = new File(targetName + ".temp");
				n++;
			}
			if (source == null || !source.exists()) {
				LOGGER.debug("压缩源目录不存在:" + downLoadPath + "，跳过打包动作，进入下一个采集点");
				continue;
			}
			LOGGER.debug("准备tar包，源目录：" + source + "，目标文件：" + target);
			if (TarUtil.tarDir(source, targetTemp, groupbyTimeFileMap.get(timeString), false)) {
				LOGGER.debug("tar打包成功：" + target);
				targetTemp.renameTo(target);
				targetTemp.delete();
				continue;
			} else {
				LOGGER.error("tar打包失败：" + target);
				return false;
			}
		}
		// 删除文件及目录
		File[] fileArray = source.listFiles();
		for (File f : fileArray) {
			f.delete();
		}
		source.delete();
		return true;
	}

	/**
	 * 打包ZIP
	 * 
	 * @param ftpInfo
	 */
	public boolean zip() {
		String fileName = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		String targetName = task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + task.getCompressPattern();
		File target = new File(targetName);
		File targetTemp = new File(targetName + ".temp");
		if (target.exists()) {
			LOGGER.debug("压缩包已存在：" + target.getAbsolutePath() + "，重新打包");
		}
		String downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
		File source = new File(downLoadPath);
		if (source == null || !source.exists()) {
			LOGGER.debug("压缩源目录不存在:" + downLoadPath + "，跳过打包动作，进入下一个采集点");
			return true;
		}
		LOGGER.debug("准备zip压缩，源目录：" + source + "，目标文件：" + target);
		if (ZipUtil.zipDir(source, targetTemp, false)) {
			LOGGER.debug("zip打包成功：" + target);
			targetTemp.renameTo(target);
			targetTemp.delete();
			// 删除文件及目录
			File[] fileArray = source.listFiles();
			for (File f : fileArray) {
				f.delete();
			}
			source.delete();
			return true;
		} else {
			LOGGER.error("zip打包失败：" + target);
			return false;
		}
	}

	/**
	 * 检查时间是否到达切换时间
	 * 
	 * @return
	 */
	protected boolean checkTime() {
		long now = System.currentTimeMillis();
		long execTime = task.getExecTime().getTime();
		long fileGeneratePeriod = task.getFileGeneratePeriod() * 60 * 1000;
		long delayZip = task.getDelayZip() * 60 * 1000;
		long nextTime = execTime + fileGeneratePeriod;
		if (now > nextTime + delayZip) {
			return true;
		}
		return false;
	}

	/**
	 * 根据采集路径，在FTP服务器上遍历出所有文件路径
	 * 
	 * @param gatherPath
	 * @param ftp
	 * @param level
	 *            分析到采集目录的第几层
	 * @return 返回本地编码的文件路径列表，下载的时候需要encode成ftp编码
	 */
	protected List<String> extractDirPath(String gatherPath, FTPClient ftp, int level) {
		LOGGER.debug("开始分析下载路径");
		String charset = task.getFtpInfo().getCharset();
		// 去掉结尾分隔符
		if (gatherPath.endsWith("/")) {
			gatherPath = gatherPath.substring(0, gatherPath.length() - 1);
		}
		if (!gatherPath.startsWith("/")) {
			gatherPath = "/" + gatherPath;
		}
		List<String> dirList = new ArrayList<String>();
		if (!gatherPath.contains("*")) {
			dirList.add(gatherPath);
			return dirList;
		}
		String[] splitPath = gatherPath.split("/");
		dirList.add("/");
		String originalPath = "";
		if (level >= splitPath.length) {
			level = splitPath.length - 1;
		}
		for (int i = 1; i <= level; i++) {
			List<String> tempDirList = new LinkedList<String>();
			String subDir = splitPath[i];
			originalPath += "/" + subDir;
			LOGGER.debug("开始分析下载第" + i + "层路径:" + originalPath);
			if (subDir == null || subDir.trim().isEmpty()) {
				continue;
			}
			for (String dir : dirList) {
				String currentPath = dir;
				if (dir.equals("/")) {
					currentPath = "";
				}
				if (subDir.contains("*")) {
					String encodeDir = StringUtil.encodeFTPPath(currentPath + "/" + subDir, charset);
					if (subDir.equals("*")) {
						encodeDir = StringUtil.encodeFTPPath(currentPath, charset);
					}
					List<FTPFile> ftpDirList = null;
					if (i == splitPath.length - 1) {
						ftpDirList = ftpTool.listFTPFiles(encodeDir);
						if(ftpDirList==null||ftpDirList.isEmpty()){
							LOGGER.debug("没有扫描到文件或扫描时出错：" + dir);
							continue;
						}
						for (FTPFile file : ftpDirList) {
							currentPath = StringUtil.decodeFTPPath(file.getName(), charset);
							tempDirList.add(currentPath);
							LOGGER.debug("扫描到第" + i + "层路径:" + currentPath);
						}
					} else {
						encodeDir = StringUtil.encodeFTPPath(currentPath, charset);
						//ftpDirList = FTPUtil.listDirectories(ftp, encodeDir);
						ftpDirList = ftpTool.listDirectories(encodeDir);
						if(ftpDirList==null||ftpDirList.isEmpty()){
							LOGGER.debug("没有扫描到目录或扫描时出错：" + dir);
							continue;
						}
						boolean flag = !subDir.equals("*");
						for (FTPFile file : ftpDirList) {
							String filename = FilenameUtils.getName(file.getName());
							if (flag) {
								if (StringUtil.wildCardMatch(subDir, filename, "*")) {
									currentPath = encodeDir + "/" + filename;
								} else
									continue;
							} else
								currentPath = encodeDir + "/" + filename;
							currentPath = StringUtil.decodeFTPPath(currentPath, charset);
							tempDirList.add(currentPath);
							LOGGER.debug("扫描到第" + i + "层路径:" + currentPath);
						}
					}

				} else {
					currentPath += "/" + subDir;
					tempDirList.add(currentPath);
					LOGGER.debug("扫描到第" + i + "层路径:" + currentPath);
				}
			}
			dirList = tempDirList;
		}
		return dirList;
	}

	/**
	 * 根据采集路径，在FTP服务器上遍历出所有文件路径
	 * 
	 * @param gatherPath
	 * @param ftp
	 * @return 返回本地编码的文件路径列表，下载的时候需要encode成ftp编码
	 */
	protected List<String> extractDirPath(String gatherPath, FTPClient ftp) {
		int maxLevel = FTPPathUtil.getPathMaxLevel(gatherPath);
		return extractDirPath(gatherPath, ftp, maxLevel);
	}

	public static class JobCreatFactory {

		public static final Map<Integer, Class<?>> jobMap = new HashMap<Integer, Class<?>>();

		static {
			jobMap.put(AbstractJob.JOB_ASYN_SCAN_DOWN, cn.uway.task.job.AsynScanAndDownJob.class);
			jobMap.put(AbstractJob.JOB_SYN_SCAN_DOWN, cn.uway.task.job.ScanAndDownJob.class);
			jobMap.put(AbstractJob.JOB_DOWN, cn.uway.task.job.DownLoadJob.class);
			jobMap.put(AbstractJob.JOB_SCAN, cn.uway.task.job.ScanOnlyJob.class);
			jobMap.put(AbstractJob.JOB_SYN_SCAN_DOWN_SFTP, cn.uway.task.job.SftpScanAndDownJob.class);
		}

		public static AbstractJob createJob(int jobType, Task task, List<String> filePathList, Date dataTime, RecordMemoryCache memoryCache,
				PoolManager ftpPool) {
			Class<?> clazz = jobMap.get(jobType);
			if (clazz == null) {
				return null;
			}
			Object newInstance = null;
			try {
				Constructor<?> constructor = clazz.getConstructor(Task.class, List.class, Date.class, RecordMemoryCache.class);
				newInstance = constructor.newInstance(task, filePathList, dataTime, memoryCache);
			} catch (Exception e) {
				LOGGER.error("创建JOB失败", e);
			}
			if (newInstance != null) {
				AbstractJob job = (AbstractJob) newInstance;
				// if (AbstractJob.JOB_SYN_SCAN_DOWN_SFTP == jobType) {
				// job.setsFtpPool((SFTPPoolManager) ftpPool);
				// } else {
				// job.setFtpPool((FTPPoolManager) ftpPool);
				// }
				if (ftpPool instanceof SFTPPoolManager) {
					job.setsFtpPool((SFTPPoolManager) ftpPool);
				} else {
					job.setFtpPool((FTPPoolManager) ftpPool);
				}
				return job;
			}
			return null;
		}
	}

	public void putAll(Map<String, Set<String>> groupbyTimeFileMapAll, Map<String, Set<String>> groupbyTimeFileMap) {
		Iterator<String> it = groupbyTimeFileMap.keySet().iterator();
		while (it.hasNext()) {
			String time = it.next();
			if (groupbyTimeFileMapAll.containsKey(time)) {
				groupbyTimeFileMapAll.get(time).addAll(groupbyTimeFileMap.get(time));
			} else {
				groupbyTimeFileMapAll.put(time, groupbyTimeFileMap.get(time));
			}
		}
	}
}
