/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ece454_project1.DataTypes;

import java.io.Serializable;

/**
 *
 * @author patrious
 */
public class PeerChunkReply implements Serializable {
    public String FileName;
    public String PeerName;
    public int ChunkNumber;
    public byte[] Data;
    public PeerData peerData;
    
    public PeerChunkReply(String fileName, String peerName, int chunkNumber, byte[] data, PeerData localPeerData)
    {
        FileName = fileName;
        PeerName = peerName;
        ChunkNumber = chunkNumber;
        Data = data;
        peerData = localPeerData;
    }
    
}
