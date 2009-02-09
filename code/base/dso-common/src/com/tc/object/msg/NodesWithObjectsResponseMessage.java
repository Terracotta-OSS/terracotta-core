/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Map;
import java.util.Set;

public interface NodesWithObjectsResponseMessage extends TCMessage {

  public void initialize(ThreadID threadID, Map<ObjectID, Set<NodeID>> response);

  public ThreadID getThreadID();

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects();

}
