package cn.uway.util;

import java.io.File;

import cn.uway.config.SystemConfig;

/**
 * 解压工具类 DeCompression
 * 
 */
public class DeCompression {

	/**
	 * 解压文件
	 * 
	 * @param nTaskID
	 * @param base
	 * @param strFile
	 * @param timestamp
	 * @param nPeriod
	 * @param bDelStrFile
	 *            是否要删除原文件，true删除，false不删除
	 * @return
	 * @throws Exception
	 */
	public static void decompress(String strFile, boolean bDelStrFile) throws Exception {
		if (Util.isWindows()) {
			decompressWin(strFile, bDelStrFile);
		} else {
			decompressUnix(strFile, bDelStrFile);
		}
	}

	private static void decompressWin(String strFile, boolean bDelStrFile) throws Exception {
		/* winrar执行文件的绝对路径。 */
		File winrarFullPath = new File(SystemConfig.getInstance().getWinrarPath());
		if (!winrarFullPath.exists())
			throw new Exception("winrar不存在，位置：" + winrarFullPath);

		/* 待解压的压缩文件。 */
		File zipFile = new File(strFile);

		/* 解压后文件的存放目录。 */
		File dir = new File(zipFile.getParent());
		String cmd = winrarFullPath.getAbsolutePath() + " e " + zipFile.getAbsolutePath() + " " + dir.getAbsolutePath() + " -y -ibck";

		int nSucceed = -1;
		try {
			nSucceed = new ExternalCmd().execute(cmd);
		} catch (Exception e) {
			throw e;
		}
		// 0表示返回无错误。成功的标记
		if (nSucceed == 0) {
			if (bDelStrFile) {
				// 删除压缩文件
				zipFile.delete();
			}
		} else {
			throw new Exception("decompress file error. file:" + strFile);
		}
	}

	/** 使用gzip解压缩文件 */
	private static void decompressUnix(String strFile, boolean bDelStrFile) throws Exception {
		/* 待解压的压缩文件。 */
		File zipFile = new File(strFile);

		/* 解压后文件的存放目录。 */
		File fFolder = new File(zipFile.getParent());
		if (!fFolder.exists()) {
			if (!fFolder.mkdir()) {
				throw new Exception("mkdir error");
			}
		}

		String cmd = (strFile.endsWith(".zip") ? "unzip -o " : "gzip -d -f ") + strFile
				+ (strFile.endsWith(".zip") ? (" -d " + fFolder.getAbsolutePath()) : "");
		int retCode = new ExternalCmd().execute(cmd);

		// 0表示返回无错误。成功的标记
		if (retCode == 0) {
			if (bDelStrFile) {
				// 删除压缩文件
				zipFile.delete();
			}
		} else {
			throw new Exception("decompress file error. file:" + strFile);
		}
	}
}
