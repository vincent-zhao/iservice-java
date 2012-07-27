package data;

public class DefaultData implements Data {

	private String content;
	
	public DefaultData(String content){
		this.content = content;
	}
	
	public String getData(){
		return content;
	}
	
	public void setData(String data){
		this.content = data;
	}
	
}
