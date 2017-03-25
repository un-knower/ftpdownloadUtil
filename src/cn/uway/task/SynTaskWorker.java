package cn.uway.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.uway.task.job.DownLoadJob;
import cn.uway.task.job.JobFuture;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.TimeUtil;

/**
 * @ClassName: SynTaskWorker
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public class SynTaskWorker extends AbstractWorker {

	protected ExecutorService es;

	/** Job执行线程池 */
	protected CompletionService<JobFuture> jobPool;

	private int compressFailedTimes = 0;

	public SynTaskWorker(Task task) {
		super(task);
	}

	public boolean init() {
		renameThread();
		if (!loginFTP()) {
			return false;
		}
		loadCache();
		return true;
	}

	@Override
	public TaskFuture call() throws Exception {
		if (!init()) {
			return new TaskFuture(-1, "FTP登陆失败", task);
		}
		Long beginTime = System.currentTimeMillis();
		String gatherPath = task.getGatherPath();
		LOGGER.debug(task.getTaskName() + "任务开始执行," + ",时区采集时间:[GM" + task.getTimeZone() + "]" + TimeUtil.getDateString(task.getTimeZoneDate())
				+ ",采集时间点:" + TimeUtil.getDateString(task.getExecTime()) + ",延迟打包分钟：" + task.getDelayZip() + ",打包时间:"
				+ TimeUtil.getDateString(new Date(task.getExecTime().getTime() + (task.getFileGeneratePeriod() + task.getDelayZip()) * 60 * 1000)));
		Date execTime = task.getExecTime();
		// 替换路径中的时间
		gatherPath = FTPPathUtil.getReplacedPath(gatherPath, execTime);
		// 遍历所有文件路径
		List<String> filePathList = extractDirPath(gatherPath, ftpTool.getFtpClient());
		ftpTool.disconn();
		if (filePathList == null || filePathList.isEmpty()) {
			LOGGER.debug("采集文件列表为空");
			cheakAndCompress();
			return new TaskFuture(1, "采集文件列表为空", task);
		}
		List<String> validatePathList = new ArrayList<String>(filePathList.size());
		// 在worker中把重复下载过滤掉，保证传递到job中的文件路径是有效的
		LOGGER.debug("分析采集路径得到文件数：" + filePathList.size());
		for (int i = 0; i < filePathList.size(); i++) {
			String filepath = filePathList.get(i);
			// 如果缓存中存在文件路径，则移除改路径。如果在job中下载完成，则由job向缓存中写入路径记录
			if (memoryCache.contains(filepath)) {
				LOGGER.debug("记录中已存在下载记录：" + filepath);
				continue;
			}
			validatePathList.add(filepath);
		}
		int count = validatePathList.size();
		if (count == 0) {
			cheakAndCompress();
			return new TaskFuture(0, "过滤后采集文件列表为空，任务结束", task);
		}
		int jobNum = task.getDownLoadJobNum();
		if (jobNum <= 0) {
			jobNum = 1;
		}
		LOGGER.debug("本次待采集对象个数：" + count);
		LOGGER.debug("实际单任务并发JOB个数（创建线程数）：" + jobNum);
		es = Executors.newFixedThreadPool(task.getDownLoadJobNum());
		jobPool = new ExecutorCompletionService<JobFuture>(es);
		LOGGER.debug("Job线程池创建");
		int perJobCount = count / jobNum;
		for (int i = 1; i <= jobNum; i++) {
			List<String> subList = filePathList.subList((i - 1) * perJobCount, i * perJobCount);
			if (i == jobNum) {
				subList = filePathList.subList((i - 1) * perJobCount, count);
			}
			DownLoadJob job = new DownLoadJob(task, subList, task.getExecTime(), memoryCache);
			job.setId(i);
			jobPool.submit(job);
		}
		int result = take(jobNum, count);
		Long endTime = System.currentTimeMillis();
		fileCache.clear();
		fileCache.addByBatch(memoryCache.getHasDoneFileSet());
		if (result == -1) {
			return new TaskFuture(result, "任务采集成功率低于98%", task);
		} else if (result == 1) {
			LOGGER.error("有job执行失败,任务需要重采集");
			return new TaskFuture(result, "有job执行失败", task);
		}
		LOGGER.debug("任务采集完成，总共耗时：" + (endTime - beginTime) / 1000.0 + "秒");
		cheakAndCompress();
		return new TaskFuture(result, "任务执行成功", task);
	}

	/**
	 * 获取job执行结果
	 * 
	 * @param submitNum
	 * @return 是否所有job都执行成功
	 */
	private int take(int submitNum, int fileCount) {
		int result = 0;
		int successCount = 0;
		for (int i = 0; i < submitNum; i++) {
			Future<JobFuture> future;
			try {
				future = jobPool.take();
				if (future == null) {
					LOGGER.error("提取job线程返回结果为空。");
					break;
				}
				JobFuture jobFuture = future.get();
				successCount += jobFuture.getSuccessNum();
				int code = jobFuture.getCode();
				result += code;
				if (result != 0) {
					LOGGER.error("job执行异常,cause=" + jobFuture.getCause());
				}
				if (jobFuture.getGroupbyTimeFileMap() != null) {
					if (groupbyTimeFileMap == null)
						groupbyTimeFileMap = new HashMap<String, Set<String>>();
					putAll(groupbyTimeFileMap, jobFuture.getGroupbyTimeFileMap());
				}
			} catch (InterruptedException e) {
				LOGGER.error("提取job线程返回结果异常。", e);
				break;
			} catch (ExecutionException e) {
				LOGGER.error("提取job线程返回结果异常。", e);
				break;
			}
		}
		if (es != null) {
			es.shutdown();
			es = null;
			jobPool = null;
		}
		if (successCount * 1.0 < fileCount * 0.98) {
			LOGGER.error("下载成功数[" + successCount + "]小于总文件数[" + fileCount + "]的98%,任务需要重采集");
			return -1;
		}
		return result == 0 ? 0 : 1;
	}

	/**
	 * 最多打包失败3次
	 */
	private void cheakAndCompress() {
		if (checkTime()) {
			while (compressFailedTimes < 3) {
				boolean result = compress(task.getCompressPattern());
				if (result) {
					return;
				} else {
					compressFailedTimes++;
				}
				try {
					LOGGER.debug("第" + (compressFailedTimes + 1) + "次打包失败，等待5秒钟后重试");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
			LOGGER.debug((compressFailedTimes + 1) + "次打包失败，放弃打包，采集进入下一时间点");
		}
	}

}
