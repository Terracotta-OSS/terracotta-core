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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.Map;


/**
 * Maintains the association that a connection has to its server and the in-flight messages associated with it.
 * The reason why this is managed out-of-line is that reconnect represents very specific ordering concerns and thread
 * interaction concerns, which are far more easily managed within a distinct object.
 * Note that all the methods in this object are synchronized so they can not block for any reason other than waiting.  That
 * is to say that nothing can perform a blocking operation while holding the monitor.
 */
public class PassthroughConnectionState {
  private PassthroughServerProcess serverProcess;
  private final Map<Long, PassthroughWait> inFlightMessages;
  // We store the reconnecting server just to assert details of correct usage.
  private PassthroughServerProcess reconnectingServerProcess;
  
  // Transaction IDs are managed here, as well.
  private long nextTransactionID;
  
  public PassthroughConnectionState(PassthroughServerProcess initialServerProcess) {
    this.serverProcess = initialServerProcess;
    this.inFlightMessages = new HashMap<Long, PassthroughWait>();
    this.nextTransactionID = 1;
  }

  public synchronized PassthroughWait sendNormal(PassthroughConnection sender, PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire) {
    // This uses the normal server process so wait for it to become available.
    while (null == this.serverProcess) {
      try {
        wait();
      } catch (InterruptedException e) {
        // The only reason we would interrupt is to kill the test.
        throw new RuntimeException(e);
      }
    }
    long oldestTransactionID = 0;
    for (long oneID : this.inFlightMessages.keySet()) {
      if ((0 == oldestTransactionID) || (oneID < oldestTransactionID)) {
        oldestTransactionID = oneID;
      }
    }
    return createAndSend(this.serverProcess, this.inFlightMessages, sender, message, oldestTransactionID, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, forceGetToBlockOnRetire);
  }

  private PassthroughWait createAndSend(PassthroughServerProcess target, Map<Long, PassthroughWait> tracker, PassthroughConnection sender, PassthroughMessage message, long oldestTransactionID, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire) {
    PassthroughWait waiter = new PassthroughWait(shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, forceGetToBlockOnRetire);
    long transactionID = this.nextTransactionID;
    this.nextTransactionID += 1;
    message.setTransactionTracking(transactionID, oldestTransactionID);
    tracker.put(transactionID, waiter);
    if (shouldWaitForSent) {
      waiter.sent();
    }
    byte[] raw = message.asSerializedBytes();
    waiter.saveRawMessageForResend(raw);
    target.sendMessageToServer(sender, raw);
    return waiter;
  }

  public synchronized boolean isConnected(PassthroughServerProcess sender) {
    return (sender == this.serverProcess) || (sender == this.reconnectingServerProcess);
  }

  public synchronized PassthroughWait sendAsReconnect(PassthroughConnection sender, PassthroughMessage message, boolean shouldWaitForSent, boolean shouldWaitForReceived, boolean shouldWaitForCompleted, boolean shouldWaitForRetired, boolean forceGetToBlockOnRetire) {
    // This is similar to the normal send but only happens in the reconnect state and creates a waiter in that in-flight set.
    Assert.assertTrue(null != this.reconnectingServerProcess);
    // We won't bother clearing transactions on re-send.
    long oldestTransactionID = 0;
    return createAndSend(this.reconnectingServerProcess, this.inFlightMessages, sender, message, oldestTransactionID, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, forceGetToBlockOnRetire);
  }

  public synchronized Map<Long, PassthroughWait> enterReconnectState(PassthroughServerProcess newServerProcess) {
    Assert.assertTrue(null == this.serverProcess);
    Assert.assertTrue(null == this.reconnectingServerProcess);
    Assert.assertTrue(null != this.inFlightMessages);
    
    this.reconnectingServerProcess = newServerProcess;
    return this.inFlightMessages;
  }

  public synchronized void sendAsResend(PassthroughConnection sender, long transactionID, PassthroughWait waiter) {
    // This is similar to the normal send but only happens in the reconnect state and creates a waiter in that in-flight set.
    Assert.assertTrue(null != this.reconnectingServerProcess);
    byte[] raw = waiter.resetAndGetMessageForResend();
    this.inFlightMessages.put(transactionID, waiter);
    // We always want to block on retire, when doing a re-send.
    waiter.blockGetOnRetire();
    this.reconnectingServerProcess.sendMessageToServer(sender, raw);
  }

  public synchronized PassthroughWait getWaiterForTransaction(PassthroughServerProcess sender, long transactionID) {
    PassthroughWait waiter = this.inFlightMessages.get(transactionID);
    Assert.assertTrue(null != waiter);
    return waiter;
  }

  public synchronized PassthroughWait removeWaiterForTransaction(PassthroughServerProcess sender, long transactionID) {
    PassthroughWait waiter = this.inFlightMessages.remove(transactionID);
    Assert.assertTrue(null != waiter);
    return waiter;
  }

  public synchronized void finishReconnectState() {
    Assert.assertTrue(null == this.serverProcess);
    Assert.assertTrue(null != this.reconnectingServerProcess);
    
    this.serverProcess = this.reconnectingServerProcess;
    this.reconnectingServerProcess = null;
    notifyAll();
  }

  public synchronized void enterDisconnectedState() {
    Assert.assertTrue(null != this.serverProcess);
    Assert.assertTrue(null != this.inFlightMessages);
    
    this.serverProcess = null;
  }

  public synchronized void forceClose() {
    Assert.assertTrue(null != this.inFlightMessages);
    for (PassthroughWait waiter : this.inFlightMessages.values()) {
      waiter.forceDisconnect();
    }
  }
}
