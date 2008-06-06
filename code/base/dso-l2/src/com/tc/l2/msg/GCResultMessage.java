/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedSet;

public class GCResultMessage extends AbstractGroupMessage implements EventContext {
  public static final int GC_RESULT = 0;
  private SortedSet       gcedOids;
  private int             gcIterationCount;

  // To make serialization happy
  public GCResultMessage() {
    super(-1);
  }

  public GCResultMessage(int type, int gcIterationCount, SortedSet deleted) {
    super(type);
    this.gcIterationCount = gcIterationCount;
    this.gcedOids = deleted;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException, ClassNotFoundException {
    Assert.assertEquals(GC_RESULT, msgType);
    gcIterationCount = in.readInt();
    gcedOids = (SortedSet) in.readObject();
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(GC_RESULT, msgType);
    out.writeInt(gcIterationCount);
    // XXX::Directly serializing instead of using writeObjectIDs() to avoid HUGE messages. Since the (wrapped) set is
    // ObjectIDSet2 and since it has optimized externalization methods, this should result in far less data written out.
    out.writeObject(gcedOids);
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
