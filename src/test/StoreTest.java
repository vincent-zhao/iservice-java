package test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;

import src.Constants;
import src.Handler;
import src.Store;
import test.conf.TestConf;
import junit.framework.Assert;
import junit.framework.TestCase;
import lib.Extends;
import lib.FS;

public class StoreTest extends TestCase {
	
	public void setUp(){
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testCreateZkError() throws Exception{		
		Store s = new Store("127.0.0.1:6000", TestConf.root, "", "", TestConf.tmpPath);
		CountDownLatch connectedSignal = new CountDownLatch(1);
		s.setEventHandle(Constants.GLOBAL_PREFIX, Constants.DISCONNECT_EVENT, new TestHandler(connectedSignal));
		s.init();
		connectedSignal.await();
	}
	
	public void testDump() throws Exception{
		ZooKeeper zk = this.initZk();
		
		Store s = new Store(TestConf.host, TestConf.root, "", "", TestConf.tmpPath);
		s.init();
		s.dump("/iserviceTest/node1");
		
		int pid = Extends.getPid();
		String content = FS.read(TestConf.tmpPath + "/" + pid + "/iserviceTest/node1/node1.data");
		Assert.assertEquals("version:0\r\nnode1-node1-node1", content);
		
		this.clearUpZk(zk);
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testDump2() throws Exception{
		ZooKeeper zk = this.initZk();
		int pid = Extends.getPid();
		
		FS.dump(TestConf.tmpPath + "/" + pid + "/iserviceTest/node2/node.data", "version:0\r\ni'm node2/node.data for check");
		FS.dump(TestConf.tmpPath + "/" + pid + "/iserviceTest/node2.data", "version:0\r\ni'm node2.data for check");
		FS.dump(TestConf.tmpPath + "/" + pid + "/iserviceTest/node1/node1.data", "version:-1\r\ni'm node1/node1.data for check");
		FS.dump(TestConf.tmpPath + "/" + pid + "/iserviceTest/node1.data", "version:-1\r\ni'm node1.data for check");
		
		Store s = new Store(TestConf.host, TestConf.root, "", "", TestConf.tmpPath);
		s.init();
		s.dump("/iserviceTest/node1");
		
		
		String content = FS.read(TestConf.tmpPath + "/" + pid + "/iserviceTest/node1/node1.data");
		Assert.assertEquals("version:0\r\nnode1-node1-node1", content);
		
		content = FS.read(TestConf.tmpPath + "/" + pid + "/iserviceTest/node1.data");
		Assert.assertEquals("version:0\r\nnode1-node1", content);
		
		content = FS.read(TestConf.tmpPath + "/" + pid + "/iserviceTest/node2.data");
		Assert.assertEquals("version:0\r\ni'm node2.data for check", content);
		
		this.clearUpZk(zk);
		this.clearUpFiles(TestConf.tmpPath);
	}
	
	public void testLoad() throws Exception{
		FS.dump(TestConf.tmpPath + "/root.data", "version:0\r\ni'am root");
		FS.dump(TestConf.tmpPath + "/root/node1.data", "version:0\r\ni'am node1");
		FS.dump(TestConf.tmpPath + "/root/node2.data", "version:0\r\ni'am node2");
		FS.dump(TestConf.tmpPath + "/root/node2/node3.data", "version:0\r\ni'am node3");
		
		Store s = new Store(TestConf.host, TestConf.root, "", "", TestConf.tmpPath);
		s.load(TestConf.tmpPath + "/root", "/root");
		
		Assert.assertEquals("i'am node1", s.get("/root/node1"));
		Assert.assertEquals("i'am node2", s.get("/root/node2"));
		Assert.assertEquals("i'am node3", s.get("/root/node2/node3"));
		
		FS.rmdir(TestConf.tmpPath + "/root");
		FS.rmdir(TestConf.tmpPath + "/root.data");
	}
	
	public void testTotalLoad(){
		FS.dump(TestConf.tmpPath + "/123/root/node1/node1-node1.data", "version:0\r\ni'm node1-node1(older)");
		FS.dump(TestConf.tmpPath + "/123/root/node1.data", "version:0\r\ni'm node1(older)");
		FS.dump(TestConf.tmpPath + "/123/root/node2/node2-node1.data", "version:0\r\ni'm node2-node1(older)");
		FS.dump(TestConf.tmpPath + "/123/root/node2.data", "version:1\r\ni'm node2(newer)");
		FS.dump(TestConf.tmpPath + "/123/root/node3.data", "version:1\r\ni'm node3(newer)");
		
		FS.dump(TestConf.tmpPath + "/234/root/node1/node1-node1.data", "version:1\r\ni'm node1-node1(newer)");
		FS.dump(TestConf.tmpPath + "/234/root/node1.data", "version:1\r\ni'm node1(newer)");
		FS.dump(TestConf.tmpPath + "/234/root/node2/node2-node1.data", "version:1\r\ni'm node2-node1(newer)");
		FS.dump(TestConf.tmpPath + "/234/root/node2.data", "version:0\r\ni'm node2(older)");
		FS.dump(TestConf.tmpPath + "/234/root/node3.data", "version:0\r\ni'm node3(older)");
		
		Store s = new Store(TestConf.host, TestConf.root, "", "", TestConf.tmpPath);
		s.totalLoad(TestConf.tmpPath);
		
		Assert.assertEquals("version:1\r\ni'm node1(newer)", FS.read(TestConf.tmpPath + "/" + Extends.getPid() + "/root/node1.data"));
		Assert.assertEquals("version:1\r\ni'm node2(newer)", FS.read(TestConf.tmpPath + "/" + Extends.getPid() + "/root/node2.data"));
		Assert.assertEquals("version:1\r\ni'm node3(newer)", FS.read(TestConf.tmpPath + "/" + Extends.getPid() + "/root/node3.data"));
		
		this.clearUpFiles(TestConf.tmpPath);
		
	}
	
	public void testMd5(){
		FS.dump(TestConf.tmpPath + "/group1/file1", "test1");
		FS.dump(TestConf.tmpPath + "/group1/app1/file2", "test2");
		FS.dump(TestConf.tmpPath + "/group1/app1/file3", "test3");
		
		Store s = new Store(TestConf.host, TestConf.root, "", "", TestConf.tmpPath);
		
		String pre = s.calMd5(TestConf.tmpPath + "/group1");
		FS.dump(TestConf.tmpPath + "/group1/app1/file2", "test22");
		String after = s.calMd5(TestConf.tmpPath + "/group1");
		
		Assert.assertFalse(pre.equals(after));
		
		FS.rmdir(TestConf.tmpPath + "/group1");
	}
	
	public void deleteFile(String path){
		File f = new File(path);
		f.delete();
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
		zk.delete("/iserviceTest/node1/node1", -1);
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
	
}
