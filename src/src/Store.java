package src;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import zk.ZkCtl;

import lib.Extends;
import lib.FS;

public class Store {

	private HashMap<String, String> cacheMap = new HashMap<String, String>();
	private HashMap<String, HashMap<String, Vector<Handler>>> eventMap = new HashMap<String, HashMap<String, Vector<Handler>>>();

	private ZkCtl zkCtl;
	private boolean zkOk;

	private String hosts;
	private String root;
	private String user;
	private String pass;
	private String cache;
	
	private Stack<String> stack;
	
	public Store(String hosts, String root, String user, String pass, 
			String cache){		
		this.zkOk = false;
		this.hosts = hosts;
		this.root = root;
		this.user = user;
		this.pass = pass;
		this.cache = cache;
		this.stack = new Stack<String>();
	}
	
	public void init(){
		this.zkCtl = new ZkCtl(this.hosts, this.root, this.user, this.pass, this);
	}
	
	public void expire(){
		this.init();
	}
	
	public void watch(String prefix){
		if (!this.zkOk) {
			this.stack.push(prefix);
		} else {
			this.zkCtl.watch(prefix);
		}
	}
	
	public void inform(int type){
		switch(type){
		case Constants.ZK_ERROR:
			this.emitEvent(Constants.GLOBAL_PREFIX, Constants.ZK_PATH_ERROR_EVENT, "");
			break;
		case Constants.ZK_NOT_CONNECT:
			this.emitEvent(Constants.GLOBAL_PREFIX, Constants.DISCONNECT_EVENT, "");
			break;
		case Constants.ZK_OK:
			this.zkOk = true;
			while(!this.stack.empty()){
				this.zkCtl.watch(this.stack.pop());
			}
			this.emitEvent(Constants.GLOBAL_PREFIX, Constants.CONNECT_EVENT, "");
			break;
		case Constants.ZK_EXPIRED:
			this.emitEvent(Constants.GLOBAL_PREFIX, Constants.EXPIRED_EVENT, "");
			break;
		default:
			break;
		}
	}
	
	public void setEventHandle(String prefix, String event, Handler handler){
		HashMap<String, Vector<Handler>> map = eventMap.get(prefix);
		if (null == map) {
			map = new HashMap<String, Vector<Handler>>();
			eventMap.put(prefix, map);
		}
		Vector<Handler> handlers = map.get(event);
		if (null == handlers) {
			handlers = new Vector<Handler>();
			eventMap.get(prefix).put(event, handlers);
		}
		handlers.add(handler);
	}
	
	public void emitEvent(String prefix, String event, Object content){
		HashMap<String, Vector<Handler>> map = eventMap.get(prefix);
		if (null == map) {
			return;
		}
		
		Vector<Handler> handlers = map.get(event);
		if (null != handlers) {
			for(int i = 0; i < handlers.size(); i++){
				if(handlers.elementAt(i).getOK()){
					handlers.elementAt(i).callback(content);
				}
			}
		}
	}
	
	public String get(String key){
		if (cacheMap.isEmpty()) {
			String path = this.cache +  "/" + Extends.getPid();
			if (FS.existDir(path)) {
				this.load(path, "");
			} else {
				this.totalLoad(this.cache);
				this.load(path, "");
			}	
		}
		return cacheMap.get(key);
	}
	
	public void set(String k, String v){
		cacheMap.put(k, v);
	}
	
	public void check(String key, String value){
		if (cacheMap.get(key) ==  null || !cacheMap.get(key).equals(value)){
			if(this.dump(key)){
				cacheMap.clear();
				this.emitEvent(key, Constants.DATA_CHANGE_EVENT, value);
			} else {
				this.emitEvent(key, Constants.DUMP_FAIL_EVENT, key);
			}
		}
	}
	
