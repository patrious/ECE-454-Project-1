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

public class PeerChunkRequest implements Serializable {
    
    public String Peername;
    public String Filename;
    public int ChunkNumber;
    
    public PeerChunkRequest(String fname, String name, int chunk)
    {
        Filename = fname;
        Peername = name;
        ChunkNumber = chunk;
    }
    
}
