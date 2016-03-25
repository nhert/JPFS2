package testjxse;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class SigningUtil {
	
	
	public static SignedContents signData(byte[] input){
		byte[] signed;
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
			kpg.initialize(1024);
			KeyPair kPair = kpg.genKeyPair();
			Signature s = Signature.getInstance("SHA1WithDSA");
			s.initSign(kPair.getPrivate());
			s.update(input);
			signed = s.sign();
			SignedContents sc = new SignedContents(kPair.getPublic(), signed);
			return sc;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean verifySignature(byte[] sign, byte[] data, PublicKey pk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
		Signature s = Signature.getInstance("SHA1WithDSA");
		s.initVerify(pk);
		s.update(data);
		return s.verify(sign);
	}
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException{
		byte[] orig = "HELLO".getBytes();
		SignedContents signed = signData(orig);
		
		if(verifySignature(signed.signature, orig, signed.pk)){
			System.out.println("SIGNED");
		}else{
			System.out.println("UNSIGNED");
		}
	}
	
	public static class SignedContents{
		public PublicKey pk;
		public byte[] signature;
		SignedContents(PublicKey p, byte[] s){
			pk = p;
			signature = s;
		}
	}
	
}


