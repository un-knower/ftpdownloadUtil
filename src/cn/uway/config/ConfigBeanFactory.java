package cn.uway.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Map.Entry;

import cn.uway.task.Task;

/**
 * ConfigBean对象转换为具体Bean对象的工厂
 * 
 * @ClassName: ConfigBeanFactory
 * @author Niow
 * @date: 2014-6-19
 */
public class ConfigBeanFactory{

	/** 配置文件中，如果配置了时间格式，则只支持yyyy-MM-dd hh:mm:ss格式 */
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	/**
	 * 通过反射，把ConfigBean转换成对应的实例对象<br>
	 * 其中需要反射的Class必须只能包含基本数据类型和java.util.Date类型的字段<br>
	 * 
	 * @param clazz 需要转换成的实例类型
	 * @param bean 从配置文件中读取出来的ConfigBean对象
	 * @return 转换后的clazz的实例对象
	 * @throws Exception 反射中遇到的异常
	 */
	public static <T>T parseBean(Class<T> clazz, ConfigBean bean) throws Exception{
		if(bean == null || clazz == null){
			return null;
		}
		T obj = clazz.newInstance();
		//如果class没有字段，则直接返回新建的实例
		if(!hasField(clazz)){
			return obj;
		}
		Map<String,String> properties = bean.getProperties();
		for(Entry<String,String> attr : properties.entrySet()){
			Field field = null;
			try{
				field = obj.getClass().getDeclaredField(attr.getKey());
				field.setAccessible(true);
			}catch(NoSuchFieldException e){
				continue;
			}
			Class<?> type = field.getType();
			Object value = getPrimitiveValue(type, attr.getValue());
			if(value == null){
				Constructor<?> constructor = type.getConstructor(new Class[]{String.class});
				value = constructor.newInstance(attr.getValue());
			}
			field.set(obj, value);
		}
		return obj;
	}

	/**
	 * 判断该类型是否有字段
	 * 
	 * @param clazz
	 * @return
	 */
	private static <T>boolean hasField(Class<T> clazz){
		return clazz.getDeclaredFields() != null && clazz.getDeclaredFields().length != 0;
	}

	/**
	 * 对基本数据类型和java.util.Date类型进行读取封装
	 * 
	 * @param type
	 * @param nodeValue
	 * @return
	 * @throws Exception
	 */
	private static Object getPrimitiveValue(Class<?> type, String nodeValue) throws Exception{
		Object value = null;
		if(type.isPrimitive()){
			if(type == Integer.TYPE){
				value = new Integer(nodeValue);
			}else if(type == Float.TYPE){
				value = new Float(nodeValue);
			}else if(type == Boolean.TYPE){
				value = new Boolean(nodeValue);
			}else if(type == Double.TYPE){
				value = new Integer(nodeValue);
			}else if(type == Long.TYPE){
				value = new Long(nodeValue);
			}else if(type == Short.TYPE){
				value = new Integer(nodeValue);
			}else if(type == Character.TYPE){
				value = new Character(nodeValue.toCharArray()[0]);
			}else if(type == Byte.TYPE){
				value = new Byte(nodeValue);
			}
		}else if(type == java.util.Date.class){
			value = timeFormat.parse(nodeValue);
		}
		return value;
	}

	public static ConfigBean objectToConfigBean(Object object) throws Exception{
		ConfigBean bean = new ConfigBean();
		Task task = (Task)object;
		bean.setName("task");
		String strTime = timeFormat.format(task.getExecTime());
		bean.addProperty("execTime", strTime);
		bean.addProperty("taskName", task.getTaskName());
		return bean;
	}

}
