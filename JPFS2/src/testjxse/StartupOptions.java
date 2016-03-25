package testjxse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.JOptionPane;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

public class StartupOptions implements DiscoveryListener{
	public PeerGroup initGroup;
	public boolean usingCus;
	public String theName = "";
	private String clientName;
	public  ModuleImplAdvertisement customGroup;
	public PeerGroup customGroupInstance;
	private String PName;
	private File gLister;
	
	
	private static boolean freezeUntilDone = true;
	public static List<String> GroupsDiscovered;
	public static Map<String, PeerGroupID> GroupInfos;
	
	public static Thread gloop;
	private static DiscoveryService discovery;
	static NetworkManager NetManager;
	static PeerGroup global;
	static Thread advChecker;
	
	StartupOptions(){
		GroupsDiscovered = new ArrayList<String>();
		GroupInfos = new HashMap<String,PeerGroupID>();
	}
	
	public void StartOptions() throws Exception{
		//TODO:
		  //PASSWORD ENTRY OPTIONS FOR SOME GROUPS? PSE
		  
		  if(P2PManager.PopCustomTwoButtons(clientName, "Would you like to see Private Subgroup options or Simply Join the Global Peer Group?", 
				  "Private Options", "Join Global") == 0){ // creation of private peer groups
			  System.out.println("Custom Group Creation Started");
			  SubgroupOPs();
			  
		  }else{ // if they choose to not use a private group, they join the global peer group
		      StartupP2PApp.StartMain();
		  }
	}
	
	private void SubgroupOPs() throws Exception{
		if(P2PManager.PopCustomTwoButtons(clientName, "Join an Existing Subgroup or Create One?", 
				  "Join a Group", "Create a Group") == 0){ // join a group
			
			    /*theName = JOptionPane.showInputDialog("Name an Existing Group",null);
			    if (!theName.equals("") && theName!=null)
			    	break;
			    System.out.println("Illegal Group Name, Please Retry");*/
				gLister = new File("./GroupLister.fxml");
				initGListStage();
				System.out.println("END OF SUB OPS BLOCK");
				
				//join global group but dont publish yourself
				String tName = "temp_"+UUID.randomUUID().toString();
				File ConfigurationFile = new File("./temp/"+tName); // setup the config file
				NetManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, tName , ConfigurationFile.toURI());
				global = NetManager.startNetwork();
				
				ModuleImplAdvertisement customIMPL = global.getAllPurposePeerGroupImplAdvertisement();
				if(customIMPL == null || global == null){
					System.out.println("NULL IMPL");
				}else{
					System.out.println("NOT NULL IMPL");
				}
				PeerGroup privateGroupPublisher = global.newGroup(IDFactory.newPeerGroupID("PrivateGroupPublisher".getBytes()), customIMPL, "PrivateGroupPublisher", "Private Group Publishing", false);
				privateGroupPublisher.startApp(new String[0]);
				
				
				discovery = privateGroupPublisher.getDiscoveryService();
				discovery.addDiscoveryListener(this);
				advChecker = new Thread(new fetch_advertisements());
				advChecker.start();
				gloop = new Thread(new GroupLoop());
				gloop.start();
		}else{
			String newGroupName = JOptionPane.showInputDialog("Input a Group Name", null);
			newGroupName += "_"+UUID.randomUUID().toString().substring(0, 8);
			System.out.println("Creating Group: " + newGroupName);
			
			String descrip = JOptionPane.showInputDialog("Input a Small Description of Group", null);
			
			
			
			StartupP2PApp.StartMainCustom(newGroupName, true, descrip);
		}
		
		
		
	}
	
	  public void discoveryEvent(DiscoveryEvent TheDiscoveryEvent) {
	      // Who triggered the event?
		  //System.out.println("DISC EVENT FROM STARTUP OPS");
	      DiscoveryResponseMsg TheDiscoveryResponseMsg = TheDiscoveryEvent.getResponse();
	      Enumeration<Advertisement> en = TheDiscoveryResponseMsg.getAdvertisements();
	      Object theGroup = TheDiscoveryEvent.getSource();
	      
	      
	      //if the peer isn't discovered already
	      if (en!=null) { 
	          while (en.hasMoreElements()) {
	        	  try{
	        	  Advertisement ad = en.nextElement();
	        	  //System.out.println("Received Type: " + ad.getAdvType().toString());
	        	  if(ad.getAdvType() == PeerGroupAdvertisement.getAdvertisementType()){
	                  //System.out.println("FOUND A PEER GROUP ADVERT!: " + ad.getID());
	                  PeerGroupAdvertisement pga = (PeerGroupAdvertisement)ad;
	                  //System.out.println("Found: " + pga.getDescription());
	                  if(!GroupsDiscovered.contains(pga.getDescription())){
	                	  GroupsDiscovered.add(pga.getDescription());
	                	  //System.out.println("PGID: "+pga.getPeerGroupID());
	                	  GroupInfos.put(pga.getDescription(), pga.getPeerGroupID());
	                  }
	        	  }
	        	  }catch(Throwable a){
	        		  a.printStackTrace();
	        	  }
	          }
	      }
	      
	  }
	  
	 

	public void initGListStage() throws MalformedURLException, IOException {
		Pane page = (Pane) FXMLLoader.load(gLister.toURI().toURL());
		GUI_Control_GroupListerHelper GUICGLH = new GUI_Control_GroupListerHelper (page);
		Scene scene = new Scene(page);
        StartupP2PApp.primaryStage.setScene(scene);
        StartupP2PApp.primaryStage.setTitle("Group Lister - JPFS2");
        StartupP2PApp.primaryStage.setResizable(false);
        StartupP2PApp.primaryStage.show();
	}
	 
	
	private static class GroupLoop implements Runnable{
		public void run(){
			while(true){
				try {
					Thread.sleep(1000);
					GUI_Control_GroupLister.doRefresh();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	
	}
	
	  //light thread which loops for advertisements
	 private static class fetch_advertisements implements Runnable{
	         public void run() {
	            while(true) {
	            	//System.out.println("SEARCHING!! ...");
	            	try{
	            	if(discovery != null){
	                  discovery.getRemoteAdvertisements(null, DiscoveryService.GROUP, "Name", "GroupADV", 1, null);
	            	}
	            	else
	            	  System.out.println("Discovery Did not Get Initialized");
	            	
	                Thread.sleep(500);
	              
	            	} catch(InterruptedException e) {} 
	                catch(IllegalStateException e) {
	            		System.out.println("The Discovery Thread is being Skipped: Thread not Ready!");
	            	}
	            }
	         }
	   }
	  
	@SuppressWarnings("deprecation")
	public void cleanup(){
		  gloop.stop();
		  advChecker.stop();
		  NetManager.stopNetwork();
		  discovery.removeDiscoveryListener(this);
    }
	
	
	
	
	
	
}
