/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collections;
import java.util.Map;

import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.object.tx.ServerTransactionID;

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
  private ObjectIDSet                 deletedOids = TCCollections.EMPTY_OBJECT_ID_SET;

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
                                 ObjectStringSerializer os, final ObjectIDSet deletedObjects) {
    this.syncedOids = synced;
    this.notSyncedOids = notSynced;
    this.dnas = buffers;
    this.dnaCount = count;
    this.serializer = os;
    this.deletedOids = deletedObjects;
    Assert.assertTrue(deletedObjects.size() + dnaCount > 0);
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
    return this.dnaCount;
  }

  public boolean hasMore() {
    return this.more || !notSyncedOids.isEmpty();
  }

  public ObjectIDSet getNewObjectIDs() {
    return new BitSetObjectIDSet();
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

  public ObjectIDSet getSynchedOids() {
    return syncedOids;
  }

  public ObjectIDSet getNotSynchedOids() {
    return notSyncedOids;
  }

  public ObjectIDSet getDeletedOids() {
    return deletedOids;
  }

  public ObjectSyncMessage createObjectSyncMessage(final ServerTransactionID stxnID) {
    return new ObjectSyncMessage(stxnID, getSynchedOids(), getDNACount(), getSerializedDNAs(), getObjectSerializer(), getRootsMap(), getSequenceID(), getDeletedOids());
  }
}
