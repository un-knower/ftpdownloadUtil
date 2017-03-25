package cn.uway.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.uway.task.FtpInfo;

/**
 * FTP工具。
 * 
 * @author ChenSijiang 2012-10-30
 */
public final class FTPUtil{

	private static final Logger log = LoggerFactory.getLogger(FTPUtil.class);

	private static FTPFileComparator ftpFileComparator = new FTPFileComparator();

	public static FTPFileComparator getFTPFileComparator(){
		return ftpFileComparator;
	}

	/**
	 * 登出并关闭FTP连接。
	 * 
	 * @param ftp FTP连接。
	 * @return 未正常登出或关闭。
	 */
	public static boolean logoutAndCloseFTPClient(FTPClient ftp){
		if(ftp == null){
			log.warn("传入的FTPClient为null.");
			return false;
		}

		boolean bOk = true;

		try{
			if(!ftp.logout()){
				log.warn("FTP登出返回false，reply={}", ftp.getReplyString());
				bOk = false;
			}
		}catch(IOException exLogout){
			bOk = false;
			log.warn("FTP登出发生异常。", exLogout);
		}finally{
			try{
				ftp.disconnect();
			}catch(IOException exClose){
				bOk = false;
				log.warn("FTP断开发生异常。", exClose);
			}
		}
		return bOk;
	}

