package testjxse;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;

import testjxse.GUI_Control.sortType;
import testjxse.JPFSPrinting.errorLevel;
import testjxse.SigningUtil.SignedContents;

public class P2PManager implements DiscoveryListener, PipeMsgListener, OutputPipeListener{

  private DiscoveryService discovery = null;
  public NetworkManager NetManager;
  public PeerGroup PGroup;
  public NetworkConfigurator Config;
  public static File ConfigurationFile;
  private PeerID MyPeerID;
  
  public static String PName;
  private String CliName;
  
  private PipeService pipe_service;
  private PipeAdvertisement myPipeAdv = null;
  @SuppressWarnings("unused") // it is used, false flag
  private InputPipe myInput = null;
  private static volatile Map<Object,OutputPipe> peers = new Hashtable<Object,OutputPipe>(); // return output pipes from urns
  private static List<Object> urns = new ArrayList<Object>(); // store an array of found peer urns (for iteration)
  private static Map<String,Object> resolver = new HashMap<String,Object>(); // resolve peer names to their urns
  private static List<String> pnames = new ArrayList<String>(); // store an array of found peer names (for GUI)
  private static volatile Map<String, Thread> timeouts = new Hashtable<String, Thread>(); // holds the kill threads for timeouts
  private static Map<String, String> peerCodes = new HashMap<String,String>(); // maps peer names to a peer "code" which identifies them for validation
  private Object timeoutLock = new Object();
  private Object foundPeer;
  private String foundPeerN;
  
  //for benchmark testing
  public static long startTime;
  public static long endTime;
  
  //for encryption - These values could be randomly generated but are specified for the purposes of demonstration
  private static String eKey = "JPFS2KEYJPFS2YEK"; // 128-Bit Cipher Key for Demonstration Purposes.   
  private static String eVector = "InitializeVector"; // 16-Bit Init Vector for Demonstration Purposes.
  
  public enum reqType{
	  FILEREQ, FILERES, FILELIST, PINGREQ, PINGRES, FILEINFOREQ, FILEINFORES, SORTREQ, DELETENOTIFICATION, PEERCODE, VALIDATEREQ, VALIDATERES
  }
  
  public void initGlobal(String n){
	  try{
		  System.out.println("Initializing JPFS Client");
		  GUI_Control_ApplicationLevel.setGroupLabel("Peers of Global Lobby");
		  CliName = n; // the clients name "J_PFS"
		  String id = UUID.randomUUID().toString().substring(0, 8);
          PName = getCompName() + " (ID: "+ id + ")"; // the host name is this users peer name + random int id for uniqueness
          GUI_Control.initFileList(PName + " (You)");
          GUI_Control.You = PName + " (You)";
          String filePName = getCompName() + id;
          pnames.add(PName + " (You)"); // display yourself in the peer list 
          MyPeerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
                  PName.getBytes()); // peer id of this user gets set up by a factory
          
          System.out.println("Created Peer ID for User: " + PName + "\n\n PID: " + MyPeerID); // info dialog to show your name and peer id
          
          ConfigurationFile = new File("./Profiles/" + filePName); // setup the config file
          NetworkManager.RecursiveDelete(ConfigurationFile);
          
		  NetManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, PName, ConfigurationFile.toURI()); // create a new network manager object for this client
		 
		  PopInformationMessage(CliName, "Starting the Network"); // inform user that basic setup is complete
		  PGroup = NetManager.startNetwork();
		  
		  //Check to see if client successfully joined the peergroup
	      if (Module.START_OK != PGroup.startApp(new String[0])){
	          System.err.println("Cannot start child peergroup");
	          System.exit(1);
	      }

	        //get the pipe service from the group you joined
	        pipe_service = PGroup.getPipeService();
	        myPipeAdv = newPipeAdvertisement(null);
	        //create a new input pipe for this client
	        myInput = pipe_service.createInputPipe(myPipeAdv, this);
	        
	        //fetch the discovery service for the group you joined
	        discovery = PGroup.getDiscoveryService();
	        //set this class as a listener for discovery messages
	        discovery.addDiscoveryListener(this);  
	        
	        //publish this client over the peer group
	        discovery.publish(myPipeAdv);
	        discovery.remotePublish(myPipeAdv);
          
