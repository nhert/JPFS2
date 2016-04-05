package testjxse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import testjxse.JPFSPrinting.errorLevel;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

public class StartupOptions implements DiscoveryListener{
	public PeerGroup initGroup;
	public boolean usingCus;
	public String theName = "";
	private String clientName;
	public  ModuleImplAdvertisement customGroup;
	public PeerGroup customGroupInstance;
	private File gLister;
	public static List<String> GroupsDiscovered;
	public static Map<String, groupContents> GroupInfos;
	public static Thread gloop;
	private static DiscoveryService discovery;
	static NetworkManager NetManager;
	static PeerGroup global;
	static Thread advChecker;
	public Stage glistStage;
	
	StartupOptions(){
		GroupsDiscovered = new ArrayList<String>();
		GroupInfos = new HashMap<String,groupContents>();
	}
	
	public class groupContents{
		public String creator;
		public boolean pwordProtected;
		public PeerGroupID pgid;
		public String hashedPassword;
		public String description;
		groupContents(String c, boolean pw, PeerGroupID id, String desc){
			creator = c;
			pwordProtected = pw;
			pgid = id;
			description = desc;
		}
		groupContents(String c, boolean pw, PeerGroupID id, String desc, String hp){
			creator = c;
			pwordProtected = pw;
			pgid = id;
			hashedPassword = hp;
			description = desc;
		}
	}
	
	public void StartOptions() throws Exception{  
		  int choice = P2PManager.PopCustomTwoButtons(clientName, "Would you like to see Private Subgroup options or Simply Join the Global Peer Group?", 
				  "Private Options", "Join Global");
		  if(choice == 0){ // creation or joining of private peer groups
			  System.out.println("Custom Group Creation Started");
			  SubgroupOPs();
		  }else if(choice==1){// if they choose to not use a private group, they join the global peer group
			  StartupP2PApp.StartMain();
		  }else{ //closing window
		      System.exit(0);
		  }
	}
	
