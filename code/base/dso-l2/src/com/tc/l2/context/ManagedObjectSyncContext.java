/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ManagedObjectSyncContext implements ObjectManagerResultsContext {

  private final NodeID                nodeID;
  private final Set<ObjectID>         oids;
  private final boolean               more;
  private final Sink                  nextSink;
  private final Map<String, ObjectID> rootsMap;

  private ObjectManagerLookupResults result;
  private TCByteBuffer[]             dnas;
  private int                        dnaCount;
  private ObjectStringSerializer     serializer;
  private long                       sequenceID;
  private final int                  totalObjectsToSync;
  private final int                  totalObjectsSynced;

  public ManagedObjectSyncContext(NodeID nodeID, Set<ObjectID> oids, boolean more, Sink sink, int totalObjectsToSync,
                                  int totalObjectsSynced) {
    this.nodeID = nodeID;
    this.oids = oids;
    this.more = more;
    this.nextSink = sink;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.rootsMap = Collections.emptyMap();
  }

  public ManagedObjectSyncContext(NodeID nodeID, Map<String, ObjectID> rootsMap, boolean more, Sink sink, int totalObjectsToSync,
                                  int totalObjectsSynced) {
    this.nodeID = nodeID;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.oids = new HashSet<ObjectID>(rootsMap.values());
    this.more = more;
    this.nextSink = sink;
    this.rootsMap = rootsMap;
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.result = results;
    nextSink.add(this);
  }

  public Set<ObjectID> getLookupIDs() {
    return oids;
  }

  public Map getRootsMap() {
    return rootsMap;
  }

  public Map getObjects() {
    Assert.assertNotNull(result);
    return result.getObjects();
  }

  public int getTotalObjectsToSync() {
    return totalObjectsToSync;
  }

  public int getTotalObjectsSynced() {
    return totalObjectsSynced;
  }

  public void setDehydratedBytes(TCByteBuffer[] buffers, int count, ObjectStringSerializer os) {
    this.dnas = buffers;
    this.dnaCount = count;
    this.serializer = os;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public ObjectStringSerializer getObjectSerializer() {
    Assert.assertNotNull(serializer);
    return serializer;
  }

  public TCByteBuffer[] getSerializedDNAs() {
    Assert.assertNotNull(dnas);
    return dnas;
  }

  public int getDNACount() {
    Assert.assertTrue(dnaCount > 0);
    return dnaCount;
  }

  public boolean hasMore() {
    return more;
  }

  public Set<ObjectID> getNewObjectIDs() {
    return Collections.emptySet();
  }

  public void setSequenceID(long nextSequence) {
    this.sequenceID = nextSequence;
  }

  public long getSequenceID() {
    Assert.assertTrue(this.sequenceID > 0);
    return this.sequenceID;
  }

  public void missingObject(ObjectID oid) {
    throw new AssertionError("Syncing missing Object : " + oid + " " + this);
  }

  public String toString() {
    return "ManagedObjectSyncContext [" + nodeID + " , oids = " + oids + " ,  rootsMap = " + rootsMap + " , more = "
           + more + "]";
  }

  public boolean updateStats() {
    return true;
  }

}
