/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;

import java.io.IOException;

public class IndexCheckSyncMessage extends AbstractGroupMessage {
  public static final int REQUEST         = 0;
  public static final int RESPONSE        = 1;
  public static final int FAILED_RESPONSE = 2;

  private boolean         syncIndex       = false;

  // To make serialization happy
  public IndexCheckSyncMessage() {
    super(-1);
  }

  public IndexCheckSyncMessage(int type) {
    super(type);
  }

  public IndexCheckSyncMessage(MessageID messageID, int type) {
    super(type, messageID);
  }

  public IndexCheckSyncMessage(MessageID messageID, int type, boolean syncIndex) {
    super(type, messageID);
    this.syncIndex = syncIndex;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case REQUEST:
        break;
      case RESPONSE:
        syncIndex = in.readBoolean();
        break;
      case FAILED_RESPONSE:
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case REQUEST:
        break;
      case RESPONSE:
        out.writeBoolean(syncIndex);
        break;
      case FAILED_RESPONSE:
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  public boolean syncIndex() {
    return this.syncIndex;
  }

  @Override
  public String toString() {
    return "ObjectListSyncMessage [ " + messageFrom() + ", type = " + getTypeString() + ", " + syncIndex + "]";
  }

  private String getTypeString() {
    switch (getType()) {
      case REQUEST:
        return "REQUEST";
      case RESPONSE:
        return "RESPONSE";
      default:
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

}
