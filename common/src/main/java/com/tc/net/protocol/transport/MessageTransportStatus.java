/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import org.slf4j.Logger;

import com.tc.util.Assert;

class MessageTransportStatus {
  private MessageTransportState state;
  private final Logger logger;

  MessageTransportStatus(MessageTransportState initialState, Logger logger) {
    this.state = initialState;
    this.logger = logger;
  }

  void reset() {
    stateChange(MessageTransportState.STATE_START);
  }

  private synchronized void stateChange(MessageTransportState newState) {
    if (logger.isDebugEnabled()) {
      logger.debug("Changing from " + state.toString() + " to " + newState.toString());
    }

    if (isEnd()) {
      Assert.eval("Transport StateChange from END state not allowed", newState == MessageTransportState.STATE_START || newState != MessageTransportState.STATE_END);
      logger.warn("Unexpected Transport StateChange attempt. Changing from " + state.toString() + " to "
                  + newState.toString(), new Throwable());
    }
    state = newState;
    notifyAll();
  }

  void synSent() {
    stateChange(MessageTransportState.STATE_SYN_SENT);
  }

  void synAckError() {
    stateChange(MessageTransportState.STATE_SYN_ACK_ERROR);
  }

  void established() {
    stateChange(MessageTransportState.STATE_ESTABLISHED);
  }
  
  void connected() {
    stateChange(MessageTransportState.STATE_CONNECTED);
  }

  void disconnect() {
    stateChange(MessageTransportState.STATE_DISCONNECTED);
  }

  void end() {
    stateChange(MessageTransportState.STATE_END);
  }
  
  private synchronized boolean checkState(MessageTransportState check) {
    return this.state.equals(check);
  }

  boolean isStart() {
    return checkState(MessageTransportState.STATE_START);
  }
  
  boolean isRestart() {
    return checkState(MessageTransportState.STATE_RESTART);
  }

  boolean isSynSent() {
    return checkState(MessageTransportState.STATE_SYN_SENT);
  }

  boolean isEstablished() {
    return checkState(MessageTransportState.STATE_ESTABLISHED);
  }
  
  boolean isDisconnected() {
    return checkState(MessageTransportState.STATE_DISCONNECTED);
  }
  
  boolean isConnected() {
    return checkState(MessageTransportState.STATE_CONNECTED);
  }
  
  synchronized boolean resetIfNotEnd() {
    if (!checkState(MessageTransportState.STATE_END)) {
      reset();
      return true;
    } else {
      return false;
    }
  }

  boolean isEnd() {
    return checkState(MessageTransportState.STATE_END);
  }
  
  synchronized boolean isAlive() {
    return this.state.isAlive();
  }

  @Override
  public String toString() {
    return state.toString();
  }
}
