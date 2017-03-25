package cn.uway.task;

/**
 * FTP方式数据源连接信息
 * 
 * @author yuy @ 28 May, 2014
 */
public class FtpInfo {

	private String id;

	private String ip;

	private int port = 21;

	private String username;

	private String password;

	private String localPath;

	private Integer bufferSize;

	private Integer dataTimeout;

	private Integer loginTryTimes;

	private Integer loginTryDelay;

	private Integer downloadTryTimes;

	private Integer downloadTryDelay;

	private Integer retentionTime;

	private String charset;

	private Boolean passiveFlag;

	private int listTryTimes;

	private int listTryDelay;

	/**
	 * 最大连接数。
	 */
	private int maxConnections;

	/**
	 * 从FTP连接池中获取连接的最大等待时长（秒）。
	 */
	private int maxWaitSecond;

	/**
	 * FTP连接测试命令，用于测试FTP连接是否存活。
	 */
	private String validateCmd;

	private boolean needToReadContent;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public Integer getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(Integer bufferSize) {
		this.bufferSize = bufferSize;
	}

	public Integer getDataTimeout() {
		return dataTimeout;
	}

	public void setDataTimeout(Integer dataTimeout) {
		this.dataTimeout = dataTimeout;
	}

	public Integer getLoginTryTimes() {
		return loginTryTimes;
	}

	public void setLoginTryTimes(Integer loginTryTimes) {
		this.loginTryTimes = loginTryTimes;
	}

	public Integer getLoginTryDelay() {
		return loginTryDelay;
	}

	public void setLoginTryDelay(Integer loginTryDelay) {
		this.loginTryDelay = loginTryDelay;
	}

	public Integer getDownloadTryTimes() {
		return downloadTryTimes;
	}

	public void setDownloadTryTimes(Integer downloadTryTimes) {
		this.downloadTryTimes = downloadTryTimes;
	}

	public Integer getDownloadTryDelay() {
		return downloadTryDelay;
	}

	public void setDownloadTryDelay(Integer downloadTryDelay) {
		this.downloadTryDelay = downloadTryDelay;
	}

	public Integer getRetentionTime() {
		return retentionTime;
	}

	public void setRetentionTime(Integer retentionTime) {
		this.retentionTime = retentionTime;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * @return the passiveFlag
	 */
	public Boolean getPassiveFlag() {
		return passiveFlag;
	}

	/**
	 * @param passiveFlag
	 *            the passiveFlag to set
	 */
	public void setPassiveFlag(Boolean passiveFlag) {
		this.passiveFlag = passiveFlag;
	}

	public int getListTryTimes() {
		return listTryTimes;
	}

	public void setListTryTimes(int listTryTimes) {
		this.listTryTimes = listTryTimes;
	}

	public int getListTryDelay() {
		return listTryDelay;
	}

	public void setListTryDelay(int listTryDelay) {
		this.listTryDelay = listTryDelay;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the maxConnections
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * @param maxConnections
	 *            the maxConnections to set
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * @return the maxWaitSecond
	 */
	public int getMaxWaitSecond() {
		return maxWaitSecond;
	}

	/**
	 * @param maxWaitSecond
	 *            the maxWaitSecond to set
	 */
	public void setMaxWaitSecond(int maxWaitSecond) {
		this.maxWaitSecond = maxWaitSecond;
	}

	/**
	 * @return the validateCmd
	 */
	public String getValidateCmd() {
		return validateCmd;
	}

	/**
	 * @param validateCmd
	 *            the validateCmd to set
	 */
	public void setValidateCmd(String validateCmd) {
		this.validateCmd = validateCmd;
	}

	public boolean getNeedToReadContent() {
		return needToReadContent;
	}

	public void setNeedToReadContent(Boolean needToReadContent) {
		this.needToReadContent = needToReadContent;
	}

}
