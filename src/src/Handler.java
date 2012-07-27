package src;

public abstract class Handler {

	private boolean ok;
	
	public Handler(){
		this.ok = true;
	}
	
	public boolean getOK(){
		return this.ok;
	}
	
	public void setOK(boolean ok){
		this.ok = ok;
	}
	
	public abstract void callback(Object content);
	
}
