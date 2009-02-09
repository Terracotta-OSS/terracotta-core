/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Set;

public interface NodesWithObjectsMessage extends TCMessage {

  public Set<ObjectID> getObjectIDs();

  public ThreadID getThreadID();

  public void setThreadID(ThreadID threadID);

  public void addObjectID(ObjectID objectID);

}