	    	PopInformationMessage(n, "Connected via Peer Group: " + PGroup.getPeerGroupName() + "\nYour Peer Name: " + PName);
	        check_timeouts();
	  } catch(IOException e){
		  JPFSPrinting.logError("IO Exception in P2P Init", errorLevel.SEVERE);
	  } catch(PeerGroupException e){
		  JPFSPrinting.logError("Group Exception in P2P Init", errorLevel.SEVERE);
	  } catch(Exception e){
		  JPFSPrinting.logError("General Exception in P2P Init", errorLevel.SEVERE);
	  }
  }
  
  public void initCustom(String n, String GroupName, boolean Creator, String desc, boolean pwordGroup, String pword){
	  try{
	  System.out.println("Initializing JPFS Client");
	  GUI_Control_ApplicationLevel.setGroupLabel("Peers of " + GroupName);
	  CliName = n; // the clients name "J_PFS"
	  String id = UUID.randomUUID().toString().substring(0, 8);
      PName = getCompName() + " (ID: "+id + ")"; // the host name is this users peer name + random int id for uniqueness
      GUI_Control.initFileList(PName + " (You)");
      GUI_Control.You = PName + " (You)";
      String filePName = getCompName() + id;
      pnames.add(PName + " (You)"); // display yourself in the peer list 
      MyPeerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
              PName.getBytes()); // peer id of this user gets set up by a factory
      
      System.out.println("Created Peer ID for User: " + PName + "\n\n PID: " + MyPeerID); // info dialog to show your name and peer id
      
      ConfigurationFile = new File("./Profiles/" + filePName); // setup the config file
      NetworkManager.RecursiveDelete(ConfigurationFile);
      
	  NetManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, PName, ConfigurationFile.toURI()); // create a new network manager object for this client
	  
	  PopInformationMessage(CliName, "Starting the Network"); // inform user that basic setup is complete
	  PGroup = NetManager.startNetwork();
	  
	  //PRIV GROUP LOGIC
	  ModuleImplAdvertisement customGroup = PGroup.getAllPurposePeerGroupImplAdvertisement();
	  
	  PeerGroup customGroupInstance = PGroup.newGroup(IDFactory.newPeerGroupID(GroupName.getBytes()), customGroup, GroupName, desc, false);
	
	  //Check to see if client successfully joined the peergroup
      if (Module.START_OK != customGroupInstance.startApp(new String[0])){
          JPFSPrinting.logError("Could not Start JPFS App Network", errorLevel.SEVERE);
      }
      
      //get the pipe service from the group you joined
      pipe_service = customGroupInstance.getPipeService();
      myPipeAdv = newPipeAdvertisement(customGroupInstance.getPeerGroupID());
      //create a new input pipe for this client
      myInput = pipe_service.createInputPipe(myPipeAdv, this);
      
      //fetch the discovery service for the group you joined
      discovery = customGroupInstance.getDiscoveryService();
      //set this class as a listener for discovery messages
      discovery.addDiscoveryListener(this);  
      
      //publish this client over the peer group
      discovery.publish(myPipeAdv);
      discovery.remotePublish(myPipeAdv);

      if(Creator){
    	  PeerGroup privateGroupPublisher = PGroup.newGroup(IDFactory.newPeerGroupID("PrivateGroupPublisher".getBytes()), customGroup, "PrivateGroupPublisher", "Private Group Publishing", false);
    	  DiscoveryService globalPublisher = privateGroupPublisher.getDiscoveryService();
    	  PeerGroupAdvertisement myPG = null;
    	  
    	  if(pwordGroup){
    		  myPG = newPGroupAdvertisement(customGroupInstance.getPeerGroupID(), GroupName, privateGroupPublisher.getAllPurposePeerGroupImplAdvertisement().getModuleSpecID(), PName, desc, pwordGroup, pword);
    	  }else{
    		  myPG = newPGroupAdvertisement(customGroupInstance.getPeerGroupID(), GroupName, privateGroupPublisher.getAllPurposePeerGroupImplAdvertisement().getModuleSpecID(), PName, desc, pwordGroup, "");
    	  }
    	  globalPublisher.publish(myPG);
    	  globalPublisher.remotePublish(myPG);
      }
      
      PopInformationMessage(n, "Connected via Custom Peer Group: " + GroupName + "\nYour Peer Name: " + PName);
      check_timeouts();
	  } catch (IOException e) {
		  JPFSPrinting.logError("IO Exception in P2P Init", errorLevel.SEVERE);
	  } catch (PeerGroupException e) {
		  JPFSPrinting.logError("Group Exception in P2P Init", errorLevel.SEVERE);
	  } catch (Exception e) {
		  JPFSPrinting.logError("General Exception in P2P Init", errorLevel.SEVERE);
	  }
	  
	  
  }
  
  //static method for creating a pipe advertisement
  private static PipeAdvertisement newPipeAdvertisement(PeerGroupID id) {
	  PipeAdvertisement advertisement = (PipeAdvertisement)
      AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
	  
	  if(id==null){
		  advertisement.setPipeID(IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID));
	  }else{
		  advertisement.setPipeID(IDFactory.newPipeID(id));
	  }
      advertisement.setType(PipeService.UnicastType);
      advertisement.setName("JPFSPipe");
      advertisement.setDescription(PName);
      
      return advertisement;
  }
  
  private static PeerGroupAdvertisement newPGroupAdvertisement(PeerGroupID id, String gname, ModuleSpecID msid, String creator, String desc ,boolean usePassword, String password) {
	  PeerGroupAdvertisement advertisement = (PeerGroupAdvertisement)
	  AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
	  
	  advertisement.setPeerGroupID(id);
	  
	  if(usePassword){
		  advertisement.setDescription(gname+"&%"+creator+"&%"+desc+"&%"+true+"&%"+HashingUtil.hash(password.toCharArray()));
	  }else{
		  advertisement.setDescription(gname+"&%"+creator+"&%"+desc+"&%"+false);
	  }
	  
	  advertisement.setName("GroupADV");
	  advertisement.setModuleSpecID(msid);
	  
      return advertisement;
  }
  
  //method to close this client, letting other peers know of the disconnection
  public void close(String n){
	  PopInformationMessage(n, "Stopping JXTA network ... \nPress 'OK'");
	  String empty = "empty";
	  for(int x = 0; x<urns.size(); x++){
			try {
				sendData(peers.get(urns.get(x)),reqType.DELETENOTIFICATION,empty.getBytes("ISO-8859-1"), "", false, null); // send message to all peers to delete me
			} catch (UnsupportedEncodingException e) {
				 JPFSPrinting.logError("General Exception in P2P Init", errorLevel.CAN_IGNORE);
			}
      }
  }
  
  //this thread sits idle until the client is manually closed or crashes
  public void shutdown() throws IOException{
	  try {
		Thread.sleep(5000);
	  } catch (InterruptedException e) {
		  JPFSPrinting.logError("Interrupted Exception in shutdown Thread", errorLevel.CAN_IGNORE);
	  }
	  PopInformationMessage(CliName, "Shutting Down Client ... \nPress 'OK'");
	  if(ConfigurationFile!=null && NetManager!=null){
		  ConfigurationFile.delete();
		  NetManager.stopNetwork();
		  delConfig();
	  }
  }
  
  public static void transmitUpdateFiles(){ // send a message to all peers with new list of files!
	      for(String peer : pnames){
		  if(!peer.contains(PName)){//if we are not looking at ourselves
			  String fileList = GUI_Control.packFiles(peer);
			  System.out.println("Peer Packing: " + peer + " File List: " + fileList);
			  try {
				sendData(peers.get(resolver.get(peer)),reqType.FILELIST,fileList.getBytes("ISO-8859-1"), "", false, null);
			  } catch (UnsupportedEncodingException e) {
				JPFSPrinting.logError("Byte Encoding failed in transmit update files method", errorLevel.RECOVERABLE);
			  }
		  }
	  	  }
  }
  
  public static void transmitUpdateFiles(String peer, OutputPipe location){ // send a message to a single peer with new list of files!
			  String fileList = GUI_Control.packFiles(peer);
			  try {
				  sendData(location,reqType.FILELIST,fileList.getBytes("ISO-8859-1"), "", false, null);
			  } catch (UnsupportedEncodingException e) {
				  JPFSPrinting.logError("Byte Encoding failed in transmit update files method", errorLevel.RECOVERABLE);
			  }
  }
  
  public static void transmitMyPeerCode(OutputPipe loc){
	  try{
		  System.out.println("Sending my peer code");
		  sendData(loc, reqType.PEERCODE, PeerCode.MyHashedCode().getBytes("ISO-8859-1"), "", false, null);
	  }catch (UnsupportedEncodingException e) {
		  JPFSPrinting.logError("Byte Encoding failed in transmit peer code method", errorLevel.RECOVERABLE);
	  }
  }
  
  
  public static void transmitSortRequest(sortType st, String peer){
	  try {
		sendData(peers.get(resolver.get(peer)), reqType.SORTREQ, (st.ordinal()+"").getBytes("ISO-8859-1"), "", false, null);
	  } catch (UnsupportedEncodingException e) {
		  JPFSPrinting.logError("Byte Encoding failed in transmit sort request method", errorLevel.RECOVERABLE);
	  }
  }
  public static void transmitFileRequest(String path, String peer){ // send a message to the respective peer about a file we want to download
	 startTime = System.nanoTime();
	 try {
	 	sendData(peers.get(resolver.get(peer)), reqType.FILEREQ, path.getBytes("ISO-8859-1"),"", false, null);
	 } catch (UnsupportedEncodingException e) {
		 JPFSPrinting.logError("Byte Encoding failed in transmit file request method", errorLevel.RECOVERABLE);
	 }
  }
  
  
  
  public void discoveryEvent(DiscoveryEvent TheDiscoveryEvent) {
      // Who triggered the event?
      DiscoveryResponseMsg TheDiscoveryResponseMsg = TheDiscoveryEvent.getResponse();
      Enumeration<Advertisement> en = TheDiscoveryResponseMsg.getAdvertisements();
      Object thePeer = TheDiscoveryEvent.getSource();
      
      //if the peer isn't discovered already
      if(!peers.containsKey(thePeer) && thePeer!=null){
      if (en!=null) { 
          while (en.hasMoreElements()) {
        	  Advertisement ad = en.nextElement();
        	  if(ad.getAdvType() == PipeAdvertisement.getAdvertisementType()){

                  
                    foundPeer = TheDiscoveryEvent.getSource();
                    foundPeerN = ((PipeAdvertisement) ad).getDescription();
                    
                    try {
						pipe_service.createOutputPipe((PipeAdvertisement) ad, this);
					} catch (IOException e) {
						JPFSPrinting.logError("Error Creating Output Pipe for Discovered Peer", errorLevel.RECOVERABLE);
        	        }
                    pnames.add(((PipeAdvertisement) ad).getDescription());
                    GUI_Control.addPeer(((PipeAdvertisement) ad).getDescription());
                    urns.add(foundPeer); // store the urns of the peers to be used as indexing in the 
                    resolver.put(((PipeAdvertisement) ad).getDescription(), foundPeer); // add peer name to urn store
        	  }
          }
          Set<String> s = new LinkedHashSet<>(pnames);
          pnames.clear();
          pnames.addAll(s); // remove duplicates
      }
      }
  }
  
  String getCompName(){
  	  String hostname = "You";

  	  try
  	  {
  	      InetAddress addr;
  	      addr = InetAddress.getLocalHost();
  	      hostname = addr.getHostName();
  	  }
  	  catch (UnknownHostException ex)
  	  {
  		JPFSPrinting.logWarning("Host Name Resolution in getCompName Method Failed");
  	  }
  	  return hostname;
  }
  
  
  //light thread which loops for advertisements
  public void fetch_advertisements() {
      new Thread("JPFS2: Search for Advertisements") {
         public void run() {
        	try {
				sleep(2000); // wait a couple seconds before getting advertisements
			} catch (InterruptedException e1) { }
            while(true) {
            	try{
            	if(discovery != null)
                  discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", "JPFSPipe", 1, null);
            	else
            		JPFSPrinting.logError("Error Initializing the Discovery Service for Advertisements", errorLevel.SEVERE);
            	
                sleep(5000);
              
            	} catch(InterruptedException e) {} 
                catch(IllegalStateException e) {
                	JPFSPrinting.logWarning("Discovery thread fetch_advertisements being skipped");
            	}
            }
         }
      }.start();
   }
  
  
