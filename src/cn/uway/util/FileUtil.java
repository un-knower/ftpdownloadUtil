package cn.uway.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

/**
 * 文件操作工具类
 * 
 * @author MikeYang
 * @Date 2012-11-1
 * @version 3.0
 * @since 1.0
 */
public final class FileUtil {

	/**
	 * 判定指定的文件路径是否为文件还是文件夹
	 * 
	 * @param filePath
	 *            文件路径
	 * @return true表示为文件，false为文件夹
	 */
	public static boolean isFile(String filePath) {
		if (filePath == null || filePath.trim().isEmpty())
			return false;

		File file = new File(filePath);
		if (file.isFile())
			return true;
		else if (file.isDirectory())
			return false;
		else
			return false;
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param filePath
	 * @return true 表示文件存在 false表示文件不存在
	 */
	public static boolean exists(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}

	/**
	 * 获取指定文件夹下的所有文件列表，包含子文件夹
	 * 
	 * @param path
	 *            文件夹路径
	 * @return 文件夹下的所有文件组成的列表
	 */
	public static List<String> getFileNames(String path) {
		if (path == null || path.trim().isEmpty())
			return new ArrayList<String>();

		List<String> lst = new ArrayList<String>();
		if (isFile(path))
			lst.add(path);
		else {
			File pathFile = new File(path);
			File[] fileLst = pathFile.listFiles();
			for (File f : fileLst) {
				if (f.isFile())
					lst.add(f.getAbsolutePath());
				else
					lst.addAll(getFileNames(f.getPath())); // 递归调用
			}
		}

		return lst;
	}

	/**
	 * 获取指定文件夹下的所有文件列表，包含子文件夹
	 * 
	 * @param path
	 *            文件夹路径
	 * @return 文件夹下的所有文件组成的列表
	 */
	public static List<String> getFileNames(String path, final String filter) {
		if (path == null || path.trim().isEmpty())
			return new ArrayList<String>();
		if (StringUtil.isEmpty(filter))
			return getFileNames(path);
		List<String> lst = new ArrayList<String>();
		if (isFile(path))
			lst.add(path);
		else {
			File pathFile = new File(path);
			File[] fileLst = pathFile.listFiles();
			if (fileLst == null)
				return lst;
			for (File f : fileLst) {
				if (f.isDirectory()) {
					lst.addAll(getFileNames(f.getPath(), filter));
					continue;
				}
				if (FilenameUtils.wildcardMatch(f.getName(), filter))
					lst.add(f.getAbsolutePath());
			}
		}
		return lst;
	}

	/**
	 * 获取指定文件夹下的所有目录，不包含子文件夹
	 * 
	 * @param path
	 *            文件夹路径
	 * @return 文件夹下的所有文件组成的列表
	 */
	public static List<String> getDirNames(String path, final String filter) {
		if (path == null || path.trim().isEmpty())
			return new ArrayList<String>();
		if (StringUtil.isEmpty(filter))
			return getFileNames(path);
		List<String> lst = new ArrayList<String>();
		if (isFile(path))
			lst.add(path);
		else {
			File pathFile = new File(path);
			File[] fileLst = pathFile.listFiles();
			if (fileLst == null)
				return lst;
			for (File f : fileLst) {
				if (f.getName().endsWith(filter))
					lst.add(f.getAbsolutePath());
			}
		}
		return lst;
	}

	/**
	 * 获取指定文件夹下的所有文件列表，包含子文件夹
	 * 
	 * @param fileList
	 *            文件列表
	 * @return 文件夹下的所有文件组成的列表
	 */
	public static List<String> getFileNames(List<String> fileList, final String filter) {
		if (fileList == null || fileList.size() == 0)
			return new ArrayList<String>();
		if (StringUtil.isEmpty(filter))
			return fileList;
		List<String> lst = new ArrayList<String>();
		for (String f : fileList) {
			File file = new File(f);
			if (file.isDirectory()) {
				lst.addAll(getFileNames(file.getPath(), filter));
				continue;
			}
			if (FilenameUtils.wildcardMatch(file.getName(), filter))
				lst.add(f);
		}
		return lst;
	}

	/**
	 * 删除一组文件
	 * 
	 * @param fileNames
	 *            文件列表
	 */
	public static void removeFiles(List<String> fileNames) {
		if (fileNames == null || fileNames.size() == 0)
			return;
		for (String fielName : fileNames) {
			if (fielName == null || fielName.trim().isEmpty())
				continue;
			File f = new File(fielName);
			f.delete();
		}
	}

	/**
	 * 删除一个文件
	 * 
	 * @param removeFileName
	 * @return
	 */
	public static boolean removeFile(String removeFileName) {
		if (removeFileName == null || removeFileName.isEmpty())
			return false;
		if (!exists(removeFileName))
			return false;
		File removeFile = new File(removeFileName);
		return removeFile.delete();
	}

	/**
	 * 创建一个文件
	 * 
	 * @param fileName
	 * @return
	 */
	public static boolean createFile(String fileName) {
		if (fileName == null || fileName.isEmpty())
			return false;
		File createFileName = new File(fileName);
		File parent = createFileName.getParentFile();
		if (!parent.exists())
			parent.mkdirs();
		try {
			return createFileName.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 创建一个文件
	 * 
	 * @param file
	 * @return
	 */
	public static boolean createFile(File file) {
		if (file == null || file.exists())
			return false;
		File parent = file.getParentFile();
		if (!parent.exists())
			parent.mkdirs();
		try {
			return file.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 通过文件名获取文件大小
	 * 
	 * @param fileName
	 * @return long 文件大小
	 */
	public static long getFileSize(String fileName) {
		File file = new File(fileName);
		if (!file.exists())
			return 0L;
		return file.length();
	}

	/**
	 * 判断文件是否存在
	 * 
	 * @param filePath
	 * @return true 表示文件存在 false表示文件不存在
	 */
	public static boolean existsDirectory(String filePath) {
		File file = new File(filePath);
		return file.exists() && file.isDirectory();
	}

	/** 判断文件是否为压缩文件 */
	public static boolean isZipFile(String strFileName, String regx) {
		boolean bReturn = false;

		if (strFileName == null || strFileName.equals(""))
			return false;

		int nExIndex = strFileName.lastIndexOf(".");

		String strExName = "";
		if (nExIndex > 0)
			strExName = strFileName.substring(nExIndex).toLowerCase();

		if (regx == null) {
			if (strExName.equals(".zip") || strExName.equals(".tar") || strExName.equals(".tar.z") || strExName.equals(".gz")
					|| strExName.equals(".tar.bz2") || strExName.equals(".rar") || strExName.equals(".z")) {
				bReturn = true;
			} else {
				String zipFileSuffixs = ".Z,.z";
				if (zipFileSuffixs != null) {
					String[] array = zipFileSuffixs.split(",");
					for (int i = 0; i < array.length; i++) {
						if (array[i].equals(strExName)) {
							bReturn = true;
							break;
						}
					}// for(int i=0; i < array.length; i++)
				}// if(zipFileSuffix != null)
			}
		} else {
			if (strExName.equals(regx)) {
				bReturn = true;
			}
		}

		return bReturn;
	}

	/**
	 * @param path
	 * @return 返回文件完整名称（包含扩展名）
	 */
	public static String getFileFullName(String path) {
		int lastIndex = getLastSeparatorIndex(path);
		return path.substring(lastIndex + 1, path.length());
	}

	/**
	 * @param path
	 * @return 返回文件名称（不包含扩展名）
	 */
	public static String getFileName(String path) {
		int lastIndex = getLastSeparatorIndex(path);
		return path.substring(lastIndex + 1, path.lastIndexOf("."));
	}

	/**
	 * @param path
	 * @return LastSeparatorIndex 路径中最后一个分隔符的索引
	 */
	public static int getLastSeparatorIndex(String path) {
		int lastIndex = path.lastIndexOf("/");
		if (lastIndex == -1)
			lastIndex = path.lastIndexOf(File.separator);
		return lastIndex;
	}

}
