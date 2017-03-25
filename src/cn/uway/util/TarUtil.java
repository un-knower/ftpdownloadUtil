package cn.uway.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import cn.uway.config.LogMgr;

public class TarUtil {

	private static final Logger log = LogMgr.getInstance().getSystemLogger();

	public static int BUFFER = 1024;

	public static boolean tarDir(File dir, File target, Set<String> regexSet, boolean needTopDir) {
		FileOutputStream f = null;
		TarArchiveOutputStream out = null;
		try {
			if (!target.exists())
				FileUtil.createFile(target);
			f = new FileOutputStream(target);
			out = new TarArchiveOutputStream(f);
			String filename = needTopDir ? dir.getName() : "";
			if (regexSet == null) {
				tarFile(out, dir, filename);
			} else {
				tarFile(out, regexSet, filename);
			}
			return true;
		} catch (IOException e) {
			log.error("压缩文件失败，dir=" + dir + "，target=" + target, e);
			return false;
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(f);
		}
	}

	private static void tarFile(TarArchiveOutputStream out, File srcDir, String filePath) throws IOException {
		for (File file : srcDir.listFiles()) {
			if (file.isDirectory()) {
				if (StringUtil.isNotEmpty(filePath))
					tarFile(out, file, filePath + "/" + file.getName());
				else
					tarFile(out, file, file.getName());
				continue;
			}
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				TarArchiveEntry entry = null;
				if (StringUtil.isNotEmpty(filePath)) {
					entry = new TarArchiveEntry(filePath + "/" + file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				} else {
					entry = new TarArchiveEntry(file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				}
				int count;
				byte data[] = new byte[BUFFER];
				while ((count = in.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				out.closeArchiveEntry();
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}

	private static void tarFile(TarArchiveOutputStream out, Set<String> regexSet, String filePath) throws IOException {
		for (String filepath : regexSet) {
			File file = new File(filepath);
			if (file.isDirectory()) {
				if (StringUtil.isNotEmpty(filePath))
					tarFile(out, regexSet, filePath + "/" + file.getName());
				else
					tarFile(out, regexSet, file.getName());
				continue;
			}
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				TarArchiveEntry entry = null;
				if (StringUtil.isNotEmpty(filePath)) {
					entry = new TarArchiveEntry(filePath + "/" + file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				} else {
					entry = new TarArchiveEntry(file.getName());
					entry.setSize(file.length());
					out.putArchiveEntry(entry);
				}
				int count;
				byte data[] = new byte[BUFFER];
				while ((count = in.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				out.closeArchiveEntry();
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}

	public static void depressGZ(String toPath, GZIPInputStream gzIn) {

	}

	public static void main(String[] args) {
		tarDir(new File("D:\\2"), new File("D:\\xx.tar"), null, false);
	}
}
