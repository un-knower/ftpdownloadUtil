package cn.uway.task.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

import cn.uway.cache.RecordMemoryCache;
import cn.uway.config.LogMgr;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.util.FTPPathUtil;
import cn.uway.util.FTPTool;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;

/**
 * 只负责扫描的job，在返回的JobFuture里提供新增的文件列表
 * 
 * @ClassName: ScanJob
 * @author Niow
 * @date: 2014-7-5
 */
public class ScanOnlyJob extends AbstractJob{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private final RecordMemoryCache memoryCache;

	private FtpInfo ftpInfo;

	private FTPTool ftpTool;

	private int sucCount;

	private int scanCount;

	private int failCount;

	private String ftpCharSet;

	private List<String> addtionalFileList;

	public ScanOnlyJob(Task task, List<String> filePathList, Date dataTime, RecordMemoryCache memoryCache){
		super(task, filePathList, dataTime);
		this.memoryCache = memoryCache;
		ftpInfo = task.getFtpInfo();
		ftpTool = new FTPTool(ftpInfo);
		ftpCharSet = ftpInfo.getCharset();
		addtionalFileList = new ArrayList<String>();
	}

	/**
	 * 线程重命名<br>
	 * 主要是为了打印日志上显示线程方法
	 */
	protected void renameThread(){
		String dataTimeStr = TimeUtil.getDateString_yyyyMMddHHmm(task.getTimeZoneDate());
		Thread.currentThread().setName("[" + task.getTaskName() + "(" + dataTimeStr + ")][ScanJob:" + id + "]");
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
			return new JobFuture(id, JobFuture.JOB_CODE_FAILED, sucCount, "FTP登录失败", failCount, filePathList);
		}
		LOGGER.debug("FTP登陆成功,开始扫描文件");
		//创建启动下载线程
		String gatherPath = FTPPathUtil.getReplacedPath(task.getGatherPath(), task.getTimeZoneDate());
		String fileName = FilenameUtils.getName(gatherPath);
		LOGGER.debug("分配到扫描路径数:" + filePathList.size() + ",扫描文件名为:" + fileName);
		try{
			for(String path : filePathList){
				String dir = path ;
				if(!fileName.trim().equals("*")){
					dir = dir + "/" + fileName;
				}
				String encodeDir = StringUtil.encodeFTPPath(dir, ftpCharSet);
				List<FTPFile> listFiles = ftpTool.listFTPFiles(encodeDir);
				if(listFiles == null){
					LOGGER.debug("没有扫描到文件或扫描时出错：" + dir);
					continue;
				}
				for(FTPFile file : listFiles){
					String decodeFileName = StringUtil.decodeFTPPath(file.getName(), ftpCharSet);
					decodeFileName = FilenameUtils.getName(decodeFileName);
					String fileFullPath = path + "/" + decodeFileName;
					if(memoryCache.contains(fileFullPath)){
						continue;
					}
					LOGGER.debug("扫描到新增文件：" + fileFullPath);
					addtionalFileList.add(fileFullPath);
					sucCount++;
				}
			}
		}catch(Exception e){
			return new JobFuture(id, JobFuture.JOB_CODE_FAILED, sucCount, "JOB执行失败" + e.getMessage(), failCount,
					addtionalFileList);
		}finally{
			ftpTool.disconn();
		}
		Long endTime = System.currentTimeMillis();
		LOGGER.info("扫描路径个数:" + filePathList.size() + ",扫描到新增文件数：" + scanCount + "总共耗时：" + (endTime - beginTime)
				/ 1000.0 + "秒");
		return new JobFuture(id, JobFuture.JOB_CODE_FAILED, sucCount, "JOB执行成功", failCount, addtionalFileList);
	}

}
