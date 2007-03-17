/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RelayedCommitTransactionMessage extends AbstractGroupMessage implements EventContext {

  public static final int        RELAYED_COMMIT_TXN_MSG_TYPE = 0;

  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;

  private ChannelID              channelID;

  // To make serialization happy
  public RelayedCommitTransactionMessage() {
    super(-1);
  }

  public RelayedCommitTransactionMessage(ChannelID channelID, TCByteBuffer[] batchData,
                                         ObjectStringSerializer serializer) {
    super(RELAYED_COMMIT_TXN_MSG_TYPE);
    this.channelID = channelID;
    this.batchData = batchData;
    this.serializer = serializer;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public TCByteBuffer[] getBatchData() {
    return batchData;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    this.channelID = new ChannelID(in.readLong());
    this.serializer = readObjectStringSerializer(in);
    this.batchData = readByteBuffers(in);
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    out.writeLong(channelID.toLong());
    writeObjectStringSerializer(out, serializer);
    writeByteBuffers(out, batchData);
  }

}
