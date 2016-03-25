package testjxse;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class GUI_Control_ApplicationLevel {
	
	private static P2PManager manager;
	private static ListView<String> peerList;
	private static ListView<String> fileList;
	private static Button but; 

	@SuppressWarnings("unchecked")
	GUI_Control_ApplicationLevel(P2PManager m, Pane GUI){
		manager = m;
		
		peerList = (ListView<String>) GUI.lookup("#PeerBox");
		fileList = (ListView<String>) GUI.lookup("#FileBox");
        but = (Button) GUI.lookup("#DiscoverButton");
        
        if(but!=null){
          but.setOnAction(new EventHandler<ActionEvent>() {

              @Override
              public void handle(ActionEvent event) {
              	  System.out.println("Manual Peer Refresh");
              	  updateGUIForced();
              }
          });
        }
        
        if(peerList!=null){
        	peerList.setOnMouseClicked(new EventHandler<MouseEvent>(){
        		@Override
        		public void handle(MouseEvent click){
        			if(click.getClickCount() == 2){
        				String selected = peerList.getSelectionModel().getSelectedItem();
        				manager.lookupPeerInfo(selected);
        			}
        		}
        	});
        } 
        
        if(fileList!=null){
        	fileList.setOnMouseClicked(new EventHandler<MouseEvent>(){
        		@Override
        		public void handle(MouseEvent click){
        			if(click.getClickCount() == 2){
        				String selected = fileList.getSelectionModel().getSelectedItem();
        				String selected2 = peerList.getSelectionModel().getSelectedItem();
        				manager.lookupFileInfo(selected, selected2);
        			}
        		}
        	});
        } 
	}
	
	public static void updateGUIForced(){ // updates GUI on interval with discovered peers
	      
		  List<String> PeersList = new ArrayList<String>();
		  ObservableList<String> Peers;
			//System.out.println("Updated GUI");
			PeersList.clear();
      	String[] peers = manager.getPeers();
      	PeersList = new ArrayList<String>();
      	if(peers!=null){
			  for(int x = 0; x<manager.getLen(); x++){
			  	PeersList.add(peers[x]);
			  }
      	}
			Peers = FXCollections.observableList(PeersList);
			try{
			  peerList.setItems(Peers);
			} catch (Exception e){
				System.out.println("Exception in UPDATING GUI");
			}
	}
	
	 public static void forceFileListUpdate(){
    	 GUI_Control.forceFileListUpdate(peerList.getSelectionModel().getSelectedIndex());
     }
	
	
	
}
