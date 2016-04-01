package testjxse;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import javax.swing.JOptionPane;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class TestEDGE {
	public static final File ConfigFile = new File("./EDGETest");
	public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, "EDGE Peer".getBytes());
	
	public static void main(String[] args){
		
		
		try{
			NetworkManager netman = new NetworkManager(NetworkManager.ConfigMode.EDGE, "EDGE Peer", ConfigFile.toURI());
			NetworkConfigurator nc = netman.getConfigurator();
			nc.clearRendezvousSeeds();
			String theSeed = "tcp://" + "127.0.0.1" + ":" + 10000;
			URI seeduri = URI.create(theSeed);
			nc.addSeedRendezvous(seeduri);
			
			
			nc.setTcpPort(10100);
			nc.setTcpEnabled(true);
			nc.setTcpIncoming(true);
			nc.setTcpOutgoing(true);
			nc.setUseMulticast(false);
			nc.setPeerID(PID);
			
			
			PeerGroup NetPeerGroup = netman.startNetwork();
			
			NetPeerGroup.getRendezVousService().setAutoStart(false);
			
			if(netman.waitForRendezvousConnection(120000)){
				JOptionPane.showConfirmDialog(null, "CONNECTED TO THE RDZV");
			}else{
				System.out.println("NO RENDEZ");
			}
			
			netman.stopNetwork();
			
			
			System.out.println("EDGE DONE");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
