/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ece454_project1;

import ece454_project1.DataTypes.FileData;
import ece454_project1.DataTypes.PeerData;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author patrious
 */
public class SuperStatus extends ece454_project1.Status {

    public SuperStatus(Map<String, PeerData> AllPeerData, PeerData LocalData) {

        TreeSet<String> UniqueFileNames = new TreeSet<String>();
        HashMap<String, BitSet> System_FileAvailability = new HashMap<String, BitSet>();
        HashMap<String, int[]> LeastReplication = new HashMap<String, int[]>();
        HashMap<String, Integer> WeightedLeastReplication = new HashMap<String, Integer>();

        for (String filename : LocalData.Dictionary_Data.keySet()) {
            if (!UniqueFileNames.contains(filename)) {
                UniqueFileNames.add(filename);
            }
        }
        for (PeerData pd : AllPeerData.values()) {
            for (String filename : pd.Dictionary_Data.keySet()) {
                //------------------------------
                // # of files in system.
                if (!UniqueFileNames.contains(filename)) {
                    UniqueFileNames.add(filename);
                }
                //--------------------------
                // Fraction of file locally.
                // Chunks / Total Chunks

                //---------------------------
                //System
                //% of the file on the system
                //Do an OR on all the bit Availability per file.

                BitSet currentFileBitSet = pd.Dictionary_Data.get(filename).Availability;
                int CurrentFileSize = currentFileBitSet.size();


                if (!System_FileAvailability.containsKey(filename)) {
                    System_FileAvailability.put(filename, new BitSet(CurrentFileSize));
                }

                BitSet get = System_FileAvailability.get(filename); //Get the bitset
                get.andNot(currentFileBitSet);                      //NAND the bitset
                System_FileAvailability.put(filename, get);         //Put it back in


                //---------------------------
                //Least Replication
                //Find chunks with smallest replication
                //Count the # of bit sets for each chunk per file.
                if (!LeastReplication.containsKey(filename)) {
                    LeastReplication.put(filename, new int[CurrentFileSize]);
                }

                List<Integer> MissingChunks = FileManager.Convert_BitSet_to_List(currentFileBitSet);
                for (int index : MissingChunks) {
                    LeastReplication.get(filename)[index]++;
                }

                //---------------------------
                //Weighted Least Replication
                // (Sum all chunks in peers) / (# of chunks in file)
                if (!WeightedLeastReplication.containsKey(filename)) {
                    WeightedLeastReplication.put(filename, 0);
                }
                Integer getReplicationCount = WeightedLeastReplication.get(filename);
                getReplicationCount += CurrentFileSize - MissingChunks.size();
                WeightedLeastReplication.put(filename, getReplicationCount);
            }
        }
        numFiles = UniqueFileNames.size();
        int counter = 0;
        local = new float[numFiles];
        system = new float[numFiles];
        leastReplication = new int[numFiles];
        weightedLeastReplication = new float[numFiles];
        for (String File : UniqueFileNames) {

            FileData fileData;
            if ((fileData = LocalData.Dictionary_Data.get(File)) != null) {
                local[counter] = (float) (fileData.Availability.size() - fileData.Availability.cardinality()) / (float) fileData.Availability.size();
            }
            BitSet getSystemAvailability = System_FileAvailability.get(File);
            if (getSystemAvailability != null) {
                float totalSize = (float) getSystemAvailability.size();
                float missingBits = (float) getSystemAvailability.cardinality();
                system[counter] = (totalSize - missingBits) / totalSize;
            }
            //Find the smallest value in each file array
            int[] getLeastReplication = LeastReplication.get(File);
            if (getLeastReplication != null) {
                int minimumReplication = FindSmallestElement(getLeastReplication);
                leastReplication[counter] = minimumReplication;
            }

            if (WeightedLeastReplication.containsKey(File)) {
                weightedLeastReplication[counter] = (float) WeightedLeastReplication.get(File) / (float) fileData.Availability.size();
            }
            counter++;
        }

    }

    @Override
    public int numberOfFiles() {
        return numFiles;
    }

    @Override
    public float fractionPresentLocally(int fileNumber) {
        if (fileNumber < local.length) {
            return local[fileNumber];
        }
        return -1;
    }

    @Override
    public float fractionPresent(int fileNumber) {
        if (fileNumber < system.length) {
            return system[fileNumber];
        }
        return -1;
    }

    @Override
    public int minimumReplicationLevel(int fileNumber) {
        if (fileNumber < leastReplication.length) {
            return leastReplication[fileNumber];
        }
        return -1;
    }

    @Override
    public float averageReplicationLevel(int fileNumber) {

        if (fileNumber < weightedLeastReplication.length) {
            return weightedLeastReplication[fileNumber];
        }
        return -1;
    }

    private int FindSmallestElement(int[] get) {
        int smallest = Integer.MAX_VALUE;
        for (int x : get) {
            if (x < smallest) {
                smallest = x;
            }
        }
        return smallest;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Local Data: \n");
        synchronized (local) {
            int counter = 0;
            for (float fileInfo : local) {
                sb.append(String.format("File %d = %f \n", counter++, fileInfo * 100));
            }
        }
        sb.append("System Data: \n");
        synchronized (system) {
            int counter = 0;
            for (float fileInfo : system) {
                sb.append(String.format("File %d = %f \n", counter++, fileInfo * 100));
            }
        }
        sb.append("LeastReplicated Data: \n");
        synchronized (leastReplication) {
            int counter = 0;
            for (float fileInfo : leastReplication) {
                sb.append(String.format("File %d = %f \n", counter++, fileInfo * 100));
            }
        }
        sb.append("Weighted Data: \n");
        synchronized (weightedLeastReplication) {
            int counter = 0;
            for (float fileInfo : weightedLeastReplication) {
                sb.append(String.format("File %d = %f \n", counter++, fileInfo * 100));
            }
        }
        return sb.toString();

    }
}