	private void SubgroupOPs() throws Exception{
		int choice = P2PManager.PopCustomTwoButtons(clientName, "Join an Existing Subgroup or Create One?", 
				  "Join a Group", "Create a Group");
		if(choice == 0){ // join a group	
				gLister = new File("./GroupLister.fxml");
				initGListStage();
				
				//join global group but don't publish yourself
				String tName = "temp_"+UUID.randomUUID().toString();
				File ConfigurationFile = new File("./temp/"+tName); // setup the config file
				NetManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, tName , ConfigurationFile.toURI());
				global = NetManager.startNetwork();
				
				ModuleImplAdvertisement customIMPL = global.getAllPurposePeerGroupImplAdvertisement();
				PeerGroup privateGroupPublisher = global.newGroup(IDFactory.newPeerGroupID("PrivateGroupPublisher".getBytes()), customIMPL, "PrivateGroupPublisher", "Private Group Publishing", false);
				privateGroupPublisher.startApp(new String[0]);
				
				discovery = privateGroupPublisher.getDiscoveryService();
				discovery.addDiscoveryListener(this);
				advChecker = new Thread(new fetch_advertisements());
				advChecker.start();
				gloop = new Thread(new GroupLoop());
				gloop.start();
		}else if(choice == 1){//create a group
			String newGroupName = "";
			while(true){
				newGroupName = JOptionPane.showInputDialog("Input a Group Name \n(14 Characters Max | Alphanumeric)", null);
				if(newGroupName == null){
					StartOptions();
					return;
				}else if(newGroupName.length() > 14 || !StringUtils.isAlphanumericSpace(newGroupName)){
					continue;
				}else if(!newGroupName.isEmpty()){
					break;
				}
			}
			
			newGroupName += "_"+UUID.randomUUID().toString().substring(0, 8);
			
			String descrip = "";
			while(true){
				descrip = JOptionPane.showInputDialog("Input a Small Description of Group \n(Alphanumeric)", null);
				if(descrip == null){
					StartOptions();
					return;
				}else if(!StringUtils.isAlphanumericSpace(descrip)){
					continue;
				}else if(!descrip.isEmpty()){
					break;
				}
			}
			
			int yesno = JOptionPane.showConfirmDialog(null, "Would you Like to Use a Password?");
			String pword = "";
			if(yesno == 0){
				while(true){
					pword = JOptionPane.showInputDialog(null, "Input a Password \n(16 Characters Max | Alphanumeric | No Spaces)");
					if(pword == null){
						StartOptions();
						return;
					}else if(!StringUtils.isAlphanumeric(pword) || pword.length() > 16){
						continue;
					}else if(!pword.isEmpty()){
						break;
					}
				}
				StartupP2PApp.StartMainCustom(newGroupName, true, descrip, true, pword);
			}else if(yesno==1){
				StartupP2PApp.StartMainCustom(newGroupName, true, descrip, false, "");
			}else{//go back
				StartOptions();
				return;
			}
		}else{//go back
			StartOptions();
			return;
		}
		
		
		
	}
	
	public void discoveryEvent(DiscoveryEvent TheDiscoveryEvent) {
		DiscoveryResponseMsg TheDiscoveryResponseMsg = TheDiscoveryEvent.getResponse();
	    Enumeration<Advertisement> en = TheDiscoveryResponseMsg.getAdvertisements();
	      
	    //if the peer isn't discovered already
	    if (en!=null) { 
	    	while (en.hasMoreElements()) {
	    		try{
	         	  Advertisement ad = en.nextElement();
	        	  if(ad.getAdvType() == PeerGroupAdvertisement.getAdvertisementType()){
	        		  PeerGroupAdvertisement pga = (PeerGroupAdvertisement)ad;
	        		  String gname = pga.getDescription().split("&%")[0];
	        		  String creator = pga.getDescription().split("&%")[1];
	        		  String desc = pga.getDescription().split("&%")[2];
	        		  boolean pwordUsed = Boolean.parseBoolean(pga.getDescription().split("&%")[3]);
	        		  if(pwordUsed){
	        			  String hashedPassword = pga.getDescription().split("&%")[4];
	        			  if(!GroupsDiscovered.contains(gname)){
	        				  GroupsDiscovered.add(gname);
	        				  GroupInfos.put(gname, new groupContents(creator, pwordUsed, pga.getPeerGroupID(), desc, hashedPassword));
	        			  }
	        		  }else{
	        			  if(!GroupsDiscovered.contains(gname)){
	        				  GroupsDiscovered.add(gname);
	        				  GroupInfos.put(gname, new groupContents(creator, pwordUsed, pga.getPeerGroupID(), desc));
	        			  }
	        		  }
	                 
	        	  }
	    		}catch(Throwable a){
	    			JPFSPrinting.logError("Throwable Error in the StartupOptions discovery thread", errorLevel.RECOVERABLE);
	    		}
	          }
	      }
	  }
	  
	 

	public void initGListStage() throws MalformedURLException, IOException {
		Pane page = (Pane) FXMLLoader.load(gLister.toURI().toURL());
		@SuppressWarnings("unused")
		GUI_Control_GroupListerHelper GUICGLH = new GUI_Control_GroupListerHelper (page);
		Scene scene = new Scene(page);
		
		glistStage = new Stage();
		
		glistStage.setScene(scene);
		glistStage.setTitle("Group Lister - JPFS2");
		glistStage.setResizable(false);
		glistStage.show();
		Platform.setImplicitExit(false);
		glistStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
			public void handle(WindowEvent we){
        		try {
        			we.consume();
        			glistStage.hide();
        			StartOptions();
				} catch (Exception e) {
					JPFSPrinting.logError("Error Closing Group Lister Window", errorLevel.RECOVERABLE);
				}
        	}
        });
	}
	 
	
	private static class GroupLoop implements Runnable{
		public void run(){
			while(true){
				try {
					Thread.sleep(1000);
					GUI_Control_GroupLister.doRefresh();
				} catch (InterruptedException e) {
					JPFSPrinting.logError("Sleep method interrupted in StartupOptions GroupLoop Thread", errorLevel.CAN_IGNORE);
				}
			}
		}
	
	}
	
	//light thread which loops for advertisements
	private static class fetch_advertisements implements Runnable{
		public void run() {
			while(true) {
				try{
					if(discovery != null){
	                  discovery.getRemoteAdvertisements(null, DiscoveryService.GROUP, "Name", "GroupADV", 1, null);
	            	}
	            	else
	            	JPFSPrinting.logWarning("Discovery wasn't ready to search for advertisements, trying again");
	            	
	                Thread.sleep(500);
	              
	            } catch(InterruptedException e) {
	           		JPFSPrinting.logError("Sleep method interrupted in StartupOptions fetch advertisements Thread", errorLevel.CAN_IGNORE);
	           	} catch(IllegalStateException e) {
	              	JPFSPrinting.logError("Discovery was not properly intialized in StartupOptions", errorLevel.RECOVERABLE);
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