private static void sendData(OutputPipe target, reqType req, byte[] input, String fn, boolean useChecksum, String path) {
      Message msg = new Message();
      MessageElement reqType = null;
      MessageElement data = null;
      MessageElement myurn = null;
      MessageElement fina = null;
      MessageElement cs = null;
      reqType = new StringMessageElement("Request", Integer.toString(req.ordinal()), null);
	  
	  myurn = new StringMessageElement("PName", PName ,null);
	  fina = new StringMessageElement("FileName", fn, null);
	  if(useChecksum){
		  cs = new StringMessageElement("Checksum", ChecksumUtil.generateMD5(path),null);
		  msg.addMessageElement(cs);
	  }
	  data = new ByteArrayMessageElement("Data", null, input, null);

      msg.addMessageElement(reqType);
      msg.addMessageElement(data);
      msg.addMessageElement(myurn);
      msg.addMessageElement(fina);

      try {
          target.send(msg);
      } catch (IOException e) {
    	  JPFSPrinting.logError("Error sending message over input pipe: " + target.toString(), errorLevel.RECOVERABLE);
      }
  }

private static void sendData(OutputPipe target, reqType req, byte[] input, String fn, boolean useChecksum, String path, boolean useSigning) {
    Message msg = new Message();
    MessageElement reqType = null;
    MessageElement data = null;
    MessageElement myurn = null;
    MessageElement fina = null;
    MessageElement cs = null;
    MessageElement pubKey = null;
    MessageElement signed = null;
    reqType = new StringMessageElement("Request", Integer.toString(req.ordinal()), null);
	  
	  myurn = new StringMessageElement("PName", PName ,null);
	  fina = new StringMessageElement("FileName", fn, null);
	  if(useChecksum){
		  cs = new StringMessageElement("Checksum", ChecksumUtil.generateMD5(path),null);
		  msg.addMessageElement(cs);
	  }
	  if(useSigning){
		  SignedContents sc = SigningUtil.signData(input);
		  pubKey = new ByteArrayMessageElement("PKey", null, sc.pk.getEncoded(), null);
		  signed = new ByteArrayMessageElement("Signature", null, sc.signature, null);
		  msg.addMessageElement(pubKey);
		  msg.addMessageElement(signed);
	  }
	  data = new ByteArrayMessageElement("Data", null, input, null);

    msg.addMessageElement(reqType);
    msg.addMessageElement(data);
    msg.addMessageElement(myurn);
    msg.addMessageElement(fina);

    try {
        target.send(msg);
    } catch (IOException e) {
    	JPFSPrinting.logError("Error sending message over input pipe: " + target.toString(), errorLevel.RECOVERABLE);
    }
}

  
  
  @Override
  public void pipeMsgEvent(PipeMsgEvent event) {
      try {
          Message msg = event.getMessage();
          byte[] dataBytes = msg.getMessageElement("Data").getBytes(true);  
          byte[] reqBytes = msg.getMessageElement("Request").getBytes(true); 
          byte[] nameBytes = msg.getMessageElement("PName").getBytes(true); 
          byte[] fnBytes = msg.getMessageElement("FileName").getBytes(true); 
          String temp = new String(reqBytes);
          int req = Integer.parseInt(temp);
          reqType request = reqType.values()[req];
          String theirName = new String(nameBytes);
          String data;
          JPFSPrinting.printMessageHeader(request, theirName, dataBytes);
          if(request == reqType.FILERES){ // they sent us a file back!
        	  byte[] csbytes = msg.getMessageElement("Checksum").getBytes(true); 
        	  String checksum = new String(csbytes);
        	  X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(msg.getMessageElement("PKey").getBytes(true));
        	  KeyFactory kf = KeyFactory.getInstance("DSA", "SUN");
        	  SignedContents sc = new SignedContents(kf.generatePublic(pkSpec), msg.getMessageElement("Signature").getBytes(true));
        	  handleFile(dataBytes, new String(fnBytes), checksum, sc); //handle the bytes
        	  return;
          }else if(request == reqType.FILEINFORES){ // we got a response about file info
        	  File f = new File(".\\temp\\"+FilenameUtils.getBaseName(new String(fnBytes))+".ser");
        	  f.createNewFile();
        	  FileOutputStream fos = new FileOutputStream(f);
        	  byte[] decryp = EncryptUtil.decryptBits(eKey, eVector, dataBytes);
        	  fos.write(decryp);
        	  FileInputStream fis = new FileInputStream(f);
        	  ObjectInputStream ois = new ObjectInputStream(fis);
        	  JPFSFile fInfo = (JPFSFile)ois.readObject();
        	  ois.close();
        	  fis.close();
        	  fos.close();
        	  displayFileInfo(fInfo, false);
        	  return;
          }else if(request == reqType.FILEINFOREQ){
        	  handleReq(new String(fnBytes),request,theirName);
        	  return;
          }
          data = new String(dataBytes);
          handleReq(data,request,theirName);
      }
      catch (Exception e) {
    	  e.printStackTrace();
    	  
    	  JPFSPrinting.logError("Error reading message from output pipe", errorLevel.RECOVERABLE);
      }
  }
  
  private void handleFile(byte[] fileBytes, String fname, String checksumCheck, SignedContents sc) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException{
		  FileOutputStream fos; // file stream for creating new file
		  byte[] decrypted = EncryptUtil.decryptBits(eKey, eVector, fileBytes);
		  String resultantDialog = "";
		  boolean discard = false;
		 
			   try {
					fos = new FileOutputStream(".\\JPFSDownloads\\"+fname);  // create the new file
					fos.write(decrypted);
					fos.close();
				   } catch (IOException e) {
					   JPFSPrinting.logError("Error writing to the Downloads Folder [Missing Folder or No Permissions?]", errorLevel.RECOVERABLE);
				}
			   
		  
		   endTime = System.nanoTime();
		   int len = fileBytes.length;
		   JPFSPrinting.printBench(startTime,endTime, len);  
		   String newFileChecksum = ChecksumUtil.generateMD5(".\\JPFSDownloads\\"+fname);
		   
		   if(SigningUtil.verifySignature(sc.signature, fileBytes, sc.pk)){
			   resultantDialog+="The Downloaded Files Origin: [Signature Properly Signed]\nFile is from Correct Sender";
		   }else{
			   resultantDialog+="The Downloaded Files Origin: [Signature Unsigned / Wrong Signature]\nFile is from Unverified Sender.";
			   JPFSPrinting.logWarning("Downloaded File had invalid digital signature");
			   discard = true;
		   }
		   
		   
		   if(newFileChecksum.equals(checksumCheck)){
			   resultantDialog+="\n\nThe Downloaded Files Checksum is: [Validated]\nFile contents are Uncorrupted";
		   }else{
			   resultantDialog+="\n\nThe Downloaded Files Checksum is: [Invalid - Corrupt]\nFile contents Corrupted.";
			   JPFSPrinting.logWarning("Downloaded File had invalid checksum - corrupt");
			   discard = true;
		   }
		   
		   if(discard){
			   resultantDialog+="\n\n[FILE DOWNLOAD WAS UNSUCCESSFUL!]";
		   }else{
			   resultantDialog+="\n\n[Download Successful]";
		   }
		   
		   PopInformationMessageNonBlocking("Download Results", resultantDialog);
		   
		   
  }
  
  
  @SuppressWarnings("deprecation")
  private void handleReq(String fName, reqType r, String peerOrigin) throws IOException, InterruptedException{ // handle the req from the other peer
	 
	  if(r == reqType.FILEREQ){ // they want a file from us! get the filepath they want from d, and send back response 
		  File sendFile = new File(GUI_Control.findJPFSFile(fName.trim()).path); // the user input from client is the file name
		  String fn = sendFile.getName();
		  String path = sendFile.getCanonicalPath();
			 byte[] barray = new byte[(int) sendFile.length()]; 
			 FileInputStream fis;
			 try{
				 fis = new FileInputStream(sendFile); // load the file into the stream
				 BufferedInputStream bis = new BufferedInputStream(fis); // buffered stream for flushing the file stream
				 bis.read(barray, 0, barray.length);
				 byte[] encrypted = EncryptUtil.encryptBits(eKey, eVector, barray);
				 sendData(peers.get(resolver.get(peerOrigin)),reqType.FILERES,encrypted, fn, true, path, true); // send the byte array to the client
				 bis.close();
			 } catch(FileNotFoundException ex){ // if the file is not found on the server
				 JPFSPrinting.logError("Could not find the shared file user requested [Was it moved in your computer?]", errorLevel.RECOVERABLE);
				 return;
			 } catch (IOException e) {
				 JPFSPrinting.logError("IO Exception in the handling request method", errorLevel.RECOVERABLE);
			 }
           
	  }
      else if (r == reqType.FILELIST){ // they sent a new file list! we need to parse it and update the list for us
    	 String[] dataParsed = fName.split("&%");
    	 //System.out.println("Got a File List");
    	 GUI_Control.updateFiles(peerOrigin, dataParsed);
    	 GUI_Control_ApplicationLevel.forceFileListUpdate();
	  }else if(r==reqType.PINGREQ){ // we got a ping request for timeouts, so just ping them back
		  sendData(peers.get(resolver.get(peerOrigin)),reqType.PINGRES,"PING".getBytes(), "PING", false, null); // send the byte array to the client
	  }else if(r==reqType.PINGRES){ // we got a ping response, stop the corresp. kill thread
		  synchronized(timeoutLock){
		  System.out.println("PEER PING REQUEST RECEIVED FROM "+peerOrigin);
		  System.out.println("My Threads: "+timeouts);
		  if(timeouts.get(peerOrigin)!=null){
			 
			  System.out.println("Stopping Kill Thread: " + peerOrigin);
			  timeouts.get(peerOrigin).interrupt();
			  timeouts.get(peerOrigin).stop();
			  System.out.println("THE SIZE OF TIMEOUTS MAP BEFORE: "+timeouts.size());
			  if(timeouts.remove(peerOrigin) == null){
				  System.out.println("Nothing in Kill Thread to Remove");
			  }
			  System.out.println("THE SIZE OF TIMEOUTS MAP AFTER: "+timeouts.size());
		  }else{
				  System.out.println("********************NO TIMEOUT THREAD FOUND FOR " + peerOrigin);
	      }
		  }
	  }else if(r==reqType.FILEINFOREQ){//we got a request for info on a file of ours
		  //System.out.println("REQUEST 5, FILE NAME: "+fName);
		  File f = new File(".\\temp\\"+fName+".ser");
		  f.createNewFile();
		  FileOutputStream fos = new FileOutputStream(f);
		  ObjectOutputStream oos = new ObjectOutputStream(fos);
		  JPFSFile theFile = GUI_Control.findJPFSFile(fName);
		  if(theFile!=null){
			  oos.writeObject(theFile);
		  }else{
			  JPFSPrinting.logError("Could not create serialized file for file info request", errorLevel.RECOVERABLE);
			  oos.close();
			  fos.close();
			  return;
		  }
		  oos.close();
		  fos.close();
		  byte[] barray = new byte[(int) f.length()]; 
		  FileInputStream fis;
		  try{
			fis = new FileInputStream(f); // load the file into the stream
			BufferedInputStream bis = new BufferedInputStream(fis); // buffered stream for flushing the file stream
			bis.read(barray, 0, barray.length);
			byte[] encrypted = EncryptUtil.encryptBits(eKey, eVector, barray);
			sendData(peers.get(resolver.get(peerOrigin)),reqType.FILEINFORES,encrypted, fName, false, ""); // send the byte array to the client
			bis.close();
		  } catch(FileNotFoundException ex){ // if the file is not found on the server
			  JPFSPrinting.logError("Could not find serialized file when creating byte array", errorLevel.RECOVERABLE);
			  return;
		  } catch (IOException e) {
			  JPFSPrinting.logError("Could not find the shared file user requested [Was it moved in your computer?]", errorLevel.RECOVERABLE);
		  }
		  f.delete();
	  }else if(r==reqType.SORTREQ){//we got a sorted file list request from one of our peers
		  int sType = Integer.valueOf(fName).intValue();
		  //String newFLPacked = GUI_Control.packFiles(GUI_Control.sortFilesReturn(sortType.values()[sType]));
		  //transmitUpdateFiles(newFLPacked, peers.get(resolver.get(peerOrigin)));
	  }
      else if(r==reqType.DELETENOTIFICATION){ // notification that this peer has closed their connection - we have to remove them from the lists
    	  deletePeer(peerOrigin);
      }else if(r==reqType.PEERCODE){
    	  if(!peerCodes.containsKey(peerOrigin)){
    		  System.out.println("ADDIND A PEER CODE: " + fName + " | " + peerOrigin);
    		  peerCodes.put(peerOrigin, fName);
    	  }
      }else if(r==reqType.VALIDATEREQ){
    	  sendData(peers.get(resolver.get(peerOrigin)), reqType.VALIDATERES, PeerCode.MyHashedCode().getBytes("ISO-8859-1"), "", false, "");
      }else if(r==reqType.VALIDATERES){
    	  if(fName.equals(peerCodes.get(peerOrigin))){
    		  PopInformationMessage("Validation Results", "Peer is [VALIDATED]\nIdentified as original Peer Source");
    	  }else{
    		  PopInformationMessage("Validation Results", "Peer is [IN-VALIDATED]\nPeer is Unknown (Group may be Breached)");
    	  }
      }
  }
  
