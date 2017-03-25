package cn.uway.task;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import cn.uway.cache.FileCache;
import cn.uway.config.LogMgr;
import cn.uway.task.job.JobFuture;
import cn.uway.task.job.TaskJob;
import cn.uway.util.FTPTool;
import cn.uway.util.FileUtil;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;
import cn.uway.util.ZipUtil;

/**
 * 任务运行器 (写的有点乱，先不做重构)
 * 
 * @author yuy @ 28 May, 2014
 */
public class TaskWorker extends AbstractWorker{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public FTPTool ftpTool;

	public String name;

	public List<List<String>> pathsList;

	public String fileName;

	public int size;

	// dataTime列表
	public List<Date> dataTimeList;

	/** Job执行线程池 */
	protected CompletionService<JobFuture> jobPool;

	public static Date dataTime;

	protected ExecutorService es;

	public TaskWorker(Task task){
		super(task);
		this.name = task.getTaskName();
	}

	// 获取目录，dataTime(时间仓促，有点乱，以后慢慢优化吧)
	public void before(){
		// 对程序重启前已经下载一部分文件的目录进行打包
		checkIsZip();

		FtpInfo ftpInfo = task.getFtpInfo();
		ftpTool = new FTPTool(ftpInfo);
		LOGGER.debug(task.getTaskName() + ": 开始FTP登陆.");
		try{
			// login
			boolean bOK = ftpTool.login(30000, ftpInfo.getLoginTryTimes());
			if(!bOK){
				LOGGER.error(task.getTaskName() + ": FTP多次尝试登陆失败，任务退出.");
				close();
				return;
			}
			LOGGER.debug(name + ": FTP登陆成功.");

			// 需要采集的文件,多个文件以;间隔
			String gatherPath = task.getGatherPath();

			FileCache fileCache = new FileCache(task.getStartTime(), task.getEndTime());
			List<String> fileList = fileCache.listFile();

			// 获取采集时间dataTime
			getDataTime(fileCache, fileList);

			if(pathsList == null)
				pathsList = new ArrayList<List<String>>();
			// 采集路径不包含*
			if(!gatherPath.contains("*")){
				List<String> list = new ArrayList<String>();
				list.add(gatherPath);
				pathsList.add(list);
				addDataTimeList(gatherPath);
				return;
			}
			int lastSeparator_part_full = FileUtil.getLastSeparatorIndex(gatherPath);
			String part = gatherPath.substring(0, lastSeparator_part_full);
			fileName = gatherPath.substring(lastSeparator_part_full + 1);
			// 采集路径只包含一个*
			if(!part.contains("*")){
				List<String> list = new ArrayList<String>();
				list.add(gatherPath);
				pathsList.add(list);
				addDataTimeList(gatherPath);
				return;
			}

			// 多个*，组装路径集合
			getPathList(fileList, part);
		}catch(Exception e){
			LOGGER.error(name + ": FTP采集异常.", e);
		}finally{
			// 结束ftp连接
			if(ftpTool != null){
				ftpTool.disconnect();
				ftpTool = null;
			}
		}
	}

	@Override
	public TaskFuture call() throws Exception{
		before();
		if(pathsList == null || pathsList.size() == 0){
			LOGGER.debug(name + ": pathsList为空，没有下载对象.");
			close();
			return new TaskFuture(1, "没有下载对象", task);
		}
		LOGGER.debug("本次待采集对象个数：" + pathsList.size());
		LOGGER.debug("实际单任务并发JOB个数（创建线程数）：" + task.getDownLoadJobNum());
		es = Executors.newFixedThreadPool(task.getDownLoadJobNum());
		jobPool = new ExecutorCompletionService<JobFuture>(es);
		LOGGER.debug("Job线程池创建。");
		int submitNum = 0;
		// 第一次最多值提交maxConcurentJobThreadCount个
		for(int i = 0; i < task.getDownLoadJobNum() && i < pathsList.size(); i++){
			// 创建具体job，并且提交至线程池中
			List<String> fileList = pathsList.get(i);
			TaskJob job = new TaskJob(task, fileList, dataTimeList.get(i));
			jobPool.submit(job);
			submitNum++;
		}
		take(submitNum);
		return new TaskFuture(0, "任务执行成功", task);
	}

