/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.Assert;

import java.io.IOException;

public class DGCStatusMessage extends AbstractGroupMessage implements EventContext {
  private GarbageCollectionInfo gcInfo;

  // to make serialization happy
  public DGCStatusMessage() {
    super(-1);
  }

  DGCStatusMessage(int type, GarbageCollectionInfo gcInfo) {
    super(type);
    this.gcInfo = gcInfo;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(DGCMessageFactory.DGC_START, getType());
    this.gcInfo = new GarbageCollectionInfo();
    this.gcInfo.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(DGCMessageFactory.DGC_START, getType());
    this.gcInfo.serializeTo(out);
  }

  public GarbageCollectionInfo getGcInfo() {
    return this.gcInfo;
  }

}
