package src;

import java.util.HashMap;

import parser.DefaultParser;
import parser.IniParser;
import data.Data;

public class Config {
	
	private Store store;
	private String prefix;
	private HashMap<String, Handler> eventMap;
	
	public Config(String prefix, Store store){
		this.prefix = prefix;
		this.store = store;
		this.eventMap = new HashMap<String, Handler>();
		this.store.watch(prefix);
	}
	
	public void setEventHandle(String event, Handler handler){
		eventMap.put(event, handler);
		this.store.setEventHandle(this.prefix, event, handler);
	}
	
	public void removeEventHandle(String event){
		this.eventMap.get(event).setOK(false);
	}
	
	public Data get(String key, int type){
		switch (type) {
		case Constants.INI:
			return new IniParser().parse(this.store.get(this.prefix + "/" + key));
			
		default: 
			return new DefaultParser().parse(this.store.get(this.prefix + "/" + key));	
		}
	}
	
}
