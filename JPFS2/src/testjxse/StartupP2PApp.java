package testjxse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;


//IDEA
//create administrated p groups with admin priv. on the host
//non admin cant start the group

public class StartupP2PApp extends Application{
	
    static P2PManager info = new P2PManager();
    static StartupOptions op;
    public static Stage primaryStage;
	
	public static final String AppName = "JPFS2";
	
	public static void main(String[] args) throws IOException{
	  //disable logs from jxta, which overly clutter the console
	  System.setProperty("net.jxta.logging.Logging", "OFF");
	  System.setProperty("net.jxta.level", "OFF");
	  //launch the GUI
	  launch(args);
	  //This block is reached once the GUI is closed
	  info.close(AppName);
	  info.shutdown();
	  System.out.println("Closing the JPFS2 Client");
	  System.exit(0);
	}
	
	@Override
	public void start(Stage pStage) throws Exception {
        try {
        	primaryStage = pStage;
        	
        	File tempDir = new File(".\\temp");
        	tempDir.mkdir();
        	File jProfsDir = new File(".\\Profiles");
        	jProfsDir.mkdir();
        	File dlDir = new File(".\\JPFSDownloads");
        	dlDir.mkdir();
        	
        	
        	
        	op = new StartupOptions();
        	op.StartOptions();
            
            
        } catch (Exception ex) {
            Logger.getLogger(StartupP2PApp.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
	
	public static void StartMain() throws MalformedURLException, IOException{
		initMainStage();
		//initialize the manager
        info.initGlobal(AppName);
        //display GUI
        primaryStage.show();
        //start the manager advertisement discovery thread
        info.fetch_advertisements();
        //start the manager timeout checker thread
        info.check_timeouts();
        //start thread that auto-refreshes the peer list
        Thread pLoop = new Thread(new peerLoop());
        pLoop.start();
	}
	
	public static void StartMainCustom(String gname, boolean creating, String desc) throws MalformedURLException, IOException{
		initMainStage();
		//initialize the manager
        info.initCustom(AppName, gname, creating, desc);
        //display GUI
        primaryStage.show();
        //start the manager advertisement discovery thread
        info.fetch_advertisements();
        //start the manager timeout checker thread
        info.check_timeouts();
        //start thread that auto-refreshes the peer list
        Thread pLoop = new Thread(new peerLoop());
        pLoop.start();
	}
	
	public static void initMainStage() throws MalformedURLException, IOException{
		File mainGUI = new File("./MyGUI.fxml");
    	Pane page = (Pane) FXMLLoader.load(mainGUI.toURI().toURL());
    	GUI_Control_ApplicationLevel GUICAL = new GUI_Control_ApplicationLevel(info, page);
    	Scene scene = new Scene(page);
        primaryStage.setScene(scene);
        primaryStage.setTitle(AppName);
        primaryStage.setResizable(false);
	}
	
	private static class peerLoop implements Runnable{
		public void run(){
			while(true){
				try {
					Thread.sleep(1000);
					GUI_Control.doRefresh();
					//System.out.println("Checking for New Peers");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	
	}
	
	
}
