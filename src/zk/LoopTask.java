package zk;

import java.util.TimerTask;

public class LoopTask extends TimerTask {

	private ZkCtl zkCtl;
	private String prefix;
	
	public LoopTask(String prefix, ZkCtl zkCtl){
		super();
		this.zkCtl = zkCtl;
		this.prefix = prefix;
	}
	
	@Override
	public void run() {
		this.zkCtl.watch(this.prefix);
		this.cancel();
	}

}
