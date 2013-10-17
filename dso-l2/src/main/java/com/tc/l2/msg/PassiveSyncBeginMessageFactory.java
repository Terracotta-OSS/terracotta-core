package com.tc.l2.msg;

import com.tc.util.State;

/**
 * @author tim
 */
public class PassiveSyncBeginMessageFactory {
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
