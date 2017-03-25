package cn.uway.task;

import java.util.List;

public abstract class TaskLoader{

	/**
	 * 任务队列<br>
	 */
	protected List<Task> taskQueue; // 当前任务队列

	/**
	 * 加载任务,把任务加载到任务队列中。
	 */
	public abstract void loadTask();

	/**
	 * @return the taskQueue
	 */
	public List<Task> getTaskQueue(){
		return taskQueue;
	}

	/**
	 * @param taskQueue the taskQueue to set
	 */
	public void setTaskQueue(List<Task> taskQueue){
		this.taskQueue = taskQueue;
	}



}
