package ece454_project1;

import ece454_project1.DataTypes.PeerChunkReply;
import ece454_project1.DataTypes.PeerChunkRequest;
import ece454_project1.DataTypes.PeerData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerThread extends Thread {

    Socket socket = null;
    FileManager fileMgr = null;
    private ThreadGroup clientThreads = new ThreadGroup("clientThreads");
    private ConcurrentLinkedQueue<PeerChunkReply> outstandingReplies = new ConcurrentLinkedQueue<PeerChunkReply>();
    private ConcurrentLinkedQueue<PeerData> pdQueue = new ConcurrentLinkedQueue<PeerData>();

    public ServerThread(Socket sock, FileManager fm) {
        socket = sock;
        fileMgr = fm;
        System.out.println("ServerThread: Connected to " + socket);
    }

    public void run() {
        try {
            Thread t = new Thread(clientThreads, new Client(socket, fileMgr, outstandingReplies));
            t.setName("Client");
            t.start();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
            boolean fileListSent = false;

            while (true) {
                //First off, read the LocalFileData, send it to FM
                Object input = new Object();
                try {
                    input = ois.readObject();
                } catch (Exception Ex) {
                    //System.out.println("Error reading the input Buffer");
                }
                if ((input instanceof PeerData) && (fileListSent == false)) {
                    PeerData data = (PeerData) input;
                    fileMgr.UpdatePeerFileAvailability(data);
                    pdQueue.add(fileMgr.LocalFileData);
                    fileListSent = true;
                } else if (input instanceof PeerData) {
                    PeerData data = (PeerData) input;
                    fileMgr.UpdatePeerFileAvailability(data);
                } else if (input instanceof PeerChunkRequest) {
                    PeerChunkRequest data = (PeerChunkRequest) input;
                    PeerChunkReply pcreply = fileMgr.Disk_To_Peer(data);
                    //Once we have the PeerChunkReply, send it to the Client thread
                    //so it can sent it.
                    outstandingReplies.add(pcreply);
                } else if (input instanceof PeerChunkReply) {
                    PeerChunkReply data = (PeerChunkReply) input;
                    fileMgr.ReceiveData(data);
                } else {
                    //System.out.println("UFO");
                }
                if (Thread.interrupted()) {
                    try {
                        socket.close();
                        clientThreads.interrupt();
                        break;
                    } catch (IOException ex) {
                        System.out.println("ServerThread: Error shutting down server");
                    }
                }

            }
        } catch (SocketException ex) {
            //Other peer disconnected, whatevs.
        } catch (SocketTimeoutException ex) {
            System.out.println("Nothing to read here...");
            //nothing to read, caused by getObject()
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
