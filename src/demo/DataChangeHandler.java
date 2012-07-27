package demo;

import java.text.SimpleDateFormat;
import java.util.Date;

import data.DefaultData;

import src.Config;
import src.Constants;
import src.Handler;

public class DataChangeHandler extends Handler{
	
	private Config c;
	private String key;
	
	public DataChangeHandler(Config c, String key){
		this.c = c;
		this.key = key;
	}

	@Override
	public void callback(Object content) {
		Date currentTime = new Date();   
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		String dateString = formatter.format(currentTime); 
		
		System.out.println(dateString + " data changed!");
		System.out.println("key: " + key + " | " + "data: " + ((DefaultData)c.get(key, Constants.DEFAULT)).getData());
		
	}

}
