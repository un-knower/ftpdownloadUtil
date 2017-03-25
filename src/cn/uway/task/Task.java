package cn.uway.task;

import java.util.Date;

/**
 * @author yuy @ 28 May, 2014
 */
public class Task {

	public static final String WORKER_TYPE_ASYN = "ASYN_TASK";

	public static final String WORKER_TYPE_SYN = "SYN_TASK";

	public static final String WORKER_TYPE_SFTP_SYN = "SYN_SFTP_TASK";

	public String taskName;

	public Date startTime;

	public Date endTime;

	public Date execTime;

	public Date lastRuntime;

	public String gatherPath;

	public float period;

	public Integer maxScanTimes;

	public Integer fileGeneratePeriod;

	public Integer downLoadJobNum;

	public String compressToolPath;

	/** 是否需要打包，默认是 */
	private boolean needCompress = true;

	public String compressToPath;

	public String compressPattern;

	public FtpInfo ftpInfo;

	public boolean isRun;

	private String ftpId;

	private String workerType;

	private String downLoadPath;

	private int delayZip;

	private int retentionTime;

	private int retryListFileCnt;

	/** 是否需要解压 */
	private boolean needDecompress;

	/** 文件完整性检测次数 */
	private int dataCheckTimes;

	/** 文件完整性检测间隔秒 */
	private int dataCheckGapSec;

	/** 时区，默认为GM+8 */
	private int timeZone = +8;

	/** 时区时间 */
	private Date timeZoneDate;

	/**
	 * 文件完整性检测时间范围（分钟），<br>
	 * 当"任务采集时间点" 大于 "当前时间" 减去 "dataCheckRegion"时，才进行文件完整检测<br>
	 * 如果是填0，则永远检测
	 */
	private int dataCheckRegion;

	/**
	 * @return the dataCheckRegion
	 */
	public int getDataCheckRegion() {
		return dataCheckRegion;
	}

	/**
	 * @param dataCheckRegion
	 *            the dataCheckRegion to set
	 */
	public void setDataCheckRegion(int dataCheckRegion) {
		this.dataCheckRegion = dataCheckRegion;
	}

	/**
	 * @return the dataCheckTimes
	 */
	public int getDataCheckTimes() {
		return dataCheckTimes;
	}

	/**
	 * @param dataCheckTimes
	 *            the dataCheckTimes to set
	 */
	public void setDataCheckTimes(int dataCheckTimes) {
		this.dataCheckTimes = dataCheckTimes;
	}

	/**
	 * @return the dataCheckGapSec
	 */
	public int getDataCheckGapSec() {
		return dataCheckGapSec;
	}

	/**
	 * @param dataCheckGapSec
	 *            the dataCheckGapSec to set
	 */
	public void setDataCheckGapSec(int dataCheckGapSec) {
		this.dataCheckGapSec = dataCheckGapSec;
	}

	/**
	 * @return the needDecompress
	 */
	public boolean isNeedDecompress() {
		return needDecompress;
	}

	/**
	 * @param needDecompress
	 *            the needDecompress to set
	 */
	public void setNeedDecompress(boolean needDecompress) {
		this.needDecompress = needDecompress;
	}

	public boolean isNeedCompress() {
		return needCompress;
	}

	public void setNeedCompress(boolean needCompress) {
		this.needCompress = needCompress;
	}

	/**
	 * @return the retentionTime
	 */
	public int getRetentionTime() {
		return retentionTime;
	}

	/**
	 * @param retentionTime
	 *            the retentionTime to set
	 */
	public void setRetentionTime(int retentionTime) {
		this.retentionTime = retentionTime;
	}

	/**
	 * @return the delayZip
	 */
	public int getDelayZip() {
		return delayZip;
	}

	/**
	 * @param delayZip
	 *            the delayZip to set
	 */
	public void setDelayZip(int delayZip) {
		this.delayZip = delayZip;
	}

