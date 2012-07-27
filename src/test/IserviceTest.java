package test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;

import client.IService;
import data.DefaultData;

import src.Config;
import src.Constants;
import src.Handler;
import test.conf.TestConf;

import junit.framework.Assert;
import junit.framework.TestCase;
import lib.Extends;
import lib.FS;

public class IserviceTest extends TestCase{

	public void setUp(){
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testGetConfigOK() throws Exception{
		ZooKeeper zk = this.initZK();
		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		TestHandler th = new TestHandler(connectedSignal);
		IService iservice = new IService(TestConf.host,null,null,null,"./src/test/tmp");		
		iservice.setEventHandle(Constants.CONNECT_EVENT, th);
		iservice.init();
		connectedSignal.await();
		
		Config config = iservice.createConfig("/iserviceTest/node1");
		DefaultData data = (DefaultData) config.get("node3", 123);
		Assert.assertEquals("node3", data.getData());
		
		this.clearUpZK(zk);
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testSetConfigOK() throws Exception {
		ZooKeeper zk = this.initZK();
		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		IService iservice = new IService(TestConf.host,null,null,null,"./src/test/tmp");		
		iservice.setEventHandle(Constants.CONNECT_EVENT, new TestHandler(connectedSignal));
		iservice.init();
		connectedSignal.await();
		
		Config config = iservice.createConfig("/iserviceTest/node1");
		DefaultData data = (DefaultData) config.get("node3", 123);
		Assert.assertEquals("node3", data.getData());
		
		
		CountDownLatch connectedSignal2 = new CountDownLatch(1);
		config.setEventHandle(Constants.DATA_CHANGE_EVENT, new TestHandler(connectedSignal2));
		
		zk.setData("/iserviceTest/node1/node3", "new-node3".getBytes(), -1);
		zk.setData("/iserviceTest/node1", "new-node1".getBytes(), -1);
		connectedSignal2.await();
		DefaultData data2 = (DefaultData) config.get("node3", 123);
		Assert.assertEquals("new-node3", data2.getData());
		
		this.clearUpZK(zk);
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	// change local file before update
	public void testSetConfigOK2() throws Exception{
		ZooKeeper zk = this.initZK();
		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		IService iservice = new IService(TestConf.host,null,null,null,"./src/test/tmp");		
		iservice.setEventHandle(Constants.CONNECT_EVENT, new TestHandler(connectedSignal));
		iservice.init();
		connectedSignal.await();
		
		Config config = iservice.createConfig("/iserviceTest/node1");
		DefaultData data = (DefaultData) config.get("node3", 123);
		Assert.assertEquals("node3", data.getData());
		
		FS.dump(TestConf.tmpPath + "/" + Extends.getPid() + "/iserviceTest/node1/node4.data", "version:1\r\nnew-node4");
		
		CountDownLatch connectedSignal2 = new CountDownLatch(1);
		config.setEventHandle(Constants.DUMP_FAIL_EVENT, new TestHandler(connectedSignal2));
		
		zk.setData("/iserviceTest/node1/node3", "new-node3".getBytes(), -1);
		zk.setData("/iserviceTest/node1", "new-node1".getBytes(), -1);
		connectedSignal2.await();
		DefaultData data2 = (DefaultData) config.get("node3", 123);
		Assert.assertEquals("node3", data2.getData());
		
		this.clearUpZK(zk);
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testLoadLocalFiles() throws InterruptedException{
		FS.dump(TestConf.tmpPath + "/123/root/node1/info.data", "version:0\r\ni'm in older node");
		FS.dump(TestConf.tmpPath + "/123/root/node1.data", "version:0\r\ni'm older info1");
		FS.dump(TestConf.tmpPath + "/123/root/node2.data", "version:1\r\ni'm newer info2");
				
		FS.dump(TestConf.tmpPath + "/234/root/node1/info.data", "version:1\r\ni'm in newer node");	
		FS.dump(TestConf.tmpPath + "/234/root/node1.data", "version:1\r\ni'm newer info1");
		FS.dump(TestConf.tmpPath + "/234/root/node2.data", "version:0\r\ni'm older info2");
		
		CountDownLatch connectedSignal = new CountDownLatch(1);
		TestHandler th = new TestHandler(connectedSignal);
		IService iservice = new IService("127.0.0.1:6000",null,null,null,"./src/test/tmp");		
		iservice.setEventHandle(Constants.DISCONNECT_EVENT, th);
		iservice.init();
		connectedSignal.await();
		
		Config config = iservice.createConfig("/root/node1");
		DefaultData data = (DefaultData) config.get("info", 111);
		Assert.assertEquals("i'm in newer node", data.getData());
		
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public ZooKeeper initZK() throws Exception{
		ZooKeeper zk = new ZooKeeper(TestConf.host, Constants.SESSION_TIMEOUT, null);
		List<ACL> acls = new ArrayList<ACL>();
		acls.add(new ACL(Perms.ALL, Ids.ANYONE_ID_UNSAFE));
		zk.create("/iserviceTest", "root".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node1", "node1".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node2", "node2".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node1/node3", "node3".getBytes(), acls, CreateMode.PERSISTENT);
		zk.create("/iserviceTest/node1/node4", "node4".getBytes(), acls, CreateMode.PERSISTENT);
		return zk;
	}
	
	public void clearUpZK(ZooKeeper zk) throws KeeperException, InterruptedException{
		zk.delete("/iserviceTest/node1/node4", -1);
		zk.delete("/iserviceTest/node1/node3", -1);
		zk.delete("/iserviceTest/node2", -1);
		zk.delete("/iserviceTest/node1", -1);
		zk.delete("/iserviceTest", -1);
	}
	
	public void clearUpFiles(String path){
		File f = new File(path);
		File[] files = f.listFiles();
		for(int i = 0; i < files.length; i++){
			if (files[i].getName().matches("\\d+")){
				FS.rmdir(files[i].getAbsolutePath());
			}	
		}
	}
	
	public class TestHandler extends Handler {

		private CountDownLatch cdl;		
		public TestHandler(CountDownLatch cdl){
			super();
			this.cdl = cdl;
		}		
		@Override
		public void callback(Object content) {
			this.cdl.countDown();
		}	
	}
	
	public class TestHandler2 extends Handler {

		private CountDownLatch cdl;		
		public TestHandler2(CountDownLatch cdl){
			super();
			this.cdl = cdl;
		}		
		@Override
		public void callback(Object content) {
			this.cdl.countDown();
		}	
	}

}
