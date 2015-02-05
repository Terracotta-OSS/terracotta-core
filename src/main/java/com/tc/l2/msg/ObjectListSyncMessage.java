/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

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

  public ObjectListSyncMessage(MessageID messageID, int type, State currentState, boolean syncAllowed, long dataStorageSize,
                               long offheapSize) {
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
    return new StringBuffer(getClass().getSimpleName()).append("(").append("type=").append(getTypeString())
        .append(",syncAllowed=").append(syncAllowed).append(",currentState=").append(currentState)
        .append(",dataStorageSize=").append(dataStorageSize).append(",offheapSize=").append(offheapSize).append(")")
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

  public static ObjectListSyncMessage createObjectListSyncRequestMessage() {
    return new ObjectListSyncMessage(ObjectListSyncMessage.REQUEST);
  }

  public static ObjectListSyncMessage createObjectListSyncResponseMessage(ObjectListSyncMessage initiatingMsg,
                                                                          State currentState, boolean syncAllowed,
                                                                          long dataStorageSize, long offheapSize) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.RESPONSE, currentState,
        syncAllowed, dataStorageSize, offheapSize);
  }

  public static ObjectListSyncMessage createObjectListSyncFailedResponseMessage(ObjectListSyncMessage initiatingMsg) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.FAILED_RESPONSE);
  }

}
