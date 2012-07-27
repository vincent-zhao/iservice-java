package zk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import src.Constants;
import src.Store;

public class ZkCtl implements Watcher{

	private Store store;
	private ZooKeeper zk;
	private Timer timer;
	private TimerTask checkTask;
	
	public CountDownLatch connectedSignal = new CountDownLatch(1);
	
	public ZkCtl(String hosts, String root, String user, String pass, Store store) {
		this.timer = new Timer();
		this.store = store;
		
		try {
			this.zk = new ZooKeeper(hosts + root, Constants.SESSION_TIMEOUT, this);
			checkTask = new CheckTask(this.store);
			this.timer.schedule(checkTask, Constants.CHECK_TM);	
		} catch(Exception e) {
			e.printStackTrace();
			this.store.inform(Constants.ZK_ERROR);
		}
	}
	
	public void watch(String prefix) {
		Stat s = new Stat();
		String info;
		try {
			info = new String(this.zk.getData(prefix, true, s));
			this.store.check(prefix, info);
		} catch (Exception e) {
			this.store.emitEvent(prefix, Constants.GET_DATA_ERROR_EVENT, "");
		}
		//this.loop(prefix);
	}
	
	public HashMap<String, String> getTree(String path) {
		HashMap<String, String> tree = new HashMap<String, String>();
		this.getChildren(path, tree);
		return tree;
	}
	
	private void getChildren(String path, HashMap<String, String> tree){
		try {
			Stat s = new Stat();
			String value = new String(this.zk.getData(path, false, s));
			value = "version:" + s.getVersion() + "\r\n" + value;			
			tree.put(path, value);
			
			List<String> list = this.zk.getChildren(path, false);
			Iterator<String> i = list.iterator();
			while (i.hasNext()) {
				String one = i.next();
				this.getChildren(path + "/" + one, tree);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loop(String prefix){
		TimerTask tt = new LoopTask(prefix, this);
		this.timer.schedule(tt, Constants.LOOP_INTERVAL);
	}
	
	@Override
	public void process(WatchedEvent event) {
		if (event.getState() == KeeperState.Disconnected) {
			this.store.inform(Constants.ZK_NOT_CONNECT);
		} else if (event.getState() == KeeperState.SyncConnected && event.getType() == EventType.None) {
			checkTask.cancel();
			this.store.inform(Constants.ZK_OK);
		} else if (event.getState() == KeeperState.Expired) {
			this.store.inform(Constants.ZK_EXPIRED);
		} else if (event.getType() == EventType.NodeDataChanged) {
			this.watch(event.getPath());
		}
	}
	
}
