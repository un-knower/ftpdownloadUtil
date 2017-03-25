package cn.uway.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;

import cn.uway.pool.FTPPoolManager;
import cn.uway.pool.SFTPClient;
import cn.uway.pool.SFTPClientPool;
import cn.uway.pool.SFTPPoolManager;
import cn.uway.task.job.AbstractJob;
import cn.uway.task.job.JobFuture;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.TimeUtil;

public class AsynTaskWorker extends AbstractWorker {

	protected ExecutorService es;

	protected int jobType = 1;

	public boolean init() {
		renameThread();
		boolean isLogin = false;
		if (ftpPool instanceof FTPPoolManager) {
			isLogin = loginFTP();
		} else {
			isLogin = loginSFTP();
		}
		if (isLogin)
			loadCache();
		return true;
	}

	public AsynTaskWorker(Task task) {
		super(task);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public TaskFuture call() throws Exception {
		try {
			if (!init()) {
				return new TaskFuture(TaskFuture.TASK_CODE_FAILED, "FTP登陆失败", task);
			}
			long delayTime = task.getDelayZip() * 1000 * 60;
			LOGGER.debug(task.getTaskName() + "任务开始执行" + ",时区采集时间:[GM" + task.getTimeZone() + "]" + TimeUtil.getDateString(task.getTimeZoneDate())
					+ ",采集时间点:" + TimeUtil.getDateString(task.getExecTime()) + ",延迟采集分钟：" + task.getDelayZip() + ",本地采集开始时间点:"
					+ TimeUtil.getDateString(new Date(task.getExecTime().getTime() + delayTime)));
			// 开始计时
			Long beginTime = System.currentTimeMillis();
			// 采集路径_本地编码
			String gatherPath_local = task.getGatherPath();
			// 替换路径中的时间
			gatherPath_local = FTPPathUtil.getReplacedPath(gatherPath_local, task.getTimeZoneDate());
			List<String> filePathList = null;
			if (ftpPool instanceof FTPPoolManager) {
				// 遍历倒数第二层所有路径
				filePathList = extractDirPath(gatherPath_local, ftpTool.getFtpClient(), FTPPathUtil.getPathMaxLevel(gatherPath_local) - 1);
				// 归还链接
				ftpTool.disconn();
			} else {
				SFTPClientPool sFTPClientPool = ((SFTPPoolManager) ftpPool).getPool(task.getFtpInfo());
				SFTPClient sFtp = sFTPClientPool.getSftpClient();
				// String encodeDir = StringUtil.encodeFTPPath(gatherPath_local, task.getFtpInfo().getCharset());
				filePathList = sFtp.extractDirPath(gatherPath_local);
				sFTPClientPool.returnSftpChannel(sFtp);
			}
			if (filePathList == null || filePathList.isEmpty()) {
				LOGGER.debug("采集文件列表为空");
				return new TaskFuture(TaskFuture.TASK_CODE_SUCCESS, "采集文件列表为空", task);
			}
			// 初始化启动线程池
			int jobNum = getEffectiveJobNum(filePathList);
			// 创建线程池
			es = Executors.newFixedThreadPool(jobNum);
			jobPool = new ExecutorCompletionService<JobFuture>(es);
			// 创建job，并且提交执行
			if (Task.WORKER_TYPE_ASYN.equalsIgnoreCase(task.getWorkerType())) {
				jobType = AbstractJob.JOB_ASYN_SCAN_DOWN;
			} else if (Task.WORKER_TYPE_SFTP_SYN.equalsIgnoreCase(task.getWorkerType())) {
				jobType = AbstractJob.JOB_SYN_SCAN_DOWN_SFTP;
			} else {
				jobType = AbstractJob.JOB_SYN_SCAN_DOWN;
			}
			createJobAndSubmit(jobType, filePathList, jobNum);

			// 阻塞读取job执行结果，并生成任务执行结果
			TaskFuture taskResult = take(jobNum);

			// 任务结束时间
			Long endTime = System.currentTimeMillis();

			// 写文件缓存
			fileCache.clear();
			fileCache.addByBatch(memoryCache.getHasDoneFileSet());

			double downLoadTimeCost = (endTime - beginTime) / 1000.0;

			LOGGER.debug("任务下载完成，总共耗时：" + downLoadTimeCost + "秒");

			// 不需要打包，直接返回
			if (!task.isNeedCompress()) {
				LOGGER.debug("任务完成，总共耗时：" + (downLoadTimeCost) * 1.0 + "秒,下载[" + downLoadTimeCost + "]秒");
				return taskResult;
			}

			// 打包检测计时开始
			beginTime = System.currentTimeMillis();
			String fileName = FilenameUtils.getName(gatherPath_local);
			// 开始打包检测，一直到新增加问件列表为空才满足打包要求
			List<String> addtionalFileList = null;
			do {
				addtionalFileList = compressCheck(task.getDataCheckTimes(), task.getDataCheckGapSec(), task.getDataCheckRegion(), filePathList,
						fileName);
				if (addtionalFileList == null) {
					taskResult.setCode(TaskFuture.TASK_CODE_FAILED);
					taskResult.setCause("打包前文件完整性检查失败");
					return taskResult;
				} else if (!addtionalFileList.isEmpty()) {
					downLoadAdditionalFile(addtionalFileList);
				}
			} while (!addtionalFileList.isEmpty());
			// 打包检测计时结束
			endTime = System.currentTimeMillis();

			double checkTimeCost = (endTime - beginTime) / 1000.0;

			// 打包计时开始
			beginTime = System.currentTimeMillis();
			// 如果打包失败
			if (!compress(task.getCompressPattern())) {
				taskResult.setCode(TaskFuture.TASK_CODE_FAILED);
				taskResult.setCause("打包失败");
			}
			// 打包计时结束
			endTime = System.currentTimeMillis();

			double compressTimeCost = (endTime - beginTime) / 1000.0;
			LOGGER.debug("任务完成，总共耗时：" + (downLoadTimeCost + compressTimeCost + checkTimeCost) * 1.0 + "秒,下载[" + downLoadTimeCost + "]秒,检测文件["
					+ checkTimeCost + "]秒,打包[" + compressTimeCost + "]秒");
			return taskResult;
		} finally {
			if (es != null) {
				es.shutdown();
				es = null;
				jobPool = null;
			}
		}
	}
	
	/**
	 * 获取有效的Job数量
	 * @param filePathList 需要采集的路径
	 * @return 有效的Job数量
	 */
	private int getEffectiveJobNum(List<String> filePathList){
		int jobNum = task.getDownLoadJobNum();
		if (jobNum <= 0) {
			jobNum = 1;
		}
		if(jobNum!=1){
			//如果perJobCount为0，只有一个Job线程会下载所有路径；其它Job线程并不会下载
			int perJobCount = filePathList.size() / jobNum;
			if(perJobCount==0){
				jobNum = filePathList.size();
			}
		}
		return jobNum;
	}

	/**
	 * 补充下载增加的文件,之前下载失败的文件
	 * 
	 * @param filePathList
	 * @return
	 */
	public boolean downLoadAdditionalFile(List<String> filePathList) {
		LOGGER.debug("文件补充下载开始");
		// DownLoadJob job = new DownLoadJob(task, filePathList, task.getTimeZoneDate(), memoryCache);
		AbstractJob job = JobCreatFactory.createJob(jobType, task, filePathList, task.getTimeZoneDate(), memoryCache, ftpPool);
		if (ftpPool instanceof SFTPPoolManager) {
			job.setsFtpPool((SFTPPoolManager) ftpPool);
		} else {
			job.setFtpPool((FTPPoolManager) ftpPool);
		}
		// job.setFtpPool((FTPPoolManager) ftpPool);
		try {
			JobFuture result = job.call();
			if (result.getCode() == JobFuture.JOB_CODE_FAILED) {
				throw new Exception("补充下载增加的文件job执行失败");
			} else if (result.getCode() == JobFuture.JOB_CODE_INCOMPLETE) {
				throw new Exception("补充下载增加的文件job下载不完整");
			}
			return true;
		} catch (Exception e) {
			LOGGER.debug("文件补充下载失败，重试3次", e);
			for (int i = 0; i < 3; i++) {
				try {
					JobFuture result = job.call();
					if (result.getCode() == JobFuture.JOB_CODE_SUCCESS) {
						LOGGER.debug("文件补充下载重试第" + i + "次成功");
						return true;
					}
				} catch (Exception e1) {
				}
			}
			LOGGER.debug("文件补充下载重试3次都失败");
		}
		return false;
	}

	/**
	 * job重新执行,如果没有job执行失败，则算成功，否则任务执行失败
	 * 
	 * @param jobList
	 */
	private boolean reSubmitJob(List<AbstractJob> jobList) {
		if (jobList.isEmpty()) {
			return true;
		}
		for (int i = 0; i < jobList.size(); i++) {
			AbstractJob job = jobList.get(i);
			job.setId(100 + i);
			jobPool.submit(job);
		}
		for (int i = 0; i < jobList.size(); i++) {
			try {
				Future<JobFuture> take = jobPool.take();
				if (take == null) {
					LOGGER.error("job重新执行结果提取为null");
					return false;
				}
				if (take.get().getCode() == JobFuture.JOB_CODE_FAILED) {
					return false;
				}
			} catch (Exception e) {
				LOGGER.error("job重新执行结果提取出错", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * 获取job执行结果
	 * 
	 * @param submitNum
	 * @return 是否所有job都执行成功
	 */
	private TaskFuture take(int submitNum) {
		int jobSuccessNum = 0;
		int jobIncompleteNum = 0;
		int jobFaildNum = 0;
		int successCount = 0;
		int faildCount = 0;
		List<AbstractJob> reExecJob = new ArrayList<AbstractJob>();
		for (int i = 0; i < submitNum; i++) {
			Future<JobFuture> future;
			try {
				future = jobPool.take();
				if (future == null) {
					LOGGER.error("提取job线程返回结果为空");
					return new TaskFuture(TaskFuture.TASK_CODE_FAILED, "提取job线程返回结果为空,任务失败", task);
				}
				JobFuture jobFuture = future.get();
				successCount += jobFuture.getSuccessNum();
				faildCount += jobFuture.getFaildNum();
				switch (jobFuture.getCode()) {
					case JobFuture.JOB_CODE_SUCCESS : {
						jobSuccessNum++;
						if (jobFuture.getGroupbyTimeFileMap() != null) {
							if (groupbyTimeFileMap == null)
								groupbyTimeFileMap = new HashMap<String, Set<String>>();
							putAll(groupbyTimeFileMap, jobFuture.getGroupbyTimeFileMap());
						}
						break;
					}
					case JobFuture.JOB_CODE_FAILED : {
						jobFaildNum++;
						LOGGER.debug("JOB[id:" + jobFuture.getJobId() + "]执行失败，原因为【" + jobFuture.getCause() + "】，需要重新执行");
						AbstractJob job = JobCreatFactory.createJob(jobType, task, jobFuture.getFilePathList(), task.getTimeZoneDate(), memoryCache,
								ftpPool);
						reExecJob.add(job);
						break;
					}
					case JobFuture.JOB_CODE_INCOMPLETE : {
						jobIncompleteNum++;
						LOGGER.debug("JOB[id:" + jobFuture.getJobId() + "]执行结果不完整，成功数：" + jobFuture.getSuccessNum() + ",失败数:"
								+ jobFuture.getFaildNum());
						if (jobFuture.getSuccessNum() > 0 && jobFuture.getFaildNum() / jobFuture.getSuccessNum() > 0.05) {
							LOGGER.debug("JOB[id:" + jobFuture.getJobId() + "]执行结果成功率低于95%,需要重新执行");
							AbstractJob job = JobCreatFactory.createJob(jobType, task, jobFuture.getFilePathList(), task.getTimeZoneDate(),
									memoryCache, ftpPool);
							reExecJob.add(job);
						}
						break;
					}
				}
			} catch (Exception e) {
				LOGGER.error("提取job线程返回结果异常。", e);
				break;
			}
		}
		LOGGER.debug("JOB执行结果提取完成，job成功数:[" + jobSuccessNum + "],job失败数：[" + jobFaildNum + "],job为完成数：[" + jobIncompleteNum + "],总共下载成功数:["
				+ successCount + "],总共失败数:[" + faildCount + "]");
		boolean result = reSubmitJob(reExecJob);
		if (result) {
			return new TaskFuture(TaskFuture.TASK_CODE_SUCCESS, "任务成功", task);
		} else {
			return new TaskFuture(TaskFuture.TASK_CODE_FAILED, "任务失败", task);
		}
	}
}
