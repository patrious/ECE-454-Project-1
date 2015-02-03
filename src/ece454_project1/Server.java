package ece454_project1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ServerSocket server = null;
    private NetworkManager netMgr = null;
    private FileManager fileMgr = null;
    private ThreadGroup serverThreads = new ThreadGroup("serverThreads");
    
    public Server(int port, NetworkManager nm, FileManager fm) {
        netMgr = nm;
        fileMgr = fm;
        try {
            server = new ServerSocket(port);
            server.setSoTimeout(1000);
        } catch (IOException ex) {
            System.out.println("Server: Error binding to port " + port);
        }
    }
    
    @Override public void run() {
        Socket socket = null;
        while (true) {
            try {
                socket = server.accept();
                if (netMgr.addPeer(socket) == 0) {
                    Thread t = new Thread(serverThreads, 
                            new ServerThread(socket, fileMgr));
                    t.setName("ServerThread");
                    t.start();
                } else {
                    System.out.println("Already connected to peer");
                    break;
                }
            } catch (IOException ex) {
                //System.out.println("Server: No new peers (timeout)");
            }
            if (Thread.interrupted()) {
                try {
                    server.close();
                    serverThreads.interrupt();
                } catch (IOException ex) {
                    System.out.println("Server: Error shutting down server");
                }
                break;
            }
        }
    }
}
