/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;

import java.util.Set;

public interface ServerMapEvictionBroadcastMessage extends TCMessage {

  public ObjectID getMapID();

  public Set getEvictedKeys();

  public int getClientIndex();

  public void initializeEvictionBroadcastMessage(ObjectID mapID, Set evictedKeys, int clientIndex);

}
