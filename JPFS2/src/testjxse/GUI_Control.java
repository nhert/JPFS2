package testjxse;



import java.awt.Event;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;

import org.apache.commons.io.FilenameUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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
    
    
    private String name = "You";
    private boolean enabler = true;
    public static List<List<String>> files = new ArrayList<List<String>>(); // location 0 is always YOU
    public static List<JPFSFile> JPFSFiles = new ArrayList<JPFSFile>();
    public MultipleSelectionModel<String> selectionModelFiles;
    public MultipleSelectionModel<String> selectionModelPeers;
    public static ObservableList<String> Peers;
    private static Map<String,Integer> indexes = new Hashtable<String,Integer>();
    public static int removedIndex = -1;
    public static List<String> FilesList = new ArrayList<String>();
    public static ObservableList<String> Files;
    public static ObservableList<String> Sorts;
    public static int curSelectedPeer = 0;
    private static boolean sortedOnce = false;

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
        selectionModelPeers.setSelectionMode(SelectionMode.SINGLE);
        for (int x = 0; x<25; x++)
          files.add(x, new ArrayList<String>());
        indexes.put("THISISYOU", 0);
        Sorts.add("By Name");
        Sorts.add("By Date");
        Sorts.add("By Size");
        Sorts.add("By Extension");
        
        
        
        // initialize your logic here: all @FXML variables will have been injected
        DownloadButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
            	System.out.println("Download");
            	if(selectionModelFiles.getSelectedIndex()!=-1 && selectionModelPeers.getSelectedIndex()!=0) // proper selections in gui
                  P2PManager.transmitFileRequest(selectionModelFiles.getSelectedItem(), selectionModelPeers.getSelectedItem());
            }
        });
        
        ShareButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
            	//System.out.println("Share");
            	final JFileChooser fc = new JFileChooser();
            	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            	if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){ // selected a file to share
            		File newf;
            		newf =  fc.getSelectedFile();
            		
            		try {
            			//Files.add(newf.getCanonicalPath()); // update the list to show your new file
            			JPFSFiles.add(new JPFSFile(newf));
            			//System.out.println(JPFSFiles.get(0).name);
            			//System.out.println(JPFSFiles.get(0).path);
            			//System.out.println(JPFSFiles.get(0).size);
						files.get(0).add(newf.getName()); // update your storage of files
						P2PManager.transmitUpdateFiles(packFiles());
						Files.clear();
		        		Files.addAll(files.get(0));
					} catch (IOException e) {
						e.printStackTrace();
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
            	
            	files.get(0).remove(selectionModelFiles.getSelectedIndex()); // remove your files from the storage
                Files.remove(selectionModelFiles.getSelectedIndex()); // update the list to show your new file list
                P2PManager.transmitUpdateFiles(packFiles());
            }
        });
 
        PeerBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent event){
        		//System.out.println("PeerBox");
        		Files.clear();
        		Files.addAll(files.get(selectionModelPeers.getSelectedIndex()));
        		if(selectionModelPeers.getSelectedIndex() == 0){
        			enabler = true;
        		}
        		else{
        			enabler = false;
        		}
        		curSelectedPeer = selectionModelPeers.getSelectedIndex();
        		
        	}
        });
        
        FileBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent event){
        		//System.out.println("FileBox");
        		
        		System.out.println(selectionModelFiles.getSelectedItem());
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
    
    public static void forceFileListUpdate(int i){
    	Platform.runLater(() -> {
    	if(curSelectedPeer == i){ // if we are looking at the peer who just sent us a new file list
    		System.out.println("New File List from Selected Peer, Updating");
    		Files.clear();
    		Files.addAll(files.get(i));
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
    	      System.out.println("Hostname can not be resolved");
    	  }
    	  return hostname;
      }
   
      static String packFiles(){ // package the file list into a single delimited string
    	  String toSend = "";
    	  for(int x = 0; x<files.get(0).size(); x++){
    		  toSend = toSend + files.get(0).get(x);
    		  toSend = toSend + "&%";
    	  }
    	  return toSend;
      }
      
      static String packFiles(List<String> in){
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
    	  
    	  if(Peers.indexOf(peerToUp) >0){
    	  files.get(Peers.indexOf(peerToUp)).clear();
    	  for(int x = f.length-1; x>-1; x--){
    		  files.get(Peers.indexOf(peerToUp)).add(f[x]);
    	  }
    	  }else{
    		  System.out.println("INVALID PEER INDEX IN UPDATE FILES METHOD");
    	  }
    	  }
    	  
      }
     
      
      public static String getFiles(){
    	  return packFiles();
      }
      
      public static int numFiles(String peer){
    	  if(files.get(Peers.indexOf(peer)).isEmpty()){
    		  return 0;
    	  }
    	  
    	  return files.get(Peers.indexOf(peer)).size();
      }
      
      public static void addPeer(String toSet){
    	  Platform.runLater(() -> {
    		  if(!Peers.contains(toSet)){
    			Peers.add(toSet);  
    	    	indexes.put(toSet,Peers.indexOf(toSet)); // persistently store for later
    	    	System.out.println("New Peer Index!!!: " + indexes.get(toSet));
    	  	  }
    	  });
      }
      
      public static void removePeer(String toRem){
    	  Platform.runLater(() -> {
    	  System.out.println("Remove Peer: " + toRem);
    	  if(Peers.indexOf(toRem) != -1){
    	  files.get(Peers.indexOf(toRem)).clear();
    	  removedIndex = Peers.indexOf(toRem);
    	  Peers.remove(indexes.get(toRem));
          // delete their file
    	  indexes.clear();
    	  for(int x = 0; x<Peers.size(); x++){
    		  indexes.put(Peers.get(x), x);
    	  }
    	  }
    	  try {
			Thread.sleep(800); // prevents problems with synching between peers / own data sets
		  } catch (InterruptedException e) {
			//blank
		  }
    	  });
      }
      
      public static int indexOf(String n){
    	  return removedIndex;
      }
      
      public static void doRefresh(){
    	  Platform.runLater(() -> {
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



