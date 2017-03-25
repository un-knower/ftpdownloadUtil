package cn.uway.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.pool.FTPPoolManager;
import cn.uway.task.FtpInfo;

/**
 * FTP工具
 * 
 * @author ChenSijiang 2010-8-24
 */
public class FTPTool {

	protected FtpInfo ftpInfo;

	protected FTPClient ftp;

	protected FTPClientConfig ftpCfg;

	protected FTPPoolManager ftpPool;

	private FTPFileComparator ftpFileComparator = new FTPFileComparator();
	
	private FTPFilePathComparator ftpFilePathComparator = new FTPFilePathComparator();

	protected static Logger logger = LogMgr.getInstance().getSystemLogger();

	/** 按时间分组的文件列表 */
	public Map<String, Set<String>> groupbyTimeFileMap = null;

	/**
	 * 构造方法，关联任务信息
	 * 
	 * @param taskInfo
	 *            任务信息
	 */
	public FTPTool(FtpInfo ftpInfo) {
		super();
		this.ftpInfo = ftpInfo;
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
	public boolean login(FTPPoolManager pool) {
		int tryTimes = ftpInfo.getLoginTryTimes();
		if (tryTimes < 0) {
			tryTimes = 3;
		}
		for (int i = 0; i < tryTimes; i++) {
			try {
				this.ftp = pool.getFTPClient(ftpInfo.getId());
			} catch (Exception e) {
				logger.debug("尝试从FTP连接池获取登陆链接失败", e);
			}
			if (this.ftp != null) {
				logger.debug("FTP连接获取登陆链接成功");
				return true;
			}
			logger.debug("尝试重新获取链接，次数:" + (i + 1));
		}
		return false;
	}

	/**
	 * 登录FTP
	 * 
	 * @param sleepTime
	 *            重试时的休眠时长，毫秒
	 * @param tryTimes
	 *            最大重试次数
	 * @return 是否登录成功
	 */
	public boolean login(int sleepTime, int tryTimes) {
		boolean b = false;
		if (login()) {
			return true;
		}
		int st = sleepTime;
		int tt = tryTimes;
		st = ftpInfo.getLoginTryDelay() * 1000;
		tt = ftpInfo.getLoginTryTimes();
		if (tt > 0) {
			for (int i = 0; i < tt; i++) {
				if (st > 0) {
					logger.debug("尝试重新登录，次数:" + (i + 1));
					try {
						Thread.sleep(st);
					} catch (InterruptedException e) {
						logger.error("休眠时线程被中断");
					}
					b = login();
					if (b) {
						logger.debug("重新登录成功");
						break;
					}
				}
			}
		}
		if (!b) {
			logger.debug("重新登录失败");
		}
		return b;
	}

	/**
	 * 下载指定的文件
	 * 
	 * @param ftpPath
	 *            需要下载的文件绝对路径
	 * @param localPath
	 *            放置下载后文件的本地文件夹
	 * @return 下载到的所有文件的本地路径，如果返回null，则表示下载失败
	 */
	public DownStructer downFile(String ftpPath, String localPath) {

		return null;
	}

	/**
	 * 断开FTP连接，归还FTP连接池
	 */
	public void disconn() {
		if (ftp != null) {
			try {
				ftp.disconnect();
			} catch (Exception e) {
				logger.error("【FTP断开时报错，不影响采集】ftp error disconnect" + ftp.getReplyString().replace("\n", "") + "  thread:" + Thread.currentThread(),
						e);
			}
			ftp = null;
		}
	}

	/**
	 * 断开FTP连接
	 */
	public void disconnect() {
		if (ftp != null) {
			try {
				ftp.logout();
				logger.debug("【FTP退出信息。】 ftp logout" + ftp.getReplyString().replace("\n", "").replace("\r", "") + "  thread:"
						+ Thread.currentThread());
			} catch (Exception e) {
				logger.error(" 【FTP断开时报错，不影响采集】ftp error logout" + ftp.getReplyString().replace("\n", "") + "  thread:" + Thread.currentThread(), e);
			}
			try {
				ftp.disconnect();
			} catch (Exception e) {
				logger.error("【FTP断开时报错，不影响采集】ftp error disconnect" + ftp.getReplyString().replace("\n", "") + "  thread:" + Thread.currentThread(),
						e);
			}
			ftp = null;
		}
	}

	/**
	 * 登录到FTP服务器
	 * 
	 * @return 是否成功
	 */
	private boolean login() {
		disconnect();
		ftp = new FTPClient();
		ftp.setBufferSize(ftpInfo.getBufferSize());
		ftp.setRemoteVerificationEnabled(false);
		int timeout = ftpInfo.getDataTimeout();
		ftp.setDataTimeout(timeout * 1000);
		ftp.setDefaultTimeout(timeout * 1000);

		boolean b = false;
		try {
			logger.debug("正在连接到 - " + ftpInfo.getIp() + ":" + ftpInfo.getPort());
			ftp.connect(ftpInfo.getIp(), ftpInfo.getPort());
			logger.debug("ftp connected");
			logger.debug("正在进行安全验证 - " + ftpInfo.getUsername() + " " + ftpInfo.getPassword());
			b = ftp.login(ftpInfo.getUsername(), ftpInfo.getPassword());
			logger.debug("ftp logged in" + "   thread:" + Thread.currentThread());
		} catch (Exception e) {
			logger.error("登录FTP服务器时异常", e);
			return false;
		}
		if (b) {
			/* ftpConfig.xml中配置了此任务使用PASV模式 */
			if (ftpInfo.getPassiveFlag()) {
				ftp.enterLocalPassiveMode();
				logger.debug("进入FTP被动模式。ftp entering passive mode");
			} else {
				logger.debug("进入FTP主动模式。ftp entering local mode");
			}
			if (this.ftpCfg == null) {
				this.ftpCfg = setFTPClientConfig();
			} else {
				ftp.configure(this.ftpCfg);
			}
			try {
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			} catch (IOException e) {
				logger.error("设置FileType时异常", e);
			}
		} else {
			String rep = "";
			try {
				rep = ftp.getReplyString().replace("\n", "").trim();
			} catch (Exception e) {
			}
			logger.warn("注意：用户名/密码可能错误，服务器返回信息：" + rep);
		}
		return b;
	}

	/**
	 * 判断FTP服务器返回码是否是2XX，即成功
	 * 
	 * @return FTP服务器返回码是否是2XX，即成功
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private boolean isPositiveCompletion() throws IOException {
		if (ftp == null) {
			return false;
		}
		return ftp.completePendingCommand();
	}

	/**
	 * 判断listFiles出来的FTP文件，是否不是空的
	 * 
	 * @param fs
	 *            listFiles出来的FTP文件
	 * @return listFiles出来的FTP文件，是否不是空的
	 */
	public static boolean isFilesNotNull(FTPFile[] fs) {
		if (fs == null) {
			return false;
		}
		if (fs.length == 0) {
			return false;
		}
		boolean b = false;
		for (FTPFile f : fs) {
			if ((f != null && StringUtil.isNotEmpty(f.getName()) && !f.getName().contains("\t"))
					|| (f != null && StringUtil.isNotEmpty(f.getName()) && f.getName().contains(".loc"))) {
				return true;
			}
		}
		return b;
	}

	/**
	 * 编码一条FTP路径
	 * 
	 * @param ftpPath
	 *            FTP路径
	 * @return 编码后的路径
	 */
	private String encodeFTPPath(String ftpPath) {
		try {
			String str = StringUtil.isNotEmpty(ftpInfo.getCharset()) ? new String(ftpPath.getBytes(ftpInfo.getCharset()), "ISO_8859_1") : ftpPath;
			return str;
		} catch (UnsupportedEncodingException e) {
			logger.error("设置的编码不正确:" + ftpInfo.getCharset(), e);
		}
		return ftpPath;
	}

	/**
	 * 解码一条FTP路径
	 * 
	 * @param ftpPath
	 *            FTP路径
	 * @return 解码后的路径
	 */
	// private String decodeFTPPath(String ftpPath){
	// try{
	// String str = StringUtil.isNotEmpty(ftpInfo.getCharset()) ? new String(ftpPath.getBytes("ISO_8859_1"),
	// ftpInfo.getCharset()) : ftpPath;
	// return str;
	// }catch(UnsupportedEncodingException e){
	// logger.error("设置的编码不正确:" + ftpInfo.getCharset(), e);
	// }
	// return ftpPath;
	// }

	/**
	 * 下载单个文件
	 * 
	 * @param path
	 *            单个文件的FTP绝对路径
	 * @param localPath
	 *            本地文件夹
	 * @param fileName
	 *            文件名
	 * @param remoteLength
	 * @return 是否下载成功
	 */
	public boolean downSingleFile(String path, String localPath, DownStructer downStruct, long remoteLength) {
		String fileName = FilenameUtils.getName(path);
		return downSingleFile(path, localPath, fileName, downStruct, remoteLength);
	}

	/**
	 * 创建下载临时文件，如果已经存在，则直接返回文件
	 * 
	 * @param downLoadPath
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	private File createDownLoadTempFile(String downLoadPath, String fileName) {
		File dir = new File(downLoadPath);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				logger.error("创建文件夹时异常:" + dir.getAbsolutePath());
				return null;
			}
		}
		File tempFile = new File(dir, fileName + ".td_" + TimeUtil.getDateString_yyyyMMddHH(new Date()));
		if (!tempFile.exists()) {
			try {
				if (!tempFile.createNewFile()) {
					logger.error("创建临时文件失败:" + tempFile.getAbsolutePath());
					return null;
				}
			} catch (IOException e) {
				logger.error("创建临时文件失败:" + tempFile.getAbsolutePath(), e);
				return null;
			}
		} else {
			logger.debug("文件已存在,文件名=" + tempFile.getAbsolutePath() + ",字节=" + tempFile.length());
		}
		return tempFile;
	}

	/**
	 * 注销后重新登录FTP
	 * 
	 * @return
	 */
	public boolean reLoginFTP() {
		logger.debug("FTP链接异常，重新登录FTP");
		disconn();
		return login(ftpPool);
	}

	/**
	 * 下载单个文件
	 * 
	 * @param path
	 * @param localPath
	 * @param fileName
	 * @param remoteLength
	 * @return
	 */
	public boolean downSingleFile(String path, String localPath, long remoteLength) {
		String fileName = FilenameUtils.getName(path);
		File tempDownFile = createDownLoadTempFile(localPath, fileName);
		if (tempDownFile == null) {
			return false;
		}
		// 判断是否接续下载
		long breakPoint = 0;
		long tempLength = tempDownFile.length();
		if (tempLength > 0 && tempLength <= remoteLength) {
			breakPoint = tempLength;
		}
		InputStream ftpInput = null;
		String encodePath = encodeFTPPath(path);
		try {
			ftpInput = ftp.retrieveFileStream(encodePath);
			if (ftpInput == null) {
				logger.error("FTP服务器返回输入流为null，可能文件不存在:" + path);
				return false;
			}
		} catch (IOException e) {
			logger.error("下载单个文件时FTP链接出现异常:" + path, e);
			reLoginFTP();
			return false;
		}

		// 输出到文件
		if (!outputToFile(tempDownFile, fileName, ftpInput, breakPoint)) {
			return false;
		}
		if (tempDownFile.length() < remoteLength) {
			logger.warn(tempDownFile.getAbsoluteFile() + ":文件下载不完整，理论长度:" + tempDownFile + "，实际下载长度:" + tempDownFile.length());
		}
		// 重命名
		if (!renameTempDownFile(tempDownFile, fileName)) {
			logger.error(tempDownFile.getAbsoluteFile() + "重命名为：" + fileName + "失败");
			return false;
		}
		return true;
	}

	/**
	 * @param path
	 * @param tempDownFile
	 * @param encodePath
	 */
	@SuppressWarnings("unused")
	public void readPartContent(byte[] bytes, File tempDownFile, String fileName) {
		String parent = tempDownFile.getParent();
		File targetFile = new File(parent, fileName);
		GZIPInputStream gzip = null;
		try {
			gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
			byte[] bytes_ = new byte[ftpInfo.getBufferSize()];
			int c;
			while ((c = gzip.read(bytes_)) != -1) {
				String str = new String(bytes_);
				String flag = "startTime=\"";
				int index = str.indexOf(flag);
				if (index > -1) {
					// startTime="2014-11-04T02:30:00.000+08:00:00"
					String time = str.substring(index + flag.length()).substring(0, 13).replace("T", " ").replace("-", "").replace(" ", "");
					if (groupbyTimeFileMap == null)
						groupbyTimeFileMap = new HashMap<String, Set<String>>();
					Set<String> fileList = groupbyTimeFileMap.get(time);
					if (fileList == null) {
						fileList = new HashSet<String>();
						groupbyTimeFileMap.put(time, fileList);
					}
					fileList.add(targetFile.getAbsolutePath());
					break;
				}
			}
		} catch (IOException e) {
			logger.error("下载时读取内容出现异常，file=" + fileName, e);
		} finally {
			IoUtil.closeQuietly(gzip);
		}
	}

	/**
	 * 重命名文件
	 * 
	 * @param tempDownFile
	 * @param fileName
	 * @return
	 */
	private boolean renameTempDownFile(File tempDownFile, String fileName) {
		String parent = tempDownFile.getParent();
		File targetFile = new File(parent, fileName);
		try {
			if (targetFile.exists()) {
				logger.info("文件:" + fileName + "已经存在，程序将进行覆盖");
				targetFile.delete();
			}
			FileUtils.moveFile(tempDownFile, targetFile);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * 将输入流输出到文件中
	 * 
	 * @param tempDownFile
	 * @param ftpInput
	 * @return
	 */
	private boolean outputToFile(File tempDownFile, String fileName, InputStream ftpInput, long skipBytes) {
		int buffSize = 1024 *8;
		OutputStream out = null;
		try {
			if (skipBytes > 0) {
				logger.debug("文件已存在,文件名=" + tempDownFile.getAbsolutePath() + ",跳过字节数量=" + tempDownFile.length());
				ftpInput.skip(skipBytes);
				logger.debug("文件已存在,文件名=" + tempDownFile.getAbsolutePath() + ",跳过字节成功");
			}
			out = new FileOutputStream(tempDownFile, true);
			buffSize = ftpInfo.getBufferSize();
			byte[] bytes = new byte[buffSize];
			int c;
			// 读取内容，从中读取到time--nokia pm
			if (ftpInfo.getNeedToReadContent()) {
				if ((c = ftpInput.read(bytes)) != -1) {
					out.write(bytes, 0, c);
				}
				readPartContent(bytes, tempDownFile, fileName);
			}
			while ((c = ftpInput.read(bytes)) != -1) {
				out.write(bytes, 0, c);
			}
			return true;
		} catch (IOException e) {
			logger.error("FTP下载文件输出到本地出错:" + tempDownFile.getAbsolutePath(), e);
			return false;
		} finally {
			IoUtil.closeQuietly(out);
			IoUtil.closeQuietly(ftpInput);
			try {
				ftp.completePendingCommand();
			} catch (IOException e) {
				logger.error("FTP结束操作失败", e);
			}
		}
	}

	/**
	 * 下载单个文件，带有多次验证
	 * 
	 * @param path
	 * @param localPath
	 * @param fileName
	 * @param remoteLength
	 * @param retryTimes
	 * @param retryDelay
	 * @return
	 */
	public boolean downSingleFile(String path, String localPath, long remoteLength, int retryTimes, int retryDelay) {
		for (int i = 1; i < retryTimes + 1; i++) {
			boolean result = downSingleFile(path, localPath, remoteLength);
			if (result) {
				return true;
			}
			logger.info("文件[" + path + "]第" + i + "次下载失败，重新获取");
		}
		return false;
	}

	/**
	 * 下载单个文件
	 * 
	 * @param path
	 *            单个文件的FTP绝对路径
	 * @param localPath
	 *            本地文件夹
	 * @param fileName
	 *            文件名
	 * @param remoteLength
	 * @return 是否下载成功
	 */
	public boolean downSingleFile(String path, String localPath, String fileName, DownStructer downStruct, long remoteLength) {
		boolean result = false;
		boolean ex = false;
		boolean end = true;
		String singlePath = encodeFTPPath(path);
		File tdFile = null;
		InputStream in = null;
		OutputStream out = null;
		long length = remoteLength;
		long tdLength = 0;
		try {
			File dir = new File(localPath);
			if (!dir.exists()) {
				dir.mkdirs();
				// if (!) {
				// throw new Exception("创建文件夹时异常:" + dir.getAbsolutePath());
				// }
			}
			tdFile = new File(dir, fileName + ".td_" + TimeUtil.getDateString_yyyyMMddHH(new Date()));
			if (!tdFile.exists()) {
				if (!tdFile.createNewFile()) {
					throw new Exception("创建临时文件失败:" + tdFile.getAbsolutePath());
				}
			} else {
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",字节=" + tdFile.length());
			}
			tdLength = tdFile.length();
			if (tdLength >= length) {
				end = true;
			}
			in = ftp.retrieveFileStream(singlePath);
			if (tdLength > 0) {
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",跳过字节数量=" + tdFile.length());
				in.skip(tdLength);
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",跳过字节成功");
			}
			out = new FileOutputStream(tdFile, true);
			int buffSize = 1024;
			buffSize = ftpInfo.getBufferSize();
			byte[] bytes = new byte[buffSize];
			int c;
			while ((c = in.read(bytes)) != -1) {
				out.write(bytes, 0, c);
			}
			if (tdFile.length() < length) {
				end = false;
				logger.warn(tdFile.getAbsoluteFile() + ":文件下载不完整，理论长度:" + length + "，实际下载长度:" + tdFile.length());
			}
		} catch (Exception e) {
			ex = true;
			if (in == null) {
				logger.error("FTP服务器返回输入流为null，可能文件不存在 - " + path, e);
			} else {
				logger.error("下载单个文件时异常:" + path, e);
			}
			result = false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			try {
				if (in != null)
					ftp.completePendingCommand();
			} catch (IOException e1) {
			}
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
				}
			}
			if (!ex && (end || tdLength < 0)) {
				if (in != null) {
					File f = new File(localPath, fileName);
					if (f.exists()) {
						f.delete();
					}
					boolean bRename = tdFile.renameTo(f);
					if (!bRename) {
						logger.error("将" + tdFile.getAbsolutePath() + "重命名为" + f.getAbsolutePath() + "时失败，" + f.getAbsolutePath() + "被占用");
					} else {
						tdFile.delete();
						result = true;

						if (f.length() == 0) {
							if (!downStruct.getFail().contains(singlePath))
								downStruct.getFail().add(singlePath);
							if (downStruct.getLocalFail().contains(f.getAbsolutePath()))
								downStruct.getLocalFail().add(f.getAbsolutePath());
							logger.error(": 文件 " + f.getAbsolutePath() + " 长度为0");
							return false;
						}

					}
				} else {
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * 下载单个文件
	 * 
	 * @param path
	 *            单个文件的FTP绝对路径
	 * @param localPath
	 *            本地文件夹
	 * @param fileName
	 *            文件名
	 * @param remoteLength
	 * @return 是否下载成功
	 */
	public boolean downSingleFileForGz(String path, String localPath, DownStructer downStruct, long remoteLength) {
		String fileName = FilenameUtils.getName(path);
		return downSingleFileForGz(path, localPath, fileName, downStruct, remoteLength);
	}

	/**
	 * 下载单个文件
	 * 
	 * @param path
	 *            单个文件的FTP绝对路径
	 * @param localPath
	 *            本地文件夹
	 * @param fileName
	 *            文件名
	 * @param remoteLength
	 * @return 是否下载成功
	 */
	public boolean downSingleFileForGz(String path, String localPath, String fileName, DownStructer downStruct, long remoteLength) {
		boolean result = false;
		boolean ex = false;
		boolean end = true;
		String singlePath = encodeFTPPath(path);
		File tdFile = null;
		GZIPInputStream in = null;
		OutputStream out = null;
		long length = remoteLength;
		long tdLength = 0;
		fileName = fileName.replace(".gz", "");
		try {
			File dir = new File(localPath);
			if (!dir.exists()) {
				dir.mkdirs();
				// if (!dir.mkdirs()) {
				// throw new Exception("创建文件夹时异常:" + dir.getAbsolutePath());
				// }
			}
			tdFile = new File(dir, fileName + ".td_" + TimeUtil.getDateString_yyyyMMddHH(new Date()));
			if (!tdFile.exists()) {
				if (!tdFile.createNewFile()) {
					throw new Exception("创建临时文件失败:" + tdFile.getAbsolutePath());
				}
			} else {
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",字节=" + tdFile.length());
			}
			tdLength = tdFile.length();
			if (tdLength >= length) {
				end = true;
			}
			in = new GZIPInputStream(ftp.retrieveFileStream(singlePath));
			if (tdLength > 0) {
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",跳过字节数量=" + tdFile.length());
				in.skip(tdLength);
				logger.debug("文件已存在,文件名=" + tdFile.getAbsolutePath() + ",跳过字节成功");
			}
			out = new FileOutputStream(tdFile, true);
			int buffSize = 1024;
			buffSize = ftpInfo.getBufferSize();;
			byte[] bytes = new byte[buffSize];
			int c;
			while ((c = in.read(bytes)) != -1) {
				out.write(bytes, 0, c);
			}
			if (tdFile.length() < length) {
				end = false;
				logger.warn(tdFile.getAbsoluteFile() + ":文件下载不完整，理论长度:" + length + "，实际下载长度:" + tdFile.length());
			}
		} catch (Exception e) {
			ex = true;
			if (in == null) {
				logger.error("FTP服务器返回输入流为null，可能文件不存在 - " + path, e);
			} else {
				logger.error("下载单个文件时异常:" + path, e);
			}
			result = false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			try {
				if (in != null)
					ftp.completePendingCommand();
			} catch (IOException e1) {
			}
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
				}
			}
			if (!ex && (end || tdLength < 0)) {
				if (in != null) {
					File f = new File(localPath, fileName);
					if (f.exists()) {
						f.delete();
					}
					boolean bRename = tdFile.renameTo(f);
					if (!bRename) {
						logger.error("将" + tdFile.getAbsolutePath() + "重命名为" + f.getAbsolutePath() + "时失败，" + f.getAbsolutePath() + "被占用");
					} else {
						tdFile.delete();
						result = true;

						if (f.length() == 0) {
							if (!downStruct.getFail().contains(singlePath))
								downStruct.getFail().add(singlePath);
							if (downStruct.getLocalFail().contains(f.getAbsolutePath()))
								downStruct.getLocalFail().add(f.getAbsolutePath());
							logger.error(": 文件 " + f.getAbsolutePath() + " 长度为0");
							return false;
						}

					}
				} else {
					result = false;
				}
			}

		}
		return result;
	}

	/**
	 * 自动设置FTP服务器类型
	 */
	private FTPClientConfig setFTPClientConfig() {
		FTPClientConfig cfg = null;
		try {
			ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_UNIX));
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NT));
			} else {
				logger.debug("ftp type:UNIX");
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_AS400));
			} else {
				logger.debug("ftp type:NT");
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_L8));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_MVS));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NETWARE));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_OS2));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_OS400));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_VMS));
			} else {
				return cfg;
			}
			if (!isFilesNotNull(ftp.listFiles("/*"))) {
				ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_NT));
				logger.debug("ftp type:NT...last");
				return cfg;
			}
		} catch (Exception e) {
			logger.error("配置FTP客户端时异常", e);
			ftp.configure(cfg = new FTPClientConfig(FTPClientConfig.SYST_UNIX));
		}
		return cfg;
	}

	public void finalize() throws Throwable {
		disconnect();
	}

	/**
	 * FTP执行LIST命令，递归的获取文件列表。如果失败，将返回<code>null</code>. 注意，之支持一级目录有星号的递归。
	 * 
	 * @param ftp
	 *            FTP连接。
	 * @param path
	 *            路径。
	 * @return 文件列表。
	 * @throws IOException
	 *             操作失败。
	 */
	public List<String> listFilesRecursive(String path) throws IOException {
		logger.debug("开始listFilesRecursive:" + path);
		// 目录无通配符的情况，直接返回。
		String parentPath = FilenameUtils.getFullPath(path);
		if (!parentPath.contains("*") && !parentPath.contains("?"))
			return listFiles(path);

		String[] spPath = path.split("/");
		String namePart = FilenameUtils.getName(path);
		String wildDir = "";

		List<String> parsedDirs = new ArrayList<String>();
		String currFullDir = "/";
		for (int i = 0; i < spPath.length; i++) {
			String dir = spPath[i];
			if (dir == null || dir.trim().isEmpty() || dir.equals(namePart))
				continue;
			if (dir.contains("*") || dir.contains("?")) {
				wildDir = dir;
				FTPFile[] dirs = ftp.listDirectories(encodeFTPPath(currFullDir + "/" + wildDir));
				// logger.debug("replyCode:" + ftp.getReplyCode() +
				// ", replyString:" + ftp.getReplyString().replace("\n", ""));
				logger.debug("listDirectories目录的个数:" + dirs.length);
				for (FTPFile ftpDir : dirs) {
					if (ftpDir == null || ftpDir.getName() == null)
						continue;
					if (FilenameUtils.wildcardMatch(ftpDir.getName(), dir))
						parsedDirs.add(ftpDir.getName());
				}
				break;
			} else {
				currFullDir += (dir + "/");
			}
		}

		List<String> files = new ArrayList<String>();

		for (int i = 0; i < parsedDirs.size(); i++) {
			if (parsedDirs.get(i) == null)
				continue;

			String oneDir = path.replace("/" + wildDir + "/", "/" + parsedDirs.get(i) + "/");
			List<String> tmp = listFilesRecursive(oneDir);
			if (tmp != null)
				files.addAll(tmp);
		}

		Collections.sort(files, getFTPFilePathComparator());
		return files;
	}
	
	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp FTP连接。
	 * @param path 路径。
	 * @return 文件列表。
	 */
	public List<FTPFile> listDirectories(String path){
		if(ftp == null){
			logger.debug("ftp为null.");
			return null;
		}
		if(path == null){
			logger.debug("path为null.");
			return null;
		}
		FTPFile [] ftpFiles = null;
		/*[临时解决方案]如果没有list文件：包括list失败和没有list到数据，此处会重试一次，如果再失败，那么就和原逻辑保持一样，将会返回空。
		 *此处有一个比较怪异的问题，那就是像下面这样的条件， 
		 *(/opt/oss/server/var/fileint/pmneexport/*)
		 * 有时候能正常返回如下：
		 /opt/oss/server/var/fileint/pmneexport/neexport_20160124
		/opt/oss/server/var/fileint/pmneexport/neexport_20160125
		 * 有时候没返回又不抛异常
		 * */
		
		int times = 0;
		do{
			try{
				ftpFiles = ftp.listDirectories(path);
				if(ftpFiles==null||ftpFiles.length==0){
					times++;
				}
			}catch(IOException e){
				// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
				times++;
				logger.error("FTP listDirectories时发生异常。", e);
			}
			//如果为0，则没有listDirectories失败；
			if(times==0){
				break;
			}else if((ftpFiles==null||ftpFiles.length==0)&&(times==1)){
				if(!reLoginFTP()){
					//如果从新登陆失败，则不再进行重试。
					break;
				}
			}else{
				break;
			}
		}while(true);
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
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp
	 *            FTP连接。
	 * @param path
	 *            路径。
	 * @return 文件列表。
	 */
	public List<String> listFiles(String path) {
		if (ftp == null) {
			logger.warn("ftp为null.");
			return null;
		}
		if (path == null) {
			logger.warn("path为null.");
			return null;
		}

		FTPFile[] ftpFiles = null;

		try {
			logger.debug("开始listFile:" + path);
			ftpFiles = ftp.listFiles(encodeFTPPath(path));
			// logger.debug("replyCode:" + ftp.getReplyCode() + ", replyString:"
			// + ftp.getReplyString().replace("\n", ""));
			logger.debug("listFile文件/目录的个数:" + ftpFiles.length);
		} catch (IOException e) {
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			logger.warn("FTP listFiles时发生异常。", e);
			return null;
		}

		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if (ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		// 正常化文件列表， 做四个处理：
		// 1、为null的FTPFile对象消除；
		// 2、文件名为null的FTP对象清除；
		// 3、文件名改名绝对路径；
		// 4、如果不是文件，跳过。
		List<String> list = new ArrayList<String>();
		for (FTPFile ff : ftpFiles) {
			if (ff == null || ff.getName() == null || ff.getName().trim().isEmpty())
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			String dir = FilenameUtils.getFullPath(path);
			ff.setName(dir + filename);
			list.add(dir + filename);
		}

		Collections.sort(list, getFTPFilePathComparator());
		return list;
	}

	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp
	 *            FTP连接。
	 * @param path
	 *            路径。
	 * @return 文件列表。
	 */
	public List<FTPFile> listFTPFiles(String path) {
		if (ftp == null) {
			logger.warn("ftp为null.");
			return null;
		}
		if (path == null) {
			logger.warn("path为null.");
			return null;
		}

		FTPFile[] ftpFiles = null;
		try {
			ftpFiles = ftp.listFiles(path);
		} catch (IOException e) {
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			logger.warn("FTP listFiles时发生异常", e);
			logger.debug("返回信息：" + ftp.getReplyCode() + "," + ftp.getReplyString());
			reLoginFTP();
			return null;
		}

		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if (ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		// 正常化文件列表， 做四个处理：
		// 1、为null的FTPFile对象消除；
		// 2、文件名为null的FTP对象清除；
		// 3、文件名改名绝对路径；
		// 4、如果不是文件，跳过。
		List<FTPFile> list = new ArrayList<FTPFile>();
		for (FTPFile ff : ftpFiles) {
			// FTPUtil中已经判断过了，此处不用判断，而且要兼容软／硬连接的文件．添加（&& !ff.isSymbolicLink()）
			if (ff == null || ff.getName() == null || ff.getName().trim().isEmpty()|| (!ff.isFile() && !ff.isSymbolicLink()))
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			String dir = FilenameUtils.getFullPath(path);
			ff.setName(dir + filename);
			list.add(ff);
		}

		return list;
	}

	/**
	 * FTP执行LIST命令，获取文件列表。如果失败，将返回<code>null</code>.
	 * 
	 * @param ftp
	 *            FTP连接。
	 * @param path
	 *            路径。
	 * @return 文件列表。
	 */
	public List<String> listFiles_(String path) {
		if (ftp == null) {
			logger.warn("ftp为null.");
			return null;
		}
		if (path == null) {
			logger.warn("path为null.");
			return null;
		}

		FTPFile[] ftpFiles = null;

		try {
			logger.debug("开始listFile:" + path);
			ftpFiles = ftp.listFiles(encodeFTPPath(path));
			logger.debug("listFile文件/目录的个数:" + ftpFiles.length);
		} catch (IOException e) {
			// 异常时，返回null，告知调用者listFiles失败，有可能是网络原因，可重试。
			logger.warn("FTP listFiles时发生异常。", e);
			return null;
		}

		// listFiles返回null或长度为0时，可认为确实无文件，即使重试，也是一样。
		// 所以此处正常返回，即返回一个长度为0的List.
		if (ftpFiles == null || ftpFiles.length == 0)
			return Collections.emptyList();

		// 正常化文件列表， 做四个处理：
		// 1、为null的FTPFile对象消除；
		// 2、文件名为null的FTP对象清除；
		// 3、文件名改名绝对路径；
		// 4、如果不是文件，跳过。
		List<String> list = new ArrayList<String>();
		for (FTPFile ff : ftpFiles) {
			if (ff == null || ff.getName() == null || ff.getName().trim().isEmpty())
				continue;
			if (ff.isFile())
				continue;
			String filename = FilenameUtils.getName(ff.getName());
			ff.setName(path + "/" + filename);
			list.add(path + "/" + filename);
		}

		Collections.sort(list, getFTPFilePathComparator());
		return list;
	}

	public FTPFileComparator getFTPFileComparator() {
		return ftpFileComparator;
	}
	public FTPFilePathComparator getFTPFilePathComparator() {
		return ftpFilePathComparator;
	}
	private class FTPFilePathComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			if (o1 == null && o2 == null)
				return 0;
			if (o1 == null)
				return -1;
			if (o2 == null)
				return 1;
			String name1 = (o1 != null ? o1 : "");
			String name2 = (o2 != null ? o2 : "");
			return name1.compareTo(name2);
		}
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

	/**
	 * @return the ftpPool
	 */
	public FTPPoolManager getFtpPool() {
		return ftpPool;
	}

	/**
	 * @param ftpPool
	 *            the ftpPool to set
	 */
	public void setFtpPool(FTPPoolManager ftpPool) {
		this.ftpPool = ftpPool;
	}

	/**
	 * @return the ftpInfo
	 */
	public FtpInfo getFtpInfo() {
		return ftpInfo;
	}

	/**
	 * @param ftpInfo
	 *            the ftpInfo to set
	 */
	public void setFtpInfo(FtpInfo ftpInfo) {
		this.ftpInfo = ftpInfo;
	}

	/**
	 * @return the ftp
	 */
	public FTPClient getFtp() {
		return ftp;
	}

	/**
	 * @param ftp
	 *            the ftp to set
	 */
	public void setFtp(FTPClient ftp) {
		this.ftp = ftp;
	}

	/**
	 * 获取此类使用的{@link FTPClient}对象。
	 * 
	 * @return 此类使用的{@link FTPClient}对象。
	 */
	public FTPClient getFtpClient() {
		return ftp;
	}

}
