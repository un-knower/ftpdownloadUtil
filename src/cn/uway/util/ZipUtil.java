package cn.uway.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import cn.uway.config.LogMgr;

public class ZipUtil{

	private static final Logger log = LogMgr.getInstance().getSystemLogger();

	public static int BUFFER = 1024*8;
	
	/**
	 * 将tar文件压缩
	 * @param source
	 * @return
	 */
	public static boolean gzipFile(File source) {
		String gz = source.getPath() + ".gz";
		String gztmp = gz + ".tmp";
		File target = new File(gz);
		File targettmp = new File(gztmp);
		FileInputStream in = null;
		GZIPOutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new GZIPOutputStream(new FileOutputStream(targettmp));
			byte[] array = new byte[1024];
			int number = -1;
			while ((number = in.read(array, 0, array.length)) != -1) {
				out.write(array, 0, number);
			}
		} catch (Exception e) {
			log.error("压缩文件失败，file=" + source.getPath(), e);
			return false;
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
			targettmp.renameTo(target);
			targettmp.delete();
		}
		return true;
	}

	public static boolean zipDir(File dir, File target, boolean needTopDir){
		FileOutputStream f = null;
		ZipArchiveOutputStream out = null;
		try{
			if(!target.exists())
				FileUtil.createFile(target);
			f = new FileOutputStream(target);
			out = new ZipArchiveOutputStream(f);
			if(needTopDir)
				zipFile(out, dir, dir.getName());
			else
				zipFile(out, dir, "");
			return true;
		}catch(IOException e){
			log.error("压缩文件失败，dir=" + dir + "，target=" + target, e);
			return false;
		}finally{
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(f);
		}
	}

	private static void zipFile(ZipArchiveOutputStream out, File srcDir, String filePath) throws IOException{
		for(File file : srcDir.listFiles()){
			if(file.isDirectory()){
				if(StringUtil.isNotEmpty(filePath))
					zipFile(out, file, filePath + "/" + file.getName());
				else
					zipFile(out, file, file.getName());
				continue;
			}
			FileInputStream in = null;
			try{
				in = new FileInputStream(file);
				ZipArchiveEntry entry = null;
				if(StringUtil.isNotEmpty(filePath)){
					entry = new ZipArchiveEntry(filePath + "/" + file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				}else{
					entry = new ZipArchiveEntry(file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				}
				int count;
				byte data[] = new byte[BUFFER];
				while((count = in.read(data, 0, BUFFER)) != -1){
					out.write(data, 0, count);
				}
				out.closeArchiveEntry();
			}finally{
				IOUtils.closeQuietly(in);
			}
		}
	}

	/**
	 * 解压GZIP文件到当前目录
	 * 
	 * @param filePath
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unGZipFile(String filePath, boolean remainSrc){
		File file = new File(filePath);
		if(!file.exists()){
			log.warn("被解压GZIP操作的文件不存在:" + file.getAbsolutePath());
			return false;
		}
		return unGZipFile(file,remainSrc);
	}
	
	/**
	 * 解压TAR.GZ文件到当前目录
	 * 
	 * @param filePath
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unTGZipFile(String filePath, boolean remainSrc){
		File file = new File(filePath);
		if(!file.exists()){
			log.warn("被解压TGZIP操作的文件不存在:" + file.getAbsolutePath());
			return false;
		}
		return unTGZipFile(file,remainSrc);
	}

	/**
	 * 解压GZIP文件到当前目录
	 * 
	 * @param gZipFile
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unGZipFile(File gZipFile, boolean remainSrc){
		String srcName = gZipFile.getName();
		if(!srcName.toUpperCase().endsWith(".GZ")){
			log.warn("被解压GZIP操作不是GZ文件:" + gZipFile.getAbsolutePath());
			return false;
		}
		String fileName = srcName.substring(0, srcName.length() - ".GZ".length());
		File unGZipFile = new File(gZipFile.getParent(), fileName);
		GZIPInputStream gzipInputStream = null;
		FileOutputStream out = null;
		try{
			if(!unGZipFile.exists()){
				unGZipFile.createNewFile();
			}
			out = new FileOutputStream(unGZipFile, true);
			gzipInputStream = new GZIPInputStream(new FileInputStream(gZipFile));
			int buffSize = 1024*8;
			byte [] bytes = new byte[buffSize];
			int c;
			while((c = gzipInputStream.read(bytes)) != -1){
				out.write(bytes, 0, c);
			}
			
		}catch(IOException e){
			log.error("解压GZ文件:" + gZipFile.getAbsolutePath() + "出错", e);
			return false;
		}finally{
			IoUtil.closeQuietly(out);
			IoUtil.closeQuietly(gzipInputStream);
		}
		if(!remainSrc){
			gZipFile.delete();
		}
		return true;
	}
	
	/**
	 * 解压TGZIP文件到当前目录
	 * 
	 * @param gZipFile
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unTGZipFile(File gZipFile, boolean remainSrc) {
		String srcName = gZipFile.getName();
		if(!srcName.toUpperCase().endsWith("TAR.GZ")){
			log.warn("被解压GZIP操作不是TAR.GZ文件:" + gZipFile.getAbsolutePath());
			return false;
		}
		log.warn("解压GZIP:" + gZipFile.getAbsolutePath());
		FileInputStream fis = null;
		ArchiveInputStream in = null;
		BufferedInputStream bufferedInputStream = null;
		BufferedOutputStream bufferedOutputStream = null;
		String outputDirectory = gZipFile.getParent();
		try {
			fis = new FileInputStream(gZipFile);
			GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(fis));
			ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
			//archiveStreamFactory.setEntryEncoding("GBK");
			in = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.TAR, is);
			bufferedInputStream = new BufferedInputStream(in);
			TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				if(name.equals("./")){
					entry = (TarArchiveEntry) in.getNextEntry();
					continue;
				}
				try {
					String[] names = name.split("/");
					String fileName = outputDirectory;
					for (int i = 0; i < names.length; i++) {
						String str = names[i];
						fileName = fileName + File.separator + str;
					}
					FileUtil.createFile(fileName);
					File file = new File(fileName);
					bufferedOutputStream = new BufferedOutputStream(
							new FileOutputStream(file));
					int b;
					while ((b = bufferedInputStream.read()) != -1) {
						bufferedOutputStream.write(b);
					}
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
					entry = (TarArchiveEntry) in.getNextEntry();
				} catch (Exception e) {
					log.warn("解压"+srcName+"中出现异常文件，名称为："+name, e);
					entry = (TarArchiveEntry) in.getNextEntry();
				}
			}
		} catch (Exception e) {
			log.error("解压TAR.GZ文件:" + gZipFile.getAbsolutePath() + "出错", e);
			return false;
		} finally {
			IoUtil.closeQuietly(bufferedOutputStream);
			IoUtil.closeQuietly(in);
			IoUtil.closeQuietly(bufferedInputStream);
			IoUtil.closeQuietly(fis);
		}
		if(!remainSrc){
			gZipFile.delete();
		}
		return true;
	}
	
	/**
	 * 解压ZIP文件到当前目录
	 * 
	 * @param filePath
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unZipFile(String filePath, boolean remainSrc){
		File file = new File(filePath);
		if(!file.exists()){
			log.warn("被解压ZIP操作的文件不存在:" + file.getAbsolutePath());
			return false;
		}
		return unZipFile(file,remainSrc);
	}
	
	/**
	 * 解压GZIP文件到当前目录
	 * 
	 * @param zipFile
	 * @param remainSrc 是否保留SRC文件
	 * @return
	 */
	public static boolean unZipFile(File zipFile, boolean remainSrc){
		String srcName = zipFile.getName();
		if(!srcName.toUpperCase().endsWith(".ZIP")){
			log.warn("被解压ZIP操作不是ZIP文件:" + zipFile.getAbsolutePath());
			return false;
		}
		ZipInputStream zipInputStream = null;
		FileOutputStream out = null;
		try{
			zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry zipEntry;
			while((zipEntry = zipInputStream.getNextEntry()) != null){
				String fileName = zipEntry.getName();
				File unZipFile = new File(zipFile.getParent(), fileName);
				if(!unZipFile.exists()){
					unZipFile.createNewFile();
				}
				out = new FileOutputStream(unZipFile, true);
				int buffSize = 1024;
				byte [] bytes = new byte[buffSize];
				int c;
				while((c = zipInputStream.read(bytes)) != -1){
					out.write(bytes, 0, c);
				}
				IoUtil.closeQuietly(out);
			}
		}catch(IOException e){
			log.error("解压ZIP文件:" + zipFile.getAbsolutePath() + "出错", e);
			return false;
		}finally{
			IoUtil.closeQuietly(out);
			IoUtil.closeQuietly(zipInputStream);
		}
		if(!remainSrc){
			zipFile.delete();
		}
		return true;
	}

	public static void main(String [] args){
		unTGZipFile("D:/HW_PM_20150810.1445+0800-1500+0800_G0205.tar.gz",true);
	}
}