	/**
	 * FTP执行LIST命令，递归的获取文件列表。如果失败，将返回<code>null</code>. 注意，之支持一级目录有星号的递归。
	 * 
	 * @param ftp FTP连接。
	 * @param path 路径。
	 * @return 文件列表。
	 * @throws IOException 操作失败。
	 */
	public static List<FTPFile> listFilesRecursive(FTPClient ftp, String path) throws IOException{
		// 目录无通配符的情况，直接返回。
		String parentPath = FilenameUtils.getFullPath(path);
		if(!parentPath.contains("*") && !parentPath.contains("?"))
			return listFiles(ftp, path);

		String [] spPath = path.split("/");
		String namePart = FilenameUtils.getName(path);
		String wildDir = "";

		List<String> parsedDirs = new ArrayList<String>();
		String currFullDir = "/";
		for(int i = 0; i < spPath.length; i++){
			String dir = spPath[i];
			if(dir == null || dir.trim().isEmpty() || dir.equals(namePart))
				continue;
			if(dir.contains("*") || dir.contains("?")){
				wildDir = dir;
				FTPFile [] dirs = ftp.listDirectories(currFullDir + "/" + wildDir);
				for(FTPFile ftpDir : dirs){
					if(ftpDir == null || ftpDir.getName() == null)
						continue;
					if(FilenameUtils.wildcardMatch(ftpDir.getName(), dir))
						parsedDirs.add(ftpDir.getName());
				}
				break;
			}else{
				currFullDir += (dir + "/");
			}
		}

		List<FTPFile> files = new ArrayList<FTPFile>();

		for(int i = 0; i < parsedDirs.size(); i++){
			if(parsedDirs.get(i) == null)
				continue;

			String oneDir = path.replace("/" + wildDir + "/", "/" + parsedDirs.get(i) + "/");
			List<FTPFile> tmp = listFilesRecursive(ftp, oneDir);
			if(tmp != null)
				files.addAll(tmp);
		}

		Collections.sort(files, getFTPFileComparator());

		return files;
	}

	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp FTP连接。
	 * @param path 路径。
	 * @return 文件列表。
	 */
	public static List<FTPFile> listFiles(FTPClient ftp, String path){
		if(ftp == null){
			log.warn("ftp为null.");
			return null;
		}
		if(path == null){
			log.warn("path为null.");
			return null;
		}

		FTPFile [] ftpFiles = null;
		try{
			ftpFiles = ftp.listFiles(path);
		}catch(IOException e){
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			log.warn("FTP listFiles时发生异常。", e);
			return null;
		}

		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if(ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		// 正常化文件列表， 做四个处理：
		// 1、为null的FTPFile对象消除；
		// 2、文件名为null的FTP对象清除；
		// 3、文件名改名绝对路径；
		// 4、如果不是文件，跳过。
		List<FTPFile> list = new ArrayList<FTPFile>();
		for(FTPFile ff : ftpFiles){
			if(ff == null || ff.getName() == null || ff.getName().trim().isEmpty() || !ff.isFile())
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			String dir = FilenameUtils.getFullPath(path);
			ff.setName(dir + filename);
			list.add(ff);
		}

		Collections.sort(list, getFTPFileComparator());
		return list;
	}

	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp FTP连接。
	 * @param path 路径。
	 * @return 文件列表。
	 */
	public static List<FTPFile> list(FTPClient ftp, String path){
		if(ftp == null){
			log.warn("ftp为null.");
			return null;
		}
		if(path == null){
			log.warn("path为null.");
			return null;
		}

		FTPFile [] ftpFiles = null;

		try{
			ftpFiles = ftp.listDirectories(path);
		}catch(IOException e){
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			log.warn("FTP listDirectories时发生异常。", e);
			return null;
		}

		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if(ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		List<FTPFile> list = new ArrayList<FTPFile>();
		for(FTPFile ff : ftpFiles){
			if(ff == null || ff.getName() == null || ff.getName().trim().isEmpty())
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			String dir = FilenameUtils.getFullPath(path);
			ff.setName(dir + filename);
			list.add(ff);
		}

		Collections.sort(list, getFTPFileComparator());
		return list;
	}

	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp FTP连接。
	 * @param path 路径。
	 * @return 文件列表。
	 */
	public static List<FTPFile> listDirectories(FTPClient ftp, String path){
		if(ftp == null){
			log.warn("ftp为null.");
			return null;
		}
		if(path == null){
			log.warn("path为null.");
			return null;
		}

		FTPFile [] ftpFiles = null;

		try{
			ftpFiles = ftp.listDirectories(path);
		}catch(IOException e){
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			log.warn("FTP listDirectories时发生异常。", e);
			return null;
		}
		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if(ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		// 正常化文件列表， 做四个处理：
		// 1、为null的FTPFile对象消除；
		// 2、文件名为null的FTP对象清除；
		// 3、文件名改名绝对路径；
		// 4、如果不是文件，跳过。
		List<FTPFile> list = new ArrayList<FTPFile>();
		for(FTPFile ff : ftpFiles){
			if(ff == null || ff.getName() == null || ff.getName().trim().isEmpty() || !ff.isDirectory())
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			String dir = FilenameUtils.getFullPath(path);
			ff.setName(dir + filename);
			list.add(ff);
		}

		Collections.sort(list, getFTPFileComparator());
		return list;
	}

	/**
	 * 下载GZ文件
	 * 
	 * @param filePath
	 * @param downLoadPath
	 * @param ftpClient
	 * @return
	 * @throws Exception
	 */
	public static File downLoadGZ(String filePath, String downLoadPath, FTPClient ftpClient) throws Exception{
		String fileName = FilenameUtils.getName(filePath);
		File file = null;
		FileOutputStream fos = null;
		GZIPInputStream inputStream = null;
		try{
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			file = new File(downLoadPath, fileName);
			fos = new FileOutputStream(file);
			inputStream = new GZIPInputStream(ftpClient.retrieveFileStream(filePath));
			int bytesWritten = 0;
			int byteCount = 0;
			byte [] bytes = new byte[1024];
			while((byteCount = inputStream.read(bytes)) != -1){
				fos.write(bytes, bytesWritten, byteCount);
				bytesWritten += byteCount;
			}
		}catch(Exception e){
			if(file != null){
				file.delete();
			}
			file = null;
			throw e;
		}finally{
			IoUtil.closeQuietly(fos);
			IoUtil.closeQuietly(inputStream);
		}
		return file;
	}

	/**
	 * 下载文件
	 * 
	 * @param filePath
	 * @param downLoadPath
	 * @param ftpClient
	 * @return
	 * @throws Exception
	 */
	public static File downLoad(String filePath, String downLoadPath, FTPClient ftpClient) throws Exception{
		String fileName = FilenameUtils.getName(filePath);
		File file = null;
		FileOutputStream fos = null;
		InputStream inputStream = null;
		try{
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			file = new File(downLoadPath, fileName);
			fos = new FileOutputStream(file);
			inputStream = download(filePath, ftpClient);
			int bytesWritten = 0;
			int byteCount = 0;
			byte [] bytes = new byte[1024];
			while((byteCount = inputStream.read(bytes)) != -1){
				fos.write(bytes, bytesWritten, byteCount);
				bytesWritten += byteCount;
			}
		}catch(Exception e){
			if(file != null){
				file.delete();
			}
			file = null;
			throw e;
		}finally{
			IoUtil.closeQuietly(fos);
			IoUtil.closeQuietly(inputStream);
		}
		return file;
	}

	/**
	 * FTP 下载过程，包括重试。
	 */
	public static InputStream download(String ftpPath, FTPClient ftpClient){
		InputStream in = retrNoEx(ftpPath, ftpClient);
		if(in != null){
			return in;
		}
		log.warn("FTP下载失败，开始重试，文件：{}，reply={}", new Object[]{ftpPath,
				ftpClient.getReplyString() != null ? ftpClient.getReplyString().trim() : ""});
		for(int i = 0; i < 3; i++){
			try{
				Thread.sleep(3000);
			}catch(InterruptedException e){
				log.warn("FTP 下载重试过程中线程被中断。", e);
				return null;
			}
			log.debug("第{}次重试下载。", i + 1);
			completePendingCommandNoEx(ftpClient);
			in = retrNoEx(ftpPath, ftpClient);
			if(in != null){
				log.debug("第{}次重试下载成功。", i + 1);
				break;
			}
		}
		return in;
	}

	/**
	 * FTP接收，处理异常。
	 * 
	 * @param ftpPath
	 * @param ftpClient
	 * @return
	 */
	public static InputStream retrNoEx(String ftpPath, FTPClient ftpClient){
		InputStream in = null;
		try{
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			in = ftpClient.retrieveFileStream(ftpPath);
		}catch(IOException e){
			log.error("FTP下载异常：" + ftpPath, e);
		}

		return in;
	}

	/**
	 * 从FTP读取了流之后，需要读取FTP响应消息，否则下次操作时将会失败
	 */
	public static boolean completePendingCommandNoEx(FTPClient ftpClient){
		boolean b = true;
		try{
			b = ftpClient.completePendingCommand();
			if(!b)
				log.warn("FTP失败响应：{}", ftpClient.getReplyString());
		}catch(Exception e){
			log.error("获取FTP响应异常。", e);
			return false;
		}

		return b;
	}

	private FTPUtil(){
		super();
	}

	private static class FTPFileComparator implements Comparator<FTPFile>{

		@Override
		public int compare(FTPFile o1, FTPFile o2){
			if(o1 == null && o2 == null)
				return 0;
			if(o1 == null)
				return -1;
			if(o2 == null)
				return 1;
			String name1 = (o1.getName() != null ? o1.getName() : "");
			String name2 = (o2.getName() != null ? o2.getName() : "");
			return name1.compareTo(name2);
		}
	}

	public FTPClient createFTPClient(FtpInfo ftpInfo){
		FTPClient ftp = new FTPClient();
		ftp.setBufferSize(ftpInfo.getBufferSize());
		ftp.setRemoteVerificationEnabled(false);
		int timeout = ftpInfo.getDataTimeout();
		ftp.setDataTimeout(timeout * 1000);
		ftp.setDefaultTimeout(timeout * 1000);

		/* ftpConfig.xml中配置了此任务使用PASV模式 */
		if("pasv".equals(ftpInfo.getPassiveFlag())){
			ftp.enterLocalPassiveMode();
			log.debug("进入FTP被动模式。ftp entering passive mode");
		}else{
			log.debug("进入FTP主动模式。ftp entering local mode");
		}
		setFTPClientConfig(ftp);
		try{
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
		}catch(IOException e){
			log.error("设置FileType时异常", e);
		}
		return ftp;
	}

	/**
	 * 自动设置FTP服务器类型
	 */
	public static FTPClientConfig setFTPClientConfig(FTPClient ftp){
		FTPClientConfig cfg = null;
		try{
			ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_UNIX));
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NT));
			}else{
				log.debug("ftp type:UNIX");
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_AS400));
			}else{
				log.debug("ftp type:NT");
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_L8));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_MVS));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NETWARE));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_OS2));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_OS400));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_VMS));
			}else{
				return cfg;
			}
			if(!isFilesNotNull(ftp.listFiles("/*"))){
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NT));
				log.debug("ftp type:NT...last");
				return cfg;
			}
		}catch(Exception e){
			log.error("配置FTP客户端时异常", e);
			ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_UNIX));
		}
		return cfg;
	}

	/**
	 * 判断listFiles出来的FTP文件，是否不是空的
	 * 
	 * @param fs listFiles出来的FTP文件
	 * @return listFiles出来的FTP文件，是否不是空的
	 */
	private static boolean isFilesNotNull(FTPFile [] fs){
		return isFileNotNull(fs);
	}

	public static boolean isFileNotNull(FTPFile [] fs){
		if(fs == null){
			return false;
		}
		if(fs.length == 0){
			return false;
		}
		boolean b = false;
		for(FTPFile f : fs){
			if((f != null && StringUtil.isNotEmpty(f.getName()) && !f.getName().contains("\t"))
					|| (f != null && StringUtil.isNotEmpty(f.getName()) && f.getName().contains(".loc"))){
				return true;
			}
		}
		return b;
	}

	/**
	 * FTP服务器linux默认FTP，windows默认GBK<br>
	 * 通过FEAT命令查看是否支持UTF8模式，如果支持则设置发送OPTS UTF8 ON命令，并返回这只UTF-8编码集<br>
	 * 如果不支持UTF8模式，则查看FTP服务器的系统类型<br>
	 * 如果是WINDOWS则默认返回GBK<br>
	 * 如果不是windows则默认返回UTF-8
	 * 
	 * @param ftp 登陆后的FTPClient
	 * @return 服务端编码集
	 * @throws IOException
	 */
	public static String autoSetCharset(FTPClient ftp) throws IOException{
		ftp.feat();
		String replay = ftp.getReplyString();
		if(replay.toUpperCase().contains("UTF8")){
			ftp.sendCommand("OPTS UTF8", "ON");
			return "UTF-8";
		}
		ftp.sendCommand("SYST");
		replay = ftp.getReplyString();
		if(replay.toUpperCase().contains("WINDOWS")){
			return "GBK";
		}
		return "UTF-8";
	}

	public static void main(String [] args) throws Exception{
		FTPClient ftp = new FTPClient();
		//		downLoad("/ftp/lte/unicome/华为/北京-性能/neexport_20140526/FBJ900004/A20140526.0215+0800-0230+0800_FBJ900004.xml.gz","",ftp);
		//		ftp.connect("ftp.hxjy.com");
		ftp.connect("delivery04-mul.dhe.ibm.com");
		//		ftp.connect("192.168.15.223");
		//		ftp.login("rd", "uway_rd_good");
		ftp.login("anonymous", "");
		System.out.println(autoSetCharset(ftp));
		//		FTPFile [] listFiles = ftp.listFiles("/ftp/lte/unicome/华为");
		//		for(int i = 0; i < listFiles.length; i++){
		//			System.out.println(StringUtil.decodeFTPPath(listFiles[i].getName(), "GBK"));
		//			System.out.println(StringUtil.decodeFTPPath(listFiles[i].getGroup(), "GBK"));
		//		}
		//		String filePath = "/临时存放/OMR0003/ST12_17111521_02A.T";
		//		String filePath = "/ftp/lte/unicome/华为/北京-性能/neexport_20140526/FBJ900001/A20140526.0200+0800-0215+0800_FBJ902499.xml";
		//		downLoad(StringUtil.encodeFTPPath(filePath, "UTF-8"), "D:/", ftp);
		//		List<FTPFile> fs = listFilesRecursive(ftp, "/20*/*_1X_*.zip");
		//		for(FTPFile f : fs)
		//			System.out.println(f.getName());
	}
}
