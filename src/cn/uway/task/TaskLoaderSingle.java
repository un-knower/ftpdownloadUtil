package cn.uway.task;

import java.util.ArrayList;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.config.SystemConfig;
import cn.uway.util.TimeUtil;

/**
 * 任务加载器
 * 
 * @author yuy @ 28 May, 2014
 */
public class TaskLoaderSingle extends TaskLoader {

	private boolean isFirstRun = true;

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public static Task task = new Task();

	public void loadTask() {
		try {
			SystemConfig config = SystemConfig.getInstance();
			task.setTaskName(config.getTaskName());
			task.setStartTime(TimeUtil.getDate(config.getStartTime()));
			task.setEndTime("".equals(config.getEndTime()) ? null : TimeUtil.getDate(config.getEndTime()));
			task.setGatherPath(config.getGatherPath());
			task.setPeriod(Float.parseFloat(config.getPeriod()));
			task.setFileGeneratePeriod(Integer.parseInt(config.getFileGeneratePeriod()));
			task.setDownLoadJobNum(Integer.parseInt(config.getDownLoadJobNum()));
			task.setCompressToPath(config.getCompressToPath());
			task.setCompressPattern(config.getCompressPattern());
			task.setRetryListFileCnt(config.getListFileCnt());
			task.setNeedDecompress(config.getNeedDecompress());
			task.setNeedCompress(config.getNeedCompress());

			FtpInfo ftpInfo = new FtpInfo();
			ftpInfo.setIp(config.getFtpIp());
			ftpInfo.setPort(Integer.parseInt(config.getFtpPort()));
			ftpInfo.setUsername(config.getFtpUserName());
			ftpInfo.setPassword(config.getFtpPassword());
			ftpInfo.setLocalPath(config.getLocalPath());
			ftpInfo.setCharset(config.getCharset());
			ftpInfo.setBufferSize(Integer.parseInt(config.getBufferSize()));
			ftpInfo.setDataTimeout(Integer.parseInt(config.getDataTimeout()));
			ftpInfo.setDownloadTryDelay(Integer.parseInt(config.getDownloadTryDelay()));
			ftpInfo.setDownloadTryTimes(Integer.parseInt(config.getDownloadTryTimes()));
			ftpInfo.setListTryDelay(Integer.parseInt(config.getListTryDelay()));
			ftpInfo.setListTryTimes(Integer.parseInt(config.getListTryTimes()));
			ftpInfo.setLoginTryDelay(Integer.parseInt(config.getLoginTryDelay()));
			ftpInfo.setLoginTryTimes(Integer.parseInt(config.getLoginTryTimes()));
			ftpInfo.setRetentionTime(Integer.parseInt(config.getRetentionTime()));
			ftpInfo.setNeedToReadContent(config.getNeedToReadContent());

			task.setFtpInfo(ftpInfo);

			this.taskQueue = new ArrayList<Task>();
			taskQueue.add(task);
			if (!isFirstRun) {
				try {
					Thread.sleep(1 * 60 * 1000);
				} catch (InterruptedException e) {
					LOGGER.debug("任务触发器休眠被打断.", e);
					return;
				}
			} else {
				isFirstRun = false;
			}
		} catch (Exception e) {
			LOGGER.error("加载任务出错", e);
		}
	}

}
