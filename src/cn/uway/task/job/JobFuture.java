package cn.uway.task.job;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.uway.util.DownStructer;

/**
 * 作业执行结果
 * 
 * @author yuy @ 28 May, 2014
 */
public class JobFuture {

	/** 执行成功 */
	public static final int JOB_CODE_SUCCESS = 0;

	/** 执行失败，需要重新执行 */
	public static final int JOB_CODE_FAILED = -1;

	/** 执行结果不完整 */
	public static final int JOB_CODE_INCOMPLETE = 1;

	private int code = 0; // 执行结果码

	private String cause; // 失败原因

	private int successNum; // 下载成功数目

	private DownStructer struct;

	/** 文件下载路径列表 */
	private List<String> filePathList;

	private int jobId;

	private int faildNum;

	private Map<String, Set<String>> groupbyTimeFileMap;

	/**
	 * @return the jobId
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * @param jobId
	 *            the jobId to set
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	/**
	 * @return the faildNum
	 */
	public int getFaildNum() {
		return faildNum;
	}

	/**
	 * @param faildNum
	 *            the faildNum to set
	 */
	public void setFaildNum(int faildNum) {
		this.faildNum = faildNum;
	}

	/**
	 * @return the filePathList
	 */
	public List<String> getFilePathList() {
		return filePathList;
	}

	/**
	 * @param filePathList
	 *            the filePathList to set
	 */
	public void setFilePathList(List<String> filePathList) {
		this.filePathList = filePathList;
	}

	/**
	 * @return the struct
	 */
	public DownStructer getStruct() {
		return struct;
	}

	/**
	 * @param struct
	 *            the struct to set
	 */
	public void setStruct(DownStructer struct) {
		this.struct = struct;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	/**
	 * 构造操作成功的结果对象
	 */
	public JobFuture(int jobId) {
		super();
		this.jobId = jobId;
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param code
	 *            操作结果码
	 * @param cause
	 *            如果失败，可以填写一下原因
	 */
	public JobFuture(int jobId, int code, String cause, DownStructer struct) {
		super();
		this.code = code;
		this.cause = cause;
		this.struct = struct;
		this.jobId = jobId;
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param code
	 *            操作结果码
	 * @param cause
	 *            如果失败，可以填写一下原因
	 */
	public JobFuture(int jobId, int code, String cause) {
		super();
		this.code = code;
		this.cause = cause;
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param code
	 *            操作结果码
	 * @param cause
	 *            如果失败，可以填写一下原因
	 */
	public JobFuture(int jobId, int code, int successNum, String cause) {
		super();
		this.code = code;
		this.cause = cause;
		this.successNum = successNum;
		this.jobId = jobId;
	}

	/**
	 * 构建任务执行的结果对象
	 * 
	 * @param code
	 *            操作结果码
	 * @param cause
	 *            如果失败，可以填写一下原因
	 */
	public JobFuture(int jobId, int code, int successNum, String cause, int faildNum, List<String> filePathList) {
		super();
		this.code = code;
		this.cause = cause;
		this.successNum = successNum;
		this.filePathList = filePathList;
		this.jobId = jobId;
		this.faildNum = faildNum;
	}

	/**
	 * 获取操作结果码
	 */
	public int getCode() {
		return code;
	}

	/**
	 * 获取失败原因
	 */
	public String getCause() {
		return cause;
	}

	/**
	 * @return the successNum
	 */
	public int getSuccessNum() {
		return successNum;
	}

	/**
	 * @param successNum
	 *            the successNum to set
	 */
	public void setSuccessNum(int successNum) {
		this.successNum = successNum;
	}

	public Map<String, Set<String>> getGroupbyTimeFileMap() {
		return groupbyTimeFileMap;
	}

	public JobFuture setGroupbyTimeFileMap(Map<String, Set<String>> groupbyTimeFileMap) {
		this.groupbyTimeFileMap = groupbyTimeFileMap;
		return this;
	}

}
