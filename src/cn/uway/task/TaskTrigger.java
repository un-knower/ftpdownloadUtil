package cn.uway.task;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.config.TaskRunRecorder;
import cn.uway.pool.FTPPoolManager;
import cn.uway.pool.SFTPPoolManager;
import cn.uway.util.TimeUtil;

/**
 * 任务触发器
 * 
 * @version <1.0> 任务触发器建立
 * @author yuy @ 28 May, 2014
 * @version <p>
 *          <1.1> 比较大的修改，把IGPV3中的模式搬迁过来<br>
 *          author Niow date: 2014-6-20
 *          </P>
 * @since1.3.0
 */
public class TaskTrigger extends Thread{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	/**
	 * 任务线程池
	 */
	private ExecutorService threadPool;

	/**
	 * 触发器开关 当triggerFlag=false时 不会有新的任务提交
	 */
	private volatile boolean triggerFlag = true;

	/**
	 * 任务线程池包装类
	 */
	private CompletionService<TaskFuture> service;

	/**
	 * 正在运行的任务的Map<br>
	 * 同时使用workingTasks来进行并发控制,而不是使用线程池的线程并发控制,目的在于将运行队列提供给控制台用于显示<br>
	 * 
	 */
	private Set<String> workingTasks = new HashSet<String>();

	/**
	 * 任务执行线程监听器<br>
	 */
	private Listener listener;

	/**
	 * 触发器对象锁.用于控制workingTasks的并发读写<br>
	 */
	private Object lock = new Object();

	private TaskLoader taskLoader;

	private static Set<String> runningTaskNames = new HashSet<String>();

	private final static FTPPoolManager ftpPool = new FTPPoolManager();

	private final static SFTPPoolManager sFtpPool = new SFTPPoolManager();

	public TaskTrigger(TaskLoader taskLoader){
		super("任务触发器");
		this.taskLoader = taskLoader;
		threadPool = Executors.newFixedThreadPool(10);
		service = new ExecutorCompletionService<TaskFuture>(threadPool);
	}

	/**
	 * 添加运行中的任务记录
	 */
	public synchronized void addRunningTask(Task task){
		runningTaskNames.add(task.getTaskName());
	}

	/**
	 * 移除运行中的任务记录
	 * 
	 * @param task
	 */
	public synchronized void removeRunningTask(Task task){
		runningTaskNames.remove(task.getTaskName());
	}

	/**
	 * 任务是否在运行
	 * 
	 * @param task
	 */
	public synchronized boolean isRunning(Task task){
		return runningTaskNames.contains(task.getTaskName());
	}

	public void run(){
		initialize();
		while(triggerFlag){
			try{
				taskLoader.loadTask();
			}catch(Exception e){
				LOGGER.error("加载任务报错！！！，请检查config.xml配置文件.", e);
				return;
			}
			List<Task> taskQueue = taskLoader.getTaskQueue();
			for(Task task : taskQueue){
				if(running(task)){
					LOGGER.debug("任务已经在运行中. [task-name={}]", task.getTaskName());
					continue;
				}
				try{
					workingTasks.add(task.getTaskName());
					// 设置任务本次启动时间
					AbstractWorker taskWorker = WorkerFactory.getWorker(task);
					taskWorker.setTaskTrigger(this);
					if(!Task.WORKER_TYPE_SFTP_SYN.equalsIgnoreCase(task.getWorkerType())){
						ftpPool.addPool(task.getFtpInfo());
						taskWorker.setFtpPool(ftpPool);
					}else{
						sFtpPool.addPool(task.getFtpInfo());
						taskWorker.setFtpPool(sFtpPool);
					}
					service.submit(taskWorker);
				}catch(Exception e){
					LOGGER.debug("为任务创建FTP连接池失败[task-name=" + task.getTaskName() + "],FTP链接信息ID:[" + task.getFtpId()
							+ "]", e);
					continue;
				}
				LOGGER.debug("taskName={}已提交至运行队列中,当前任务运行队列大小={}", new Object[]{task.getTaskName(),workingTasks.size()});
			}
		}
		LOGGER.warn("由于触发器已停止，任务不再启动。");
	}

	/**
	 * 判断任务是否在运行 周期性任务 判断task_id和数据时间 非周期性任务 直接通过task_id进行判断
	 * 
	 * @param task
	 * @return 任务是否已经在运行 true表示已经在运行 false 表示未运行
	 */
	protected boolean running(Task task){
		return workingTasks.contains(task.getTaskName());
	}

	private void initialize(){
		threadPool = Executors.newFixedThreadPool(10);
		service = new ExecutorCompletionService<TaskFuture>(threadPool);
		// 启动任务执行完毕后处理线程
		listener = new Listener();
		// 设置为守护线程
		listener.setDaemon(true);
		listener.start();
	}

	/**
	 * 停止任务触发
	 */
	public synchronized void stopTrigger(){
		triggerFlag = false;
		threadPool.shutdownNow();
		this.interrupt();
		workingTasks.clear();
		LOGGER.warn(" 将被外部停止");
	}

	/**
	 * 任务执行结果处理监听器<br>
	 * 监听任务的执行结果
	 */
	class Listener extends Thread{

		Listener(){
			super("任务结果处理器");
		}
		
		/**
		 * 将采集时间点调向下一个时间点
		 */
		protected void turnExecTimeToNext(Task task){
			long execTime = task.getExecTime().getTime();
			long fileGeneratePeriod = task.getFileGeneratePeriod() * 60 * 1000;
			long nextTime = execTime + fileGeneratePeriod;
			task.setLastRuntime(task.getExecTime());
			task.setExecTime(new Date(nextTime));
			TaskRunRecorder.getInstance().setDate(task.getTaskName(), task.getExecTime());
			LOGGER.debug("到达采集时间切换点,执行时间变更为：" + TimeUtil.getDateString(task.getExecTime()));
		}

		@Override
		public void run(){
			TaskFuture taskFuture = null;
			LOGGER.debug("任务运行结果提取线程启动。");
			while(true){
				try{
					// 取出任务运行结果 如果没有返回 则线程会挂起
					Future<TaskFuture> future = service.take();
					if(future == null){
						LOGGER.error("提取线程返回结果异常.Future==null");
						continue;
					}
					taskFuture = future.get();
					if(taskFuture == null){
						LOGGER.error("提取线程返回结果异常.TaskFuture==null");
						continue;
					}
					Task task = taskFuture.getTask();
					if(taskFuture.getCode() == TaskFuture.TASK_CODE_SUCCESS){
						turnExecTimeToNext(task);
					}
					LOGGER.debug("[taskName={},{}]", new Object[]{task.getTaskName(),"cause=" + taskFuture.getCause()});
				}catch(InterruptedException e){
					LOGGER.error("提取任务线程运行结果失败", e);
					continue;
				}catch(ExecutionException e){
					LOGGER.error("提取任务线程运行结果失败", e);
					continue;
				}finally{
					// 无论返回成功与否都必须从 当前运行任务表 中清除掉
					if(taskFuture != null && taskFuture.getTask() != null)
						removeTask(taskFuture.getTask());
				}
			}
		}
	}

	/**
	 * 将任务从运行队列中移除 同时唤醒trigger线程
	 * 
	 * @param task
	 */
	public void removeTask(Task task){
		synchronized(lock){
			if(!workingTasks.remove(task.getTaskName())){
				LOGGER.error("任务已从运行队列移除失败,taskName={}在运行任务队列中不存在", task.getTaskName());
				return;
			}
			lock.notifyAll();
			LOGGER.debug("任务已从运行队列移除：{}", task.getTaskName());
		}

	}
}
