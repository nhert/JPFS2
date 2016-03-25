package testjxse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;

public class GUI_Control_GroupLister implements Initializable{
	
	@FXML
	private ListView<String> GroupList;
	
	public static ObservableList<String> groups;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		 assert GroupList != null : "fx:id=\"GroupList\" was not injected: check your FXML file 'GroupLister.fxml'.";
		 List<String> GroupBackend = new ArrayList<String>();
		 groups = FXCollections.observableList(GroupBackend);
		 GroupList.setItems(groups);
		 GroupList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		 
	        	GroupList.setOnMouseClicked(new EventHandler<MouseEvent>(){
	        		@Override
	        		public void handle(MouseEvent click){
	        			if(click.getClickCount() == 2){
	        				System.out.println("A GROUP WAS SELECTED " + GroupList.getSelectionModel().getSelectedItem());
	        				try {
	        					StartupP2PApp.op.cleanup();
								StartupP2PApp.StartMainCustom(GroupList.getSelectionModel().getSelectedItem(), false, "");
							} catch (IOException e) {
								e.printStackTrace();
							}
	        			}
	        		}
	        	});
	}
	
	public static void doRefresh(){
		Platform.runLater(() -> {
			GUI_Control_GroupListerHelper.doRefresh();
		});
	}
	

}
