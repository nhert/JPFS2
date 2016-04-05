package testjxse;

import testjxse.P2PManager.reqType;

public class JPFSPrinting {
	
	public enum errorLevel{
		SEVERE, RECOVERABLE, CAN_IGNORE
	}
	
	public static void printBench(long start, long end, int leng){
		long time = end - start;
		time = time/1000000;
		int kleng;
		int mleng;
		int gleng;
		kleng = leng/1024;
		mleng = kleng/1024;
		gleng = mleng/1024;
		long sec = time/1000;
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("BENCHMARK RESULTS\n");
		built+=("Start Time of Request (mil-sec): " + start/1000000 + "\n");
		built+=("End Time of Request (mil-sec): " + end/1000000 + "\n");
		built+=("Request Total Time (mil-sec): " + time + "\n");
		built+=("Length of File (GB): " + gleng + "\n");
		built+=("Length of File (MB): " + mleng + "\n");
		built+=("Length of File (KB): " + kleng + "\n");
		built+=("Length of File (B): " + leng + "\n");
		if(sec>0){
			built+=("\nAverage Transfer Rate (GB / s): " + gleng/sec + "\n");
			built+=("Average Transfer Rate (MB / s): " + mleng/sec + "\n");
			built+=("Average Transfer Rate (KB / s): " + kleng/sec + "\n");
			built+=("Average Transfer Rate (B / s): " + leng/sec + "\n");
		}else{
			built+=("Average Transfer Rate (GB / ms): " + gleng/time + "\n");
			built+=("Average Transfer Rate (MB / ms): " + mleng/time + "\n");
			built+=("Average Transfer Rate (KB / ms): " + kleng/time + "\n");
			built+=("Average Transfer Rate (B / ms): " + leng/time + "\n");
		}
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
	 }
	
	public static void printNewPeer(String thePeer){
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("|- A New Peer Was Found and Added to the List\n");
		built+=("|* CREATED A NEW OUTPUT PIPE FOR "+thePeer+"*\n");
		built+=("|- The Peer List has been Updated!\n");
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
	}
	
	public static void printDeletedPeer(){
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("|- One or more of your Peers has closed their connection!\n");
		built+=("|* DELETING THEIR PEER OUTPUT PIPE *\n");
		built+=("|- The Peer List has been Updated!\n");
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
	}
	
	public static void printMessageHeader(reqType reqNum, String originName, byte[] data){
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("| Request Type: " + reqNum.toString() + " | Peer Origin: " + originName + "\n");
		built+=("| Data Length: " + data.length + " bytes\n");
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
	}
	
	//method for logging errors to the console in JPFS
	public static void logError(String error, errorLevel level){
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("|JPFS ERROR: " + error + "\n");
		built+=("|ERROR LEVEL: " + level.toString() + "\n");
		if(level == errorLevel.SEVERE){
			built+=("|ABORTING JPFS PROGRAM\n");
		}
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
		if(level==errorLevel.SEVERE){
			System.exit(1);
		}
	}
	
	//method for logging warnings to the console in JPFS
	public static void logWarning(String warning){
		String built = "";
		built+=("------------------------------------------------\n");
		built+=("|JPFS WARNING: " + warning + "\n");
		built+=("------------------------------------------------\n\n");
		System.out.print(built);
	}
	
	
}
