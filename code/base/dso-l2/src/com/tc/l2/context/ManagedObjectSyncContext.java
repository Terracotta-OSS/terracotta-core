/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.Map;

public class ManagedObjectSyncContext implements ObjectManagerResultsContext {

  private final NodeID                nodeID;
  private final ObjectIDSet           oids;
  private final boolean               more;
  private final Sink                  nextSink;
  private final Map<String, ObjectID> rootsMap;

  private ObjectManagerLookupResults  result;
  private TCByteBuffer[]              dnas;
  private int                         dnaCount;
  private ObjectStringSerializer      serializer;
  private long                        sequenceID;
  private final int                   totalObjectsToSync;
  private final int                   totalObjectsSynced;

  public ManagedObjectSyncContext(NodeID nodeID, ObjectIDSet oids, boolean more, Sink sink, int totalObjectsToSync,
                                  int totalObjectsSynced) {
    this.nodeID = nodeID;
    this.oids = oids;
    this.more = more;
    this.nextSink = sink;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.rootsMap = Collections.emptyMap();
  }

  public ManagedObjectSyncContext(NodeID nodeID, Map<String, ObjectID> rootsMap, ObjectIDSet oids, boolean more,
                                  Sink sink, int totalObjectsToSync, int totalObjectsSynced) {
    this.nodeID = nodeID;
    this.totalObjectsToSync = totalObjectsToSync;
    this.totalObjectsSynced = totalObjectsSynced;
    this.oids = oids;
    this.more = more;
    this.nextSink = sink;
    this.rootsMap = rootsMap;
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.result = results;
    assertNoMissingObjects(results.getMissingObjectIDs());
    this.nextSink.add(this);
  }

  public ObjectIDSet getLookupIDs() {
    return this.oids;
  }

  public Map getRootsMap() {
    return this.rootsMap;
  }

  public Map getObjects() {
    Assert.assertNotNull(this.result);
    return this.result.getObjects();
  }

  public int getTotalObjectsToSync() {
    return this.totalObjectsToSync;
  }

  public int getTotalObjectsSynced() {
    return this.totalObjectsSynced;
  }

  public void setDehydratedBytes(TCByteBuffer[] buffers, int count, ObjectStringSerializer os) {
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
    return this.more;
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

  private void assertNoMissingObjects(ObjectIDSet missing) {
    if (!missing.isEmpty()) { throw new AssertionError("Syncing missing Objects : " + missing + " lookup context is : "
                                                       + this); }
  }

  @Override
  public String toString() {
    return "ManagedObjectSyncContext [" + this.nodeID + " , oids = " + this.oids + " ,  rootsMap = " + this.rootsMap
           + " , more = " + this.more + "]";
  }

  public boolean updateStats() {
    return true;
  }

}
