package cn.uway.task.job;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import cn.uway.cache.RecordMemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.pool.SFTPClient;
import cn.uway.pool.SFTPClient.SFTPFileEntry;
import cn.uway.pool.SFTPClientPool;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.TimeUtil;
import cn.uway.util.ZipUtil;

public class SftpScanAndDownJob extends AbstractJob {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private final RecordMemoryCache memoryCache;

	private FtpInfo ftpInfo;

	private SFTPClientPool sFTPClientPool;

	private SFTPClient sftp;

	private int sucCount;

	private int scanCount;

	private int failCount;

	private int repeatCount;

	private String downLoadPath;

	public SftpScanAndDownJob(Task task, List<String> filePathList, Date dataTime) {
		this(task, filePathList, dataTime, null);
	}

	public SftpScanAndDownJob(Task task, List<String> filePathList, Date dataTime, RecordMemoryCache memoryCache) {
		super(task, filePathList, dataTime);
		this.memoryCache = memoryCache;
		ftpInfo = task.getFtpInfo();
		this.downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
		File file = new File(downLoadPath);
		LOGGER.debug("文件下载目录：{}", this.downLoadPath);
		if (!file.exists()) {
			boolean isMkdir = file.mkdirs();
			LOGGER.warn("创建文件下载目录结果：{}", isMkdir);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public JobFuture call() throws Exception {
		// 重命名线程
		renameThread();
		// 开始计时
		Long beginTime = System.currentTimeMillis();
		sFTPClientPool = this.sFtpPool.getPool(ftpInfo);
		this.sftp = sFTPClientPool.getSftpClient();
		if (sftp == null) {
			LOGGER.error("SFTP多次尝试登陆失败，任务退出.");
			return new JobFuture(id, JobFuture.JOB_CODE_FAILED, sucCount, "SFTP登录失败", failCount, filePathList);
		}
		LOGGER.debug("SFTP登陆成功,开始扫描文件");
		// 创建启动下载线程
		String gatherPath = FTPPathUtil.getReplacedPath(task.getGatherPath(), task.getTimeZoneDate());
		String fileName = FilenameUtils.getName(gatherPath);
		LOGGER.debug("分配到扫描路径数:" + filePathList.size() + ",扫描文件名为:" + fileName);
		try {
			for (String path : filePathList) {
				if (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
				String dir = path;
				if (!fileName.trim().equals("*")) {
					dir = dir + "/" + fileName;
				}
				// String encodeDir = StringUtil.encodeFTPPath(dir, ftpCharSet);
				List<SFTPFileEntry> listFiles = this.sftp.listSFTPFile(dir);
				if (listFiles == null || listFiles.size() == 0) {
					LOGGER.debug("目录下没有扫描到文件或扫描时出错，目录：{}/{}", new Object[]{path, fileName});
					continue;
				}
				for (SFTPFileEntry file : listFiles) {
					String decodeFileName = FilenameUtils.getName(file.fileName);
					String fileFullPath = path + "/" + decodeFileName;
					LOGGER.debug("扫描到文件:" + fileFullPath);
					scanCount++;
					if (memoryCache.contains(fileFullPath)) {
						LOGGER.debug("记录中已存在下载记录：" + fileFullPath);
						repeatCount++;
						continue;
					}
					downLoad(fileFullPath, downLoadPath);
				}
			}
		} catch (Exception e) {
			return new JobFuture(id, JobFuture.JOB_CODE_FAILED, sucCount, "JOB执行失败" + e.getMessage(), failCount, filePathList);
		} finally {
			sFTPClientPool.returnSftpChannel(sftp);
		}
		Long endTime = System.currentTimeMillis();
		LOGGER.info("扫描路径个数:" + filePathList.size() + ",扫描到文件数：" + scanCount + ",过滤文件数：" + repeatCount + ",下载成功数:" + sucCount + ",下载失败数:" + failCount
				+ ",总共耗时：" + (endTime - beginTime) / 1000.0 + "秒");
		if (failCount > 0) {
			return new JobFuture(id, JobFuture.JOB_CODE_INCOMPLETE, sucCount, "JOB执行结果不完整", failCount, filePathList);
		}
		return new JobFuture(id, JobFuture.JOB_CODE_SUCCESS, sucCount, "JOB执行成功", failCount, filePathList);
	}

	/**
	 * 线程重命名<br>
	 * 主要是为了打印日志上显示线程方法
	 */
	protected void renameThread() {
		String dataTimeStr = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		Thread.currentThread().setName("[" + task.getTaskName() + "(" + dataTimeStr + ")][SftpScanAndDownJob:" + id + "]");
	}

	/**
	 * 下载文件
	 * 
	 * @param filePath
	 * @param file
	 * @return
	 */
	private boolean downLoad(String filePath, String downLoadPath) {
		LOGGER.debug("文件下载：" + filePath);
		String fileName = FilenameUtils.getName(filePath);
		// 下载
		try {
			this.sftp.downRemoteFile(filePath, downLoadPath + File.separator + fileName);
			if (task.isNeedDecompress()) {
				// 解压
				if (fileName.toUpperCase().endsWith(".ZIP")) {
					ZipUtil.unZipFile(downLoadPath + File.separator + fileName, false);
				} else if (fileName.toUpperCase().endsWith(".TAR.GZ")) {
					ZipUtil.unTGZipFile(downLoadPath + File.separator + fileName, false);
				} else if (fileName.toUpperCase().endsWith(".GZ")) {
					ZipUtil.unGZipFile(downLoadPath + File.separator + fileName, false);
				} 
			}
			sucCount++;
			memoryCache.addRecord(filePath);
		} catch (Exception e) {
			LOGGER.debug("文件下载失败：" + filePath, e);
			failCount++;
			return false;
		}
		return true;
	}
}
