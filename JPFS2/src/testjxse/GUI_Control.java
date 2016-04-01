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
import java.util.Hashtable;
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

public class GUI_Control
    implements Initializable {
	
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
    //private static Map<String,Integer> indexes = new Hashtable<String,Integer>();
    public static int removedIndex = -1;
    public static List<String> FilesList = new ArrayList<String>();
    public static ObservableList<String> Files;
    public static ObservableList<String> Sorts;
    public static String curSelectedPeer = "";
    private static boolean sortedOnce = false;
    public static String You;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert DownloadButton != null : "fx:id=\"DownloadButton\" was not injected: check your FXML file 'simple.fxml'.";
        assert ShareButton != null : "fx:id=\"ShareButton\" was not injected: check your FXML file 'simple.fxml'.";
        assert DiscoverButton != null : "fx:id=\"DiscoverButton\" was not injected: check your FXML file 'simple.fxml'.";
        assert UnshareButton != null : "fx:id=\"UnshareButton\" was not injected: check your FXML file 'simple.fxml'.";
        assert PeerBox != null : "fx:id=\"PeerBox\" was not injected: check your FXML file 'simple.fxml'.";
        assert FileBox != null : "fx:id=\"FileBox\" was not injected: check your FXML file 'simple.fxml'.";
        assert AboutMenu != null : "fx:id=\"About\" was not injected: check your FXML file 'simple.fxml'.";
        assert HelpMenu != null : "fx:id=\"Help\" was not injected: check your FXML file 'simple.fxml'.";
        assert SortPicker != null : "fx:id=\"SortPicker\" was not injected: check your FXML file 'simple.fxml'.";
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
        //indexes.put("THISISYOU", 0);
        Sorts.add("By Name");
        Sorts.add("By Date");
        Sorts.add("By Size");
        Sorts.add("By Extension");
        
        
        
        // initialize your logic here: all @FXML variables will have been injected
        DownloadButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
            	System.out.println("Download");
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
            		
            		try {
            			List<String> selectedItems = PeerBox.getSelectionModel().getSelectedItems();
            			System.out.println(selectedItems);
            			if((selectedItems.size() >= 1 && selectedItems.contains(You))||(selectedItems==null)||(selectedItems.isEmpty())){ // if you select nothing or selected yourself as the only peer
            				System.out.println("SHARING WITH EVERYONE");
            				JPFSFiles.add(new JPFSFile(newf));
            				List<String> temp = new ArrayList<>();
            				temp.add("ALL");
    						yourFilePermissions.put(newf.getName(), temp);
    						files.get(You).add(newf.getName());
    						P2PManager.transmitUpdateFiles();
    						Files.clear();
    		        		Files.addAll(files.get(You));
            			}else if(selectedItems.size() >= 1 && !selectedItems.contains(You)){
            				System.out.println("SHARING WITH SPECIFIC");
            				JPFSFiles.add(new JPFSFile(newf));
            				List<String> temp = new ArrayList<>();
            				temp.addAll(selectedItems);
            				yourFilePermissions.put(newf.getName(), temp);
            				files.get(You).add(newf.getName());
            				P2PManager.transmitUpdateFiles();
            				Files.clear();
            				Files.addAll(files.get(You));
            			}else{
            				System.out.println("ERROR SHARE");
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
            	//System.out.println("Unshare");
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
        		//System.out.println("PeerBox");
        		Files.clear();
        		if(selectionModelPeers.getSelectedItem() != null){
        		System.out.println("SELECTED: "+selectionModelPeers.getSelectedItem());
        		Files.addAll(files.get(selectionModelPeers.getSelectedItem()));
        		if(selectionModelPeers.getSelectedItem().contains(You)){
        			enabler = true;
        		}
        		else{
        			enabler = false;
        		}
        		curSelectedPeer = selectionModelPeers.getSelectedItem();
        		}
        	}
        });
        
        /*PeerBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent event){
        		//System.out.println("PeerBox");
        		
        		
        	}
        });*/
        
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
        		//System.out.println("About!");
        		P2PManager.PopInformationMessage("About Page", "Developed by Nathan Hertner for\n"
        				+ "B.C.S Honours at Carleton University\n"
        				+ "Student Number: 100894822\n"
        				+ "Developed February-April 2016");
        	}
        });
        
        HelpMenu.setOnAction(new EventHandler<ActionEvent>(){
        	
        	@Override
        	public void handle(ActionEvent event){
        		//System.out.println("Help!");
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
        				System.out.println("SORTING NAME FOR FILES");
        				sortFiles(resolveModeEnum(mode));
        			}else{ // sorting by file infos
        				//REQUEST A SORTED FILE LIST
        				//TODO: TIME PERMITTING
        				//System.out.println("SORTING INFO FOR FILES");
        				//P2PManager.transmitSortRequest(resolveModeEnum(mode), selectionModelPeers.getSelectedItem());
        			}
        		}
        		
        	}
        });
        
        
        
        
    }
    
    public static void forceFileListUpdate(String i){
    	Platform.runLater(() -> {
    	if(i!=null){
    	if(curSelectedPeer.contains(i)){// if we are looking at the peer who just sent us a new file list
    		System.out.println("New File List from Selected Peer, Updating");
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
    	  /*for(int x = 0; x<files.get(You).size(); x++){
    		  toSend = toSend + files.get(You).get(x);
    		  toSend = toSend + "&%";
    	  }
    	  return toSend;*/
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
      
      /*static String packFiles(List<String> in){
    	  String toSend = "";
    	  for(int x = 0; x<in.size(); x++){
    		  toSend = toSend + in.get(x);
    		  toSend = toSend + "&%";
    	  }
    	  return toSend;
      }*/
      
      public static void updateFiles(String peerToUp, String[] f){
    	 if(Peers!=null){
    	  //update the peers files for gui
    		if(!Peers.contains(peerToUp))
    		  addPeer(peerToUp);
    	  
    	  	if(!peerToUp.contains("(You)") && files.get(peerToUp)!=null){
    		  files.get(peerToUp).clear();
    	  	  for(int x = f.length-1; x>-1; x--){
    	  		files.get(peerToUp).add(f[x]);
    	  	  }
    	  	}else{
    		  System.out.println("Skipping File List Update");
    	  	}
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
    	    	System.out.println("New Peer Discovered: "+toSet);
    	  	  }
    	  });
      }
      
      public static void removePeer(String toRem){
    	  Platform.runLater(() -> {
    	  //System.out.println("Removing Peer: " + toRem);
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
    	  System.out.println("exited loop on file find");
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
    	  System.out.println("SORT BY NAME");
    	  	break;
    	  case SIZE: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return Long.compare(findJPFSFile(ob1).size.getByteSize(), findJPFSFile(ob2).size.getByteSize());
    			}
    			});
    	  System.out.println("SORT BY SIZE");
    	  	break;
    	  case EXTENSION: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return (FilenameUtils.getExtension(ob1).compareTo(FilenameUtils.getExtension(ob2)));
    			}
    			});
    	  System.out.println("SORT BY EXT");
    	  	break;
    	  case DATE: Collections.sort(Files, new Comparator<String>() {
    		  public int compare(String ob1, String ob2) {
    			return findJPFSFile(ob1).date.compareTo(findJPFSFile(ob2).date);
    			}
    			});
    	  System.out.println("SORT BY DATE");
    	  	break;
    	  
    	  default:
			break;
      	  }
      }
	
	public static List<String> sortFilesReturn(sortType mode){
		List<String> temp = Files;
		Platform.runLater(() -> {
		switch(mode){
  	  case NAME: Collections.sort(temp, Collator.getInstance());
  	  System.out.println("SORT BY NAME");
  	  	break;
  	  case SIZE: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return Long.compare(findJPFSFile(ob1).size.getByteSize(), findJPFSFile(ob2).size.getByteSize());
  			}
  			});
  	  System.out.println("SORT BY SIZE");
  	  	break;
  	  case EXTENSION: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return (FilenameUtils.getExtension(ob1).compareTo(FilenameUtils.getExtension(ob2)));
  			}
  			});
  	  System.out.println("SORT BY EXT");
  	  	break;
  	  case DATE: Collections.sort(temp, new Comparator<String>() {
  		  public int compare(String ob1, String ob2) {
  			return findJPFSFile(ob1).date.compareTo(findJPFSFile(ob2).date);
  			}
  			});
  	  System.out.println("SORT BY DATE");
  	  	break;
  	  
  	  default:
			break;
    	  }
		 
		});
		return temp;
	}
	
      
}



