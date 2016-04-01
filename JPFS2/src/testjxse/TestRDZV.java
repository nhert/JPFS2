package testjxse;

import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class TestRDZV {
	public static final File ConfigFile = new File("./RDZVTest");
	public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, "RDZV Peer".getBytes());
	
	public static void main(String[] args){
		
		
		try{
			NetworkManager netman = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS, "RDZV Peer", ConfigFile.toURI());
			NetworkConfigurator nc = netman.getConfigurator();
			nc.setTcpPort(10000);
			nc.setTcpEnabled(true);
			nc.setTcpIncoming(true);
			nc.setTcpOutgoing(true);
			nc.setUseMulticast(false);
			nc.setPeerID(PID);
			PeerGroup NetPeerGroup = netman.startNetwork();
			JOptionPane.showConfirmDialog(null, "PRESS OK WHEN DONE CONNECTING EDGE");
			List<PeerID> ev= NetPeerGroup.getRendezVousService().getLocalEdgeView();
			List<PeerID> rv= NetPeerGroup.getRendezVousService().getLocalRendezVousView();
			
			for(PeerID pid : ev){
				System.out.println("EDGE: "+pid.toString());
			}
			
			for(PeerID pid : rv){
				System.out.println("RDZV: "+pid.toString());
			}
			
			System.out.println("RDZV DONE");
			netman.stopNetwork();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
