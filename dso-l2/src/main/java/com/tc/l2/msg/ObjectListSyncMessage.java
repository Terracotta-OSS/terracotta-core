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
  private long            resourceSize;

  // To make serialization happy
  public ObjectListSyncMessage() {
    super(-1);
  }

  public ObjectListSyncMessage(int type) {
    super(type);
  }

  public ObjectListSyncMessage(MessageID messageID, int type, State currentState, final boolean syncAllowed, final long resourceSize) {
    super(type, messageID);
    this.syncAllowed = syncAllowed;
    this.currentState = currentState;
    this.resourceSize = resourceSize;
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
        resourceSize = in.readLong();
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
        out.writeLong(resourceSize);
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + getType());
    }
  }

  public State getCurrentState() {
    return this.currentState;
  }

  public long getResourceSize() {
    return resourceSize;
  }

  public boolean isSyncAllowed() {
    return syncAllowed;
  }

  @Override
  public String toString() {
    return "ObjectListSyncMessage{" +
           "type=" + getTypeString() +
           ", syncAllowed=" + syncAllowed +
           ", currentState=" + currentState +
           ", resourceSize=" + resourceSize +
           '}';
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
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

}
