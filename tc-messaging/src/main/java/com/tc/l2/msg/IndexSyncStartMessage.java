/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;

public class IndexSyncStartMessage extends AbstractGroupMessage implements OrderedEventContext {
  public static final int INDEX_SYNC_START_TYPE = 0;
  private long            sequenceID;
  private int             idxPerCache;

  public IndexSyncStartMessage() {
    super(-1);

  }

  public IndexSyncStartMessage(long sID, int idxCt) {
    super(IndexSyncStartMessage.INDEX_SYNC_START_TYPE);
    this.sequenceID = sID;
    this.idxPerCache = idxCt;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(INDEX_SYNC_START_TYPE, getType());
    this.sequenceID = in.readLong();
    this.idxPerCache = in.readInt();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(INDEX_SYNC_START_TYPE, getType());
    out.writeLong(this.sequenceID);
    out.writeInt(this.idxPerCache);
  }

  @Override
  public long getSequenceID() {
    return this.sequenceID;
  }

  public int getIdxPerCache() {
    return this.idxPerCache;
  }
}
