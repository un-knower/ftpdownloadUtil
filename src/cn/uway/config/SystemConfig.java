package cn.uway.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import cn.uway.task.Task;

/**
 * 系统配置类
 * <p>
 * 对应 config.xml 文件
 * </p>
 * 
 * @author yuy
 * @since 1.0
 */
public class SystemConfig {

	// 以键值对方式读写xml文件的工具类
	private PropertiesXML propertiesXML;

	private static String systemFile = "./conf" + "/" + "config.xml";

	private static final Logger logger = LogMgr.getInstance().getSystemLogger();

	private static SystemConfig instance = null;

	private ConfigReader configReader;

	private ConfigWirtter configWritter;

	private static Map<Class<?>, String> tagClassMapping;

	private SystemConfig() throws Exception {
		propertiesXML = new PropertiesXML(systemFile);
		configReader = new XMLConfigReader();
		configWritter = new XMLConfigWritter();
	}

	/**
	 * 获取实例
	 * 
	 * @return
	 */
	public static SystemConfig getInstance() {
		if (instance == null) {
			try {
				instance = new SystemConfig();
			} catch (Exception e) {
				logger.error("创建SystemConfig对象时出现异常", e);
				return null;
			}
		}
		return instance;
	}

	// ip
	public String getFtpIp() {
		return propertiesXML.getProperty("config.ftp.ip");
	}

	// port
	public String getFtpPort() {
		return propertiesXML.getProperty("config.ftp.port");
	}

	// 用户名
	public String getFtpUserName() {
		return propertiesXML.getProperty("config.ftp.username");
	}

	// 密码
	public String getFtpPassword() {
		return propertiesXML.getProperty("config.ftp.password");
	}

	// localPath
	public String getLocalPath() {
		return propertiesXML.getProperty("config.ftp.localPath");
	}

	// transMode
	public String getTransMode() {
		return propertiesXML.getProperty("config.ftp.transMode");
	}

	// charset
	public String getCharset() {
		return propertiesXML.getProperty("config.ftp.charset");
	}

	// bufferSize
	public String getBufferSize() {
		return propertiesXML.getProperty("config.ftp.bufferSize");
	}

	// dataTimeout
	public String getDataTimeout() {
		return propertiesXML.getProperty("config.ftp.dataTimeout");
	}

	// loginTryTimes
	public String getLoginTryTimes() {
		return propertiesXML.getProperty("config.ftp.loginTryTimes");
	}

	// loginTryDelay
	public String getLoginTryDelay() {
		return propertiesXML.getProperty("config.ftp.loginTryDelay");
	}

	// listTryTimes
	public String getListTryTimes() {
		return propertiesXML.getProperty("config.ftp.listTryTimes");
	}

	// bufferSize
	public String getListTryDelay() {
		return propertiesXML.getProperty("config.ftp.listTryDelay");
	}

	// downloadTryTimes
	public String getDownloadTryTimes() {
		return propertiesXML.getProperty("config.ftp.downloadTryTimes");
	}

	// downloadTryDelay
	public String getDownloadTryDelay() {
		return propertiesXML.getProperty("config.ftp.downloadTryDelay");
	}

	// retentionTime
	public String getRetentionTime() {
		return propertiesXML.getProperty("config.ftp.retentionTime");
	}

	// retentionTime
	public Boolean getNeedToReadContent() {
		String needToReadContent = propertiesXML.getProperty("config.ftp.needToReadContent");
		if ("true".equals(needToReadContent))
			return true;
		return false;
	}

	// taskName
	public String getTaskName() {
		return propertiesXML.getProperty("config.task.taskName");
	}

	// startTime
	public String getStartTime() {
		return propertiesXML.getProperty("config.task.startTime");
	}

	// endTime
	public String getEndTime() {
		return propertiesXML.getProperty("config.task.endTime");
	}

	// period
	public String getPeriod() {
		return propertiesXML.getProperty("config.task.period");
	}

	// gatherPath
	public String getGatherPath() {
		return propertiesXML.getProperty("config.task.gatherPath");
	}

	// fileGeneratePeriod
	public String getFileGeneratePeriod() {
		return propertiesXML.getProperty("config.task.fileGeneratePeriod");
	}

