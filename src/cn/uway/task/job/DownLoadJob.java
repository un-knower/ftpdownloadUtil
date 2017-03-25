package cn.uway.task.job;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

import cn.uway.cache.RecordMemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.util.FTPTool;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;
import cn.uway.util.ZipUtil;

/**
 * @ClassName: DownLoadJob
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public class DownLoadJob extends AbstractJob {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private RecordMemoryCache memoryCache;

	private String ftpCharSet;

	FtpInfo ftpInfo;

	FTPTool ftpTool;

	int downSucCount = 0;// 统计成功下载的文件个数

	int count = 0;// 总数

	int failTimes = 0;// 没有被下载过，但是下载失败的次数，一个down失败算一次

	public DownLoadJob(Task task, List<String> list, Date dataTime, RecordMemoryCache cache) {
		super(task, list, dataTime);
		this.memoryCache = cache;
		this.ftpCharSet = task.getFtpInfo().getCharset();
		ftpInfo = task.getFtpInfo();
		ftpTool = new FTPTool(ftpInfo);
	}

	@Override
	public JobFuture call() throws Exception {
		renameThread();

		String downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());

		LOGGER.debug(task.getTaskName() + ": 开始FTP登陆.");
		try {
			// login
			boolean bOK = ftpTool.login(ftpPool);
			ftpTool.setFtpPool(ftpPool);
			if (!bOK) {
				LOGGER.error("FTP多次尝试登陆失败，任务退出.");
				return new JobFuture(-1, downSucCount, "ftp登录失败");
			}
			LOGGER.debug("FTP登陆成功,准备开始下载.");
			LOGGER.debug("文件路径个数=" + filePathList.size());
			Long beginTime = System.currentTimeMillis();
			// 开始下载判断
			for (String filePath : filePathList) {
				count++;
				try {
					String encodePath = StringUtil.encodeFTPPath(filePath, this.ftpCharSet);
					List<FTPFile> listFiles = ftpTool.listFTPFiles(encodePath);
					if (listFiles == null || listFiles.isEmpty()) {
						LOGGER.debug("没有扫描到文件或扫描时出错：" + filePath);
						continue;
					}
					for (FTPFile file : listFiles) {
						downLoad(filePath, downLoadPath, file);
					}
				} catch (Exception e) {
					LOGGER.debug("文件下载失败：" + filePath, e);
					failTimes++;
				}
			}
			Long endTime = System.currentTimeMillis();
			LOGGER.info("文件路径个数:" + filePathList.size() + ",总下载次数：" + count + ",成功数:" + downSucCount + ",失败数:" + failTimes + ",总共耗时："
					+ (endTime - beginTime) / 1000.0 + "秒");
		} catch (Exception e) {
			LOGGER.error("FTP采集异常.", e);
			return new JobFuture(-1, downSucCount, "job异常");
		} finally {
			ftpTool.disconn();
			ftpTool = null;
		}
		// 处理结果
		JobFuture jobFutrue = createJobFuture();
		jobFutrue.setSuccessNum(downSucCount);
		return jobFutrue;
	}

	public JobFuture createJobFuture() {
		JobFuture jobFuture = new JobFuture(id);
		jobFuture.setCause("下载完成");
		jobFuture.setCode(0);
		return jobFuture;
	}

	/**
	 * 下载文件
	 * 
	 * @param filePath
	 * @param file
	 * @return
	 */
	private boolean downLoad(String filePath, String downLoadPath, FTPFile file) {
		LOGGER.debug("文件下载：" + filePath);
		String fileName = FilenameUtils.getName(filePath);
		// 下载
		boolean result = ftpTool.downSingleFile(filePath, downLoadPath, file.getSize(), ftpInfo.getDownloadTryTimes(), ftpInfo.getDownloadTryDelay());
		if (fileName.toUpperCase().endsWith(".TAR.GZ") && task.isNeedDecompress()) {
			result = ZipUtil.unTGZipFile(downLoadPath + File.separator + fileName, false);
		} else
		// 解压
		if (result && fileName.toUpperCase().endsWith(".GZ") && task.isNeedDecompress()) {
			result = ZipUtil.unGZipFile(downLoadPath + File.separator + fileName, false);
		}
		if (result) {
			downSucCount++;
			memoryCache.addRecord(filePath);
		} else {
			LOGGER.debug("文件下载失败：" + filePath);
			failTimes++;
		}
		return result;
	}

	/**
	 * 线程重命名<br>
	 * 主要是为了打印日志上显示线程方法
	 */
	protected void renameThread() {
		String dataTimeStr = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		Thread.currentThread().setName("[" + task.getTaskName() + "(" + dataTimeStr + ")][DownLoadJob:" + id + "]");
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

}
