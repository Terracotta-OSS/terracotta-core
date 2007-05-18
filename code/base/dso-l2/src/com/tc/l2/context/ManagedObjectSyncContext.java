/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.NodeID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ManagedObjectSyncContext implements ObjectManagerResultsContext {

  private final NodeID               nodeID;
  private final Set                  oids;
  private final boolean              more;
  private final Sink                 nextSink;
  private final Map                  rootsMap;

  private ObjectManagerLookupResults result;
  private TCByteBuffer[]             dnas;
  private int                        dnaCount;
  private ObjectStringSerializer     serializer;
  private long                       sequenceID;

  public ManagedObjectSyncContext(NodeID nodeID, Set oids, boolean more, Sink sink) {
    this.nodeID = nodeID;
    this.oids = oids;
    this.more = more;
    this.nextSink = sink;
    this.rootsMap = Collections.EMPTY_MAP;
  }

  public ManagedObjectSyncContext(NodeID nodeID, HashMap rootsMap, boolean more, Sink sink) {
    this.nodeID = nodeID;
    this.oids = new HashSet(rootsMap.values());
    this.more = more;
    this.nextSink = sink;
    this.rootsMap = rootsMap;
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.result = results;
    nextSink.add(this);
  }

  public Set getLookupIDs() {
    return oids;
  }

  public Map getRootsMap() {
    return rootsMap;
  }

  public Map getObjects() {
    Assert.assertNotNull(result);
    return result.getObjects();
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

  public Set getNewObjectIDs() {
    return Collections.EMPTY_SET;
  }

  public void setSequenceID(long nextSequence) {
    this.sequenceID = nextSequence;
  }

  public long getSequenceID() {
    Assert.assertTrue(this.sequenceID > 0);
    return this.sequenceID;
  }

}
