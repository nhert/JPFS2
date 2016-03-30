package testjxse;

import javax.crypto.Cipher;
import javax.crypto.spec.*;

import org.apache.commons.codec.binary.Base64;

import testjxse.JPFSPrinting.errorLevel;

public class EncryptUtil {

	//encrypts a stream of bytes using AES encryption and a JPFS unique key and vector
	public static byte[] encryptBits(String ckey, String vector, byte[] input){
		try{
			IvParameterSpec ivPS = new IvParameterSpec(vector.getBytes("UTF-8"));
			SecretKeySpec keyS = new SecretKeySpec(ckey.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, keyS, ivPS);
			
			byte[] encryptedValue = cipher.doFinal(input);
			//System.out.println("JPFS2 - EncryptUtil - Original Data: " + new String (input));
			//System.out.println("JPFS2 - EncryptUtil - Encrypted String: " + Base64.encodeBase64String(encryptedValue));
			return Base64.encodeBase64(encryptedValue);
		}catch (Exception ex){
			JPFSPrinting.logError("General Exception: EncryptUtil encryption method", errorLevel.RECOVERABLE);
		}
		return null;
	}
	
	//decrypts the stream of bytes with the JPFS key and vector in a Cipher
	public static byte[] decryptBits(String ckey, String vector, byte[] encryptedIn){
		try{
			IvParameterSpec ivPS = new IvParameterSpec(vector.getBytes("UTF-8"));
			SecretKeySpec keyS = new SecretKeySpec(ckey.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, keyS, ivPS);
			
			byte[] decrypted = cipher.doFinal(Base64.decodeBase64(encryptedIn));
			
			return decrypted;
		}catch(Exception ex){
			JPFSPrinting.logError("General Exception: EncrypUtil decryption method", errorLevel.RECOVERABLE);
		}
		return null;
	}
	
	//test for valid encryption / decryption
	public static void main(String[] args){
		String keyTest = "Bar12345Bar12345";
		String vectorTest = "InitializeVector";
		String test = "JPFS Encryption Utility Test String";
		
		byte[] enc = encryptBits(keyTest, vectorTest, test.getBytes());
		System.out.println(new String(decryptBits(keyTest, vectorTest, enc)));
	}
}
