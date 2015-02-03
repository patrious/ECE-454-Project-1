package ece454_project1;

import ece454_project1.DataTypes.PeerChunkReply;
import ece454_project1.DataTypes.PeerChunkRequest;
import ece454_project1.DataTypes.PeerData;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

class Client implements Runnable {

    private FileManager fileMgr = null;
    private Socket socket = null;
    private ConcurrentLinkedQueue<PeerChunkReply> pcreplyQueue = new ConcurrentLinkedQueue<PeerChunkReply>();
    private ConcurrentLinkedQueue<PeerData> pdQueue = new ConcurrentLinkedQueue<PeerData>();
    private ThreadGroup readerThread = new ThreadGroup("readerThread");

    public Client(Socket sock, FileManager fm, ConcurrentLinkedQueue queue) {
        fileMgr = fm;
        socket = sock;
        pcreplyQueue = queue;
        System.out.println("Client: Connected to " + socket);
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(1000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            //Give other peer our local file data
            PeerData lfd = fileMgr.CurrentPeerData();
            oos.writeObject(lfd);
            oos.flush();

            //Now, just keep polling FM for PeerChunkRequests
            while (true) {
                PeerChunkRequest pcreq = fileMgr.PrepareChunkRequestPackage();
                if (pcreq.ChunkNumber != -1) {
                    //System.out.println(String.format("Requesting %s Chunk# %d", pcreq.Filename, pcreq.ChunkNumber));
                    oos.writeObject(pcreq);
                    oos.flush();
                }

                //Also, get any PeerChunkReply from ServerThread
                if (!pcreplyQueue.isEmpty()) {
                    PeerChunkReply data = pcreplyQueue.poll();
                    //System.out.println(String.format("Reply to  %s Chunk# %d", data.FileName, data.ChunkNumber));
                    oos.writeObject(data);
                    oos.flush();
                }
                
                if (!pdQueue.isEmpty()) {
                    PeerData data = pdQueue.poll();
                    //System.out.println(String.format("Reply to  %s Chunk# %d", data.FileName, data.ChunkNumber));
                    oos.writeObject(data);
                    oos.flush();
                }
                
                if (fileMgr.newFile == true) {
                    lfd = fileMgr.CurrentPeerData();
                    oos.writeObject(lfd);
                    oos.flush();
                    fileMgr.newFile = false;
                }

                if (Thread.interrupted()) {
                    try {
                        socket.close();
                        oos.close();
                        readerThread.interrupt();
                    } catch (IOException ex) {
                        System.out.println("Error shutting down server");
                    }
                    break;
                }
            }
        } catch (SocketException ex) {
            System.out.println("Client: Other peer disconnected");
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Client: Error creating client connection to " + socket + ":");
            ex.printStackTrace();
        }
    }
}
