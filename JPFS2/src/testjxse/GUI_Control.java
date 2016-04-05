package testjxse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.JFileChooser;
import org.apache.commons.io.FilenameUtils;
import testjxse.JPFSPrinting.errorLevel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import junit.framework.Assert;

public class GUI_Control
    implements Initializable {
	
	//enum for sorting types in the file sorting
	public enum sortType{
		NAME, DATE, EXTENSION, SIZE
	}

    @FXML 
    private Button DownloadButton; // Value injected by FXMLLoader
    @FXML 
    private Button ShareButton; // Value injected by FXMLLoader
    @FXML 
    private Button DiscoverButton; // Value injected by FXMLLoader
    @FXML 
    private Button UnshareButton; // Value injected by FXMLLoader
    @FXML 
    private ListView<String> PeerBox; // Value injected by FXMLLoader
    @FXML 
    private ListView<String> FileBox; // Value injected by FXMLLoader
    @FXML
    private MenuItem AboutMenu;
    @FXML
    private MenuItem HelpMenu;
    @FXML
    private ChoiceBox<String> SortPicker;
    @FXML
    private Label GroupLabel;
    
    
    private String name = "You";
    private boolean enabler = true;
    public static Map<String,List<String>> files = new HashMap<String,List<String>>(); // location 0 is always YOU
    public static Map<String,List<String>> yourFilePermissions = new HashMap<String, List<String>>(); // contains file name as key, list of peer names that have access to that file
    public static List<JPFSFile> JPFSFiles = new ArrayList<JPFSFile>();
    public MultipleSelectionModel<String> selectionModelFiles;
    public MultipleSelectionModel<String> selectionModelPeers;
    public static ObservableList<String> Peers;
    public static int removedIndex = -1;
    public static List<String> FilesList = new ArrayList<String>();
    public static ObservableList<String> Files;
    public static ObservableList<String> Sorts;
    public static String curSelectedPeer = "";
    private static boolean sortedOnce = false;
    public static String You;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
    	//assert that our ui components were all injected
    	Assert.assertNotNull(DownloadButton);
    	Assert.assertNotNull(ShareButton);
    	Assert.assertNotNull(DiscoverButton);
    	Assert.assertNotNull(UnshareButton);
    	Assert.assertNotNull(PeerBox);
    	Assert.assertNotNull(FileBox);
    	Assert.assertNotNull(AboutMenu);
    	Assert.assertNotNull(HelpMenu);
    	Assert.assertNotNull(SortPicker);
    	
        List<String> PeersList = new ArrayList<String>();
        List<String> SortsList = new ArrayList<String>();
        name = getCompName();
        PeersList.add(name);
        Peers = FXCollections.observableList(PeersList);
        Files = FXCollections.observableList(FilesList);
        Sorts = FXCollections.observableList(SortsList);
        PeerBox.setItems(Peers);
        FileBox.setItems(Files);
        SortPicker.setItems(Sorts);
        DownloadButton.setDisable(true);
        UnshareButton.setDisable(true);
        selectionModelFiles = FileBox.getSelectionModel();
        selectionModelPeers = PeerBox.getSelectionModel();
        selectionModelFiles.setSelectionMode(SelectionMode.SINGLE);
        selectionModelPeers.setSelectionMode(SelectionMode.MULTIPLE);
        Sorts.add("By Name");
        Sorts.add("By Date");
        Sorts.add("By Size");
        Sorts.add("By Extension");
          
        // initialize your logic here: all @FXML variables will have been injected
        DownloadButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
            	if(selectionModelFiles.getSelectedIndex()!=-1 && !selectionModelPeers.getSelectedItem().equals(You)) // proper selections in gui
                  P2PManager.transmitFileRequest(selectionModelFiles.getSelectedItem(), selectionModelPeers.getSelectedItem());
            }
        });
        
        ShareButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	final JFileChooser fc = new JFileChooser();
            	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            	if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){ // selected a file to share
            		File newf;
            		newf =  fc.getSelectedFile();
            		if(yourFilePermissions!=null){
            			if(yourFilePermissions.containsKey(newf.getName())){
            				P2PManager.PopInformationMessage("File Not Shared", "The file you selected has already been shared.\nIf you want to edit permissions, use the edit permissions button");
            				return;
            			}
            		}
            		
            		try {
            			List<String> selectedItems = PeerBox.getSelectionModel().getSelectedItems();
            			if((selectedItems.size() >= 1 && selectedItems.contains(You))||(selectedItems==null)||(selectedItems.isEmpty())){ // if you select nothing or selected yourself as the only peer
            				JPFSFiles.add(new JPFSFile(newf));
            				List<String> temp = new ArrayList<>();
            				temp.add("ALL");
    						yourFilePermissions.put(newf.getName(), temp);
    						files.get(You).add(newf.getName());
    						P2PManager.transmitUpdateFiles();
    						Files.clear();
    		        		Files.addAll(files.get(You));
            			}else if(selectedItems.size() >= 1 && !selectedItems.contains(You)){
            				JPFSFiles.add(new JPFSFile(newf));
            				List<String> temp = new ArrayList<>();
            				temp.addAll(selectedItems);
            				yourFilePermissions.put(newf.getName(), temp);
            				files.get(You).add(newf.getName());
            				P2PManager.transmitUpdateFiles();
            				Files.clear();
            				Files.addAll(files.get(You));
            			}else{
            				JPFSPrinting.logError("Error getting file sharing permissions list", errorLevel.CAN_IGNORE);
            			}
            			
					} catch (IOException e) {
						JPFSPrinting.logError("IO Exception: GUI Control Class Share Button handler", errorLevel.RECOVERABLE);
					}
            	}
            	//sort the new file list if there is a sort picked atm
            	if(sortedOnce){
            		sortFiles(resolveModeEnum(SortPicker.getSelectionModel().getSelectedItem()));
            	}
            }
        });
        
        UnshareButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
            	if(selectionModelFiles.getSelectedIndex()<0)
            		return;
            	
            	yourFilePermissions.remove(selectionModelFiles.getSelectedItem());
            	files.get(You).remove(selectionModelFiles.getSelectedIndex()); // remove your files from the storage
                Files.remove(selectionModelFiles.getSelectedIndex()); // update the list to show your new file list
                yourFilePermissions.remove(selectionModelFiles.getSelectedItem());
                P2PManager.transmitUpdateFiles();
            }
        });
 
        PeerBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent event){
        		int numSel = selectionModelPeers.getSelectedItems().size();
        		if(numSel<=1){//if we are clicking the peer box and only selecting
        			Files.clear();
        			if(selectionModelPeers.getSelectedItem() != null){
        				Files.addAll(files.get(selectionModelPeers.getSelectedItem()));
        				if(selectionModelPeers.getSelectedItem().contains(You)){
        					enabler = true;
        				}
        				else{
        					enabler = false;
        				}
        				curSelectedPeer = selectionModelPeers.getSelectedItem();
        			}
        		}else{
        			Files.clear();
        		}
        	}
        });
        
        
        FileBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent event){
        		if(enabler){ // if they are choosing files as themselves
        		 UnshareButton.setDisable(false);
        		 DownloadButton.setDisable(true);
        		}
        		else{ // if they are choosing files from another peer
        		 DownloadButton.setDisable(false);
        		 UnshareButton.setDisable(true);
        		}
        	}
        });
        
        AboutMenu.setOnAction(new EventHandler<ActionEvent>(){
        	
        	@Override
        	public void handle(ActionEvent event){
        		P2PManager.PopInformationMessage("About Page", "Developed by Nathan Hertner for\n"
        				+ "B.C.S Honours at Carleton University\n"
        				+ "Student Number: 100894822\n"
        				+ "Developed February-April 2016");
        	}
        });
        
        HelpMenu.setOnAction(new EventHandler<ActionEvent>(){
        	
        	@Override
        	public void handle(ActionEvent event){
        		StartupP2PApp.helpStage.show();
        	}
        });
        
        SortPicker.setOnAction(new EventHandler<ActionEvent>(){
        	
        	@Override
        	public void handle(ActionEvent event){
        		sortedOnce = true;
        		String mode = SortPicker.getSelectionModel().getSelectedItem();
        		if(PeerBox.getSelectionModel().getSelectedIndex() <= 0){
        			//sorting my own files
        			sortFiles(resolveModeEnum(mode));
        		}else{
        			if(SortPicker.getSelectionModel().getSelectedIndex() == 0){ // we are just sorting by name
        				sortFiles(resolveModeEnum(mode));
        			}else{ // sorting by file infos
        				P2PManager.transmitSortRequest(resolveModeEnum(mode), selectionModelPeers.getSelectedItem());
        			}
        		}
        		
        	}
        });
        
        
        
        
    }
    
    public static void forceFileListUpdate(String i){
    	Platform.runLater(() -> {
    		if(i!=null){
    			if(curSelectedPeer.contains(i)){// if we are looking at the peer who just sent us a new file list
    				Files.clear();
    				Files.addAll(files.get(i));
    			}
    		}
    	});
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
    		  JPFSPrinting.logError("Unknown Host Exception: GUI Control Class in getCompName method", errorLevel.RECOVERABLE);
    	  }
    	  return hostname;
      }
   
      public static String packFiles(String peerPackingFor){ // package the file list into a single delimited string
    	  String toSend = "";
    	  Set<String> temp = yourFilePermissions.keySet();
    	  for(String file : temp){
    		  if(yourFilePermissions.get(file).contains("ALL") || 
    				  yourFilePermissions.get(file).contains(peerPackingFor)){
    			  toSend += file;
    			  toSend += "&%";
    		  }
    	  }
    	  return toSend;
      }
      
      static String packFilesList(List<String> in){
    	  String toSend = "";
    	  for(int x = 0; x<in.size(); x++){
    		  toSend = toSend + in.get(x);
    		  toSend = toSend + "&%";
    	  }
    	  return toSend;
      }
      
      public static void updateFiles(String peerToUp, String[] f){
    	 if(Peers!=null){
    	  //update the peers files for gui
    		if(!Peers.contains(peerToUp))
    		  addPeer(peerToUp);
    	  
    	  	if(!peerToUp.contains(You) && files.get(peerToUp)!=null){
    		  files.get(peerToUp).clear();
    	  	  for(int x = 0; x<f.length; x++){
    	  		files.get(peerToUp).add(f[x]);
    	  	  }
    	  	}else{
    		  JPFSPrinting.logWarning("Skipping File List Update: Not Ready. Will receive another update shortly");
    	  	}
    	  	forceFileListUpdate(peerToUp);
    	 }
      }
     
      
      public static String getFiles(String peer){
    	  return packFiles(peer);
      }
      
      public static int numFiles(String peer){
    	  if(files.get(peer).isEmpty()){
    		  return 0;
    	  }
    	  
    	  return files.get(peer).size();
      }
      
      public static void addPeer(String toSet){
    	  Platform.runLater(() -> {
    		  if(!Peers.contains(toSet)){
    			Peers.add(toSet);  
    	    	files.put(toSet, new ArrayList<String>());
    	  	  }
    	  });
      }
      
      public static void removePeer(String toRem){
    	  Platform.runLater(() -> {
    		  if(Peers.indexOf(toRem) != -1){
    			  files.get(toRem).clear();
    			  removedIndex = Peers.indexOf(toRem);
    			  Peers.remove(toRem);
    		  }
    		  try {
    			  Thread.sleep(800); // prevents problems with synching between peers / own data sets
    		  } catch (InterruptedException e) {
    			  JPFSPrinting.logError("Sleep method was interrupted in GUI Control removePeer method", errorLevel.CAN_IGNORE);
    		  }
    	  });
      }
      
      public static int indexOf(String n){
    	  return removedIndex;
      }
      
      public static void sortYouPeerToTop(){
    	  for(int x = 0; x<Peers.size(); x++){
          	   if(Peers.get(x).contains(You) && x!=0){ // if we created the list without YOU at the top, fix it
          		   String str = Peers.get(0);
          		   Peers.set(0, You);
          		   Peers.set(x, str);
          		   break;
          	   }
             }
      }
      
      public static void doRefresh(){
    	  Platform.runLater(() -> {
    		  sortYouPeerToTop();
    		  GUI_Control_ApplicationLevel.updateGUIForced();
    	  });
      }
      
      public static JPFSFile findJPFSFile(String fName){
    	  for(int x = 0; x<JPFSFiles.size(); x++){
    		  if(JPFSFiles.get(x).name.contains(fName)){
    			  return JPFSFiles.get(x);
    		  }
    	  }
    	  return null;
      }
      
      public sortType resolveModeEnum(String s){
    	  if(s.equals("By Name")){
    		  return sortType.NAME;
    	  }else if(s.equals("By Size")){
    		  return sortType.SIZE;
    	  }else if(s.equals("By Extension")){
    		  return sortType.EXTENSION;
    	  }else if(s.equals("By Date")){
    		  return sortType.DATE;
    	  }
    	  return null;
      }
      
      public static void initFileList(String you){
           files.put(you, new ArrayList<String>());
      }
      
	public static void sortFiles(sortType mode){
    	  switch(mode){
    	  case NAME: Collections.sort(Files, Collator.getInstance());
    
    	  	break;
    	  case SIZE: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return Long.compare(findJPFSFile(ob1).size.getByteSize(), findJPFSFile(ob2).size.getByteSize());
    			}
    			});
 
    	  	break;
    	  case EXTENSION: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return (FilenameUtils.getExtension(ob1).compareTo(FilenameUtils.getExtension(ob2)));
    			}
    			});
    
    	  	break;
    	  case DATE: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return findJPFSFile(ob1).date.compareTo(findJPFSFile(ob2).date);
    			}
    			});
    	
    	  	break;
    	  
    	  default:
			break;
      	  }
      }
	
	public static List<String> sortFilesReturn(sortType mode, String files){
		String[] list = files.split("&%");
		List<String> temp = new ArrayList<String>();
		for(String file : list){
			temp.add(file);
		}
		switch(mode){
  	  case NAME: Collections.sort(temp, Collator.getInstance());
  	  
  	  	break;
  	  case SIZE: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return Long.compare(findJPFSFile(ob1).size.getByteSize(), findJPFSFile(ob2).size.getByteSize());
  			}
  			});
 
  	  	break;
  	  case EXTENSION: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return (FilenameUtils.getExtension(ob1).compareTo(FilenameUtils.getExtension(ob2)));
  			}
  			});
  	 
  	  	break;
  	  case DATE: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return findJPFSFile(ob1).date.compareTo(findJPFSFile(ob2).date);
  			}
  			});
 
  	  	break;
  	  
  	  	default:
			break;
    	}
		return temp;
	}
	
      
}