	// downLoadJobNum
	public String getDownLoadJobNum() {
		return propertiesXML.getProperty("config.task.downLoadJobNum");
	}

	// compressToPath
	public String getCompressToPath() {
		return propertiesXML.getProperty("config.task.compressToPath");
	}

	// compressPattern
	public String getCompressPattern() {
		return propertiesXML.getProperty("config.task.compressPattern");
	}

	// 获取winrar安装目录，默认值为空字符串
	public String getWinrarPath() {
		return propertiesXML.getProperty("config.task.zipTool");
	}

	// 目录下未扫描到文件后重试扫描次数，默认值为0
	public int getListFileCnt() {
		String val = propertiesXML.getProperty("config.task.retryListFileCnt");
		if (val == null || "".equals(val)) {
			return 0;
		}
		try {
			return Integer.valueOf(val);
		} catch (Exception e) {
			return 0;
		}
	}

	// 是否需要对下载的文件进行解压操作
	public boolean getNeedDecompress() {
		String needDecompress = propertiesXML.getProperty("config.task.needDecompress");
		if ("true".equalsIgnoreCase(needDecompress))
			return true;
		return false;
	}

	// 是否需要对下载的文件进行打包操作
	public boolean getNeedCompress() {
		String needCompress = propertiesXML.getProperty("config.task.needCompress");
		if ("false".equalsIgnoreCase(needCompress))
			return false;
		return true;
	}

	/**
	 * @return the tagClassMapping
	 */
	public Map<Class<?>, String> getTagClassMapping() {
		return tagClassMapping;
	}

	/**
	 * @param tagClassMapping
	 *            the tagClassMapping to set
	 */
	public void setTagClassMapping(Map<Class<?>, String> tagClassMapping) {
		SystemConfig.tagClassMapping = tagClassMapping;
	}

	/**
	 * 获取配置文件中所有配置的Bean对象Map
	 * 
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public synchronized <T> List<T> getBeans(Class<T> clazz) throws Exception {
		List<ConfigBean> beanList = configReader.readConfigBean(systemFile);
		if (beanList == null) {
			return null;
		}
		String beanName = clazz.getSimpleName().toUpperCase();
		if (tagClassMapping != null) {
			String alias = tagClassMapping.get(clazz);
			if (alias != null && !alias.trim().isEmpty()) {
				beanName = alias.toUpperCase();
			}
		}
		List<T> parseBeanList = new ArrayList<T>();
		for (ConfigBean bean : beanList) {
			if (beanName.equals(bean.getName())) {
				try {
					T parseBean = ConfigBeanFactory.parseBean(clazz, bean);
					parseBeanList.add(parseBean);
				} catch (Exception e) {
					logger.error("获取Bean列表异常,ClassName:" + clazz.getName() + ",bean[name:" + bean.getName() + ",values:" + bean.getProperties(), e);
				}
			}
		}
		return parseBeanList;
	}

	/**
	 * @return the systemFile
	 */
	public static String getSystemFile() {
		return systemFile;
	}

	/**
	 * @param systemFile
	 *            the systemFile to set
	 */
	public static void setSystemFile(String systemFile) {
		SystemConfig.systemFile = systemFile;
	}

	public void writterConfig(Task task) {
		ConfigBean bean;
		try {
			bean = ConfigBeanFactory.objectToConfigBean(task);
			configWritter.writerConfigBean(bean, systemFile);
		} catch (Exception e) {
			logger.error("更新任务信息失败", e);
		}
	}

	// public static void main(String [] args){
	// Map<Class<?>,String> tagClassMapping = new HashMap<Class<?>,String>();
	// tagClassMapping.put(FtpInfo.class, "FTP");
	// tagClassMapping.put(Task.class, "TASK");
	// SystemConfig.getInstance().setTagClassMapping(tagClassMapping);
	//
	// List<Task> beans = SystemConfig.getInstance().getBeans(Task.class);
	// for(Task t : beans){
	// System.out.println(t.getTaskName());
	// System.out.println(t.getGatherPath());
	// }
	//
	// List<FtpInfo> bean2 = SystemConfig.getInstance().getBeans(FtpInfo.class);
	// for(FtpInfo t : bean2){
	// System.out.println(t.getIp());
	// System.out.println(t.getId());
	// }
	// }
}