	void take(int submitNum){
		for(int i = 0; i < submitNum; i++){
			Future<JobFuture> future;
			try{
				future = jobPool.take();
				if(future == null){
					LOGGER.error("提取job线程返回结果为空。");
					break;
				}
				JobFuture jobFuture = future.get();
				int code = jobFuture.getCode();
				if(code != 0){
					LOGGER.error("job执行异常,cause=" + jobFuture.getCause());
				}
			}catch(InterruptedException e){
				LOGGER.error("提取job线程返回结果异常。", e);
				break;
			}catch(ExecutionException e){
				LOGGER.error("提取job线程返回结果异常。", e);
				break;
			}
			if(!hasMoreEntry(submitNum)){
				continue;
			}
			List<String> fileList = pathsList.get(submitNum);
			TaskJob job = new TaskJob(task, fileList, dataTimeList.get(submitNum));
			jobPool.submit(job);
			submitNum++;
		}
		// 全部做完
		if(submitNum == pathsList.size()){
			close();
		}
	}

	/**
	 * 是否还有更多的采集对象需要采集<br>
	 * 
	 * @param submitNum
	 * @return boolean
	 */
	boolean hasMoreEntry(int submitNum){
		return submitNum < pathsList.size();
	}

	public void close(){
		// 结束ftp连接
		if(ftpTool != null){
			ftpTool.disconnect();
			ftpTool = null;
		}

		// 任务结束
		if(task != null){
			task.setRun(false);
			// task = null;
		}
		taskTrigger.removeRunningTask(task);
	}

	/**
	 * 组装路径集合
	 * 
	 * @param fileList
	 * @param part
	 * @throws IOException
	 * @throws ParseException
	 */
	public void getPathList(List<String> fileList, String part) throws IOException, ParseException{
		int last = part.lastIndexOf("*");
		int first = part.indexOf("*");
		// 只有一个* 爱立信/诺西性能
		if(last == first){
			// *在/后
			handleSingleDir(fileList, part);
			return;
		}
		// 两个* 华为性能
		else{
			handleMultiDir(fileList, part);
			return;
		}
	}

	/**
	 * 获取采集时间dataTime
	 * 
	 * @param fileCache
	 * @param fileList
	 * @throws Exception
	 */
	public void getDataTime(FileCache fileCache, List<String> fileList) throws Exception{
		if(fileList == null || fileList.size() == 0){
			if(dataTimeList == null)
				dataTimeList = new ArrayList<Date>();
			if(dataTime != null){
				task.setStartTime(dataTime);
				dataTime = null;
			}
			dataTimeList.add(task.getStartTime());
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(task.getStartTime());
			for(int n = 0; n < FileCache.times; n++){
				calendar.add(Calendar.MINUTE, task.getFileGeneratePeriod());
				dataTimeList.add(calendar.getTime());
			}
			// gatherPath = StringUtil.convertCollectPath(gatherPath,
			// task.getStartTime());
		}else{
			dataTimeList = fileCache.handleDataTime(fileList, dataTime);
			// gatherPath = StringUtil.convertCollectPath(gatherPath,
			// dataTimeList.get(0));
		}
	}

	/**
	 * 对程序重启前已经下载一部分文件的目录进行打包
	 */
	public void checkIsZip(){
		List<String> downFiles = FileUtil.getDirNames(task.getFtpInfo().getLocalPath(), ".tmp");
		if(downFiles == null || downFiles.size() == 0)
			return;
		for(String filePath : downFiles){
			int pos = FileUtil.getLastSeparatorIndex(filePath);
			String fileName = filePath.substring(pos + 1).replace(".tmp", "");
			File target = new File(task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName
					+ task.getCompressPattern());
			if(target.exists()){
				continue;
			}
			LOGGER.debug("准备zip压缩，源目录：" + filePath + "，目标文件：" + target);
			File source = new File(filePath);
			if(ZipUtil.zipDir(source, target, false)){
				LOGGER.debug("zip打包成功：" + target);
				// 删除文件及目录
				File [] fileArray = source.listFiles();
				for(File f : fileArray){
					f.delete();
				}
				source.delete();
			}else{
				LOGGER.error("zip打包失败：" + target);
			}
		}
	}

