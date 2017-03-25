package cn.uway.cleaner;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.config.SystemConfig;
import cn.uway.config.TaskRunRecorder;
import cn.uway.task.Task;
import cn.uway.util.FileUtil;
import cn.uway.util.StringUtil;
import cn.uway.util.TimeUtil;

public class FileCleaner extends Thread{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	// 任务时间周期
	protected static final long period = 24 * 60 * 60 * 1000;

	/** 加载定时器 **/
	private static Timer timer;

	public FileCleaner(){
		super("文件清理线程");
	}

	@Override
	public void run(){
		load();
		long min = period / 1000 / 60;
		LOGGER.debug("文件清理线程" + min + "分钟后开始执行，执行周期为" + min + "分钟");
		timer = new Timer("文件清理线程");
		timer.schedule(new ReloadTimerTask(), period, period);
	}

	/**
	 * 重载定时器任务
	 */
	class ReloadTimerTask extends TimerTask{

		public void run(){
			load();
		}
	}

	/**
	 * 从数据库中加载网元信息
	 * 
	 * @throws ParseException
	 */
	public void load(){
		LOGGER.debug("开始启动文件清理线程.");
		//获取可以执行的任务列表
		List<Task> taskList = new ArrayList<Task>();;
		try{
			taskList = SystemConfig.getInstance().getBeans(Task.class);
		}catch(Exception e){
			LOGGER.debug("文件清理线程任务加载失败", e);
		}
		for(Task task : taskList){
			if(!task.isRun()){
				continue;
			}
			int count = 0;
			int retentionTime = task.getRetentionTime();
			Date execTime = TaskRunRecorder.getInstance().getDate(task.getTaskName());
			//当执行之间为空或者小于开始时间则执行时间置为开始时间
			if(execTime == null || execTime.getTime() < task.getStartTime().getTime()){
				execTime = task.getStartTime();
			}
			String filePath = task.getCompressToPath();
			LOGGER.debug("准备清理任务[" + task.getTaskName() + "]的压缩文件,路径:" + filePath);
			List<String> list = FileUtil.getFileNames(filePath, "*"+task.getCompressPattern());
			for(String file : list){
				try{
					String patternTime = StringUtil.getPattern(file, "\\d{8}");
					Date dataTime = TimeUtil.getyyyyMMddDate(patternTime);
					int days = (int)((execTime.getTime() - dataTime.getTime()) / 1000 / 60 / 60 / 24);
					if(days >= retentionTime / 24){
						new File(file).delete();
						LOGGER.debug("清除文件：" + file);
						count++;
					}
				}catch(Exception e){
					LOGGER.debug("文件清理出错:" + file, e);
				}
			}
			LOGGER.debug("本次共清理任务[" + task.getTaskName() + "]的" + count + "个文件.");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String [] args){
		// TODO Auto-generated method stub

	}

}
