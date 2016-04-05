package testjxse;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import org.apache.commons.io.FilenameUtils;
import testjxse.GUI_Control.sortType;
import testjxse.JPFSPrinting.errorLevel;
import testjxse.SigningUtil.SignedContents;

public class P2PManager implements DiscoveryListener, PipeMsgListener, OutputPipeListener{	
	
  //jxta vars
  public NetworkManager NetManager; // manages the JXTA network
  public PeerGroup PGroup; // instance of the global peer group of JXTA
  public NetworkConfigurator Config; // configuration options for JXTA network accessed here
  public static File ConfigurationFile; // The 'Profile' file created for this user
  private PeerID MyPeerID; // The peer ID of this user, generated randomly
  
  //string information of client
  public static String PName; // the peer name of this client (created from the host computer name)
  private String CliName; // name of the JPFS-2 client
  
  //services
  private PipeService pipe_service; // used to create pipes for the JXTA group
  private PipeAdvertisement myPipeAdv = null; // used to generate an advertisement for this client
  @SuppressWarnings("unused") // it is used, false flag
  private InputPipe myInput = null; // input pipe that receives messages from other clients
  private DiscoveryService discovery = null; // the service for discovering other peers in your peer group
 
  //utility vars
  private Object timeoutLock = new Object(); // locks the timeout thread handling in handleReq
  private Object messageInLock = new Object(); // locks access to message elements when receiving messages
  private Object discoveryLock = new Object(); // locks access to resolver, peers, and pipes when discovering a peer
  private Object foundPeer; // helper variable for discovery events (URN)
  private String foundPeerName; // helper variable for discovery events (NAME STRING)
  
  //for benchmark testing
  public static long startTime;
  public static long endTime;
  
  //for encryption - These values could be randomly generated but are specified for the purposes of demonstration
  private static String eKey = "JPFS2KEYJPFS2YEK"; // 128-Bit Cipher Key for Demonstration Purposes.   
  private static String eVector = "InitializeVector"; // 16-Bit Initialize Vector for Demonstration Purposes.
  
  //maps for peer functionality
  private static volatile Map<Object,OutputPipe> pipes = new Hashtable<Object,OutputPipe>(); // return output pipes from peer urns
  private static Map<String,Object> resolver = new HashMap<String,Object>(); // resolve peer names to their urns
  private static List<String> peers = new ArrayList<String>(); // store an array of found peer names (for GUI)
  private static Map<String,Object> pipeToURN = new HashMap<String,Object>(); // helper map that maps pipeIDs to peer urns
  private static Map<String,String> pipeToPName = new HashMap<String,String>(); // helper map that maps pipeIDs to peer names
  
  //utility lists
  private static volatile Map<String, Timer> timeouts = new Hashtable<String, Timer>(); // holds the kill timers for timeouts
  private static Map<String, String> peerCodes = new HashMap<String,String>(); // maps peer names to a peer "code" which identifies them for validation
  
  public enum reqType{ // used for specifying request types in message headers
	  FILEREQ, FILERES, FILELIST, PINGREQ, PINGRES, FILEINFOREQ, 
	  FILEINFORES, SORTREQ, DELETENOTIFICATION, PEERCODE, VALIDATEREQ, VALIDATERES
  }
  
