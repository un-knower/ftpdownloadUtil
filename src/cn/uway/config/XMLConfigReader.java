package cn.uway.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * XML配置文件读取器实现类
 * 
 * @ClassName: XMLConfigReader
 * @author Niow
 * @date: 2014-6-18
 */
public class XMLConfigReader implements ConfigReader{

	private static final Logger logger = LogMgr.getInstance().getSystemLogger();

	public XMLConfigReader(){

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.uway.config.ConfigReader#readBeanToMap(java.io.File)
	 */
	@Override
	public List<ConfigBean> readConfigBean(File file) throws Exception{
		// 存放要进行操作的xml文档对象
		Document document = loadXML(file);
		return readAllBeans(document);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.uway.config.ConfigReader#readBeanToMap(java.lang.String)
	 */
	@Override
	public List<ConfigBean> readConfigBean(String filePath) throws Exception{
		// 存放要进行操作的xml文档对象
		Document document = loadXML(filePath);
		return readAllBeans(document);
	}

	/**
	 * 从配置文件中把所有的Bean读取出来，只有根节点下的一级子节点才会作为bean节点
	 * 
	 * @param document 配置文件
	 * @return
	 * @throws SystemConfigException
	 */
	@SuppressWarnings("unchecked")
	private List<ConfigBean> readAllBeans(Document document) throws Exception{
		Element rootElement = document.getRootElement();
		if(rootElement == null || !rootElement.getName().equalsIgnoreCase("config")){
			throw new SystemConfigException("配置文件根节点不是config!");
		}
		List<Element> elements = (List<Element>)rootElement.elements();
		List<ConfigBean> beanMapList = new ArrayList<ConfigBean>();
		for(Element bean : elements){
			ConfigBean configBean = readBean(bean);
			beanMapList.add(configBean);
		}
		return beanMapList;
	}

	/**
	 * 从一个完整的bean元素中读取一个ConfigBean
	 * 
	 * @param beanElement 文件中的根元素的一级子元素
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ConfigBean readBean(Element beanElement){
		ConfigBean configBean = new ConfigBean();
		//Bean节点的标签名称读取到程序中都以大写存储
		configBean.setName(beanElement.getName().toUpperCase());
		//读取bean节点属性值
		List<Attribute> attrList = beanElement.attributes();
		for(Attribute attr : attrList){
			configBean.addProperty(attr.getName(), attr.getValue());
		}
		//读取bean节点的子节点值，如果子节点中有名称与bean节点属性相同，则覆盖属性的值
		List<Element> propertyElements = beanElement.elements();
		for(Element property : propertyElements){
			configBean.addProperty(property.getName(), property.getStringValue());
		}
		return configBean;
	}

	/**
	 * 通过文件路径加载xml文件
	 * 
	 * @param xmlLocation 文件路径
	 * @return
	 * @throws SystemConfigException 文件未找到，或者无权限
	 */
	private Document loadXML(String xmlLocation) throws SystemConfigException{
		SAXReader reader = new SAXReader();
		try{
			Document document = reader.read(new FileInputStream(xmlLocation));
			return document;
		}catch(Exception e){
			logger.error(e.getMessage());
			throw new SystemConfigException("载入xml文件时发生异常", e);
		}
	}

	/**
	 * 通过文件对象加载xml文件
	 * 
	 * @throws SystemConfigException 文件未找到，或者无权限
	 */
	private Document loadXML(File file) throws SystemConfigException{
		SAXReader reader = new SAXReader();
		try{
			Document document = reader.read(file);
			return document;
		}catch(Exception e){
			logger.error(e.getMessage());
			throw new SystemConfigException("载入xml文件时发生异常:" + file.getName(), e);
		}
	}
}
