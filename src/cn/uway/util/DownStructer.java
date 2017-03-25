package cn.uway.util;

import java.util.ArrayList;
import java.util.List;

public class DownStructer {

	private List<String> sucLocalFiles;

	private List<String> suc;

	private List<String> fail;

	private List<String> localFail;

	private int hasDownCount;

	private int notHasDownButFailTimes;

	private int hasDownTimes;

	public DownStructer() {
		sucLocalFiles = new ArrayList<String>();
		suc = new ArrayList<String>();
		fail = new ArrayList<String>();
		localFail = new ArrayList<String>();
	}

	public List<String> getSucLocalFiles() {
		return sucLocalFiles;
	}

	public void setSucLocalFiles(List<String> sucLocalFiles) {
		this.sucLocalFiles = sucLocalFiles;
	}

	public List<String> getSuc() {
		return suc;
	}

	public void setSuc(List<String> suc) {
		this.suc = suc;
	}

	public List<String> getFail() {
		return fail;
	}

	public void setFail(List<String> fail) {
		this.fail = fail;
	}

	public List<String> getLocalFail() {
		return localFail;
	}

	public void setLocalFail(List<String> localFail) {
		this.localFail = localFail;
	}

	public int getHasDownCount() {
		return hasDownCount;
	}

	public void setHasDownCount(int hasDownCount) {
		this.hasDownCount = hasDownCount;
	}

	public int getNotHasDownButFailTimes() {
		return notHasDownButFailTimes;
	}

	public void setNotHasDownButFailTimes(int notHasDownButFailTimes) {
		this.notHasDownButFailTimes = notHasDownButFailTimes;
	}

	public int getHasDownTimes() {
		return hasDownTimes;
	}

	public void setHasDownTimes(int hasDownTimes) {
		this.hasDownTimes = hasDownTimes;
	}

}
