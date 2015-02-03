/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ece454_project1.DataTypes;

import java.io.Serializable;
import java.util.BitSet;

/**
 *
 * @author patrious
 */

public class FileData implements Serializable
{
    public String Filename;
    public int NumberChunks;
    public BitSet Availability;
    
    
    public FileData(String filename, int numChunks, boolean OnDisk)
    {
        Filename = filename;
        NumberChunks = numChunks;
        Availability = new BitSet(numChunks);
        //Should set all the bits to 1. Indicating that we DO NOT have those chunks.
        if(!OnDisk) Availability.flip(0,numChunks);
    }
            
}