@SuppressWarnings("deprecation")
private void deletePeer(String n){
	  JPFSPrinting.printDeletedPeer();
	  GUI_Control.removePeer(n);
	  pnames.remove(n);
}



  @Override
  public void outputPipeEvent(OutputPipeEvent arg0) {
	if(!peers.containsKey(foundPeer)){
		OutputPipe newPipe = arg0.getOutputPipe();
		System.out.println("PEER OUTPUT PIPE EVENT " + foundPeer);
		peers.put(foundPeer, newPipe);
		transmitUpdateFiles(foundPeerN,newPipe); // send our new peer our existing files!
		transmitMyPeerCode(newPipe);
		JPFSPrinting.printNewPeer(foundPeerN);
	}
	
  }
  
  
  
  
   //pings all peers every 20 seconds, and removes any peers who do not respond with a time of S
   public void check_timeouts() {
      Thread newThread = new Thread(new timeout());
      newThread.start();
   }
   private class timeout implements Runnable{
	   public void run() {
           while(true) {
           	try{
           		Thread.sleep(30000);//sleep for 20 seconds
           		//timeout logic
           		System.out.println("PNAMES SIZE IN TIMEOUT " + pnames.size());
           		for(int x = 0; x<pnames.size(); x++){
           			if(pnames.get(x)!=null && !timeouts.containsKey(pnames.get(x))){ // if we already have a kill thread going, don't start another
           			if(!pnames.get(x).contains(PName)){
           				sendData(peers.get(resolver.get(pnames.get(x))),reqType.PINGREQ,"PING".getBytes(), "PING", false, null); // send a ping
           				kill_thread(pnames.get(x), 10000); // kill peer if they dont ping back in 15 seconds
           			}
           			}else{
           			if(pnames.get(x)==null){
           				JPFSPrinting.logError("Error fetching a peer to ping [They were deleted?]", errorLevel.CAN_IGNORE);
           			}else if(timeouts.containsKey(pnames.get(x))){
           				JPFSPrinting.logWarning("Thread already running for peer timeout [Already in Kill Thread]" + pnames.get(x));
           			}
           			}
           		}
           	} catch(InterruptedException e) {
           		JPFSPrinting.logError("Sleep method was interrupted in the timeouts method", errorLevel.CAN_IGNORE);
           	} catch(IllegalStateException e) {
           		JPFSPrinting.logError("Timeout thread was skipped: Not Ready", errorLevel.RECOVERABLE);
           		e.printStackTrace();
           	}
           }
        }
   }
  
  
  //start a thread that will delete a peer from your list after delay time (for timeouts)
  public void kill_thread(String pnameToKill, long delay){
	System.out.println("CREATING KILL THREAD FOR " + pnameToKill);
	kill newKill = new kill(pnameToKill, delay);
	Thread newThr = new Thread(newKill);
	timeouts.put(pnameToKill, newThr);
	newThr.start();
  }
  private class kill implements Runnable{
	  private long delay;
	  private String peer;
	  kill(String p, long d){
		  peer = p;
		  delay = d;
	  }
	  public void run(){
		  try {
			Thread.sleep(delay);
			JPFSPrinting.logWarning("A Peer Timed Out ["+peer+"]");
			deletePeer(peer);
		  } catch (InterruptedException e) {
			  System.out.println("INTERRUPTED IN KILL THREAD");
			//interrupted is expected behaviour for stopping the kill threads
		  }
	  }
  }
 
  
  
  
  
  //UTILITY FUNCTIONS

  public List<String> getPeers(){
	  return pnames;
  }

  public int getLen(){
	return peers.size();
  }
  
  public static void PopInformationMessage(String Name, String Message) {   
    JOptionPane.showMessageDialog(null, Message, Name, JOptionPane.INFORMATION_MESSAGE); 
  }
  
  public static int PopCustomTwoButtons(String Name, String Question, String label0, String label1){
	  return JOptionPane.showOptionDialog(null, Question, Name, JOptionPane.OK_CANCEL_OPTION, 
			  JOptionPane.INFORMATION_MESSAGE, null, new String[]{label0, label1}, "default");
  }

  public static int PopYesNoQuestion(String Name, String Question) {    
    return JOptionPane.showConfirmDialog(null, Question, Name, JOptionPane.YES_NO_OPTION);   
  }
  
  public static void PopInformationMessageNonBlocking(String Name, String Message){
	  new Thread("Dialog Info Thread"){
		  public void run(){
			  JOptionPane.showMessageDialog(null, Message, Name, JOptionPane.INFORMATION_MESSAGE);
		  }
	  }.start();
  }

  public void delConfig() throws IOException{
	NetworkManager.RecursiveDelete(ConfigurationFile);
	File config = new File("./" + PName);
	deleteDirectory(config);
  }

  static public boolean deleteDirectory(File path) {
    if( path.exists() ) {
      File[] files = path.listFiles();
      for(int i=0; i<files.length; i++) {
         if(files[i].isDirectory()) {
           deleteDirectory(files[i]);
         }
         else {
           files[i].delete();
         }
      }
    }
    return(path.delete());
  }
  
  public void lookupPeerInfo(String pname){
	  if(pname == pnames.get(0)){
		  PopInformationMessage("You", "This is You");
		  return;
	  }
	  
	  String builtString = "Peer Name: " + pname + "\n";
	  builtString += "URN: " + resolver.get(pname) + "\n";
	  builtString += "Shared Files: " + GUI_Control.numFiles(pname) + "\n";
	  builtString += "Output Pipe (Name): " + peers.get(resolver.get(pname)).getName() + "\n";
	  builtString += "Output Pipe (Type): " + peers.get(resolver.get(pname)).getType() + "\n";
	  builtString += "Validation Peer Code: " + peerCodes.get(pname) + "\n";
	  if(!peers.get(resolver.get(pname)).isClosed()){
		  builtString += "Connection Status: Active\n";
	  }else{
		  builtString += "Connection Status: Closed\n";
	  }
	  
	  //PopInformationMessage(pname + " Info", ""+builtString);
	  int choice = PopCustomTwoButtons(pname + " Info", builtString, "Validate Peer", "Close");
	  if(choice == 0){ // validation req
		  sendData(peers.get(resolver.get(pname)), reqType.VALIDATEREQ, "".getBytes(), "", false, "");
	  }
  }
  
  public void lookupFileInfo(String filename, String selectedPeer){
	  //TODO:
	  //FIX THE LOOKUP OF OTHER PEOPLES FILE -> NEED TO MAINTAIN A JPFSFILE ARRAY OF OUR PEERS FILES
	  if(selectedPeer == pnames.get(0) || selectedPeer == null){
		  JPFSFile temp = GUI_Control.findJPFSFile(filename);
		  if(temp!=null){
			  displayFileInfo(temp, true);
		  }
		  return;
	  }else{
		  byte[] tmp = new byte[1];
		  sendData(peers.get(resolver.get(selectedPeer)), reqType.FILEINFOREQ, tmp, filename, false, "");
	  }
  }
  
  public static void displayFileInfo(JPFSFile temp, boolean printPath){
	  if(printPath){
		  PopInformationMessageNonBlocking("File Info Dialog", "File Name: " + temp.name + "\nFile Path: " + temp.path + "\nFile Size (B): "+temp.size.getByteSize()
			  + "\nFile Size (KB): "+temp.size.getKByteSize()
			  + "\nFile Size (MB): "+temp.size.getMByteSize()
			  + "\nFile Size (GB): "+temp.size.getGByteSize()
			  + "\nExtension: "+temp.extension
			  + "\nUploaded On: "+temp.date);
	  }else{
		  PopInformationMessageNonBlocking("File Info Dialog", "File Name: " + temp.name + "\nFile Size (B): "+temp.size.getByteSize()
				  + "\nFile Size (KB): "+temp.size.getKByteSize()
				  + "\nFile Size (MB): "+temp.size.getMByteSize()
				  + "\nFile Size (GB): "+temp.size.getGByteSize()
				  + "\nExtension: "+temp.extension
				  + "\nUploaded On: "+temp.date);
	  }
  }

}
