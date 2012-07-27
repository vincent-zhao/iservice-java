package demo;

import java.text.SimpleDateFormat;
import java.util.Date;

import src.Handler;

public class ExpiredHandler extends Handler{

	@Override
	public void callback(Object content) {
		Date currentTime = new Date();   
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		String dateString = formatter.format(currentTime); 
		
		System.out.println(dateString + " zk connection expired!");		
	}

}