	/**
	 * @param fileGeneratePeriod
	 *            the fileGeneratePeriod to set
	 */
	public void setFileGeneratePeriod(Integer fileGeneratePeriod) {
		this.fileGeneratePeriod = fileGeneratePeriod;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getGatherPath() {
		return gatherPath.trim();
	}

	public void setGatherPath(String gatherPath) {
		this.gatherPath = gatherPath;
	}

	public float getPeriod() {
		return period;
	}

	public void setPeriod(float period) {
		this.period = period;
	}

	public Integer getMaxScanTimes() {
		return maxScanTimes;
	}

	public void setMaxScanTimes(Integer maxScanTimes) {
		this.maxScanTimes = maxScanTimes;
	}

	public Integer getDownLoadJobNum() {
		return downLoadJobNum;
	}

	public void setDownLoadJobNum(Integer downLoadJobNum) {
		this.downLoadJobNum = downLoadJobNum;
	}

	public int getFileGeneratePeriod() {
		return fileGeneratePeriod;
	}

	public void setFileGeneratePeriod(int fileGeneratePeriod) {
		this.fileGeneratePeriod = fileGeneratePeriod;
	}

	public String getCompressToolPath() {
		return compressToolPath;
	}

	public void setCompressToolPath(String compressToolPath) {
		this.compressToolPath = compressToolPath;
	}

	public String getCompressToPath() {
		return compressToPath;
	}

	public void setCompressToPath(String compressToPath) {
		this.compressToPath = compressToPath;
	}

	public String getCompressPattern() {
		return compressPattern;
	}

	public void setCompressPattern(String compressPattern) {
		this.compressPattern = compressPattern;
	}

	public FtpInfo getFtpInfo() {
		return ftpInfo;
	}

	public void setFtpInfo(FtpInfo ftpInfo) {
		this.ftpInfo = ftpInfo;
	}

	public boolean isRun() {
		return isRun;
	}

	public void setRun(boolean isRun) {
		this.isRun = isRun;
	}

	/**
	 * @return the ftpId
	 */
	public String getFtpId() {
		return ftpId;
	}

	/**
	 * @param ftpId
	 *            the ftpId to set
	 */
	public void setFtpId(String ftpId) {
		this.ftpId = ftpId;
	}

	/**
	 * @return the lastRuntime
	 */
	public Date getLastRuntime() {
		return lastRuntime;
	}

	/**
	 * @param lastRuntime
	 *            the lastRuntime to set
	 */
	public void setLastRuntime(Date lastRuntime) {
		this.lastRuntime = lastRuntime;
	}

	/**
	 * @return the workerType
	 */
	public String getWorkerType() {
		return workerType;
	}

	/**
	 * @param workerType
	 *            the workerType to set
	 */
	public void setWorkerType(String workerType) {
		this.workerType = workerType;
	}

	/**
	 * @return the execTime
	 */
	public Date getExecTime() {
		return execTime;
	}

	/**
	 * @param execTime
	 *            the execTime to set
	 */
	public void setExecTime(Date execTime) {
		this.execTime = execTime;
	}

	/**
	 * @return the downLoadPath
	 */
	public String getDownLoadPath() {
		return downLoadPath;
	}

	/**
	 * @param downLoadPath
	 *            the downLoadPath to set
	 */
	public void setDownLoadPath(String downLoadPath) {
		this.downLoadPath = downLoadPath;
	}

	/**
	 * @return the timeZone
	 */
	public int getTimeZone() {
		return timeZone;
	}

	/**
	 * @param timeZone
	 *            the timeZone to set
	 */
	public void setTimeZone(int timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * @return the timeZoneDate
	 */
	public Date getTimeZoneDate() {
		return timeZoneDate;
	}

	/**
	 * @param timeZoneDate
	 *            the timeZoneDate to set
	 */
	public void setTimeZoneDate(Date timeZoneDate) {
		this.timeZoneDate = timeZoneDate;
	}

	public int getRetryListFileCnt() {
		return retryListFileCnt;
	}

	public void setRetryListFileCnt(int retryListFileCnt) {
		this.retryListFileCnt = retryListFileCnt;
	}

}
