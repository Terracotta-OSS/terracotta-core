/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.SortedSet;

public class GCResultMessage extends AbstractGroupMessage implements EventContext {
  public static final int       GC_RESULT = 0;
  private ObjectIDSet           gcedOids;
  private GarbageCollectionInfo gcInfo;

  // To make serialization happy
  public GCResultMessage() {
    super(-1);
  }

  public GCResultMessage(int type, GarbageCollectionInfo gcInfo, ObjectIDSet deleted) {
    super(type);
    this.gcInfo = gcInfo;
    this.gcedOids = deleted;
  }

  public int getGCIterationCount() {
    return (int) gcInfo.getGarbageCollectionID().toLong();
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(GC_RESULT, getType());
    this.gcInfo = new GarbageCollectionInfo();
    this.gcInfo.deserializeFrom(in);
    this.gcedOids = new ObjectIDSet();
    this.gcedOids.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(GC_RESULT, getType());
    this.gcInfo.serializeTo(out);
    this.gcedOids.serializeTo(out);
  }

  public SortedSet getGCedObjectIDs() {
    return gcedOids;
  }

  public GarbageCollectionInfo getGCInfo() {
    return gcInfo;
  }

  @Override
  public String toString() {
    return "DGCResultMessage@" + System.identityHashCode(this) + " : DGC Info = " + gcInfo + " Result size = "
           + (gcedOids == null ? "null" : "" + gcedOids.size());
  }

}
