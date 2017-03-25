package cn.uway.task.job;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import cn.uway.cache.FileCache;
import cn.uway.cache.MemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.util.DownStructer;
import cn.uway.util.FTPTool;
import cn.uway.util.FileUtil;
import cn.uway.util.TimeUtil;
import cn.uway.util.ZipUtil;

/**
 * 下载job
 * 
 * @author yuy @ 28 May, 2014
 */
public class TaskJob implements Callable<JobFuture>{

	private int id;

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public Task task;

	public FtpInfo ftpInfo;

	public List<String> fileList;

	public String fileName;

	public FileCache fileCache;

	public MemoryCache memoryCache;

	public Date dataTime;

	public String destPath;

	public int num;

	public int checkNum = 15;// 检测值

	public String dataTimeStr;

	public TaskJob(Task task, List<String> list, Date dataTime){
		this.task = task;
		this.ftpInfo = task.getFtpInfo();
		this.fileList = list;
		this.num = list.size();
		this.dataTime = dataTime;
		dataTimeStr = TimeUtil.getDateString_yyyyMMddHHmmss(dataTime);
		this.fileCache = new FileCache(dataTimeStr);
		this.memoryCache = new MemoryCache();
		this.memoryCache.setFileCache(fileCache);
	}

	public JobFuture call(){
		renameThread();
		FtpInfo ftpInfo = task.getFtpInfo();
		FTPTool ftpTool = new FTPTool(ftpInfo);
		int downSucCount = 0;// 统计成功下载的文件个数
		int hasDownCount = 0;// 已经下载过的文件个数，即被过滤掉的
		int count = 0;// 总数
		int hasDownTimes = 0;// 下载成功的次数，一个down成功算一次
		int notHasDownButFailTimes = 0;// 没有被下载过，但是下载失败的次数，一个down失败算一次
		LOGGER.debug(task.getTaskName() + ": 开始FTP登陆.");
		try{
			// login
			boolean bOK = ftpTool.login(30000, ftpInfo.getLoginTryTimes());
			if(!bOK){
				LOGGER.error(task.getTaskName() + ": FTP多次尝试登陆失败，任务退出.");
				return new JobFuture(id, -1, "ftp登录失败");
			}
			LOGGER.debug(task.getTaskName() + ": FTP登陆成功,准备开始下载.");
			boolean flag = true;
			LOGGER.debug(task.getTaskName() + ": 文件路径个数=" + fileList.size());
			// 下载
			for(String filepath : fileList){
				count++;
				int pos = FileUtil.getLastSeparatorIndex(filepath);
				// 获取filename destPath
				if(fileName == null){
					fileName = filepath.substring(pos + 1).replace("*", "");
					destPath = ftpInfo.getLocalPath() + "/" + fileName + FileCache.tmpExdName;
					File destFile = new File(destPath);
					int num = 0;
					while(destFile.exists()){
						destPath = ftpInfo.getLocalPath() + "/" + fileName + "_" + num + FileCache.tmpExdName;
						destFile = new File(destPath);
						num++;
					}
				}
				DownStructer dataStr = ftpTool.downFile(filepath, destPath);
				memoryCache.addByBatch(dataStr.getSuc());
				downSucCount += dataStr.getSuc().size();
				hasDownCount += dataStr.getHasDownCount();
				hasDownTimes += dataStr.getHasDownTimes();
				notHasDownButFailTimes += dataStr.getNotHasDownButFailTimes();
				// 已经下载失败超过检测值，停止继续下载
				if(count == this.checkNum && downSucCount == 0 && hasDownCount == 0){
					LOGGER.debug("已连续" + count + "次下载失败，即将停止下载，等待下次下载.");
					flag = false;
					break;
				}
				// 解压
				// if (dataStr.getSucLocalFiles().size() > 0) {
				// for (int n = 0; n < dataStr.getSucLocalFiles().size(); n++) {
				// String strPath = dataStr.getSucLocalFiles().get(n);
				// if (!strPath.endsWith(".gz") && !strPath.endsWith(".zip"))
				// break;
				// DeCompression.decompress(strPath, true);
				// }
				// }
			}
			// 打包(两种情况不打包：1）连续15次下载失败，默认为文件还没生成；2）第二次执行，文件个数没变化，即该下载的已下载完，没下载的也没生成完）
			if(count == hasDownTimes + notHasDownButFailTimes && downSucCount == 0)
				flag = false;
			if(flag)
				zip(ftpInfo);
		}catch(Exception e){
			LOGGER.error(task.getTaskName() + ": FTP采集异常.", e);
			return new JobFuture(id, -1, "job异常");
		}finally{
			ftpTool.disconnect();
			ftpTool = null;
			fileCache.close();
			// 流关了之后才能改名字
			// 判断是否tmp重命名为log：1)判断文件个数是否够;2)如果文件个数没变化；3）当前时间大于四个文件产生粒度
			long time = (new Date().getTime() - this.dataTime.getTime()) / 1000 / 60;
			if(downSucCount == this.num || (downSucCount == 0 && hasDownCount > 0)
					|| time > 4 * task.getFileGeneratePeriod())
				fileCache.reName();
		}
		// 处理结果
		JobFuture jobFutrue = createJobFuture();
		return jobFutrue;
	}

	/**
	 * 打包
	 * 
	 * @param ftpInfo
	 */
	public void zip(FtpInfo ftpInfo){
		File source = new File(destPath);
		File target = new File(task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName
				+ task.getCompressPattern());
		int num = 0;
		while(target.exists()){
			target = new File(task.getCompressToPath() + "/" + task.getTaskName() + "_" + fileName + "_" + num
					+ task.getCompressPattern());
			num++;
		}
		LOGGER.debug("准备zip压缩，源目录：" + source + "，目标文件：" + target);
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

	public JobFuture createJobFuture(){
		JobFuture jobFuture = new JobFuture(id);
		jobFuture.setCause("下载完成");
		jobFuture.setCode(0);
		return jobFuture;
	}

	/**
	 * 线程重命名<br>
	 * 主要是为了打印日志上显示线程方法
	 */
	protected void renameThread(){
		Thread.currentThread().setName("[" + task.getTaskName() + "(" + dataTimeStr.substring(0, 12) + ")]Job");
	}

	/**
	 * @param args
	 */
	public static void main(String [] args){

	}

}
