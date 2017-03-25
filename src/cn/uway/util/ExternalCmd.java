package cn.uway.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 外部命令 类
 * 
 * @author YangJian
 * @version 1.0
 */
public class ExternalCmd {

	public String _cmd;
	
	public long taskID;

	public ExternalCmd() {
		super();
	}

	public void setCmd(String cmd) {
		_cmd = cmd;
	}

	/** 执行命令 */
	public int execute(String cmd) throws Exception {
		_cmd = cmd;

		return execute();
	}

	/** 执行命令 */
	public int execute() throws Exception {
		if (Util.isNull(_cmd))
			return 0;

		int retCode = -1;

		Process proc = null;

		try {
			proc = Runtime.getRuntime().exec(_cmd);

			new StreamGobbler(proc.getErrorStream()).start();
			new StreamGobbler(proc.getInputStream()).start();
			
			proc.waitFor();
			
			retCode = proc.exitValue();
		} catch (Exception e) {
			throw e;
		} finally {
			if (proc != null)
				proc.destroy();
		}

		return retCode;
	}

	public class StreamGobbler extends Thread {

		InputStream is;

		public StreamGobbler(InputStream is) {
			this.is = is;
		}

		public void run() {
			BufferedReader br = null;

			try {
				br = new BufferedReader(new InputStreamReader(is));

				while (br.readLine() != null) {

				}
			} catch (Exception e) {
				// logger.error("StreamGobbler run() error.", e);
			} finally {
				try {
					if (br != null) {
						br.close();
					}
				} catch (Exception e) {
				}
			}
		}
	}

}
