package cn.uway;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import cn.uway.cleaner.FileCleaner;
import cn.uway.config.LogMgr;
import cn.uway.config.SystemConfig;
import cn.uway.task.FtpInfo;
import cn.uway.task.Task;
import cn.uway.task.TaskLoader;
import cn.uway.task.TaskLoaderMulti;
import cn.uway.task.TaskTrigger;


/**
 * FTP小工具启动类
 * @author 
 * @ 
 */
public class FTPRunner {

	private static final Logger LOGGER = LogMgr.getInstance().getSystemLogger();
	
	/*
	 * 
	 * @version 1.1.0.0 日志乱码问题已解决
	 * @version 1.2.0.0 实现多任务配置和多ftp配置
	 * 1、数据补采 {基于1.2.0多任务配置后，可以在配置文件中新添加一个补采任务，限定起始和结束时间} 
	 * 2、新增文件路径格式时间通配符 {在路径中增加时间通配格式{d yyyyMMdd HHmm}} 
	 * 3、自动删除过期文件未实现 
	 * 4、实现多线程采集
	 * @version 1.3.0.0 代码整体重构，任务机制增加异步和同步处理
	 * @version 1.3.4.0 增加异步任务，增加压缩temp文件
	 * @version 1.3.5.0 修复异步任务下载阻塞问题，增加对爱立信厂家这类FTP不支持*号路径list的处理
	 * @version 1.3.6.0 增加FTP连接池，修改同步任务逻辑，增加自动识别FTP编码逻辑
	 * @version 1.3.7.0 调整采集延迟逻辑
	 * @version 1.3.8.0 修复FTP链接为归还bug，增加数据完整性检测
	 * @version 1.3.9.0 增加FTP断链重连
	 * @version 1.4.1.0 增加时区配置
	 * @version 1.4.4.0 修改任务卡死、目录扫描不到导致的数据丢失和单路径下的多个Job启动的问题
	 * @version 1.4.5.0 上海lte华为性能下载解压失败，导致数据缺失问题
	 * @version 1.4.5.1 修改sftp中长时间连接后再cd时会导致输入流关闭，现在改为重新登录一次
	 * @version 1.4.5.2 修改list ftp连接文件的时候，是软硬连接不是文件类型的判断
	 */
	private static final String version="1.4.5.2";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<Class<?>, String> tagClassMapping = new HashMap<Class<?>, String>();
		tagClassMapping.put(FtpInfo.class, "FTP");
		tagClassMapping.put(Task.class, "TASK");
		tagClassMapping.put(TaskLoaderMulti.class, "TASKLOADER");
		SystemConfig.setSystemFile("./conf/config.xml");
		SystemConfig.getInstance().setTagClassMapping(tagClassMapping);
		LOGGER.debug("程序开始运行...");
		LOGGER.debug("Version_v"+version);
		TaskLoader loader = null;
		try {
			loader = SystemConfig.getInstance().getBeans(TaskLoaderMulti.class).get(0);
		} catch (Exception e) {
			LOGGER.debug("创建任务加载器失败", e);
			System.exit(0);
		}
		new TaskTrigger(loader).start();
		new FileCleaner().start();
	}
}
