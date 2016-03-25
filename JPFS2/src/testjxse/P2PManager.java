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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;

import testjxse.GUI_Control.sortType;
import testjxse.SigningUtil.SignedContents;

public class P2PManager implements DiscoveryListener, PipeMsgListener, OutputPipeListener{

  private DiscoveryService discovery = null;
  public NetworkManager NetManager;
  public PeerGroup PGroup;
  public NetworkConfigurator Config;
  public static File ConfigurationFile;
  private PeerID MyPeerID;
  
  private static String PName;
  private String CliName;
  
  private PipeService pipe_service;
  private PipeAdvertisement myPipeAdv = null;
  private PeerGroupAdvertisement myGroupAdv = null;
  @SuppressWarnings("unused") // it is used, false flag
  private InputPipe myInput = null;
  private static Map<Object,OutputPipe> peers = new Hashtable<Object,OutputPipe>(); // return output pipes from urns
  private static final int MAXPEERS = 25; // maximum number of peers on client
  private static Object urns [] = new Object[MAXPEERS]; // store an array of found peer urns (for iteration)
  private static Map<String,Object> resolver = new Hashtable<String,Object>(); // resolve peer names to their urns
  private String pnames [] = new String[MAXPEERS]; // store an array of found peer names (for GUI)
  private int plen=1;
  private static int ulen=0;
  private static Map<String, Thread> timeouts = new Hashtable<String, Thread>();
  
  private Object foundPeer;
  
  //for benchmark testing
  public static long startTime;
  public static long endTime;
  
  //for encryption - These values could be randomly generated but are specified for the purposes of demonstration
  private static String eKey = "JPFS2KEYJPFS2YEK"; // 128-Bit Cipher Key for Demonstration Purposes. 
  private static String eVector = "InitializeVector"; // 16-Bit Init Vector for Demonstration Purposes.
  
  
  public void initGlobal(String n){
	  try{
		  System.out.println("Initializing JPFS Client");
		  CliName = n; // the clients name "J_PFS"
		  String id = UUID.randomUUID().toString().substring(0, 8);
          PName = getCompName() + " (ID: "+ id + ")"; // the host name is this users peer name + random int id for uniqueness
          String filePName = getCompName() + id;
          pnames[0] = PName + " (You)"; // display yourself in the peer list 
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
          
	      /*if(!op.usingCus){
              PopInformationMessage(n, "Connected via Peer Group: " + "Global Peer Lobby" + "\nPeer Name: " + PName);
	      }
	      else{*/
	    	  PopInformationMessage(n, "Connected via Peer Group: " + PGroup.getPeerGroupName() + "\nPeer Name: " + PName);
	      //}
	  } catch(IOException e){
		  System.out.println("IO Exception on Network Manager INIT :" + e);
	  } catch(PeerGroupException e){
		  System.out.println("Peer Group Error :" + e);
	  } catch(Exception e){
		  System.out.println("General Exception :" + e);
	  }
  }
  
