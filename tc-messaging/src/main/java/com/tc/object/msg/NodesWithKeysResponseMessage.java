/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.object.locks.ThreadID;

import java.util.Map;
import java.util.Set;

/**
 * @author Alex Snaps
 */
public interface NodesWithKeysResponseMessage extends ClusterMetaDataResponseMessage {

  public void initialize(ThreadID threadID, Map<Object, Set<NodeID>> response);

  public Map<Object, Set<NodeID>> getNodesWithKeys();

}
