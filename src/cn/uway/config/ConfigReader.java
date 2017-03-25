package cn.uway.config;

import java.io.File;
import java.util.List;

/**
 * 配置文件读取器<br>
 * 主要是把配置文件中配置的bean对象读取为Map，提供对象转换器将map转换为实例对象。
 * 
 * 
 * @ClassName: ConfigReader
 * @author Niow
 * @date: 2014-6-18
 */
public interface ConfigReader{

	/**
	 * 把一个配置文件中配置的所有Bean读取到内存中<br>
	 * 
	 * @param file 文件对象
	 * @return 返回一个包含bean名称和属性的列表集合
	 */
	public List<ConfigBean> readConfigBean(File file) throws Exception;

	/**
	 * 把一个配置文件中配置的所有Bean读取到内存中<br>
	 * 
	 * @param filePath 文件路径，具体是相对路径还是绝对路径由实现类控制
	 * @return 返回一个包含bean名称和属性的列表集合
	 */
	public List<ConfigBean> readConfigBean(String filePath) throws Exception;
	

}
