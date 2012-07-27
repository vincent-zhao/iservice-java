package demo.demo2;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import org.apache.zookeeper.ZooKeeper;

import src.Constants;

public class ChangeZkTask2 extends TimerTask{

	private ZooKeeper zk;
	private int count;
	
	public ChangeZkTask2(String host, String root) throws IOException{
		this.zk = new ZooKeeper(host + root, Constants.SESSION_TIMEOUT, null);
		this.count = 0;
	}
	
	@Override
	public void run() {
		Date currentTime = new Date();   
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		String dateString = formatter.format(currentTime); 
		
		System.out.println(dateString + " change data");
		try {
			this.zk.setData("/IserviceDemoTest/group1/app2/key1", ("i'm in group1/app2, i'm key1 version" + count).getBytes(), -1);
			this.zk.setData("/IserviceDemoTest/group1", ("i'm in group1, i'm app2" + count).getBytes(), -1);
			count++;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
