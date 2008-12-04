/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.SortedSet;

public class GCResultMessage extends AbstractGroupMessage implements EventContext {
  public static final int GC_RESULT = 0;
  private ObjectIDSet     gcedOids;
  private int             gcIterationCount;

  // To make serialization happy
  public GCResultMessage() {
    super(-1);
  }

  public GCResultMessage(int type, int gcIterationCount, ObjectIDSet deleted) {
    super(type);
    this.gcIterationCount = gcIterationCount;
    this.gcedOids = deleted;
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(GC_RESULT, getType());
    gcIterationCount = in.readInt();
    gcedOids = new ObjectIDSet();
    gcedOids.deserializeFrom(in);
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(GC_RESULT, getType());
    out.writeInt(gcIterationCount);
    gcedOids.serializeTo(out);
  }

  public SortedSet getGCedObjectIDs() {
    return gcedOids;
  }

  public int getGCIterationCount() {
    return gcIterationCount;
  }

  public String toString() {
    return "GCResultMessage@" + System.identityHashCode(this) + " : GC Iteration Count = " + gcIterationCount
           + " Result size = " + (gcedOids == null ? "null" : "" + gcedOids.size());
  }

}
