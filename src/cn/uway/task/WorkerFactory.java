package cn.uway.task;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;

/**
 * @ClassName: WorkerFactory
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public class WorkerFactory{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public static Map<String,Class<?>> workerMapping = new HashMap<String,Class<?>>();

	/** 同步任务，在所有job执行完毕后进行统一打包 */
	public static final String SYN_TASK = "SYN_TASK";

	/** 异步任务，由每个job各自完成打包 */
	public static final String ASYN_TASK = "ASYN_TASK";

	static{
		workerMapping.put(ASYN_TASK, cn.uway.task.AsynTaskWorker.class);
//		workerMapping.put(SYN_TASK, cn.uway.task.SynTaskWorker.class);
	}

	/**
	 * 通过任务类型获取worker，如果没有匹配则默认为SYN_TASK
	 * 
	 * @param task
	 * @return
	 */
	public static AbstractWorker getWorker(Task task){
		Class<?> workerType = workerMapping.get(task.getWorkerType());
		if(workerType == null){
			return new cn.uway.task.AsynTaskWorker(task);
		}
		AbstractWorker worker = null;
		try{
			Constructor<?> constructor = workerType.getConstructor(new Class[]{Task.class});
			worker = (AbstractWorker)constructor.newInstance(task);
		}catch(Exception e){
			LOGGER.error("获取Worker失败,[Task:" + task.getTaskName() + "][WorkerType:[" + task.getWorkerType() + "]", e);
		}
		return worker;
	}
}
