/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collections;
import java.util.Map;

public class ManagedObjectSyncContext implements EventContext {

  private final NodeID                nodeID;
  private final ObjectIDSet           requestedOids;
  private final boolean               more;
  private final Map<String, ObjectID> rootsMap;
  private final int                   totalObjectsToSync;
  private final int                   totalObjectsSynced;
  private final long                  sessionId;

  private TCByteBuffer[]              dnas;
  private int                         dnaCount;
  private ObjectStringSerializer      serializer;
  private long                        sequenceID;
  private ObjectIDSet                 syncedOids    = TCCollections.EMPTY_OBJECT_ID_SET;
  private ObjectIDSet                 notSyncedOids = TCCollections.EMPTY_OBJECT_ID_SET;

  public ManagedObjectSyncContext(final NodeID nodeID, final ObjectIDSet oids, final boolean more,
                                  final int totalObjectsToSync, final int totalObjectsSynced, final long sessionId) {
    this.nodeID = nodeID;
    this.requestedOids = oids;
    this.more = more;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.rootsMap = Collections.emptyMap();
    this.sessionId = sessionId;
  }

  public ManagedObjectSyncContext(final NodeID nodeID, final Map<String, ObjectID> rootsMap, final ObjectIDSet oids,
                                  final boolean more, final int totalObjectsToSync, final int totalObjectsSynced,
                                  final long sessionId) {
    this.nodeID = nodeID;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.requestedOids = oids;
    this.more = more;
    this.rootsMap = rootsMap;
    this.sessionId = sessionId;
  }

  public ObjectIDSet getRequestedObjectIDs() {
    return this.requestedOids;
  }

  public Map getRootsMap() {
    return this.rootsMap;
  }

  public int getTotalObjectsToSync() {
    return this.totalObjectsToSync;
  }

  public int getTotalObjectsSynced() {
    return this.totalObjectsSynced;
  }

  public long getSessionId() {
    return this.sessionId;
  }

  public void setDehydratedBytes(ObjectIDSet synced, ObjectIDSet notSynced, TCByteBuffer[] buffers, int count,
                                 ObjectStringSerializer os) {
    this.syncedOids = synced;
    this.notSyncedOids = notSynced;
    this.dnas = buffers;
    this.dnaCount = count;
    this.serializer = os;
  }

  public NodeID getNodeID() {
    return this.nodeID;
  }

  public ObjectStringSerializer getObjectSerializer() {
    Assert.assertNotNull(this.serializer);
    return this.serializer;
  }

  public TCByteBuffer[] getSerializedDNAs() {
    Assert.assertNotNull(this.dnas);
    return this.dnas;
  }

  public int getDNACount() {
    Assert.assertTrue(this.dnaCount > 0);
    return this.dnaCount;
  }

  public boolean hasMore() {
    return this.more || !notSyncedOids.isEmpty();
  }

  public ObjectIDSet getNewObjectIDs() {
    return new ObjectIDSet();
  }

  public void setSequenceID(long nextSequence) {
    this.sequenceID = nextSequence;
  }

  public long getSequenceID() {
    Assert.assertTrue(this.sequenceID > 0);
    return this.sequenceID;
  }

  @Override
  public String toString() {
    return "ManagedObjectSyncContext [" + this.nodeID + " , sessionId = " + this.sessionId + " , oids = "
           + this.requestedOids + " ,  rootsMap = " + this.rootsMap + " , more = " + this.more + "]";
  }

  public boolean updateStats() {
    return true;
  }

  public ObjectIDSet getSynchedOids() {
    return syncedOids;
  }

  public ObjectIDSet getNotSynchedOids() {
    return notSyncedOids;
  }

}
