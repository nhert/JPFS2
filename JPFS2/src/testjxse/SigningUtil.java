package testjxse;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import testjxse.JPFSPrinting.errorLevel;

public class SigningUtil {
	
	//sign an input file with the clients digital signature using SHA1 and DSA algorithms
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
			JPFSPrinting.logError("Invalid Key Exception: SigningUtil signData method", errorLevel.RECOVERABLE);
		} catch (SignatureException e) {
			JPFSPrinting.logError("Signature Exception: SigningUtil signData method", errorLevel.RECOVERABLE);
		} catch (NoSuchAlgorithmException e) {
			JPFSPrinting.logError("Algorithm Not Found Exception: SigningUtil signData method", errorLevel.RECOVERABLE);
		}
		return null;
	}
	
	//verify a signature
	public static boolean verifySignature(byte[] sign, byte[] data, PublicKey pk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
		Signature s = Signature.getInstance("SHA1WithDSA");
		s.initVerify(pk);
		s.update(data);
		return s.verify(sign);
	}
	
	//test the signing util class
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException{
		byte[] orig = "HELLO".getBytes();
		SignedContents signed = signData(orig);
		
		if(verifySignature(signed.signature, orig, signed.pk)){
			System.out.println("SIGNED");
		}else{
			System.out.println("UNSIGNED");
		}
	}
	
	//helper class for storing signing information
	public static class SignedContents{
		public PublicKey pk;
		public byte[] signature;
		SignedContents(PublicKey p, byte[] s){
			pk = p;
			signature = s;
		}
	}
	
}


