package cn.uway.task.job;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

import cn.uway.cache.RecordMemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.util.DownStructer;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.FTPTool;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;

public class AsynScanAndDownJob extends AbstractJob{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private final BlockingQueue<Property> filePathQueue;

	private final RecordMemoryCache memoryCache;

	private DownLoadWorker worker;

	private FtpInfo ftpInfo;

	private FTPTool ftpTool;

	private int sucCount;

	private int scanCount;

	private int failCount;

	private int repeatCount;

	private String ftpCharSet;

	public AsynScanAndDownJob(Task task, List<String> filePathList, Date dataTime){
		this(task, filePathList, dataTime, null);
	}

	public AsynScanAndDownJob(Task task, List<String> filePathList, Date dataTime, RecordMemoryCache memoryCache){
		super(task, filePathList, dataTime);
		this.memoryCache = memoryCache;
		filePathQueue = new LinkedBlockingQueue<Property>(filePathList.size() * 10);
		ftpInfo = task.getFtpInfo();
		ftpTool = new FTPTool(ftpInfo);
		ftpCharSet = ftpInfo.getCharset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public JobFuture call() throws Exception{
		//重命名线程
		renameThread();
		//开始计时
		Long beginTime = System.currentTimeMillis();
		// login
		boolean bOK = ftpTool.login(ftpPool);
		ftpTool.setFtpPool(ftpPool);
		if(!bOK){
			LOGGER.error("FTP多次尝试登陆失败，任务退出.");
			return new JobFuture(-1, sucCount, "ftp登录失败");
		}
		LOGGER.debug("FTP登陆成功,开始扫描文件");
		//创建启动下载线程
		FTPClient ftpClient = ftpTool.getFtpClient();
		worker = new DownLoadWorker(task, ftpClient);
		worker.start();
		String gatherPath = FTPPathUtil.getReplacedPath(task.getGatherPath(), task.getTimeZoneDate());
		String fileName = FilenameUtils.getName(gatherPath);
		LOGGER.debug("分配到扫描路径数:" + filePathList.size() + ",扫描文件名为:" + fileName);
		for(String path : filePathList){
			String encodeDir = StringUtil.encodeFTPPath(path + "/" + fileName, ftpCharSet);
			List<FTPFile> listFiles = ftpTool.listFTPFiles(encodeDir);
			if(listFiles==null||listFiles.isEmpty()){
				LOGGER.debug("没有扫描到文件或扫描时出错：" + path);
				continue;
			}
			for(FTPFile file : listFiles){
				String fileFullPath = StringUtil.decodeFTPPath(file.getName(), ftpCharSet);
				LOGGER.debug("扫描到文件:" + fileFullPath);
				scanCount++;
				if(memoryCache.contains(fileFullPath)){
					LOGGER.debug("记录中已存在下载记录：" + fileFullPath);
					repeatCount++;
					continue;
				}
				filePathQueue.put(new Property(fileFullPath, file));
			}
		}
		ftpTool.disconn();
		worker.close();
		//通知下载线程文件已经扫描完毕
		//等待下载线程下载完队列中的文件
		worker.join();
		Long endTime = System.currentTimeMillis();
		LOGGER.info("扫描路径个数:" + filePathList.size() + ",扫描到文件数：" + scanCount + ",过滤文件数：" + repeatCount + ",下载成功数:"
				+ sucCount + ",下载失败数:" + failCount + ",总共耗时：" + (endTime - beginTime) / 1000.0 + "秒");
		return new JobFuture(0, sucCount, "JOB执行成功");
	}

	/**
	 * 线程重命名<br>
	 * 主要是为了打印日志上显示线程方法
	 */
	protected void renameThread(){
		String dataTimeStr = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		Thread.currentThread().setName("[" + task.getTaskName() + "(" + dataTimeStr + ")][AsynScanAndDownJob:" + id + "]");
	}

	/**
	 * 临时存放ftp文件
	 * 
	 * @ClassName: Property
	 * @author Niow
	 * @date: 2014-6-26
	 */
	private class Property{

		public String fileFullPath;

		public FTPFile ftpFile;

		public Property(String fileFullPath, FTPFile ftpFile){
			this.fileFullPath = fileFullPath;
			this.ftpFile = ftpFile;
		}
	}

	/**
	 * job的下载线程
	 * 
	 * @ClassName: DownLoadWorker
	 * @author Niow
	 * @date: 2014-6-27
	 */
	private class DownLoadWorker extends Thread{

		private boolean keepRunning = true;

		private String downLoadPath;

		private FTPTool ftpTools;

		public DownLoadWorker(Task task, FTPClient ftpClient){
			super(task.getTaskName() + "[ScanAndDownJob][id:" + id + "][DownLoadWorker]");
			this.downLoadPath = task.getDownLoadPath() + "/" + TimeUtil.getDateStringyyyyMMddHHmm(task.getTimeZoneDate());
			ftpTools = new FTPTool(ftpInfo);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run(){
			LOGGER.debug("下载线程启动,准备开始下载.");
			boolean bOK = ftpTools.login(ftpPool);
			if(!bOK){
				LOGGER.error("FTP多次尝试登陆失败，任务退出.");
				keepRunning = false;
			}
			while(keepRunning || !filePathQueue.isEmpty()){
				try{
					Property peropery = filePathQueue.take();
					String filePath = peropery.fileFullPath;
					FTPFile file = peropery.ftpFile;
					boolean downResult = downLoad(filePath, file);
					if(downResult){
						sucCount++;
						memoryCache.addRecord(filePath);
					}else{
						LOGGER.debug("文件下载失败：" + filePath);
						failCount++;
					}
				}catch(InterruptedException e){
					LOGGER.debug("ScanAndDownJob文件路径队列获取唤醒");
				}
			}
			ftpTools.disconn();
		}

		/**
		 * 下载文件
		 * 
		 * @param filePath
		 * @param file
		 * @return
		 */
		private boolean downLoad(String filePath, FTPFile file){
			boolean result = false;
			LOGGER.debug("文件下载：" + filePath);
			if(filePath.endsWith(".gz") && task.isNeedDecompress()){
				result = ftpTools.downSingleFileForGz(filePath, downLoadPath, new DownStructer(), file.getSize());
			}else{
				result = ftpTools.downSingleFile(filePath, downLoadPath, new DownStructer(), file.getSize());
			}
			return result;
		}

		public void close(){
			keepRunning = false;
			this.interrupt();
		}
	}

}