	public void handleSingleDir(List<String> fileList, String onePart) throws IOException, ParseException{
		// 去掉最后一级目录，考虑兼容各种ftp服务器，想要的效果是一样的(北京就是这种情况)
		int lastIndex = FileUtil.getLastSeparatorIndex(onePart);
		String onePart_ = onePart.substring(0, lastIndex);
		// String otherPart = onePart.substring(lastIndex + 1);
		// 得出所有目录
		List<String> list = ftpTool.listFiles_(onePart_);
		List<String> fileNameList = new ArrayList<String>(dataTimeList.size());
		String extName = fileName.substring(FileUtil.getLastSeparatorIndex(fileName) + 1);
		// 诺西性能另行处理
		if(task.getTaskName().equals("NOKIA_PM")){
			for(int n = 0; n < dataTimeList.size(); n++){
				if(task.getEndTime() != null && dataTimeList.get(n).getTime() > task.getEndTime().getTime()){
					continue;
				}
				if(dataTimeList.get(n).getTime() < task.getStartTime().getTime()){
					continue;
				}
				String timeStr = TimeUtil.getDateString_yyyyMMddHH(dataTimeList.get(n));
				for(String str : list){
					if(str.indexOf(timeStr) > -1){
						List<String> filePathList = new ArrayList<String>();
						filePathList.add(str + "/" + "*" + timeStr + "*" + extName);
						pathsList.add(filePathList);
						break;
					}

				}
			}
			return;
		}
		for(int n = 0; n < dataTimeList.size(); n++){
			if(task.getEndTime() != null && dataTimeList.get(n).getTime() > task.getEndTime().getTime()){
				continue;
			}
			if(dataTimeList.get(n).getTime() < task.getStartTime().getTime()){
				continue;
			}
			fileNameList.add("*" + TimeUtil.getDateString_yyyyMMddHHmm(dataTimeList.get(n)) + "*" + extName);
		}
		for(int n = 0; n < fileNameList.size(); n++){
			List<String> filePathList = new ArrayList<String>(list.size());
			for(String str : list){
				filePathList.add(str + "/" + fileNameList.get(n));
			}
			pathsList.add(filePathList);
		}
	}

	/**
	 * 多级带*的目录，目前只支持两级带*的目录
	 * 
	 * @param fileList
	 * @param part
	 * @throws IOException
	 * @throws ParseException
	 */
	public void handleMultiDir(List<String> fileList, String part) throws IOException, ParseException{
		// 兼容所有带*的目录
		char [] array = part.toCharArray();
		int rightSeparatorIndex = getRightSeparatorIndex(array);
		if(rightSeparatorIndex == 0)
			return;

		// 得出所有目录
		String onePart = part.substring(0, rightSeparatorIndex);
		String otherPart = part.substring(rightSeparatorIndex + 1);
		List<String> list = ftpTool.listFiles(onePart);
		Date currentTaskDateTime = dataTimeList.get(0);// 取最小时间
		// 增序
		sortDirs(list);
		// 没有记录，第一次做，获取pathList
		if((fileList == null || fileList.size() == 0) || dataTime != null){
			int n = 0;
			while(n < list.size()){
				String path = list.get(n);
				String patternTime = StringUtil.getPattern(path, "\\d{8}");
				Date currentFileDataTime = TimeUtil.getyyyyMMddDate(patternTime);
				// 比较时间是否一致
				if(!compareTime(currentFileDataTime, currentTaskDateTime, true)){
					n++;
					continue;
				}
				// 与任务开始时间比较
				if(currentTaskDateTime.getTime() >= task.getStartTime().getTime()){
					if((task.getEndTime() != null && currentTaskDateTime.getTime() <= task.getEndTime().getTime())
							|| task.getEndTime() == null){
						handlePathsList(path + "/" + otherPart);
						dataTime = null;
						return;
					}
				}
				n++;
			}
			return;
		}
		// 有记录，不是第一次做，获取pathList
		sortDirs(fileList);
		int m = 0;
		while(m < fileList.size() && fileList.get(m).endsWith(FileCache.tmpExdName)){
			String patternTime = StringUtil.getPattern(fileList.get(m), "\\d{8}");
			Date currentFileDataTime = TimeUtil.getyyyyMMddDate(patternTime);
			// 比较时间是否一致
			if(!compareTime(currentFileDataTime, currentTaskDateTime, false)){
				m++;
				continue;
			}
			// 与任务开始时间比较
			if(currentTaskDateTime.getTime() >= task.getStartTime().getTime()){
				if((task.getEndTime() != null && currentTaskDateTime.getTime() <= task.getEndTime().getTime())
						|| task.getEndTime() == null){
					String path = onePart.replace("*", patternTime);
					if(list.contains(path)){
						handlePathsList(path + "/" + otherPart);
						dataTime = null;
						return;
					}
				}
			}
			m++;
		}
	}

