package cn.uway.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.util.FileUtil;

/**
 * 文件生成器
 * 
 * @author yuy 2014-3-13
 * @version 1.0
 * @since 3.0
 */
public class FileGenerator {

	/**
	 * 文件
	 */
	public File file;

	/**
	 * 文件输出流
	 */
	public FileOutputStream out;

	/**
	 * 是否续写
	 */
	public boolean append = false;

	/**
	 * 目的文件
	 */
	public File destFile;

	/**
	 * 日志记录
	 */
	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	public FileGenerator() {

	}

	public FileGenerator(String filePath, String fileFullName) {
		try {
			// 初始化生成文件
			this.file = new File(filePath, fileFullName);
			if (file.exists()) {
				LOGGER.debug(file.getPath() + "文件已存在，需要加载到内存中。");
				this.append = true;
				this.out = new FileOutputStream(file, append);
			} else {
				FileUtil.createFile(file);
				this.out = new FileOutputStream(file);
			}
		} catch (IOException e) {
			LOGGER.debug("文件生成错误", e);
		}
	}

	public FileGenerator(String fileFullName, String filePath, boolean append) {
		try {
			// 初始化生成文件
			this.file = new File(filePath, fileFullName);
			this.append = append;
			FileUtil.createFile(file);
			this.out = new FileOutputStream(file, append);
		} catch (IOException e) {
			LOGGER.debug("文件生成错误", e);
		}
	}

	/**
	 * 写入文件
	 * 
	 * @param b
	 */
	public void write(byte[] b) {
		try {
			this.out.write(b, 0, b.length);
		} catch (IOException e) {
			LOGGER.debug("文件写入错误");
		}
	}

	/**
	 * 重命名 修改扩展名
	 */
	public void reFileName(String fromExt, String toExt) {
		destFile = new File(file.getAbsolutePath().replace(fromExt, toExt));
		file.renameTo(destFile);
		LOGGER.debug("成功将日志文件" + file.getName() + "修改为：" + destFile.getName());
	}

	/**
	 * 生成文件结束
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.out.flush();
		this.out.close();
	}

	/**
	 * @return 文件
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 */
	public void setFile(File file) {
		this.file = file;
	}

}
