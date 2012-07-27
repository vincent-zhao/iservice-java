package demo.demo2;

import java.util.Timer;
import java.util.TimerTask;

import demo.conf.DemoConf;

public class ConfigChangeDemo2 {

	public static void main(String args[]){
		Timer timer = new Timer();
		try {
			TimerTask task = new ChangeZkTask2(DemoConf.host, DemoConf.root);
			timer.schedule(task, 0, 5000);		
		} catch(Exception e){
			e.printStackTrace();
		}
	}

}
