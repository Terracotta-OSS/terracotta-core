package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.State;

import java.io.IOException;

/**
 * @author tim
 */
public class PassiveSyncBeginMessage extends AbstractGroupMessage {
  static final int REQUEST = 0;
  static final int RESPONSE = 1;
  static final int ERROR = 2;

  private State currentState;

  public PassiveSyncBeginMessage() {
    this(-1);
  }

  public PassiveSyncBeginMessage(final int type) {
    super(type);
  }

  public PassiveSyncBeginMessage(final int type, final MessageID requestID) {
    super(type, requestID);
  }

  public boolean isRequest() {
    return getType() == REQUEST;
  }

  public boolean isResponse() {
    return getType() == RESPONSE;
  }

  public boolean isError() {
    return getType() == ERROR;
  }

  public void setCurrentState(State state) {
    if (!isResponse()) {
      throw new IllegalStateException();
    }
    this.currentState = state;
  }

  public State getCurrentState() {
    if (!isResponse()) {
      throw new IllegalStateException();
    }
    return currentState;
  }

  @Override
  protected void basicDeserializeFrom(final TCByteBufferInput in) throws IOException {
    if (getType() == RESPONSE) {
      currentState = new State(in.readString());
    }
  }

  @Override
  protected void basicSerializeTo(final TCByteBufferOutput out) {
    if (getType() == RESPONSE) {
      out.writeString(currentState.getName());
    }
  }

  private String getTypeString() {
    switch (getType()) {
      case REQUEST:
        return "REQUEST";
      case RESPONSE:
        return "RESPONSE";
      case ERROR:
        return "ERROR";
      default:
        return "UNKNOWN";
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PassiveSyncBeginMessage message = (PassiveSyncBeginMessage)o;

    if (getType() != message.getType()) return false;
    if (currentState != null ? !currentState.equals(message.currentState) : message.currentState != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int hash = getType();
    if (currentState != null) {
      hash = 31 * hash + currentState.hashCode();
    }
    return hash;
  }

  @Override
  public String toString() {
    return "PassiveSyncBeginMessage{" +
           "type=" + getTypeString() + ", " +
           "currentState=" + currentState +
           '}';
  }

  public static PassiveSyncBeginMessage beginRequest() {
    return new PassiveSyncBeginMessage(PassiveSyncBeginMessage.REQUEST);
  }

  public static PassiveSyncBeginMessage beginResponse(State currentState) {
    PassiveSyncBeginMessage message = new PassiveSyncBeginMessage(PassiveSyncBeginMessage.RESPONSE);
    message.setCurrentState(currentState);
    return message;
  }

  public static PassiveSyncBeginMessage beginError() {
    return new PassiveSyncBeginMessage(PassiveSyncBeginMessage.ERROR);
  }
}
