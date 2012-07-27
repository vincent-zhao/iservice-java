package client;

import src.Config;
import src.Constants;
import src.Handler;
import src.Store;

public class IService {
	
	private Store store;
	
	private String hosts = "localhost:2181";
	private String root = "/";
	private String user = "";
	private String pass = "";
	private String cache = "./run";
	
	public IService(String hosts, String root, String user, String pass, String cache){
		this.hosts = (null == hosts) ? this.hosts : hosts;
		this.root  = (null == root)  ? this.root  : root;
		this.user  = (null == user)  ? this.user  : user;
		this.pass  = (null == pass)  ? this.pass  : pass;
		this.cache = (null == cache) ? this.cache : cache;
		
		store = new Store(this.hosts, this.root, this.user, this.pass, this.cache);
	}
	
	public void init(){
		this.store.init();
	}
	
	public Config createConfig(String prefix){
		return new Config(prefix, store);
	}
	
	public void setEventHandle(String event, Handler handler){
		store.setEventHandle(Constants.GLOBAL_PREFIX, event, handler);
	}
	
}
