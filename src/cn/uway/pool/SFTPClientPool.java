package cn.uway.pool;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.uway.task.FtpInfo;


public class SFTPClientPool {
	private FtpInfo info;
	private int maxPoolSize;
	private LinkedList<SFTPClient> sftpClientList = new LinkedList<SFTPClient>();
	private static final Logger LOGGER = LoggerFactory.getLogger(SFTPClientPool.class);
	
	public SFTPClientPool(FtpInfo ftpInfo) {
		this.maxPoolSize = ftpInfo.getMaxConnections();
		this.info = ftpInfo;
	}
	
	public synchronized SFTPClient getSftpClient() {
		SFTPClient sftp = null;
		while (sftpClientList.size() > 0) {
			sftp = sftpClientList.removeFirst();
			if (sftp.isAvaliable()) {
				LOGGER.debug("SFTPClientPool::getSftpClient(), 本次从连接池中成功获取。连接池还有可用客户端数：{}, 最大可存放客户端数：{}", sftpClientList.size(), this.maxPoolSize);
				return sftp;
			}
			sftp.close();
		}
		sftp = new SFTPClient(info.getIp(), info.getPort(), info.getUsername(), info.getPassword(), info.getCharset());
		if (sftp.connectServer())
			return sftp;
		return null;
	}
	
	/**
	 * 归还SFTPClient;
	 * @param sftp
	 */
	public synchronized  void returnSftpChannel(SFTPClient sftp) {
		try {
			LOGGER.debug("归还SFTPClient到连接池, 当前池已有client个数：{}, 连接池最大可存放client容量个数:{}", sftpClientList.size(), this.maxPoolSize);
			if (!sftp.isAvaliable() ) {
				LOGGER.debug("当前SFTPClient已失效，将销毁当前的SFTPClient. 当前池已有client个数：{},连接池最大可存放client容量个数:{}", sftpClientList.size(), this.maxPoolSize);
				sftp.close();
				return;
			}
			if (sftpClientList.size() >= this.maxPoolSize) {
				LOGGER.debug("当前SFTPClient池个数已超过最大限度值,　将销毁当前的SFTPClient. 当前池已有client个数：{},连接池最大可存放client容量个数:{}", sftpClientList.size(), this.maxPoolSize);
				sftp.close();
				return;
			}
			sftp.beforeReturnPool();
			sftpClientList.add(sftp);
			LOGGER.debug("已成功归还SFTPClient到连接池, 当前池已有client个数：{}, 连接池最大可存放client容量个数:{}", sftpClientList.size(), this.maxPoolSize);
		} catch (Exception e) {
			LOGGER.error("将SFTPClient归还到pool发生了异常", e);
		}
	}
	
}
