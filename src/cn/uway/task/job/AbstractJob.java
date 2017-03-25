package cn.uway.task.job;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import cn.uway.pool.FTPPoolManager;
import cn.uway.pool.SFTPPoolManager;
import cn.uway.task.Task;

/**
 * 抽象job类
 * 
 * @ClassName: AbstractJob
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public abstract class AbstractJob implements Callable<JobFuture>{

	public static final int JOB_ASYN_SCAN_DOWN = 1;

	public static final int JOB_SYN_SCAN_DOWN = 2;

	public static final int JOB_DOWN = 3;

	public static final int JOB_SCAN = 4;
	
	public static final int JOB_SYN_SCAN_DOWN_SFTP = 5;

	protected int id;

	protected Task task;

	protected List<String> filePathList;

	protected Date execTime;

	protected FTPPoolManager ftpPool;
	
	protected SFTPPoolManager sFtpPool;

	public AbstractJob(Task task, List<String> filePathList, Date dataTime){
		this.task = task;
		this.filePathList = filePathList;
		this.execTime = dataTime;
	}

	public AbstractJob(Task task, List<String> filePathList, Date dataTime, FTPPoolManager ftpPool){
		this.task = task;
		this.filePathList = filePathList;
		this.execTime = dataTime;
	}

	/**
	 * @return the id
	 */
	public int getId(){
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id){
		this.id = id;
	}

	/**
	 * @return the task
	 */
	public Task getTask(){
		return task;
	}

	/**
	 * @param task the task to set
	 */
	public void setTask(Task task){
		this.task = task;
	}

	/**
	 * @return the filePathList
	 */
	public List<String> getFilePathList(){
		return filePathList;
	}

	/**
	 * @param filePathList the filePathList to set
	 */
	public void setFilePathList(List<String> filePathList){
		this.filePathList = filePathList;
	}

	/**
	 * @return the execTime
	 */
	public Date getExecTime(){
		return execTime;
	}

	/**
	 * @param execTime the execTime to set
	 */
	public void setExecTime(Date execTime){
		this.execTime = execTime;
	}

	/**
	 * @return the ftpPool
	 */
	public FTPPoolManager getFtpPool(){
		return ftpPool;
	}

	/**
	 * @param ftpPool the ftpPool to set
	 */
	public void setFtpPool(FTPPoolManager ftpPool){
		this.ftpPool = ftpPool;
	}
	
	public SFTPPoolManager getsFtpPool() {
		return sFtpPool;
	}

	
	public void setsFtpPool(SFTPPoolManager sFtpPool) {
		this.sFtpPool = sFtpPool;
	}

}
