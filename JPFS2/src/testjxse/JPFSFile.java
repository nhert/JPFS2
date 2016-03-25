package testjxse;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.*;

public class JPFSFile implements java.io.Serializable{
	public String name;
	public String strLitName;
	public transient String path;
	public JPFSSize size;
	public String extension;
	public Date date;
	
	//generate easy to retrieve info when creating new files in JPFS
	JPFSFile(File f) throws IOException{
		size = new JPFSSize();
		name = f.getName();
		strLitName = FilenameUtils.getBaseName(name);
		path = f.getCanonicalPath();
		size.bytes = f.length();
		extension = FilenameUtils.getExtension(path);
		
		Calendar c = Calendar.getInstance();
		date = new Date();
		date = c.getTime();
	}
	
	public class JPFSSize implements java.io.Serializable{
		private static final long serialVersionUID = 1L;
		private long bytes;
		
		public long getByteSize(){return bytes;}
		public long getKByteSize(){return bytes/1024;}
		public long getMByteSize(){return (bytes/1024)/1024;}
		public long getGByteSize(){return ((bytes/1024)/1024)/1024;}
	}
}
