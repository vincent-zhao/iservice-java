package demo.demo1;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;

import src.Config;
import src.Constants;
import client.IService;
import data.DefaultData;
import demo.ConnectHandler;
import demo.DataChangeHandler;
import demo.DisconnectHandler;
import demo.ExpiredHandler;
import demo.conf.DemoConf;

public class Main {
	
	public static void main(String args[]) throws Exception{
		Main m = new Main();
		m.initZk();
		
		IService client = new IService(DemoConf.host, DemoConf.root, "", "", DemoConf.tmpPath);
		client.setEventHandle(Constants.CONNECT_EVENT, new ConnectHandler());
		client.setEventHandle(Constants.DISCONNECT_EVENT, new DisconnectHandler());
		client.setEventHandle(Constants.EXPIRED_EVENT, new ExpiredHandler());
		client.init();
		
		String key = "key1";
		Config config = client.createConfig("/IserviceDemoTest/group1/app2");
		config.setEventHandle(Constants.DATA_CHANGE_EVENT, new DataChangeHandler(config, key));
		
		Timer timer = new Timer();
		TimerTask task = new Main().new GetDataTask(config);
		
		timer.schedule(task, 0, 10000);
		
	}
	
	public void initZk() throws Exception{
		ZooKeeper zk = new ZooKeeper(DemoConf.host + DemoConf.root, Constants.SESSION_TIMEOUT, null);
		List<ACL> acls = new ArrayList<ACL>();
		acls.add(new ACL(Perms.ALL, Ids.ANYONE_ID_UNSAFE));
		
		if(zk.exists("/IserviceDemoTest", false) == null){
			zk.create("/IserviceDemoTest", "".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group1", "".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group2", "".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group1/app1", "".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group1/app2", "".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group1/app2/key1", "in group1/app2, i'm key1 version-1".getBytes(), acls, CreateMode.PERSISTENT);
			zk.create("/IserviceDemoTest/group1/app2/key2", "".getBytes(), acls, CreateMode.PERSISTENT);
		}	

	}
	
	public class GetDataTask extends TimerTask {

		private Config config;
		
		public GetDataTask(Config config){
			this.config = config;
		}
		
		@Override
		public void run() {
			System.out.println("get for check");
			System.out.println("key: key1 | data: " + ((DefaultData)this.config.get("key1", Constants.DEFAULT)).getData());
			Runtime runtime = Runtime.getRuntime();
			System.out.println("memoryUse: " + (runtime.totalMemory() - runtime.freeMemory()));
		}
		
	}
	
}
