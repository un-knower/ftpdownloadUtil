package cn.uway.config;

import java.io.File;

public interface ConfigWirtter{

	public boolean writerConfigBean(ConfigBean bean, String filePath) throws Exception;

	public boolean writerConfigBean(ConfigBean bean, File file) throws Exception;

}
