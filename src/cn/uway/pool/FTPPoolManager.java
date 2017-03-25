package cn.uway.pool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;

import cn.uway.framework.connection.Device;
import cn.uway.framework.connection.FTPConnectionInfo;
import cn.uway.framework.connection.pool.ftp.FTPClientPool;
import cn.uway.framework.connection.pool.ftp.BasicFTPClientPool;
import cn.uway.task.FtpInfo;

public class FTPPoolManager implements PoolManager{

	private Map<String,FTPClientPool> ftpPoolMap = new HashMap<String,FTPClientPool>();

	public FTPPoolManager(){}

	public FTPPoolManager(List<FtpInfo> ftpList) throws Exception{
		for(FtpInfo info : ftpList){
			addPool(info);
		}
	}

	public synchronized FTPClient getFTPClient(String ftpId) throws Exception{
		FTPClientPool ftpClientPool = ftpPoolMap.get(ftpId);
		if(ftpClientPool == null){
			return null;
		}
		return ftpClientPool.getFTPClient();
	}

	public void addPool(FtpInfo ftpInfo) throws Exception{
		if(ftpPoolMap.get(ftpInfo.getId()) != null){
			return;
		}
		FTPConnectionInfo info = convertInfo(ftpInfo);
		FTPClientPool pool = new BasicFTPClientPool(info);
		ftpPoolMap.put(ftpInfo.getId(), pool);
	}

	private FTPConnectionInfo convertInfo(FtpInfo ftpInfo){
		FTPConnectionInfo info = new FTPConnectionInfo();
		info.setCharset(ftpInfo.getCharset());
		Device device = new Device();
		device.setIp(ftpInfo.getIp());
		info.setDevice(device);
		info.setUserName(ftpInfo.getUsername());
		info.setUserPwd(ftpInfo.getPassword());
		info.setPort(ftpInfo.getPort());
		info.setMaxActive(ftpInfo.getMaxConnections());
		info.setMaxWait(ftpInfo.getMaxWaitSecond());
		info.setPassiveFlag(ftpInfo.getPassiveFlag());
		info.setValidateCommand(ftpInfo.getValidateCmd());
		return info;
	}

//	public static void main(String [] args) throws Exception{
//		LogMgr.getInstance();
//		FtpInfo ftpInfo = new FtpInfo();
//		ftpInfo.setPort(21);
//		ftpInfo.setId("hw");
//		ftpInfo.setIp("192.168.15.223");
//		ftpInfo.setUsername("rd");
//		ftpInfo.setPassword("uway_rd_good");
//		//		ftpInfo.setUsername("anonymous");
//		//		ftpInfo.setPassword("");
//		ftpInfo.setValidateCmd("pwd");
//		ftpInfo.setMaxConnections(4);
//		ftpInfo.setMaxWaitSecond(15);
//		ftpInfo.setPassiveFlag(true);
//		FTPPoolManager pools = new FTPPoolManager();
//		pools.addPool(ftpInfo);
//
//		FTPClient ftp = pools.getFTPClient("hw");
//		String charset = FTPUtil.autoSetCharset(ftp);
//		System.out.println("auto set Charset:" + charset);
//		FTPFile [] listFiles = ftp
//				.listFiles(StringUtil
//						.encodeFTPPath(
//								"/ftp/lte/unicome/华为/北京-性能/neexport_20140526/FBJ900001/A20140526.0215+0800-0230+0800_FBJ900001.xml.gz",
//								charset));
//		for(int i = 0; i < listFiles.length; i++){
//			System.out.println(StringUtil.decodeFTPPath(listFiles[i].getName(), charset));
//		}
////		InputStream in = ftp.retrieveFileStream(StringUtil.encodeFTPPath("/ftp/lte/unicome/华为/北京-性能/neexport_20140526/FBJ900001/A20140526.0215+0800-0230+0800_FBJ900001.xml.gz", charset));
//		InputStream in = ftp.retrieveFileStream("/ftp/lte/unicome/åä¸º/åäº¬-æ§è½/neexport_20140526/FBJ900001/A20140526.0215+0800-0230+0800_FBJ900001.xml.gz");
//		ftp.disconnect();
//	}
}
