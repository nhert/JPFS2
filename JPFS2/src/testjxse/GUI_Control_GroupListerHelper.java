package testjxse;

import java.util.ArrayList;
import java.util.List;

import testjxse.JPFSPrinting.errorLevel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;

public class GUI_Control_GroupListerHelper {
	private static ListView<String> groupLister;
	
	@SuppressWarnings("unchecked")
	GUI_Control_GroupListerHelper(Pane page){
		groupLister = (ListView<String>) page.lookup("#GroupList");
	}
	
	public static void doRefresh(){
		Platform.runLater(() -> {
			List<String> GroupsList = new ArrayList<String>();
			ObservableList<String> groups;
			//System.out.println("Updated GUI");
			GroupsList.clear();
	      	Object[] groupArray = StartupOptions.GroupsDiscovered.toArray();
	      	System.out.println("");
	      	GroupsList = new ArrayList<String>();
	      	if(groupArray!=null){
				  for(int x = 0; x<groupArray.length; x++){
				  	GroupsList.add(groupArray[x].toString());
				  }
	      	}
				groups = FXCollections.observableList(GroupsList);
				try{
				  groupLister.setItems(groups);
				} catch (Exception e){
					JPFSPrinting.logError("Error when refreshing the GUI in the GroupList Control Helper", errorLevel.RECOVERABLE);
				}
		});
	}
}
