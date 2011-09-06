/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.locks.ThreadID;

import java.util.Set;

public interface KeysForOrphanedValuesResponseMessage extends ClusterMetaDataResponseMessage {

  public void initialize(ThreadID threadID, byte[] orphanedKeysDNA);

  public void initialize(ThreadID threadID, Set<ObjectID> orphanedValuesObjectIDs);

  public byte[] getOrphanedKeysDNA();

  public Set<ObjectID> getOrphanedValuesObjectIDs();

}