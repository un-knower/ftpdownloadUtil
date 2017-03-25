package cn.uway.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.config.SystemConfig;
import cn.uway.config.TaskRunRecorder;
import cn.uway.util.TimeUtil;

/**
 * 多任务加载
 * 
 * @ClassName: TaskLoaderMulti
 * @author Niow
 * @date: 2014-6-19
 */
public class TaskLoaderMulti extends TaskLoader{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private boolean isFirstRun = true;

	/** 任务加载周期:分钟 */
	protected Integer period = 1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.uway.task.TaskLoader#loadTask()
	 */
	@Override
	public void loadTask(){
		LOGGER.debug("任务加载器开始执行，加载周期为:" + period + "分钟");
		if(!isFirstRun){
			try{
				Thread.sleep(period * 60 * 1000);
			}catch(InterruptedException e){
				LOGGER.debug("任务触发器休眠被打断.", e);
				return;
			}
		}else{
			isFirstRun = false;
		}
		taskQueue = new ArrayList<Task>();
		try{
			List<Task> beans = SystemConfig.getInstance().getBeans(Task.class);
			List<FtpInfo> ftps = SystemConfig.getInstance().getBeans(FtpInfo.class);
			Long nowTime = System.currentTimeMillis();
			for(Task task : beans){
				if(!task.isRun()){
					LOGGER.info("任务" + task.getTaskName() + "未启动,isRun:" + task.isRun());
					continue;
				}
				Date execTime = TaskRunRecorder.getInstance().getDate(task.getTaskName());
				Date startTime = task.getStartTime();
				if(startTime == null){
					LOGGER.info("任务" + task.getTaskName() + "开始时间为空");
					continue;
				}
				//当执行之间为空或者小于开始时间则执行时间置为开始时间
				if(execTime == null || execTime.getTime() < task.getStartTime().getTime()){
					execTime = startTime;
				}
				Date endTime = task.getEndTime();
				long delayTime = task.getDelayZip() * 1000 * 60;
				long timeZonePos = (task.getTimeZone() - 8) * 60 * 60 * 1000;
				task.setTimeZoneDate(new Date(execTime.getTime() + timeZonePos));
				task.setExecTime(execTime);

				if(endTime != null && execTime.getTime() > endTime.getTime()){
					LOGGER.info("任务" + task.getTaskName() + ",时区采集时间:[GM" + task.getTimeZone() + "]"
							+ TimeUtil.getDateString(task.getTimeZoneDate()) + ",任务采集时间点execTime:"
							+ TimeUtil.getDateString(execTime) + ",已超过结束时间endTime:" + TimeUtil.getDateString(endTime));
					continue;
					//如果任务下次执行之间加上延迟时间，大于当前时间，则满足执行条件
				}else if(execTime.getTime() + delayTime > nowTime){
					LOGGER.info("任务" + task.getTaskName() + ",时区采集时间:[GM" + task.getTimeZone() + "]"
							+ TimeUtil.getDateString(task.getTimeZoneDate()) + ",下次采集时间点:"
							+ TimeUtil.getDateString(execTime) + ",延迟时间:" + task.getDelayZip() + "分钟,实际开始执行时间："
							+ TimeUtil.getDateString(new Date(execTime.getTime() + delayTime)));
					continue;
				}
				for(FtpInfo ftp : ftps){
					if(task.getFtpId().equals(ftp.getId())){
						task.setFtpInfo(ftp);
					}
				}
				taskQueue.add(task);
			}
		}catch(Exception e){
			LOGGER.error("加载任务出错", e);
		}

	}

	/**
	 * @return the loadPeriod
	 */
	public int getLoadPeriod(){
		return period;
	}

	/**
	 * @param loadPeriod the loadPeriod to set
	 */
	public void setLoadPeriod(int loadPeriod){
		this.period = loadPeriod;
	}
}