  public void initCustom(String n, String GroupName, boolean Creator, String desc){
	  try{
	  System.out.println("Initializing JPFS Client");
	  CliName = n; // the clients name "J_PFS"
	  String id = UUID.randomUUID().toString().substring(0, 8);
      PName = getCompName() + " (ID: "+id + ")"; // the host name is this users peer name + random int id for uniqueness
      String filePName = getCompName() + id;
      pnames[0] = PName + " (You)"; // display yourself in the peer list 
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
	  System.out.println(customGroupInstance.getPeerGroupName());
	
	  
	  
	  //Check to see if client successfully joined the peergroup
      if (Module.START_OK != customGroupInstance.startApp(new String[0])){
          System.err.println("Cannot start child peergroup");
          System.exit(1);
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
      PeerGroupAdvertisement myPG = newPGroupAdvertisement(customGroupInstance.getPeerGroupID(), GroupName, privateGroupPublisher.getAllPurposePeerGroupImplAdvertisement().getModuleSpecID());
      globalPublisher.publish(myPG);
      globalPublisher.remotePublish(myPG);
      }
      
      
	  } catch (IOException e) {
		e.printStackTrace();
	  } catch (PeerGroupException e) {
		e.printStackTrace();
	  } catch (Exception e) {
		 e.printStackTrace();
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
  
  private static PeerGroupAdvertisement newPGroupAdvertisement(PeerGroupID id, String gname, ModuleSpecID msid) {
	  PeerGroupAdvertisement advertisement = (PeerGroupAdvertisement)
	  AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
	  
	  advertisement.setPeerGroupID(id);
	  advertisement.setDescription(gname);
	  advertisement.setName("GroupADV");
	  advertisement.setModuleSpecID(msid);
	  
      return advertisement;
  }
  
  //method to close this client, letting other peers know of the disconnection
  void close(String n){
	  PopInformationMessage(n, "Stopping JXTA network ... \nPress 'OK'");
	  String empty = "empty";
	  for(int x = 0; x<ulen; x++){
			try {
				sendData(peers.get(urns[x]),100,empty.getBytes("ISO-8859-1"), "", false, null); // send message to all peers to delete me
			} catch (UnsupportedEncodingException e) {
				System.out.println("Encoding the String to Bytes Failed!");
			}
      }
  }
  
  //this thread sits idle until the client is manually closed or crashes
  public void shutdown() throws IOException{
	  try {
		Thread.sleep(5000);
	  } catch (InterruptedException e) {
		e.printStackTrace();
	  }
	  PopInformationMessage(CliName, "Shutting Down Client ... \nPress 'OK'");
	  ConfigurationFile.delete();
	  NetManager.stopNetwork();
	  delConfig();
  }
  
  
  public static void transmitUpdateFiles(String Files){ // send a message to all peers with new list of files!
	  for(int x = 0; x<ulen; x++){
		try {
			sendData(peers.get(urns[x]),2,Files.getBytes("ISO-8859-1"), "", false, null);
		} catch (UnsupportedEncodingException e) {
			System.out.println("Encoding the String to Bytes Failed!");
		}
	  }
  }
  public static void transmitUpdateFiles(String Files, OutputPipe location){ // send a message to a single peer with new list of files!
	  for(int x = 0; x<ulen; x++){
		try {
			sendData(location,2,Files.getBytes("ISO-8859-1"),"", false, null);
		} catch (UnsupportedEncodingException e) {
			System.out.println("Encoding the String to Bytes Failed!");
		}
	  }
  }
  
  
  public static void transmitSortRequest(sortType st, String peer){
	  try {
		sendData(peers.get(resolver.get(peer)), 7, (st.ordinal()+"").getBytes("ISO-8859-1"), "", false, null);
	  } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	  }
  }
  public static void transmitFileRequest(String path, String peer){ // send a message to the respective peer about a file we want to download
	 startTime = System.nanoTime();
	 try {
	 	sendData(peers.get(resolver.get(peer)), 0, path.getBytes("ISO-8859-1"),"", false, null);
	 } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	 }
  }
  
  
  
