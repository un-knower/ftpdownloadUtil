package cn.uway.task;

/**
 * 任务执行的结果
 * 
 * @author Niow
 * @Date 2014-6-20
 * @version 1.0
 * @since 1.3.0
 */
public class TaskFuture{

	/** 执行成功 */
	public static final int TASK_CODE_SUCCESS = 0;

	/** 执行失败，需要重新执行 */
	public static final int TASK_CODE_FAILED = -1;

	/** 执行结果不完整 */
	public static final int TASK_CODE_INCOMPLETE = 1;

	private String cause; // 失败原因

	private Task task; // 参考对象

	private int code;

	/**
	 * 构造操作成功的结果对象
	 */
	public TaskFuture(){
		super();
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param refObj 如果失败，可以填写一下参考对象
	 */
	public TaskFuture(int code, Task refObj){
		super();
		this.task = refObj;
		this.code = code;
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param code 操作结果码
	 * @param cause 如果失败，可以填写一下原因
	 * @param refObj 如果失败，可以填写一下参考对象
	 */
	public TaskFuture(int code, String cause, Task refObj){
		super();
		this.cause = cause;
		this.task = refObj;
		this.code = code;
	}

	/**
	 * 获取失败原因
	 */
	public String getCause(){
		return cause;
	}

	public void setCause(String cause){
		this.cause = cause;
	}

	/**
	 * 获取参考引用对象
	 */
	public Task getTask(){
		return task;
	}

	public void setTask(Task task){
		this.task = task;
	}

	/**
	 * @return the code
	 */
	public int getCode(){
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(int code){
		this.code = code;
	}

}
