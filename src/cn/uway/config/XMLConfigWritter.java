package cn.uway.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;

import cn.uway.task.Task;

public class XMLConfigWritter implements ConfigWirtter{

	private static final Logger logger = LogMgr.getInstance().getSystemLogger();

	public XMLConfigWritter(){

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.uway.config.ConfigWirtter#writerConfigBean(cn.uway.config.ConfigBean,
	 * java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean writerConfigBean(ConfigBean bean, String filePath) throws Exception{
		Document doc = loadXML(filePath);
		Element beanElement = findElement(doc, bean);
		List<Attribute> attrList = beanElement.attributes();
		for(Attribute attr : attrList){
			String newValue = bean.getProperty(attr.getName());
			attr.setValue(newValue);
		}
		//读取bean节点的子节点值，如果子节点中有名称与bean节点属性相同，则覆盖属性的值
		for(Entry<String,String> attr : bean.getProperties().entrySet()){
			String newValue = attr.getValue();
			Element element = beanElement.element(attr.getKey());
			element.setText(newValue);
		}
		doc2XmlFile(doc, SystemConfig.getSystemFile());
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.uway.config.ConfigWirtter#writerConfigBean(cn.uway.config.ConfigBean,
	 * java.io.File)
	 */
	@Override
	public boolean writerConfigBean(ConfigBean bean, File file) throws Exception{
		// TODO Auto-generated method stub
		return false;
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


	@SuppressWarnings("unchecked")
	private Element findElement(Document document, ConfigBean bean) throws Exception{
		Element rootElement = document.getRootElement();
		if(rootElement == null || !rootElement.getName().equalsIgnoreCase("config")){
			throw new SystemConfigException("配置文件根节点不是config!");
		}
		List<Element> elements = (List<Element>)rootElement.elements();
		for(Element target : elements){
			ConfigBean configBean = readBean(target);
			if(!configBean.getName().equalsIgnoreCase(bean.getName())){
				continue;
			}
			if(bean.getProperty("taskName").equalsIgnoreCase(configBean.getProperty("taskName"))){
				return target;
			}
		}
		return null;
	}

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

	public synchronized static boolean doc2XmlFile(Document document, String filename){
		boolean flag = true;
		try{
			XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
			writer.write(document);
			writer.close();
		}catch(Exception ex){
			flag = false;
			ex.printStackTrace();
		}
		return flag;
	}

	public static void main(String [] args) throws Exception{
		List<Task> tasks = SystemConfig.getInstance().getBeans(Task.class);
		Task hw = tasks.get(0);
		hw.setExecTime(new Date());
		ConfigBean bean = ConfigBeanFactory.objectToConfigBean(hw);
		XMLConfigWritter w = new XMLConfigWritter();
		w.writerConfigBean(bean, SystemConfig.getSystemFile());
		System.out.println(bean.getName());
	}
}
