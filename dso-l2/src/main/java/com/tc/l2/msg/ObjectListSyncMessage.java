/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.google.common.base.Objects;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.State;

import java.io.IOException;

public class ObjectListSyncMessage extends AbstractGroupMessage {

  public static final int REQUEST         = 0;
  public static final int RESPONSE        = 1;
  public static final int FAILED_RESPONSE = 2;

  private boolean         syncAllowed;
  private State           currentState;
  private long            dataStorageSize;
  private long            offheapSize;

  // To make serialization happy
  public ObjectListSyncMessage() {
    super(-1);
  }

  public ObjectListSyncMessage(int type) {
    super(type);
  }

  public ObjectListSyncMessage(MessageID messageID, int type, State currentState, final boolean syncAllowed, final long dataStorageSize,
                               final long offheapSize) {
    super(type, messageID);
    this.syncAllowed = syncAllowed;
    this.currentState = currentState;
    this.dataStorageSize = dataStorageSize;
    this.offheapSize = offheapSize;
  }

  public ObjectListSyncMessage(MessageID messageID, int type) {
    super(type, messageID);
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case REQUEST:
      case FAILED_RESPONSE:
        // Nothing to read
        break;
      case RESPONSE:
        currentState = new State(in.readString());
        syncAllowed = in.readBoolean();
        dataStorageSize = in.readLong();
        offheapSize = in.readLong();
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case REQUEST:
      case FAILED_RESPONSE:
        // Nothing to write
        break;
      case RESPONSE:
        out.writeString(this.currentState.getName());
        out.writeBoolean(syncAllowed);
        out.writeLong(dataStorageSize);
        out.writeLong(offheapSize);
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  public State getCurrentState() {
    return this.currentState;
  }

  public long getDataStorageSize() {
    return dataStorageSize;
  }

  public boolean isSyncAllowed() {
    return syncAllowed;
  }

  public long getOffheapSize() {
    return offheapSize;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", getTypeString())
        .add("syncAllowed", syncAllowed)
        .add("currentState", currentState)
        .add("dataStorageSize", dataStorageSize)
        .add("offheapSize", offheapSize)
        .toString();
  }

  private String getTypeString() {
    switch (getType()) {
      case REQUEST:
        return "REQUEST";
      case FAILED_RESPONSE:
        return "FAILED_RESPONSE";
      case RESPONSE:
        return "RESPONSE";
      default:
        throw new AssertionError("Unknown Type ! : " + getType());
    }
  }

}
