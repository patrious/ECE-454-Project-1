package ece454_project1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkManager {
    private ThreadGroup myServerThread = new ThreadGroup("myServerThread");
    private ThreadGroup myClientThreads = new ThreadGroup("myClientThreads");
    private FileManager FileMgr = null;
    int myPort;
    
    public ArrayList<String> knownPeers = new ArrayList<String>();
    public ArrayList<String> connectedPeers = new ArrayList<String>();

    public NetworkManager() {
        
    }
    
    public void StartManager(FileManager fm, int port)
    {
        myPort = port;
        FileMgr = fm;
        
        Thread t = new Thread(myServerThread, new Server(myPort, this, fm));
        t.setName("Server");
        t.start();
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("peerlist.txt"));
        } catch (FileNotFoundException ex) {
            try {
                br = new BufferedReader(new FileReader("../peerlist.txt"));
            } catch (FileNotFoundException ex2) {
                System.err.println("peerlist.txt file not found!");
                System.exit(-1);
            }
        }
        try {
            String line = br.readLine();
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> localIPs = new ArrayList<String>();
            
            //Get all local addresses, so we don't end up trying to join them
            for (NetworkInterface netint : Collections.list(ifaces)) {
                Enumeration<InetAddress> addrs = netint.getInetAddresses();
                for (InetAddress addr : Collections.list(addrs)) {
                    localIPs.add(addr.getHostAddress());
                }
            }
            
            while (line != null) {
                String[] peer = line.split(",");
                knownPeers.add(peer[0] + ":" + peer[1]);
                
                //If the peer in the list is us, then add it to connectedPeers
                //so nobody tries to connect to themselves
                if ((myPort == Integer.parseInt(peer[1])) && (localIPs.contains(peer[0]))) {
                    connectedPeers.add(peer[0] + ":" + peer[1]);
                } else {
                    join(FileMgr, peer[0], Integer.parseInt(peer[1]));
                }
                
                line = br.readLine();
            }
            //System.out.println("ConnectedPeers: " + connectedPeers);
            
            br.close();
        } catch (IOException ex) {
            System.err.println("Error reading peerlist.txt");
        }
    }
    
    synchronized public int addPeer(Socket socket) {
        if (!connectedPeers.contains(socket.getInetAddress().getHostAddress() +
                ":" + socket.getPort())) {
            connectedPeers.add(socket.getInetAddress().getHostAddress() +
                ":" + socket.getPort());
            System.out.println(connectedPeers);
            return 0;
        } else {
            return -1;
        }
    }
    
    synchronized public void join(FileManager fm, String peerHost, int peerPort) {
        System.out.println("Connecting to " + peerHost + ":" + peerPort);
        Socket peer = null;
        try {
            peer = new Socket(peerHost, peerPort);
            if (addPeer(peer) == 0) {
                Thread t = new Thread(myClientThreads, new ServerThread(peer, FileMgr));
                t.setName("ServerThread");
                t.start();
            } else {
                peer.close();
            }
        } catch (IOException ex) {
            System.out.println("Error connecting to " + peerHost + ":" + 
                    peerPort + " - Is peer turned on?");
        }
    }
    
    public int leave() {
        try {            
            System.out.println("Exiting...");
            
            myServerThread.interrupt();
            myClientThreads.interrupt();
            
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}