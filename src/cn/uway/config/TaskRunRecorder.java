package cn.uway.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class TaskRunRecorder{

	private static final String cachePath="."+File.separator+"cache";
	
	public static final String recordFilePath = cachePath + File.separator + "taskPace.ini";

	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static final Logger logger = LogMgr.getInstance().getSystemLogger();

	private File iniFile;

	private static TaskRunRecorder instance;
	
	static{
		try {
			instance = new TaskRunRecorder();
		} catch (Exception e) {
			logger.error("创建任务记录器失败", e);
		}
	}
	
	private TaskRunRecorder() throws Exception{
		this.iniFile = new File(recordFilePath);
		File path=new File(cachePath);
		if(!path.exists()){
			path.mkdirs();
			iniFile.createNewFile();
		 }else if(!iniFile.exists()){
			iniFile.createNewFile();
		}
	}

	public static TaskRunRecorder getInstance(){
		if(instance == null){
			try{
				instance = new TaskRunRecorder();
			}catch(Exception e){
				logger.error("创建任务记录器失败", e);
				return null;
			}
		}
		return instance;
	}

	public String getValue(String key){
		Properties propertise = new Properties();
		synchronized(iniFile){
			FileInputStream in = null;
			try{
				in = new FileInputStream(iniFile);
				propertise.load(in);
			}catch(Exception e){
				logger.error("获取任务运行记录失败", e);
			}finally{
				IOUtils.closeQuietly(in);
			}
		}
		return propertise.getProperty(key);
	}

	public synchronized Date getDate(String key){
		String date = getValue(key);
		if(date != null && !date.trim().equals("")){
			try{
				return timeFormat.parse(date);
			}catch(ParseException e){
				logger.error("时间格式不符合yyyy-MM-dd HH:mm:ss:[" + key + "--" + date, e);
			}
		}
		return null;
	}

	public void setValue(String key, String value){
		synchronized(iniFile){
			Properties propertise = new Properties();
			FileInputStream in = null;
			FileOutputStream out = null;
			try{
				in = new FileInputStream(iniFile);
				propertise.load(in);
				propertise.put(key, value);
				out = new FileOutputStream(iniFile);
				propertise.store(out, "");
			}catch(Exception e){
				logger.error("保存任务运行记录失败", e);
			}finally{
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		}
	}

	public synchronized void setDate(String key, Date value){
		if(value != null){
			String date = timeFormat.format(value);
			setValue(key, date);
		}
	}
}
