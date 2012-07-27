package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import test.conf.TestConf;

import junit.framework.Assert;
import junit.framework.TestCase;
import lib.FS;

public class FsTest extends TestCase {
	
	public void tearDown(){
		
	}
	
	public void testDirOperator(){
		String testPath = TestConf.tmpPath + "/testDir";
		
		FS.mkdir(testPath);
		Assert.assertTrue(FS.existDir(testPath));
		
		FS.rmdir(testPath);
		Assert.assertFalse(FS.existDir(testPath));
	}
	
	public void testNormalize(){
		String path = ".///a1//a2/a3//a4";
		Assert.assertEquals("./a1/a2/a3/a4", FS.normalize(path));
	}
	
	public void testRead() throws IOException{
		String path = TestConf.tmpPath + "/readTestFile.txt";
		String testContent = "i'am test info";
		
		File f = new File(path);
		f.createNewFile();
		
		PrintWriter pw = new PrintWriter(new FileWriter(f), true);
		pw.print(testContent);
		pw.close();
		
		Assert.assertEquals(testContent, FS.read(path));
		f.delete();
	}
	
	public void testDump() {
		String path = TestConf.tmpPath + "/a1/testDump.txt";
		String testContent = "i'am test info";
		
		FS.dump(path, testContent);
		Assert.assertEquals(testContent, FS.read(path));
		File f = new File(path);
		f.delete();
		File f2 = new File(TestConf.tmpPath + "/a1");
		f2.delete();
	}
	
	public void testCp() throws IOException {
		FS.dump(TestConf.tmpPath + "/a1/test1.txt", "blabla");
		FS.dump(TestConf.tmpPath + "/a1/a2/test2.txt", "blablabla");
		FS.cp(TestConf.tmpPath + "/a1", TestConf.tmpPath + "/new-a1");
		
		Assert.assertEquals("blablabla", FS.read(TestConf.tmpPath + "/new-a1/a2/test2.txt"));
		
		FS.rmdir(TestConf.tmpPath + "/a1");
		FS.rmdir(TestConf.tmpPath + "/new-a1");		
	}

}
