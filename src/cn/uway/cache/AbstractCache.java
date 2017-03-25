package cn.uway.cache;

public abstract class AbstractCache {

	public void add(String filePath) {
	}

	/**
	 * compute the CRC-32 of a string
	 * 
	 * @param str
	 * @return
	 */
	public static long crc32(String str) {
		java.util.zip.CRC32 x = new java.util.zip.CRC32();
		x.update(str.getBytes());
		return x.getValue();
	}

}