  //initialize this client for connecting to the global JXTA peer group
  public void initGlobal(String n){
	  try{
		  System.out.println("Initializing JPFS-2 Client");
		  GUI_Control_ApplicationLevel.setGroupLabel("Peers of Global Lobby");
		  CliName = n; // the clients name
		  String id = UUID.randomUUID().toString().substring(0, 8); // generate a random UUID for this user
          PName = getCompName() + " (ID: "+ id + ")"; // the host name is this users peer name + random id
          GUI_Control.initFileList(PName + " (You)"); 
          GUI_Control.You = PName + " (You)"; // store your full peer name in the GUI control
          String filePName = getCompName() + id; // file friendly name of this peer
          peers.add(PName + " (You)"); // display yourself in the peer list 
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
	          JPFSPrinting.logError("Peer Group did not start properly in the initGlobal method", errorLevel.SEVERE);
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
  
  //initialize this client for connecting to a subgroup of the JXTA global peer group
  public void initCustom(String n, String GroupName, boolean Creator, String desc, boolean pwordGroup, String pword){
	  try{
	  System.out.println("Initializing Custom JPFS-2 Client");
	  GUI_Control_ApplicationLevel.setGroupLabel("Peers of " + GroupName);
	  CliName = n; // the clients name "J_PFS"
	  String id = UUID.randomUUID().toString().substring(0, 8);
      PName = getCompName() + " (ID: "+id + ")"; // the host name is this users peer name + random int id for uniqueness
      GUI_Control.initFileList(PName + " (You)");
      GUI_Control.You = PName + " (You)";
      String filePName = getCompName() + id;
      peers.add(PName + " (You)"); // display yourself in the peer list 
      MyPeerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
              PName.getBytes()); // peer id of this user gets set up by a factory
      
      System.out.println("Created Peer ID for User: " + PName + "\n\n PID: " + MyPeerID); // info dialog to show your name and peer id
      
      ConfigurationFile = new File("./Profiles/" + filePName); // setup the config file
      NetworkManager.RecursiveDelete(ConfigurationFile);
      
	  NetManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, PName, ConfigurationFile.toURI()); // create a new network manager object for this client
	  
	  PopInformationMessage(CliName, "Starting the Network"); // inform user that basic setup is complete
	  PGroup = NetManager.startNetwork();
	  
	  //PRIVATE GROUP LOGIC
	  ModuleImplAdvertisement customGroup = PGroup.getAllPurposePeerGroupImplAdvertisement();
	  PeerGroup customGroupInstance = PGroup.newGroup(IDFactory.newPeerGroupID(GroupName.getBytes()), customGroup, GroupName, desc, false);
	
	  //Check to see if client successfully joined the peergroup
      if (Module.START_OK != customGroupInstance.startApp(new String[0])){
    	  JPFSPrinting.logError("Peer Group did not start properly in the initCustom method", errorLevel.SEVERE);
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

      if(Creator){ // if you are creating this group then...
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
  
  //static method for creating a peer group advertisement
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
	  for(int x = 0; x<peers.size(); x++){
			try {
				sendData(pipes.get(resolver.get(peers.get(x))),reqType.DELETENOTIFICATION,empty.getBytes("ISO-8859-1"), "", false, null); // send message to all peers to delete me
			} catch (UnsupportedEncodingException e) {
				 JPFSPrinting.logError("General Exception in P2P Init", errorLevel.CAN_IGNORE);
			}
      }
  }
  
  //gracefully shuts down the JPFS-2 client, notifying all peers
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
  
  //transmit a message to all peers about our file listing
  public static void transmitUpdateFiles(){
	      for(String peer : peers){
		  if(!peer.contains(PName)){//if we are not looking at ourselves
			  String fileList = GUI_Control.packFiles(peer);
			  try {
				sendData(pipes.get(resolver.get(peer)),reqType.FILELIST,fileList.getBytes("ISO-8859-1"), "", false, null);
			  } catch (UnsupportedEncodingException e) {
				JPFSPrinting.logError("Byte Encoding failed in transmit update files method", errorLevel.RECOVERABLE);
			  }
		  }
	  	  }
  }
  
  //transmit a message to a specific peer about our file listing
  public static void transmitUpdateFiles(String peer, OutputPipe location){ // send a message to a single peer with new list of files!
			  String fileList = GUI_Control.packFiles(peer);
			  try {
				  sendData(location,reqType.FILELIST,fileList.getBytes("ISO-8859-1"), "", false, null);
			  } catch (UnsupportedEncodingException e) {
				  JPFSPrinting.logError("Byte Encoding failed in transmit update files method", errorLevel.RECOVERABLE);
			  }
  }
  
  //transmit sorted file list to a peer
  public static void transmitSortedFiles(String fList, OutputPipe location){
	  try {
		  sendData(location,reqType.FILELIST,fList.getBytes("ISO-8859-1"), "", false, null);
	  } catch (UnsupportedEncodingException e) {
		  JPFSPrinting.logError("Byte Encoding failed in transmit update files method", errorLevel.RECOVERABLE);
	  }
  }
  
  //transmit my peer code to a peer when i first discover them
  public static void transmitMyPeerCode(OutputPipe loc){
	  try{
		  sendData(loc, reqType.PEERCODE, PeerCode.MyHashedCode().getBytes("ISO-8859-1"), "", false, null);
	  }catch (UnsupportedEncodingException e) {
		  JPFSPrinting.logError("Byte Encoding failed in transmit peer code method", errorLevel.RECOVERABLE);
	  }
  }
  
  //transmit a request to a peer for a sorted list of their files
  public static void transmitSortRequest(sortType st, String peer){
	  try {
		sendData(pipes.get(resolver.get(peer)), reqType.SORTREQ, (st.ordinal()+"").getBytes("ISO-8859-1"), "", false, null);
	  } catch (UnsupportedEncodingException e) {
		  JPFSPrinting.logError("Byte Encoding failed in transmit sort request method", errorLevel.RECOVERABLE);
	  }
  }
  
  //transmit a file request to a peer
  public static void transmitFileRequest(String path, String peer){ // send a message to the respective peer about a file we want to download
	 startTime = System.nanoTime();
	 try {
	 	sendData(pipes.get(resolver.get(peer)), reqType.FILEREQ, path.getBytes("ISO-8859-1"),"", false, null);
	 } catch (UnsupportedEncodingException e) {
		 JPFSPrinting.logError("Byte Encoding failed in transmit file request method", errorLevel.RECOVERABLE);
	 }
  }
  
  
  //this ASYNC event is triggered whenever we discover a peer through the JXTA discovery service
  public void discoveryEvent(DiscoveryEvent TheDiscoveryEvent) {
      // Who triggered the event?
      DiscoveryResponseMsg TheDiscoveryResponseMsg = TheDiscoveryEvent.getResponse();
      Enumeration<Advertisement> en = TheDiscoveryResponseMsg.getAdvertisements();
      Object thePeer = TheDiscoveryEvent.getSource();
      
      synchronized(discoveryLock){ // lock discovery so that the peer information is in-sync (lots of incoming discovery events at once)
      //if the peer isn't discovered already
    	  if(thePeer!=null){
    		  if (en!=null) { 
    			  while (en.hasMoreElements()) {
    				  Advertisement ad = en.nextElement();
    				  if(ad.getAdvType() == PipeAdvertisement.getAdvertisementType()){
    					  foundPeer = TheDiscoveryEvent.getSource();
    					  foundPeerName = ((PipeAdvertisement) ad).getDescription();
    					  if(!peers.contains(foundPeerName)){
    						  peers.add(((PipeAdvertisement) ad).getDescription());
    						  GUI_Control.addPeer(((PipeAdvertisement) ad).getDescription());
    					  }
                    
    					  if(!resolver.containsKey(foundPeerName)){
    						  try {
    							  pipe_service.createOutputPipe((PipeAdvertisement) ad, this);
    						  } catch (IOException e) {
    							  JPFSPrinting.logError("Error Creating Output Pipe for Discovered Peer", errorLevel.RECOVERABLE);
    						  }
    						  resolver.put(((PipeAdvertisement) ad).getDescription(), foundPeer); // add peer name to urn store
    						  pipeToURN.put(((PipeAdvertisement) ad).getPipeID().toString(), foundPeer);
    						  pipeToPName.put(((PipeAdvertisement) ad).getPipeID().toString(), foundPeerName);
    					  }
        	  	
    				  }
    			  }
    			  Set<String> s = new LinkedHashSet<>(peers);
    			  peers.clear();
    			  peers.addAll(s); // remove duplicates
    		  }
    	  }
      }
  }
  
  //get the host name of this computer for use in creating a peer name
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
  
  
  //light thread which loops for advertisements every 3 seconds
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
            	
            			sleep(3000);
              
            		} catch(InterruptedException e) {} 
            		catch(IllegalStateException e) {
                	JPFSPrinting.logWarning("Discovery thread fetch_advertisements being skipped");
            		}
            	}
         }
      }.start();
   }
  
  //send data to another peer 
  //target: the OutputPipe of the other peer
  //req: the request type
  //input: the data we are sending to the other peer (sometimes empty)
  //fn: filename, for file requests and responses
  //useChecksum: use a checksum in transmitted data?
  //path: the path of the file when using checksum
  private static void sendData(OutputPipe target, reqType req, byte[] input, String fn, boolean useChecksum, String path) {
      Message msg = new Message();
      MessageElement request = null;
      MessageElement data = null;
      MessageElement myurn = null;
      MessageElement fina = null;
      MessageElement cs = null;
      request = new StringMessageElement("Request", Integer.toString(req.ordinal()), null);
	  
	  myurn = new StringMessageElement("PName", PName ,null);
	  fina = new StringMessageElement("FileName", fn, null);
	  if(useChecksum){
		  cs = new StringMessageElement("Checksum", ChecksumUtil.generateMD5(path),null);
		  msg.addMessageElement(cs);
	  }
	  data = new ByteArrayMessageElement("Data", null, input, null);

      msg.addMessageElement(request);
      msg.addMessageElement(data);
      msg.addMessageElement(myurn);
      msg.addMessageElement(fina);

     
      try {
    	  if(target==null){
    		  JPFSPrinting.logError("Error with send message target: null ", errorLevel.RECOVERABLE);
    		  return;
    	  }
          target.send(msg);
      } catch (IOException e) {
    	  JPFSPrinting.logError("Error sending message over input pipe: " + target.toString(), errorLevel.RECOVERABLE);
      }
  }

  //send data to another peer 
  //target: the OutputPipe of the other peer
  //req: the request type
  //input: the data we are sending to the other peer (sometimes empty)
  //fn: filename, for file requests and responses
  //useChecksum: use a checksum in transmitted data?
  //path: the path of the file when using checksum
  //useSigning: sign the data with your digital signature? (for file responses)
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

  
  //JXTA ASYNC event triggered when we receive data through our input pipe
  @Override
  public void pipeMsgEvent(PipeMsgEvent event) {
	  synchronized(messageInLock){
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
    	  JPFSPrinting.logError("Error reading message from output pipe", errorLevel.RECOVERABLE);
		  }
	  }
  }
  
  //method for handling file responses from peers
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
  
  //handles generic messages from other peers (non-file messages)
 
  private void handleReq(String fName, reqType r, String peerOrigin) throws IOException, InterruptedException{ // handle the req from the other peer
	  if(r == reqType.FILEREQ){ // they want a file from us! get the file they want and send its encrypted bytes back
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
			 sendData(pipes.get(resolver.get(peerOrigin)),reqType.FILERES,encrypted, fn, true, path, true); // send the byte array to the client
			 bis.close();
		 } catch(FileNotFoundException ex){ // if the file is not found on the server
			 JPFSPrinting.logError("Could not find the shared file user requested [Was it moved in your computer?]", errorLevel.RECOVERABLE);
			 return;
		 } catch (IOException e) {
			 JPFSPrinting.logError("IO Exception in the handling request method", errorLevel.RECOVERABLE);
		 }  
	  }else if (r == reqType.FILELIST){ // they sent a new file list! we need to parse it and update the list for us
    	 String[] dataParsed = fName.split("&%");
    	 GUI_Control.updateFiles(peerOrigin, dataParsed);
    	 GUI_Control_ApplicationLevel.forceFileListUpdate();
	  }else if(r==reqType.PINGREQ){ // we got a ping request for timeouts, so just ping them back
		  sendData(pipes.get(resolver.get(peerOrigin)),reqType.PINGRES,"".getBytes(), "", false, null); // send the byte array to the client
	  }else if(r==reqType.PINGRES){ // we got a ping response, stop the corresp. kill thread
		  synchronized(timeoutLock){
			  if(timeouts.get(peerOrigin)!=null){ 
				  timeouts.get(peerOrigin).stop();
				  if(timeouts.remove(peerOrigin) == null){
					  JPFSPrinting.logWarning("Attempted to Remove a peer from the timeouts thread who does not exist");
				  }
			  }else{
				  JPFSPrinting.logWarning("Timeout thread not found for peer");
			  }
		  }
	  }else if(r==reqType.FILEINFOREQ){//we got a request for info on a file of ours
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
			  sendData(pipes.get(resolver.get(peerOrigin)),reqType.FILEINFORES,encrypted, fName, false, ""); // send the byte array to the client
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
		  String newFLPacked = GUI_Control.packFilesList(GUI_Control.sortFilesReturn(sortType.values()[sType], GUI_Control.packFiles(peerOrigin)));
		  transmitSortedFiles(newFLPacked, pipes.get(resolver.get(peerOrigin)));
	  }
      else if(r==reqType.DELETENOTIFICATION){ // notification that this peer has closed their connection - we have to remove them from the lists
    	  deletePeer(peerOrigin);
      }else if(r==reqType.PEERCODE){ // peer is sending us their peer code upon discovering us for the first time
    	  if(!peerCodes.containsKey(peerOrigin)){
    		  peerCodes.put(peerOrigin, fName);
    	  }
      }else if(r==reqType.VALIDATEREQ){ // peer is requesting validation from us
    	  sendData(pipes.get(resolver.get(peerOrigin)), reqType.VALIDATERES, PeerCode.MyHashedCode().getBytes("ISO-8859-1"), "", false, "");
      }else if(r==reqType.VALIDATERES){ // we got a response about a peer validation request
    	  if(fName.equals(peerCodes.get(peerOrigin))){
    		  PopInformationMessage("Validation Results", "Peer is [VALIDATED]\nIdentified as original Peer Source");
    	  }else{
    		  PopInformationMessage("Validation Results", "Peer is [IN-VALIDATED]\nPeer is Unknown (Group may be Breached)");
    	  }
      }
  }

  //delete a peer from the client
  private void deletePeer(String n){
	  JPFSPrinting.printDeletedPeer();
	  GUI_Control.removePeer(n);
	  peers.remove(n);
	  timeouts.get(n).stop();
	  timeouts.remove(n);
  }


  //called when a new output pipe is created for a new peer (triggered in discovery event)
  @Override
  public void outputPipeEvent(OutputPipeEvent arg0) {
	String sourcePipe = arg0.getPipeID();
	String peerOrigin = pipeToPName.get(sourcePipe);
	Object urnOrigin = pipeToURN.get(sourcePipe);
	OutputPipe newPipe = arg0.getOutputPipe();
	pipes.put(urnOrigin, newPipe);
	transmitUpdateFiles(peerOrigin,newPipe); // send our new peer our existing files!
	transmitMyPeerCode(newPipe);
	JPFSPrinting.printNewPeer(peerOrigin);
  }
  
  //pings all peers every 25 seconds, and removes any peers who do not respond within a period of 10 seconds
  public void check_timeouts() {
      Timer newTim = new Timer(25000,new timeout());
      newTim.start();
  }
  //custom ActionListener class that pings all peers in our peer list every 25 seconds
  private class timeout implements ActionListener{
	  @Override
	  public void actionPerformed(ActionEvent e) {
		  //timeout logic
          for(int x = 0; x<peers.size(); x++){
        	  if(peers.get(x)!=null && !timeouts.containsKey(peers.get(x))){ // if we already have a kill thread going, don't start another
        		  if(!peers.get(x).contains(PName)){
           	   	  	sendData(pipes.get(resolver.get(peers.get(x))),reqType.PINGREQ,"".getBytes(), "", false, null); // send a ping
           			kill_thread(peers.get(x), 10000); // kill peer if they dont ping back in 15 seconds
           		  }
           	  }else{
           		  if(peers.get(x)==null){
           			  JPFSPrinting.logError("Error fetching a peer to ping [They were deleted?]", errorLevel.CAN_IGNORE);
           		  }else if(timeouts.containsKey(peers.get(x))){
           			  JPFSPrinting.logWarning("Thread already running for peer timeout [Already in Kill Thread]" + peers.get(x));
           		  }
           	  }
          }
	   }
   }
  
  
  //start a thread that will delete a peer from your list after delay time (for timeouts)
  public void kill_thread(String pnameToKill, int delay){
	Timer t = new Timer(delay,new KillClass(pnameToKill));
	t.setRepeats(false);
	timeouts.put(pnameToKill, t);
	timeouts.get(pnameToKill).start();
  }
  class KillClass implements ActionListener{
	public String p;
	KillClass(String in){
		p=in;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		JPFSPrinting.logWarning("A Peer Timed Out ["+p+"]");
		deletePeer(p);
	} 
  }
  

  //UTILITY FUNCTIONS
  public List<String> getPeers(){
	  return peers;
  }

  public int getLen(){
	  return pipes.size();
  }
  
  //pop a simple dialog box with an "ok" option
  public static void PopInformationMessage(String Name, String Message) {   
	  JOptionPane.showMessageDialog(null, Message, Name, JOptionPane.INFORMATION_MESSAGE); 
  }
  
  //pop a custom dialog box with 2 custom labels
  public static int PopCustomTwoButtons(String Name, String Question, String label0, String label1){
	  return JOptionPane.showOptionDialog(null, Question, Name, JOptionPane.OK_CANCEL_OPTION, 
			  JOptionPane.INFORMATION_MESSAGE, null, new String[]{label0, label1}, "default");
  }
  
  //pop a yes no question box
  public static int PopYesNoQuestion(String Name, String Question) {    
    return JOptionPane.showConfirmDialog(null, Question, Name, JOptionPane.YES_NO_OPTION);   
  }
  
  //pop a message box that does not block
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
  
  //method called when you double click on a peer
  public void lookupPeerInfo(String pname){
	  if(pname == peers.get(0)){
		  PopInformationMessage("You", "This is You");
		  return;
	  }
	  
	  String builtString = "Peer Name: " + pname + "\n";
	  builtString += "URN: " + resolver.get(pname) + "\n";
	  builtString += "Shared Files: " + GUI_Control.numFiles(pname) + "\n";
	  builtString += "Output Pipe (Name): " + pipes.get(resolver.get(pname)).getName() + "\n";
	  builtString += "Output Pipe (Type): " + pipes.get(resolver.get(pname)).getType() + "\n";
	  builtString += "Validation Peer Code: " + peerCodes.get(pname) + "\n";
	  if(!pipes.get(resolver.get(pname)).isClosed()){
		  builtString += "Connection Status: Active\n";
	  }else{
		  builtString += "Connection Status: Closed\n";
	  }
	 
	  int choice = PopCustomTwoButtons(pname + " Info", builtString, "Validate Peer", "Close");
	  if(choice == 0){ // validation request handler
		  sendData(pipes.get(resolver.get(pname)), reqType.VALIDATEREQ, "".getBytes(), "", false, "");
	  }
  }
  
  //method called when you double click on a file
  public void lookupFileInfo(String filename, String selectedPeer){
	  if(selectedPeer == peers.get(0) || selectedPeer == null){
		  JPFSFile temp = GUI_Control.findJPFSFile(filename);
		  if(temp!=null){
			  displayFileInfo(temp, true);
		  }
		  return;
	  }else{
		  byte[] tmp = new byte[1];
		  sendData(pipes.get(resolver.get(selectedPeer)), reqType.FILEINFOREQ, tmp, filename, false, "");
	  }
  }
  
  //displays file info
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
