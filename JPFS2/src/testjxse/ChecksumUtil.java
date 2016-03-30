package testjxse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import testjxse.JPFSPrinting.errorLevel;

public class ChecksumUtil {
	//calculates the checksum for a file
	private static byte[] calcChecksum(String fname){
		InputStream is;
	    byte[] buffer;
	    MessageDigest md5;
		try {
			is =  new FileInputStream(fname);
		    buffer = new byte[1024];
			md5 = MessageDigest.getInstance("MD5");
			int numRead;

			do {
			    numRead = is.read(buffer);
			    if (numRead > 0) {
			    	md5.update(buffer, 0, numRead);
			    }
			} while (numRead != -1);

			is.close();
			return md5.digest();
		} catch (NoSuchAlgorithmException e) {
			JPFSPrinting.logError("No Such Algorithm EX: ChecksumUtil calc method", errorLevel.RECOVERABLE);
		} catch (FileNotFoundException e) {
			JPFSPrinting.logError("File not Found EX: ChecksumUtil calc method", errorLevel.RECOVERABLE);
		} catch (IOException e) {
			JPFSPrinting.logError("IO EX: ChecksumUtil calc method", errorLevel.RECOVERABLE);
		}
	    return null;
	}
	
	//generates the md5 checksum from a file
	public static String generateMD5(String fname){
		byte[] ck = calcChecksum(fname);
		if(ck == null){
			return null;
		}
		String returnVal = "";
		for(int x = 0; x<ck.length; x++){
			returnVal += Integer.toString((ck[x] & 0xff)+0x100, 16).substring(1);
		}
		return returnVal;
	}
	
	//test the creation of a checksum
	public static void main(String args[]){
		System.out.println(generateMD5("MyGUI.fxml"));
	}
}
