package cn.uway.pool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.task.FtpInfo;


public class SFTPPoolManager implements PoolManager {

	protected static Logger logger = LogMgr.getInstance().getSystemLogger();

	private Map<String,SFTPClientPool> ftpPoolMap = new HashMap<String,SFTPClientPool>();

	public SFTPPoolManager(){}

	public SFTPPoolManager(List<FtpInfo> ftpList) throws Exception{
		for(FtpInfo info : ftpList){
			addPool(info);
		}
	}

	public synchronized SFTPClient getFTPClient(String ftpId) throws Exception{
		SFTPClientPool ftpClientPool = ftpPoolMap.get(ftpId);
		if(ftpClientPool == null){
			return null;
		}
		return ftpClientPool.getSftpClient();
	}
	
	/**
	 * 登录FTP
	 * 
	 * @param pool
	 *            FTP连接池
	 * @param tryTimes
	 *            最大重试次数
	 * @return 是否登录成功
	 */
	public SFTPClient login(FtpInfo ftpInfo) {
		int tryTimes = ftpInfo.getLoginTryTimes();
		if (tryTimes < 0) {
			tryTimes = 3;
		}
		SFTPClient sFtpClient = null;
		for (int i = 0; i < tryTimes; i++) {
			try {
				sFtpClient = this.getFTPClient(ftpInfo.getId());
			} catch (Exception e) {
				logger.debug("尝试从FTP连接池获取登陆链接失败", e);
			}
			if (sFtpClient != null) {
				logger.debug("FTP连接获取登陆链接成功");
				return sFtpClient;
			}
			logger.debug("尝试重新获取链接，次数:" + (i + 1));
		}
		return null;
	}

	public void addPool(FtpInfo ftpInfo) throws Exception{
		if(ftpPoolMap.get(ftpInfo.getId()) != null){
			return;
		}
		SFTPClientPool pool = new SFTPClientPool(ftpInfo);
		ftpPoolMap.put(ftpInfo.getId(), pool);
	}
	
	public SFTPClientPool getPool(FtpInfo ftpInfo){
		return ftpPoolMap.get(ftpInfo.getId());
	}

}
