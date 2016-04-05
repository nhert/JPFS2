package testjxse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import testjxse.JPFSPrinting.errorLevel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class StartupP2PApp extends Application{
	
    static final P2PManager info = new P2PManager(); // instance of the manager
    static StartupOptions op; // instance of the startup options class
    public static Stage primaryStage; // the primary JPFS stage that displays the GUI
    public static String helpText; // string containing the help page document
    public static Stage helpStage; // stage that displays the help page GUI
	
	public static final String AppName = "JPFS2"; // name of the client application
	
	public static void main(String[] args) throws Exception{
	  //disable logs from jxta, which overly clutter the console
	  System.setProperty("net.jxta.logging.Logging", "OFF");
	  System.setProperty("net.jxta.level", "OFF");
	  //launch the GUI
	  launch(args);
	  //This block is reached once the GUI is closed
	  info.close(AppName);
	  info.shutdown();
	  System.out.println("Closing the JPFS-2 Client");
	  System.exit(0);
	}
	
	@Override
	public void start(Stage pStage) throws Exception {
        try {
        	primaryStage = pStage;
        	//create directories for JPFS
        	File tempDir = new File(".\\temp");
        	tempDir.mkdir();
        	File jProfsDir = new File(".\\Profiles");
        	jProfsDir.mkdir();
        	File dlDir = new File(".\\JPFSDownloads");
        	dlDir.mkdir();
        	//initialize the help page
        	readHelpPage();
        	initHelpStage();
        	//get startup options from the user
        	op = new StartupOptions();
        	op.StartOptions();
        } catch (Exception ex) {
        	JPFSPrinting.logError("Exception in initializing StartupOptions in StartupP2PApp", errorLevel.SEVERE);
        }
	}
	
	//Initializes the main gui stage
	public static void StartMain() throws MalformedURLException, IOException{
		//init the main stage GUI
		initMainStage();
		//initialize the manager
        info.initGlobal(AppName);
        //display GUI
        primaryStage.show();
        //start the manager advertisement discovery thread
        info.fetch_advertisements();
        //start thread that auto-refreshes the peer list
        Thread pLoop = new Thread(new peerLoop());
        pLoop.start();
        GUI_Control.doRefresh();
	}
	
	//Start JPFS-2 as a private subgroup of the global peer group
	public static void StartMainCustom(String gname, boolean creating, String desc, boolean pwordGroup, String pword) throws MalformedURLException, IOException{
		initMainStage();
		//initialize the manager
        info.initCustom(AppName, gname, creating, desc, pwordGroup, pword);
        //display GUI
        primaryStage.show();
        //start the manager advertisement discovery thread
        info.fetch_advertisements();
        //start thread that auto-refreshes the peer list
        Thread pLoop = new Thread(new peerLoop());
        pLoop.start();
        GUI_Control.doRefresh();
	}
	
	//Start JPFS-2 in its default state as part of the global peer group
	public static void initMainStage() throws MalformedURLException, IOException{
		Platform.setImplicitExit(true);
		File mainGUI = new File("./MyGUI.fxml");
    	Pane page = (Pane) FXMLLoader.load(mainGUI.toURI().toURL());
    	@SuppressWarnings("unused")
		GUI_Control_ApplicationLevel GUICAL = new GUI_Control_ApplicationLevel(info, page);
    	Scene scene = new Scene(page);
        primaryStage.setScene(scene);
        primaryStage.setTitle(AppName);
        primaryStage.setResizable(false);
	}
	
	//Initialize the help stage for help menu
	public static void initHelpStage() throws MalformedURLException, IOException{
		File mainGUI = new File("./HelpGUI.fxml");
    	Pane page = (Pane) FXMLLoader.load(mainGUI.toURI().toURL());
    	Scene scene = new Scene(page);
    	TextArea helpArea = (TextArea) page.lookup("#HelpTextArea");
    	helpArea.setText(helpText);
    	helpStage = new Stage();
    	helpStage.setScene(scene);
    	helpStage.setTitle("HELP WINDOW");
    	helpStage.setResizable(false);
	}
	
	//Loads the help_page.txt document into the client
	public static void readHelpPage() throws IOException{
		File helpTXT = new File("./help_page.txt");
		BufferedReader br = new BufferedReader(new FileReader(helpTXT));
		String line;
		helpText = "";
		while((line = br.readLine()) != null){
			helpText += line;
			helpText += "\n";
		}
		br.close();
	}
	
	//class thread that loops for peer GUI updates every second
	private static class peerLoop implements Runnable{
		public void run(){
			while(true){
				try {
					Thread.sleep(1000);
					GUI_Control.doRefresh();
				} catch (InterruptedException e) {
					JPFSPrinting.logError("Sleep method interrupted in StartupP2PApp peerLoop thread", errorLevel.CAN_IGNORE);
				}
			}
		}
	}
	
	
}
