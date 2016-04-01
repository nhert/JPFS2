package testjxse;

import java.util.UUID;

public class PeerCode {
	public static final String MyCode = HashingUtil.hash(UUID.randomUUID().toString().toCharArray()); // generate an id and then hash it for security
	
	public static String MyHashedCode(){
		return MyCode;
	}
}
