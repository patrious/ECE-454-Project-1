/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ece454_project1.DataTypes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author patrious
 */
public class PeerData implements Serializable {

    public String PeerName;
    public Map<String, FileData> Dictionary_Data = new HashMap<String, FileData>();

    public PeerData() {
    }

    public PeerData(PeerData copy) {
        this.PeerName = copy.PeerName;
        this.Dictionary_Data.putAll(copy.Dictionary_Data);
    }
}
