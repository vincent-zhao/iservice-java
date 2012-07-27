package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.junit.Assert;

import src.Constants;
import src.Store;
import test.conf.TestConf;

import zk.ZkCtl;
import junit.framework.TestCase;

public class ZkCtlTest extends TestCase {

	public void testConnectOk() throws InterruptedException{
		CountDownLatch connectedSignal = new CountDownLatch(1);
		TestStore ts = new TestStore("","","","","");
		ts.setSignal(connectedSignal);
		
		new ZkCtl(TestConf.host, TestConf.root, "", "", ts);
		connectedSignal.await();
		Assert.assertEquals(Constants.ZK_OK, ts.type);
	}

	public void testConnectToWrongAddress() throws InterruptedException{		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		TestStore ts = new TestStore("","","","","");
		ts.setSignal(connectedSignal);
		
		new ZkCtl("127.0.0.1:6000", "/", "", "", ts);
		connectedSignal.await();
		Assert.assertEquals(Constants.ZK_NOT_CONNECT, ts.type);
	}
	
	public void testConnectToTwoAddresses() throws InterruptedException{
		int times = 5;
		for(int i = 0; i < times; i++){
			CountDownLatch connectedSignal = new CountDownLatch(1);
			TestStore ts = new TestStore("","","","","");
			ts.setSignal(connectedSignal);
			
			new ZkCtl("127.0.0.1:6000," + TestConf.host, TestConf.root, "", "", ts);
			connectedSignal.await();
			Assert.assertEquals(Constants.ZK_OK, ts.type);
		}
	}
	
	public void testGetTree() throws Exception {
		ZooKeeper zk = this.initZk();
		
		ZkCtl zkCtl = new ZkCtl(TestConf.host, TestConf.root, "", "", null);
		HashMap<String, String> map = zkCtl.getTree("/iserviceTest/node1");
		Assert.assertEquals("version:0\r\nnode1-node1", map.get("/iserviceTest/node1"));
		Assert.assertEquals("version:0\r\nnode1-node1-node1", map.get("/iserviceTest/node1/node1"));
		
		this.clearUpZk(zk);
	}
	
	public void testWatch() throws Exception{
		ZooKeeper zk = this.initZk();
		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		TestStore ts = new TestStore("","","","","");
		ts.setSignal(connectedSignal);
		ZkCtl zkCtl = new ZkCtl(TestConf.host, TestConf.root, "", "", ts);
		zkCtl.watch("/iserviceTest/node1");
		zk.setData("/iserviceTest/node1", "node1-node1-new".getBytes(), 0);

		connectedSignal.await();
		this.clearUpZk(zk);
	}
	
	public ZooKeeper initZk() throws Exception{
		ZooKeeper zk = new ZooKeeper(TestConf.host + TestConf.root, Constants.SESSION_TIMEOUT, null);
		List<ACL> acls = new ArrayList<ACL>();
		acls.add(new ACL(Perms.ALL, Ids.ANYONE_ID_UNSAFE));
		zk.create("/iserviceTest", "node1".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node1", "node1-node1".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node1/node1", "node1-node1-node1".getBytes(), acls, CreateMode.PERSISTENT);
		return zk;
	}
	
	public void clearUpZk(ZooKeeper zk) throws Exception {
		Stat s = new Stat();
		zk.getData("/iserviceTest/node1/node1", false, s);
		zk.delete("/iserviceTest/node1/node1", s.getVersion());
		
		zk.getData("/iserviceTest/node1", false, s);
		zk.delete("/iserviceTest/node1", s.getVersion());
		
		zk.getData("/iserviceTest", false, s);
		zk.delete("/iserviceTest", s.getVersion());
	}
	
	public class TestStore extends Store {

		private int step;
		private CountDownLatch cdl;
		public int type;
		
		public TestStore(String hosts, String root, String user, String pass,
				String cache) {
			super(hosts, root, user, pass, cache);
			this.step = 0;
		}
		
		public void init(){
			//do nothing
		}
		
		public void setSignal(CountDownLatch cdl){
			this.cdl = cdl;
		}
		
		public void inform(int type){
			this.type = type;
			switch(type){
			case Constants.ZK_ERROR:
				this.cdl.countDown();
				break;
			case Constants.ZK_NOT_CONNECT:
				this.cdl.countDown();
				break;
			case Constants.ZK_OK:
				this.cdl.countDown();
				break;
			default:
				break;
			}
		}
		
		public void check(String key, String value){
			if (step == 0){
				Assert.assertEquals("/iserviceTest/node1", key);
				Assert.assertEquals("node1-node1", value);
			} else if (step == 1){
				Assert.assertEquals("/iserviceTest/node1", key);
				Assert.assertEquals("node1-node1-new", value);
				this.cdl.countDown();
			}
			step++;
		}
		
		public void emitEvent(String prefix, String event){}
		
	}
	
}
