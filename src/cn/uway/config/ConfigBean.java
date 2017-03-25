package cn.uway.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置文件中的原始bean对象，保存配置bean的原始格式和内容<br>
 * 程序需要将ConfigBean转换为其他对应的实例对象
 * 
 * @ClassName: ConfigBean
 * @author Niow
 * @date: 2014-6-19
 */
public class ConfigBean{

	private String name;

	private Map<String,String> properties;

	public void addProperty(String key, String value){
		if(properties == null){
			properties = new HashMap<String,String>();
		}
		properties.put(key, value);
	}

	public String getProperty(String key){
		return properties.get(key);
	}

	/**
	 * @return the name
	 */
	protected String getName(){
		return name;
	}

	/**
	 * @param name the name to set
	 */
	protected void setName(String name){
		this.name = name;
	}

	/**
	 * @return the properties
	 */
	protected Map<String,String> getProperties(){
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	protected void setProperties(Map<String,String> properties){
		this.properties = properties;
	}

}
