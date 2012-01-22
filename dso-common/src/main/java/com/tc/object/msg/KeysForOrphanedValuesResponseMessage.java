/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
