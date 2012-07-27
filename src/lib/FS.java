package lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

public class FS {
	
	public static boolean existDir(String path){
		File f = new File(path);
		if (!f.exists()) {
			return false;
		}
		
		return f.isDirectory();
	}
	
	public static boolean existFile(String path){
		File f = new File(path);
		if (!f.exists()) {
			return false;
		}
		
		return f.isFile();
	}
	
	public static void mkdir(String path){
		if (!existDir(path)) {
			File f = new File(path);
			f.mkdir();
		}
	}
	
	public static void rmdir(String path){
		if (existDir(path) || existFile(path)) {
			File f = new File(path);
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for(int i = 0; i < files.length; i++){
					FS.rmdir(files[i].getAbsolutePath());
				}
				f.delete();
			} else {
				f.delete();
			}
		}
	}
	
	public static void cp(String src, String dest) throws IOException{
		File sourceFile = new File(src);
		File destFile = new File(dest);
		if(destFile.exists()){
			FS.rmdir(destFile.getAbsolutePath());
		}
		
		if (sourceFile.isDirectory()) {
			File f = new File(src + "_tmp");
			f.mkdir();
			File[] files = sourceFile.listFiles();
			for(int i = 0; i < files.length; i++){
				FS.cp(src + "/" + files[i].getName(), src + "_tmp/" + files[i].getName());
			}
			File f2 = new File(dest);
			f.renameTo(f2);
		} else {		
			File targetFile = new File(dest);
			FileInputStream input = new FileInputStream(sourceFile);  
	        BufferedInputStream inBuff=new BufferedInputStream(input);  
	  
	        // 新建文件输出流并对它进行缓冲   
	        FileOutputStream output = new FileOutputStream(targetFile);  
	        BufferedOutputStream outBuff=new BufferedOutputStream(output);  
	          
	        // 缓冲数组   
	        byte[] b = new byte[1024 * 5];  
	        int len;  
	        while ((len = inBuff.read(b)) != -1) {  
	            outBuff.write(b, 0, len);  
	        }  
	        // 刷新此缓冲的输出流   
	        outBuff.flush();  
	          
	        //关闭流   
	        inBuff.close();  
	        outBuff.close();  
	        output.close();  
	        input.close();
		}
	}
	
	public static boolean dump(String path, String content){
		Stack<String> s = new Stack<String>();
		File f = new File(path);
		f = new File(f.getAbsolutePath());
		String parent = f.getParent();
		while(null != parent){
			s.push(parent);
			f = new File(parent);
			parent = f.getParent();
		}
		while(!s.empty()){
			FS.mkdir(s.pop());
		}
		
		try {
			File newFile = new File(path);
			if (!FS.existFile(path)) {
				newFile.createNewFile();
			}
			PrintWriter pw = new PrintWriter(new FileWriter(newFile), true);
			pw.print(content);
			pw.close();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	public static String read(String path){
		File f = new File(path);
		if (!f.exists()) {
			return null;
		}
		try {
			String content = "";
			String tmpLine = null;
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			while ((tmpLine = reader.readLine()) != null) {
				content += tmpLine + "\r\n";
			}
			
			if(content.length() >=2 
				|| content.substring(content.length() - 2).equals("\r\n")){
				content = content.substring(0, content.length() - 2);
			}
			
			reader.close();
			return content;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String normalize(String path){
		return path.replaceAll("/{2,}", "/");
	}
	
}