  public void discoveryEvent(DiscoveryEvent TheDiscoveryEvent) {
      // Who triggered the event?
      DiscoveryResponseMsg TheDiscoveryResponseMsg = TheDiscoveryEvent.getResponse();
      Enumeration<Advertisement> en = TheDiscoveryResponseMsg.getAdvertisements();
      Object thePeer = TheDiscoveryEvent.getSource();
      
      //if the peer isn't discovered already
      if(!peers.containsKey(thePeer)){
      if (en!=null) { 
          while (en.hasMoreElements()) {
        	  Advertisement ad = en.nextElement();
        	  if(ad.getAdvType() == PipeAdvertisement.getAdvertisementType()){
                    pnames[plen] = ((PipeAdvertisement) ad).getDescription();
                    plen++;
                    GUI_Control.addPeer(((PipeAdvertisement) ad).getDescription());
                    foundPeer = TheDiscoveryEvent.getSource();
                    urns[ulen] = foundPeer; // store the urns of the peers to be used as indexing in the 
                    ulen++;
                    resolver.put(((PipeAdvertisement) ad).getDescription(), foundPeer); // add peer name to urn store
                    try {
						pipe_service.createOutputPipe((PipeAdvertisement) ad, this);
					} catch (IOException e) {
						e.printStackTrace();
					    System.out.println("ERROR CREATING OUTPUT PIPE!");
        	        }
        	  }
          }     
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
  	      System.out.println("Hostname can not be resolved");
  	  }
  	  return hostname;
  }
  
  
  //light thread which loops for advertisements
  public void fetch_advertisements() {
      new Thread("JPFS2: Search for Advertisements") {
         public void run() {
            while(true) {
            	try{
            	if(discovery != null)
                  discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", "JPFSPipe", 1, null);
            	else
            	  System.out.println("Discovery Did not Get Initialized");
            	
                sleep(1000);
              
            	} catch(InterruptedException e) {} 
                catch(IllegalStateException e) {
            		System.out.println("The Discovery Thread is being Skipped: Thread not Ready!");
            	}
            }
         }
      }.start();
   }
  
  
private static void sendData(OutputPipe target, int req, byte[] input, String fn, boolean useChecksum, String path) {
      Message msg = new Message();
      MessageElement reqType = null;
      MessageElement data = null;
      MessageElement myurn = null;
      MessageElement fina = null;
      MessageElement cs = null;
      reqType = new StringMessageElement("Request", Integer.toString(req), null);
	  
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
          System.out.println("Couldn't Write to the Output Pipe!");
      }
  }

private static void sendData(OutputPipe target, int req, byte[] input, String fn, boolean useChecksum, String path, boolean useSigning) {
    Message msg = new Message();
    MessageElement reqType = null;
    MessageElement data = null;
    MessageElement myurn = null;
    MessageElement fina = null;
    MessageElement cs = null;
    MessageElement pubKey = null;
    MessageElement signed = null;
    reqType = new StringMessageElement("Request", Integer.toString(req), null);
	  
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
        System.out.println("Couldn't Write to the Output Pipe!");
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
          int request = Integer.parseInt(temp);
          String theirName = new String(nameBytes);
          String data;
          if(request == 1){ // they sent us a file back!
        	  byte[] csbytes = msg.getMessageElement("Checksum").getBytes(true); 
        	  String checksum = new String(csbytes);
        	  X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(msg.getMessageElement("PKey").getBytes(true));
        	  KeyFactory kf = KeyFactory.getInstance("DSA", "SUN");
        	  SignedContents sc = new SignedContents(kf.generatePublic(pkSpec), msg.getMessageElement("Signature").getBytes(true));
        	  handleFile(dataBytes, new String(fnBytes), checksum, sc); //handle the bytes
        	  return;
          }else if(request == 6){ // we got a response about file info
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
        	  printFileInfo(fInfo, false);
        	  //f.delete();
        	  return;
          }else if(request == 5){
        	  handleReq(new String(fnBytes),request,theirName);
        	  return;
          }
          data = new String(dataBytes);
          
          
          System.out.println("Rec: Data: " + data + " Request Type: " + request + "Their Peer Name: " + theirName);
          handleReq(data,request,theirName);
      }
      catch (Exception e) {
          //an error was thrown trying to read from the pipe
          e.printStackTrace();
      }
  }
  
  private void handleFile(byte[] fileBytes, String fname, String checksumCheck, SignedContents sc) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException{
		  System.out.println("Got a File Response");
		  FileOutputStream fos; // file stream for creating new file
		  byte[] decrypted = EncryptUtil.decryptBits(eKey, eVector, fileBytes);
		  //fname is the path here
		  String resultantDialog = "";
		  boolean discard = false;
		   
		 
			   try {
					fos = new FileOutputStream(".\\JPFSDownloads\\"+fname);  // create the new file
					fos.write(decrypted);
					fos.close();
				   } catch (IOException e) {
					  e.printStackTrace();
				}
			   
		  
		   endTime = System.nanoTime();
		   int len = fileBytes.length;
		   printBench(startTime,endTime, len);
		   System.out.println("Downloaded file name: "+fname);
		   String newFileChecksum = ChecksumUtil.generateMD5(".\\JPFSDownloads\\"+fname);
		   System.out.println("Downloaded Checksum: "+newFileChecksum);
		   System.out.println("Orig Checksum: " + checksumCheck);
		   
		   if(SigningUtil.verifySignature(sc.signature, fileBytes, sc.pk)){
			   resultantDialog+="The Downloaded Files Origin: [Signature Properly Signed]\nFile is from Correct Sender";
		   }else{
			   resultantDialog+="The Downloaded Files Origin: [Signature Unsigned / Wrong Signature]\nFile is from Unverified Sender.";
			   discard = true;
		   }
		   
		   
		   if(newFileChecksum.equals(checksumCheck)){
			   resultantDialog+="\n\nThe Downloaded Files Checksum is: [Validated]\nFile contents are Uncorrupted";
		   }else{
			   resultantDialog+="\n\nThe Downloaded Files Checksum is: [Invalid - Corrupt]\nFile contents Corrupted.";
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
private void handleReq(String fName, int r, String peerOrigin) throws IOException{ // handle the req from the other peer
	  if(r == 0){ // they want a file from us! get the filepath they want from d, and send back response 
		  System.out.println("Got a File Request");
		  System.out.println("User Requested: " + fName);
		  //we need to assemble the file, then send a response the the sender with the bytes
		  //the file path is in the input from the message
		  System.out.println(GUI_Control.findJPFSFile(fName.trim()));
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
				 sendData(peers.get(resolver.get(peerOrigin)),1,encrypted, fn, true, path, true); // send the byte array to the client
				 bis.close();
			 } catch(FileNotFoundException ex){ // if the file is not found on the server
				 System.out.println("Could not find the file specified\n");
				 return;
			 } catch (IOException e) {
				e.printStackTrace();
			 }
           
	  }
      else if (r == 2){ // they sent a new file list! we need to parse it and update the list for us
    	 String[] dataParsed = fName.split("&%");
    	 System.out.println("Got a File List");
    	 GUI_Control.updateFiles(peerOrigin, dataParsed);
    	 GUI_Control_ApplicationLevel.forceFileListUpdate();
	  }else if(r==3){ // we got a ping request for timeouts, so just ping them back
		  sendData(peers.get(resolver.get(peerOrigin)),4,"PING".getBytes(), "PING", false, null); // send the byte array to the client
	  }else if(r==4){ // we got a ping response, stop the corresp. kill thread
		  timeouts.get(peerOrigin).stop();
		  timeouts.remove(peerOrigin);
		  System.out.println("Got Pinged Back!");
	  }else if(r==5){//we got a request for info on a file of ours
		  System.out.println("REQUEST 5, FILE NAME: "+fName);
		  File f = new File(".\\temp\\"+fName+".ser");
		  f.createNewFile();
		  FileOutputStream fos = new FileOutputStream(f);
		  ObjectOutputStream oos = new ObjectOutputStream(fos);
		  JPFSFile theFile = GUI_Control.findJPFSFile(fName);
		  if(theFile!=null){
			  oos.writeObject(theFile);
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
			sendData(peers.get(resolver.get(peerOrigin)),6,encrypted, fName, false, ""); // send the byte array to the client
			bis.close();
		  } catch(FileNotFoundException ex){ // if the file is not found on the server
			  System.out.println("Could not find the file specified\n");
			  return;
		  } catch (IOException e) {
				e.printStackTrace();
		  }
		  f.delete();
	  }else if(r==7){//we got a sorted file list request from one of our peers
		  //FNAME HAS THE SORT ENUM TYPE IN IT
		  //METHOD IN GUI CONTROL SORTS THE LIST WITH A RETURN VALUE
		  //GET THE RETURN VALUE AND PACKAGE IT LIKE A NORMAL FILE LIST WITH THE PROPER REQ TYPE
		  //DONE
		  int sType = Integer.valueOf(fName).intValue();
		  String newFLPacked = GUI_Control.packFiles(GUI_Control.sortFilesReturn(sortType.values()[sType]));
		  transmitUpdateFiles(newFLPacked, peers.get(resolver.get(peerOrigin)));
	  }
      else{ // notification that this peer has closed their connection - we have to remove them from the lists
    	  System.out.println("Got a Delete Req");
    	  deletePeer(peerOrigin);
      }
  }
  
  private void deletePeer(String n){
	  notifyClientDeletedPeer();
	  GUI_Control.removePeer(n);
	  String[] temp = pnames;
	  int ind = GUI_Control.indexOf(n);
	  if(ind == -1){
		  System.out.println("Issue Removing PName");
		  return;
	  }
	  temp[ind] = null;
	  pnames = new String[MAXPEERS]; // clear out our list
	  boolean flag = false;
	  for(int x = 0; x<plen; x++){
		  if(temp[x] == null){
			 flag = true;
			 continue;
		  }
		  if (flag == true){ // all additions now are -1
			  pnames[x-1] = temp[x];
		  }
		  else
			  pnames[x] = temp[x];
	  }
	  plen--;
  }

  @Override
  public void outputPipeEvent(OutputPipeEvent arg0) {
	OutputPipe newPipe = arg0.getOutputPipe();
	peers.put(foundPeer, newPipe);
	transmitUpdateFiles(GUI_Control.getFiles(),newPipe); // send our new peer our existing files!
	notifyClientNewPeer();
  }
  
  //pings all peers every 20 seconds, and removes any peers who do not respond with a time of S
  public void check_timeouts() {
      new Thread("JPFS2: Check Timeouts") {
         public void run() {
            while(true) {
            	try{
            		sleep(20000);
            		//timeout logic
            		for(int x = 1; x<pnames.length; x++){
            			if(pnames[x]!=null){
            				System.out.println("I am: " + PName + " Pinging: " + pnames[x]);
            				sendData(peers.get(resolver.get(pnames[x])),3,"PING".getBytes(), "PING", false, null); // send a ping
            				kill_thread(pnames[x], 5000);
            			}
            		}
            		
            		
            	} catch(InterruptedException e) {
            		e.printStackTrace();
            	} catch(IllegalStateException e) {
            		System.out.println("The Timeout Thread is being Skipped: Thread not Ready!");
            		e.printStackTrace();
            	}
            }
         }
      }.start();
   }
  
  
  //start a thread that will delete a peer from your list after delay time (for timeouts)
  public void kill_thread(String pnameToKill, long delay){
	  Thread newThr = new Thread(new kill(pnameToKill, delay));
	  if(!timeouts.containsKey(pnameToKill)){
		  System.out.println("Starting New Kill Thread: " + pnameToKill);
		  timeouts.put(pnameToKill, newThr);
		  newThr.start();
	  }
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
			System.out.println("In Kill Thread: Started!");
			Thread.sleep(delay);
			System.out.println("In Kill Thread: Deleting!!");
			deletePeer(peer);
		  } catch (InterruptedException e) {
			e.printStackTrace();
		  }
	  }
  }
 
  
  
  
  
  //UTILITY FUNCTIONS

  public String[] getPeers(){
	return pnames;
  }

  public int getLen(){
	return plen;
  }
  
  public void notifyClientNewPeer(){
	System.out.println("************************************************");
	System.out.println("\n- A New Peer Was Found and Added to the List");
	System.out.println("* CREATED A NEW OUTPUT PIPE FOR THE PEER *");
	System.out.println("- The Peer List has been Updated!\n");
	System.out.println("************************************************");
  }

  public void notifyClientDeletedPeer(){
	System.out.println("************************************************");
	System.out.println("\n- One or more of your Peers has closed their connection!");
	System.out.println("* DELETING THEIR PEER OUTPUT PIPE *");
	System.out.println("- The Peer List has been Updated!\n");
	System.out.println("************************************************");
  }
  
  public static void PopInformationMessage(String Name, String Message) {   
    JOptionPane.showMessageDialog(null, Message, Name, JOptionPane.INFORMATION_MESSAGE); 
  }
  
  public static Thread PopGroupWaitingBox(){
	  Thread started = new Thread(new NoButtonDisplay());
	  started.start();
	  return started;
  }
  private static class NoButtonDisplay implements Runnable{
		public void run(){
			while(true){
				JOptionPane.showOptionDialog(null, "Searching for Groups, Please Wait ...", "Group Searcher", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{}, null);
			}
		}
	
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
	  if(pname == pnames[0]){
		  PopInformationMessage("You", "This is You");
		  return;
	  }
	  
	  String builtString = "Peer Name: " + pname + "\n";
	  builtString += "URN: " + resolver.get(pname) + "\n";
	  builtString += "Shared Files: " + GUI_Control.numFiles(pname) + "\n";
	  builtString += "Output Pipe (Name): " + peers.get(resolver.get(pname)).getName() + "\n";
	  builtString += "Output Pipe (Type): " + peers.get(resolver.get(pname)).getType() + "\n";
	  if(!peers.get(resolver.get(pname)).isClosed()){
		  builtString += "Connection Status: Active\n";
	  }else{
		  builtString += "Connection Status: Closed\n";
	  }
	  
	  PopInformationMessage(pname + " Info", ""+builtString);
  }
  
  public void lookupFileInfo(String filename, String selectedPeer){
	  //TODO:
	  //FIX THE LOOKUP OF OTHER PEOPLES FILE -> NEED TO MAINTAIN A JPFSFILE ARRAY OF OUR PEERS FILES
	  if(selectedPeer == pnames[0] || selectedPeer == null){
		  JPFSFile temp = GUI_Control.findJPFSFile(filename);
		  if(temp!=null){
			  printFileInfo(temp, true);
		  }
		  return;
	  }else{
		  byte[] tmp = new byte[1];
		  sendData(peers.get(resolver.get(selectedPeer)), 5, tmp, filename, false, "");
	  }
  }
  
  public static void printFileInfo(JPFSFile temp, boolean printPath){
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

  static public void printBench(long start, long end, int leng){
	long time = end - start;
	time = time/1000000;
	int kleng;
	int mleng;
	int gleng;
	kleng = leng/1024;
	mleng = kleng/1024;
	gleng = mleng/1024;
	long sec = time/1000;
	System.out.println("**********************************");
	System.out.println("BENCHMARK RESULTS");
	System.out.println("Start Time of Request (mil-sec): " + start/1000000);
	System.out.println("End Time of Request (mil-sec): " + end/1000000);
	System.out.println("Request Total Time (mil-sec): " + time);
	System.out.println("Length of File (GB): " + gleng);
	System.out.println("Length of File (MB): " + mleng);
	System.out.println("Length of File (KB): " + kleng);
	System.out.println("Length of File (B): " + leng);
	if(sec>0){
		System.out.println("\nAverage Transfer Rate (GB / s): " + gleng/sec);
		System.out.println("Average Transfer Rate (MB / s): " + mleng/sec);
		System.out.println("Average Transfer Rate (KB / s): " + kleng/sec);
		System.out.println("Average Transfer Rate (B / s): " + leng/sec);
	}else{
		System.out.println("Average Transfer Rate (GB / ms): " + gleng/time);
		System.out.println("Average Transfer Rate (MB / ms): " + mleng/time);
		System.out.println("Average Transfer Rate (KB / ms): " + kleng/time);
		System.out.println("Average Transfer Rate (B / ms): " + leng/time);
	}
	System.out.println("**********************************");
  }

}