	public int getRightSeparatorIndex(char [] array){
		int rightSeparatorIndex = 0;
		boolean flag = false;
		for(int n = 0; n < array.length; n++){
			if(array[n] == '*')
				flag = true;
			if(flag && array[n] == '/'){
				rightSeparatorIndex = n;
				break;
			}
		}
		return rightSeparatorIndex;
	}

	// 比较时间是否一致
	public boolean compareTime(Date currentFileDataTime, Date currentTaskDataTime, boolean flag){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentFileDataTime);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		calendar.setTime(currentTaskDataTime);
		int day_ = calendar.get(Calendar.DAY_OF_MONTH);
		if(day != day_){
			dataTime = flag ? currentFileDataTime : currentTaskDataTime;
			LOGGER.debug("任务开始时间" + currentTaskDataTime + "与原始文件时间" + currentFileDataTime + "不匹配.");
			return false;
		}
		return true;
	}

	public void handlePathsList(String part) throws IOException, ParseException{
		List<String> pathList = ftpTool.listFiles(part);
		List<String> fileNameList = new ArrayList<String>(dataTimeList.size());
		String extName = fileName.substring(FileUtil.getLastSeparatorIndex(fileName) + 1);
		for(int n = 0; n < dataTimeList.size(); n++){
			fileNameList.add("*" + TimeUtil.getDateString_yyyyMMddHHmm(dataTimeList.get(n)) + "*" + extName);
		}
		for(int n = 0; n < fileNameList.size(); n++){
			List<String> filePathList = new ArrayList<String>(pathList.size());
			for(String str : pathList){
				filePathList.add(str + "/" + fileNameList.get(n));
			}
			pathsList.add(filePathList);
		}
	}

	public void addDataTimeList(String gatherPath) throws ParseException{
		String patternTime = StringUtil.getPattern(gatherPath, "\\d{8}[.]\\d{4}");
		patternTime = patternTime.replace(".", "_");
		Date currentDataTime = TimeUtil.getyyyyMMdd_HHmmDate(patternTime);
		dataTimeList.add(currentDataTime);
	}

	// 排序 增序
	public void sortDirs(List<String> fileList) throws ParseException{
		int n = 0;
		while(true){
			int count = 0;
			for(int m = n + 1; m < fileList.size(); n++, m++){
				String file = fileList.get(n);
				String patternTime = StringUtil.getPattern(file, "\\d{8}");
				Date date = TimeUtil.getyyyyMMddDate(patternTime);
				String file_ = fileList.get(m);
				String patternTime_ = StringUtil.getPattern(file_, "\\d{8}");
				Date date_ = TimeUtil.getyyyyMMddDate(patternTime_);
				if(date.after(date_)){
					fileList.set(n, file_);
					fileList.set(m, file);
					count++;
				}
			}
			if(count == 0)
				break;
			n = 0;
		}
	}

	public void setTask(Task task){
		this.task = task;
	}

	/**
	 * @param args
	 */
	public static void main(String [] args){
		// TODO Auto-generated method stub

	}

}
