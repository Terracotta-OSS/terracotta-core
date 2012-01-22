/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.util.Assert;

class MessageTransportStatus {
  private MessageTransportState state;
  private TCLogger              logger;

  MessageTransportStatus(MessageTransportState initialState, TCLogger logger) {
    this.state = initialState;
    this.logger = logger;
  }

  synchronized void reset() {
    state = MessageTransportState.STATE_START;
  }

  private void stateChange(MessageTransportState newState) {

    if (logger.isDebugEnabled()) {
      logger.debug("Changing from " + state.toString() + " to " + newState.toString());
    }

    if (isEnd()) {
      Assert.eval("Transport StateChange from END state not allowed", newState != MessageTransportState.STATE_END);
      logger.warn("Unexpected Transport StateChange attempt. Changing from " + state.toString() + " to "
                  + newState.toString(), new Throwable());
    }
    state = newState;
  }

  synchronized void synSent() {
    stateChange(MessageTransportState.STATE_SYN_SENT);
  }

  synchronized void synAckError() {
    stateChange(MessageTransportState.STATE_SYN_ACK_ERROR);
  }

  synchronized void established() {
    stateChange(MessageTransportState.STATE_ESTABLISHED);
  }

  synchronized void closed() {
    stateChange(MessageTransportState.STATE_CLOSED);
  }

  synchronized void disconnect() {
    stateChange(MessageTransportState.STATE_DISCONNECTED);
  }

  synchronized void end() {
    stateChange(MessageTransportState.STATE_END);
  }

  synchronized boolean isStart() {
    return this.state.equals(MessageTransportState.STATE_START);
  }

  public boolean isRestart() {
    return this.state.equals(MessageTransportState.STATE_RESTART);
  }

  synchronized boolean isSynSent() {
    return this.state.equals(MessageTransportState.STATE_SYN_SENT);
  }

  synchronized boolean isEstablished() {
    return this.state.equals(MessageTransportState.STATE_ESTABLISHED);
  }
  
  synchronized boolean isDisconnected() {
    return this.state.equals(MessageTransportState.STATE_DISCONNECTED);
  }
  
  synchronized boolean isClosed() {
    return this.state.equals(MessageTransportState.STATE_CLOSED);
  }

  synchronized boolean isEnd() {
    return this.state.equals(MessageTransportState.STATE_END);
  }

  public String toString() {
    return state.toString();
  }

}
