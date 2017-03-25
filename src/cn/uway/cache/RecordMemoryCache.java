package cn.uway.cache;

import java.util.Collection;
import java.util.Set;

import org.apache.mina.util.ConcurrentHashSet;

import cn.uway.task.Task;

public class RecordMemoryCache{

	private Set<String> hasDoneFileSet = new ConcurrentHashSet<String>();


	public RecordMemoryCache(){

	}

	public RecordMemoryCache(Task task){
	}

	public void addRecord(String content){
		hasDoneFileSet.add(content);
	}

	public void addBatchRecord(Collection<String> content){
		if(content == null || content.isEmpty()){
			return;
		}
		hasDoneFileSet.addAll(content);
	}

	public void clear(){
		hasDoneFileSet.clear();
	}

	public void remove(String content){
		hasDoneFileSet.remove(content);
	}

	public void removeAll(Collection<String> content){
		hasDoneFileSet.removeAll(content);
	}

	public boolean contains(String fileFullPath){
		boolean contains = hasDoneFileSet.contains(fileFullPath);
		return contains;
	}

	/**
	 * @return the hasDoneFileSet
	 */
	public Set<String> getHasDoneFileSet(){
		return hasDoneFileSet;
	}

	/**
	 * @param hasDoneFileSet the hasDoneFileSet to set
	 */
	public void setHasDoneFileSet(Set<String> hasDoneFileSet){
		this.hasDoneFileSet = hasDoneFileSet;
	}

	//	public static void main(String [] args) throws InterruptedException{
	//		final RecordMemoryCache cache = new RecordMemoryCache();
	//		Thread [] ths = new Thread[3];
	//		ths[0] = new Thread("T" + 0){
	//			public void run(){
	//				for(int j = 0; j < 10; j++){
	//					cache.addRecord(j + "-" + j);
	//					try{
	//						Thread.sleep(Math.round(Math.random() * 100));
	//					}catch(InterruptedException e){
	//						e.printStackTrace();
	//					}
	//				}
	//			};
	//		};
	//		ths[1] = new Thread("T" + 1){
	//			public void run(){
	//				for(int j = 10; j < 20; j++){
	//					cache.addRecord(j + "-" + j);
	//					try{
	//						Thread.sleep(Math.round(Math.random() * 100));
	//					}catch(InterruptedException e){
	//						e.printStackTrace();
	//					}
	//				}
	//			};
	//		};
	//		ths[2] = new Thread("T" + 0){
	//			public void run(){
	//				for(int j = 20; j < 30; j++){
	//					cache.addRecord(j + "-" + j);
	//					try{
	//						Thread.sleep(Math.round(Math.random() * 100));
	//					}catch(InterruptedException e){
	//						e.printStackTrace();
	//					}
	//				}
	//			};
	//		};
	//		for(int i = 0; i < ths.length; i++){
	//			ths[i].start();
	//		}
	//		for(int i = 0; i < ths.length; i++){
	//			ths[i].join();
	//		}
	//		for(String a : cache.hasDoneFileSet){
	//			System.out.println(a);
	//		}
	//	}
}
