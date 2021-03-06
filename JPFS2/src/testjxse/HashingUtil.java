package testjxse;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class HashingUtil {
	private static byte[] salt = "ZJE7TN5HD81JE895".getBytes(); // unique random salt value for JPFS client. all clients share salt value
	
	//hash a string with the JPFS salt
	public static String hash(char[] input){
		KeySpec spec = new PBEKeySpec(input, salt, 65536, 128);
		try {
			SecretKeyFactory sFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			byte[] hashed = sFact.generateSecret(spec).getEncoded();
			return Base64.getEncoder().encodeToString(hashed);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//verify input matches a hashed password
	public static boolean verifyPassword(char[] attempt, String hashedPassword){
		String newHash = hash(attempt);
		return newHash.equals(hashedPassword);
	}
	
	//test the hashing utility
	public static void main(String[] args){
		String p = "password1";
		String origHash = hash(p.toCharArray());
		String origHash2 = hash("TestFalse".toCharArray());
		System.out.println("Hash: " + origHash);
		System.out.println("Hash: " + origHash2);
		System.out.println(verifyPassword("TestFalse".toCharArray(), origHash));
	}
}