	public void totalLoad(String path){
		File file = new File(path);
		File[] files = file.listFiles();
		
		File tmpFile = new File(path + "/" + Extends.getPid() + "_tmp");
		FS.mkdir(tmpFile.getAbsolutePath());
		
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		for(int i = 0; i < files.length; i++){
			if(!files[i].getName().matches("\\d+")){
				continue;
			}
			
			try {
				this.innerLoad(files[i].getName(), tmpFile.getAbsolutePath(), map);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		File dest = new File(this.cache + "/" + String.valueOf(Extends.getPid()));
		if (dest.exists()) {
			FS.rmdir(tmpFile.getAbsolutePath());
		} else {
			tmpFile.renameTo(dest);
		}	
	}
	
	public void innerLoad(String src, String dest, HashMap<String, Integer> map) throws IOException{
		File[] files = new File(this.cache + "/" + src).listFiles();
		for(int i = 0; i < files.length; i++){
			String name = files[i].getName();
			String path = files[i].getAbsolutePath();
			
			if (files[i].isFile()) {
				File file = new File(path.substring(0, path.length() - Constants.FILE_SUFFIX.length()));
				if((file.exists() && file.isDirectory()) || name.matches(".*\\" + Constants.MD5_SUFFIX + "$")){
					continue;
					
				} else {
					int version = this.getVersion(FS.read(path));				
					String key = this.getKey(src, name);
					if (map.get(key) == null || version > map.get(key)){					
						FS.cp(path, dest + "/" + name);
						map.put(key, version);
					}
				}
			}
			
			if (files[i].isDirectory()) {
				File file = new File(path + ".data");
				if (file.exists()) {
					int version = this.getVersion(FS.read(file.getAbsolutePath()));
					String key = src.substring(src.indexOf("/") + 1) + "/" + name;
					if (map.get(key) == null || version > map.get(key)){
						FS.cp(file.getAbsolutePath(), dest + "/" + file.getName());
						FS.cp(path, dest + "/" + name);
						this.calMd5(files[i].getAbsolutePath());
						
						map.put(key, version);
					}
					
				} else {
					FS.mkdir(dest + "/" + name);
					this.innerLoad(src + "/" + name, dest + "/" + name, map);
				}
			}
		}
	}
	
	private String getKey(String prefix, String name){
		prefix = prefix.substring(prefix.indexOf("/") + 1);
		name = name.substring(0, name.length() - ".data".length());
		return prefix + "/" + name;
	}
	
	private int getVersion(String content){
		content = content.substring(0, content.indexOf("\r\n"));
		return Integer.parseInt(content.substring(content.indexOf(":") + 1));
	}
	
	public void load(String path, String relative){
		File file = new File(path);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				String name = files[i].getName();
				this.load(path + "/" + name, relative + "/" + name);
			}
		} else if (file.isFile()) {
			String key = relative.substring(0, relative.length()- ".data".length());
			String val = FS.read(file.getAbsolutePath());
			val = val.substring(val.indexOf("\r\n") + 2);
			this.set(key, val);
		}
	}
	
	public boolean dump(String key){
		File f = new File(this.cache + "/" + Extends.getPid() + "/" + key);	

		String md5File = f.getAbsolutePath() + Constants.MD5_SUFFIX;
		if (new File(md5File).exists() && !FS.read(md5File).equals(calMd5(f.getAbsolutePath()))) {
			return false;
		}
		
		String prefix = key.substring(0,key.lastIndexOf("/"));
		HashMap<String, String> tree = this.zkCtl.getTree(key);
		
		String tmpPath = f.getParent() + "_tmp";
		
		Iterator<String> i = tree.keySet().iterator();
		while (i.hasNext()) {
			String k = i.next().toString();
			String v = tree.get(k);
			FS.dump(this.addPath(tmpPath, k.substring(prefix.length())) + Constants.FILE_SUFFIX, v);
		}
		
		File file2 = new File(f.getParent());
		if (file2.exists()) {
			File[] files = file2.listFiles();
			for(int j = 0; j < files.length; j++){
				if (!files[j].getName().equals(f.getName()) && !files[j].getName().equals(f.getName() + Constants.FILE_SUFFIX)) {
					try {
						FS.cp(files[j].getAbsolutePath(), tmpPath + "/" + files[j].getName());
					} catch (IOException e) {
						this.emitEvent(Constants.GLOBAL_PREFIX, Constants.DUMP_ERROR_EVENT, key);
						e.printStackTrace();
					}
				}
			}
		}

		FS.dump(tmpPath + "/" + f.getName() + Constants.MD5_SUFFIX, this.calMd5(tmpPath + "/" + f.getName()));
		
		FS.rmdir(f.getParent());
		File oldFile = new File(tmpPath);
		File newFile = new File(f.getParent());
		oldFile.renameTo(newFile);
		return true;
	}

	public String calMd5(String path) {
		File f = new File(path);
		File[] files = f.listFiles();
		String md5s = "";
		for(int i = 0; i < files.length; i++){
			if (files[i].getName().matches(".md5$")) {
				continue;
			}
			if (files[i].isDirectory()) {
				md5s += this.calMd5(files[i].getAbsolutePath());
			} else {
				md5s += Extends.getMD5Str(FS.read(files[i].getAbsolutePath()));
			}
		}
		return Extends.getMD5Str(md5s);
	}
	
	private String addPath(String prefix, String path){
		if (!prefix.substring(prefix.length()-1).equals("/") && !path.substring(0).equals("/")) {
			return FS.normalize(prefix + "/" + path);
		}
		return FS.normalize(prefix + path);
	}
		
}
