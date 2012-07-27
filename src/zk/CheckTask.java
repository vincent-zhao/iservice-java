package zk;

import java.util.TimerTask;

import src.Constants;
import src.Store;

public class CheckTask extends TimerTask{

	private Store store;
	
	public CheckTask(Store store){
		super();
		this.store = store;
	}

	@Override
	public void run() {
		this.store.inform(Constants.ZK_NOT_CONNECT);
		this.cancel();
	}
	
}
