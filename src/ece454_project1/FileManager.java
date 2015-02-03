/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ece454_project1;

import ece454_project1.DataTypes.FileData;
import ece454_project1.DataTypes.PeerChunkReply;
import ece454_project1.DataTypes.PeerChunkRequest;
import ece454_project1.DataTypes.PeerData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrious
 */
public class FileManager {
    public boolean newFile = false;
    final Object FileAccessToken = new Object();
    final PeerData LocalFileData;
    final Map<String, PeerData> AllPeerData;
    final Map<String, TreeSet<Integer>> OutstandingRequests = new HashMap<String, TreeSet<Integer>>();
    //Constructor

    public FileManager() {
        LocalFileData = new PeerData();
        AllPeerData = new HashMap<String, PeerData>();
    }

    public void StartFileManager(int port) {
        LocalFileData.PeerName = "127.0.0.1:" + port;
        ConstructLocalPeerData();

        // Dig through files located at "Blank" and pull them in. Contruct the File list.
    }

    public PeerChunkRequest PrepareChunkRequestPackage() {
        PeerChunkRequest pcr = new PeerChunkRequest("", "", -1);
        //Not sure how to error check here.
        synchronized (LocalFileData) {
            for (String fileName : LocalFileData.Dictionary_Data.keySet()) {
                synchronized (OutstandingRequests) {
                    TreeSet<Integer> Requests = OutstandingRequests.get(fileName);
                    if (Requests == null) {
                        Requests = new TreeSet<Integer>();
                    }
                    BitSet fileAvailability = LocalFileData.Dictionary_Data.get(fileName).Availability;
                    int FileCardinality = fileAvailability.cardinality();
                    int RequestSize = Requests.size();

                    if (FileCardinality > 0 && RequestSize < FileCardinality) {
                        //Convert the bitset, into a list of missing chunks ( should return the indexs where we are missing chunks)
                        List<Integer> MissingChunks = Convert_BitSet_to_List(fileAvailability);
                        int i = 0;
                        int ChunkNumber = -1;
                        String PeerName = "";
                        do {
                            if (i >= MissingChunks.size()) {
                                break;
                            }

                            ChunkNumber = MissingChunks.get(i);
                            if (Requests.contains(ChunkNumber)) {
                                i++;
                                continue;
                            }
                            //Find a peer who has this chunk. If no one has it, loop and check the next missing chunk.
                            PeerName = CheckPeers(ChunkNumber, fileName);
                            i++;

                        } while (PeerName.equalsIgnoreCase(""));
                        pcr.ChunkNumber = ChunkNumber;
                        pcr.Peername = PeerName;
                        pcr.Filename = fileName;
                        Requests.add(ChunkNumber);
                        OutstandingRequests.put(fileName, Requests);
                    }
                    if (!pcr.Peername.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return pcr;
    }

    /*
     * Check Peers if they have a particular file & chunk number
     * Return the Name of the peer
     */
    private String CheckPeers(int chunkNum, String FileName) {
        //Lock PeerData so noone else can use it.
        synchronized (AllPeerData) {
            //Go through peer list and check if this file chunk is available.
            for (PeerData peerData : AllPeerData.values()) {
                FileData fileData = peerData.Dictionary_Data.get(FileName);
                if (fileData.Availability.get(chunkNum) == false) {
                    return peerData.PeerName;
                }
            }
        }
        //No one has this chunk available.
        return "";
    }

    /*
     * Convert a BitSet into a list of Indexs that are set true.
     * ex. 001001001
     * return [2,5,8];
     * A bit set to 1, is a chunk we do not have yet.
     */
    static public List Convert_BitSet_to_List(BitSet fileAvailabilitiy) {
        String[] split = fileAvailabilitiy.toString().split("[\\{\\},\\ ]");
        List returnList = new ArrayList();
        for (String value : split) {
            if (!(value.equalsIgnoreCase(""))) {
                returnList.add(Integer.parseInt(value));
            }
        }
        return returnList;

    }

    //Allow NM to get LocalFileData;
    public PeerData CurrentPeerData() {
        synchronized (LocalFileData) {
            return new PeerData(LocalFileData);
        }
    }

    /*
     * Receive Data from the Network Manager. Update the Peers availability.
     * Write the chunk to disk. If all good, update local availability.
     */
    public void ReceiveData(PeerChunkReply replyData) {
        //Availability. 
        UpdatePeerFileAvailability(replyData.peerData);

        //Write to the file.
        if (WriteChunk(replyData.FileName, replyData.ChunkNumber, replyData.Data) == 0) {
            //Update Local Info.
            UpdateLocalFileAvailability(replyData.FileName, replyData.ChunkNumber);
        }
    }

    private void UpdateLocalFileAvailability(String FileName, int Chunk_Number) {
        synchronized (LocalFileData) {
            LocalFileData.Dictionary_Data.get(FileName).Availability.set(Chunk_Number, false);
        }
        synchronized (OutstandingRequests) {
            TreeSet<Integer> get = OutstandingRequests.get(FileName);
            get.remove(Chunk_Number);
            OutstandingRequests.put(FileName, get);
        }
    }

    /*
     * Write a chunk of data to disk.
     */
    private int WriteChunk(String FileName, int Chunk_Number, byte[] Data) {
        synchronized (FileAccessToken) {
            try {
                //Open Data Stream
                RandomAccessFile rac = new RandomAccessFile(FileName, "rw");
                //System.out.println(String.format("Writing %s to Chunk# %d", FileName, Chunk_Number));
                //Write to the chunk place.
                rac.skipBytes(Chunk_Number * Config.CHUNK_SIZE);
                rac.write(Data);
                //Close Stream?
                rac.close();
            } catch (IOException ex) {
                return -1;
            }
        }
        return 0;
    }

    public void UpdatePeerFileAvailability(PeerData remotePeerData) {
        synchronized (AllPeerData) {
            AllPeerData.put(remotePeerData.PeerName, remotePeerData);
        }

        //Check to see if any files exist in the peer and not in local.
        ArrayList<String> MissingFiles = new ArrayList<String>();
        for (String FileName : remotePeerData.Dictionary_Data.keySet()) {
            if (!LocalFileData.Dictionary_Data.containsKey(FileName)) {
                MissingFiles.add(FileName);
            }
        }
        if (MissingFiles.size() > 0) {
            synchronized (LocalFileData) {
                for (String FileName : MissingFiles) {
                    int numChunks = remotePeerData.Dictionary_Data.get(FileName).NumberChunks;
                    LocalFileData.Dictionary_Data.put(FileName, new FileData(FileName, numChunks, false));
                    //SetUpEmptyFile?
                    CreateNewFile(FileName, numChunks);
                }
            }
        }
    }

    /*
     * Add a new file to the Local File Lists.
     */
    public int Insert(String FileName) {
        //Add to local list        
        int numChunks;
        if ((numChunks = FindNumChunks(FileName)) == -1) {
            return ReturnCodes.ERR_UNKNOWN_FATAL;
        }
        //Add to local Dictionary.
        synchronized (LocalFileData) {
            FileData fileData = new FileData(FileName, numChunks, true);
            LocalFileData.Dictionary_Data.put(FileName, fileData);
        }
        newFile = true;
        return ReturnCodes.ERR_OK;
    }

    /*
     * Ask for a chunk from a file. Return a package to send to client requesting.
     */
    public PeerChunkReply Disk_To_Peer(PeerChunkRequest pcrequest) {
        byte[] data = ReadChunkFromFile(pcrequest.Filename, pcrequest.ChunkNumber);
        if (data != null) {
            synchronized (LocalFileData) {
                return new PeerChunkReply(pcrequest.Filename, pcrequest.Peername, pcrequest.ChunkNumber, data, LocalFileData);
            }
        }
        return null;
    }

    /*
     * Will find a collection of chunks held exclusively by this peer and return a list 
     * of reply data. MAX 10.
     */
    public List<PeerChunkReply> Leave() {
        ArrayList<PeerChunkReply> replyList = new ArrayList<PeerChunkReply>();
        Map<String, BitSet> ExclusiveBitSets = new HashMap<String, BitSet>();
        synchronized (LocalFileData) {
            Set<String> keySet = LocalFileData.Dictionary_Data.keySet();
            for (String key : keySet) {
                //Get a new set of empty bitSets
                BitSet newSet = new BitSet(LocalFileData.Dictionary_Data.get(key).NumberChunks);
                newSet.flip(0, newSet.size()); //Flip them all to missing.  1 -> missing 0 ->has chunk          
                ExclusiveBitSets.put(key, newSet);
            }
            // Find any exclusive Chunks
            synchronized (AllPeerData) {
                for (PeerData pd : AllPeerData.values()) //Walk through all peer data
                {
                    for (String key : pd.Dictionary_Data.keySet()) //Grab the keyset for this peer
                    {
                        BitSet get = ExclusiveBitSets.get(key);     //Pull out local availability of this file
                        get.and(pd.Dictionary_Data.get(key).Availability);  //And the bitsets together
                        ExclusiveBitSets.put(key, get);                //Put back 
                    }
                    
                }
                if(AllPeerData.size() < 1)
                    {
                        return null;
                    }
                //At the end of this, any chunk with a 1 is exclusive to this peer.
                int exclusiveChunkCounter = 0;
                for (String fileName : ExclusiveBitSets.keySet()) {
                    List<Integer> ExclusiveChunks = Convert_BitSet_to_List(ExclusiveBitSets.get(fileName));
                    for (int chunkNumber : ExclusiveChunks) {
                        //Where am I getting the Peer and Data from;
                        byte[] data = ReadChunkFromFile(fileName, chunkNumber);
                        //Since we have an exclusivity on this Chunk, Any peer will need this chunk, so grab the first one.
                        String PeerName = (String) AllPeerData.keySet().toArray()[0];
                        PeerChunkReply pcr = new PeerChunkReply(fileName, PeerName, chunkNumber, data, LocalFileData);
                        replyList.add(pcr);
                        exclusiveChunkCounter++;
                        if (exclusiveChunkCounter > Config.MAX_NUMBER_EXCLUSIVE_CHUNKS) {
                            break;
                        }
                    }
                }
            }
        }
        return replyList;
    }

    /*
     * Read a chunk from a file.
     */
    private byte[] ReadChunkFromFile(String fileName, int chunkNumber) {
        synchronized (FileAccessToken) {
            byte[] readData;
            byte[] tempBuffer = new byte[Config.CHUNK_SIZE];
            try {
                //Open the stream. See how many available.
                InputStream in = new FileInputStream(fileName);
                in.skip(chunkNumber * Config.CHUNK_SIZE);
                int byteRead = in.read(tempBuffer, 0, Config.CHUNK_SIZE);
                readData = new byte[byteRead];
                System.arraycopy(tempBuffer, 0, readData, 0, byteRead);
                return readData;
            } catch (Exception ex) {
                ///NOM NOM NOM
            }
        }
        return null;
    }

    /*
     * Find the number of chunks in a file.
     */
    private int FindNumChunks(String FileName) {
        synchronized (FileAccessToken) {
            try {
                //Open the stream. See how many available.
                InputStream in = new FileInputStream(FileName);
                double numChunks = Math.ceil((double) in.available() / (double) Config.CHUNK_SIZE);
                return Math.max(1, (int) numChunks);

            } catch (Exception ex) {
                ///NOM NOM NOM
            }
        }
        return -1;

    }

    //Remove all knowledge of a peer from our lists.
    public int RemovePeerFromLocalLists(String peerName) {
        try {
            synchronized (AllPeerData) {
                AllPeerData.remove(peerName);
            }
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    /*
     * At startup, run through the local directory and store all files as ones that are to be replicated
     */
    synchronized private void ConstructLocalPeerData() {

        // Directory path here
        String path = ".";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                Insert(listOfFiles[i].getName());
            }
        }
    }

    /*
     * Query the local node to get information about itself.
     */
    public Status query() {
        PeerData LFD;
        Map<String, PeerData> APD;
        synchronized (LocalFileData) {
            LFD = new PeerData(LocalFileData);

        }
        synchronized (AllPeerData) {
            APD = new HashMap<String, PeerData>();
            APD.put(LFD.PeerName, LFD);
            APD.putAll(AllPeerData);

        }
        return new SuperStatus(APD, LFD);
    }

    /*
     * Create a new "empty" file on disk to be writen to.
     */
    private void CreateNewFile(String FileName, int NumChunks) {
        RandomAccessFile rac;
        try {
            synchronized (FileAccessToken) {
                rac = new RandomAccessFile(FileName, "rw");
                Random x = new Random();
                byte[] RandomData = new byte[(NumChunks - 1) * Config.CHUNK_SIZE];
                x.nextBytes(RandomData);
                rac.write(RandomData);
                rac.close();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
