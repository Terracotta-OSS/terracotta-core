package com.tc.object.msg;

import com.tc.net.NodeID;

/**
 * @author Alex Snaps
 */
public interface NodesWithKeysMessageFactory {

  public NodesWithKeysMessage newNodesWithKeysMessage(NodeID nodeID);

}
