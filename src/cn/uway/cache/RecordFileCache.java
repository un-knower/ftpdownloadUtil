package cn.uway.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import cn.uway.config.LogMgr;
import cn.uway.file.FileGenerator;
import cn.uway.task.Task;
import cn.uway.util.TimeUtil;

public class RecordFileCache extends AbstractCache{

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();

	private static final String path = "./cache";

	// dataTime的日志，正在记录所有文件
	private static final String tmpSmallExdName = ".log.tmp";

	// 每天的日志记录，表示正在生成
	private static final String tmpExdName = ".tmp";

	public FileGenerator fileWriter;

	public void clear(){
		FileWriter fw = null;
		try{
			fw = new FileWriter(fileWriter.file);
			fw.write("");
		}catch(IOException e){
			LOGGER.error("清理缓存文件失败",e);
		}finally{
			IOUtils.closeQuietly(fw);
		}
		
	}

	public RecordFileCache(Task task){
		String runningDay = TimeUtil.getDateString_yyyyMMdd(task.getExecTime());
		String lastRuntime = TimeUtil.getDateString_yyyyMMddHHmm(task.getExecTime());
		String filePath = path + "/" + task.getTaskName() + "/" + runningDay + tmpExdName;
		String fileName = lastRuntime + tmpSmallExdName;
		fileWriter = new FileGenerator(filePath, fileName);
	}

	public List<String> getAllRecords(){
		List<String> records = new LinkedList<String>();
		File file = fileWriter.file;
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = reader.readLine()) != null){
				records.add(line);
			}
		}catch(IOException e){
			LOGGER.error("读取缓存文件出错",e);
		}
		return records;
	}

	// 增加一条文件记录
	public synchronized void add(String filename){
		fileWriter.write((filename + "\n").getBytes());
	}

	public synchronized void addByBatch(Collection<String> list){
		if(list == null || list.size() == 0)
			return;
		for(String str : list){
			fileWriter.write((str + "\n").getBytes());
		}
	}

	public synchronized void close(){
		try{
			fileWriter.close();
		}catch(IOException e){}
	}
}
