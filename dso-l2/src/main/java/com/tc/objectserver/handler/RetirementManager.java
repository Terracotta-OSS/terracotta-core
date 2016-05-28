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
package com.tc.objectserver.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.entity.EntityMessage;

import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;


/**
 * The ability to defer retirement introduces a complex dependency graph (tree) between the messages in the system.
 * While a message can wait for another to complete, before retiring (a "defer" operation), the messages within a given
 * concurrency key also represent a logical ordering which must be preserved.
 * This means that each message retirement can branch in 2 ways:
 *  1) The next message in the same key
 *  2) Any message which had deferred to it
 * The side-effect of these 2 statements is that it is possible for a single message completion to result in the
 * retirement of a great number of other messages, as each message unblocked can similarly unblock 2 more.
 * 
 * NOTE:  It may be possible to avoid the synchronization on most operations since we know that there is only one running
 * message, per key.
 */
public class RetirementManager {
  private final Map<EntityMessage, LogicalSequence> currentlyRunning;
  private final Map<EntityMessage, LogicalSequence> waitingForDeferredRegistration;
  private final Map<Integer, LogicalSequence> mostRecentRegisteredToKey;


  public RetirementManager() {
    this.currentlyRunning = new HashMap<EntityMessage, LogicalSequence>();
    this.waitingForDeferredRegistration = new HashMap<EntityMessage, LogicalSequence>();
    this.mostRecentRegisteredToKey = new HashMap<Integer, LogicalSequence>();
  }

  public synchronized void registerWithMessage(ServerEntityRequest request, EntityMessage invokeMessage, int concurrencyKey) {
    Assert.assertTrue(ServerEntityAction.INVOKE_ACTION == request.getAction());
    
    LogicalSequence newWrapper = new LogicalSequence(request);
    // See if there is anything for this key
    LogicalSequence lastInKey = this.mostRecentRegisteredToKey.remove(concurrencyKey);
    if ((null != lastInKey) && (!lastInKey.isRetired)) {
      lastInKey.nextInKey = newWrapper;
      newWrapper.isWaitingForPreviousInKey = true;
    }
    
    LogicalSequence toUpdateWithReference = waitingForDeferredRegistration.remove(invokeMessage);
    if (null != toUpdateWithReference) {
      Assert.assertTrue(toUpdateWithReference.isWaitingForExplicitDefer);
      newWrapper.deferNotify = toUpdateWithReference;
    }
    
    LogicalSequence previous = this.currentlyRunning.put(invokeMessage, newWrapper);
    // We can't find something else there.
    Assert.assertNull(previous);
    
    this.mostRecentRegisteredToKey.put(concurrencyKey, newWrapper);
  }

  /**
   * This returns a list because it is possible to return a sequence of queued up retirements:  completedMessage may unblock
   * an earlier retirement which is followed by a logical sequence of operations which couldn't retire until it did.
   * 
   * @param completedMessage
   * @return
   */
  public synchronized List<ServerEntityRequest> retireForCompletion(EntityMessage completedMessage) {
    List<ServerEntityRequest> combined = new Vector<ServerEntityRequest>();
    
    LogicalSequence myRequest = this.currentlyRunning.remove(completedMessage);
    Assert.assertNotNull(myRequest);
    
    LogicalSequence startingPoint = myRequest;
    if (!myRequest.isWaitingForExplicitDefer) {
      LogicalSequence deferUnblock = myRequest.deferNotify;
      while (null != deferUnblock) {
        startingPoint = deferUnblock;
        deferUnblock.isWaitingForExplicitDefer = false;
        deferUnblock = deferUnblock.deferNotify;
      }
    }
    followPath(combined, startingPoint);
    return combined;
  }

  private void followPath(List<ServerEntityRequest> combined, LogicalSequence toComplete) {
    Assert.assertFalse(toComplete.isRetired);
    // See if we are still waiting for anyone.
    if (!toComplete.isWaitingForExplicitDefer && ! toComplete.isWaitingForPreviousInKey) {
      // We can retire.
      combined.add(toComplete.request);
      toComplete.isRetired = true;
      
      if (null != toComplete.nextInKey) {
        Assert.assertTrue(toComplete.nextInKey.isWaitingForPreviousInKey);
        toComplete.nextInKey.isWaitingForPreviousInKey = false;
        followPath(combined, toComplete.nextInKey);
      }
    }
  }

  public synchronized void deferRetirement(EntityMessage invokeMessageToDefer, EntityMessage laterMessage) {
    LogicalSequence myRequest = this.currentlyRunning.get(invokeMessageToDefer);
    // We can only defer by currently running messages.
    Assert.assertNotNull(myRequest);
    
    Assert.assertFalse(myRequest.isWaitingForExplicitDefer);
    myRequest.isWaitingForExplicitDefer = true;
    
    LogicalSequence previous = this.waitingForDeferredRegistration.put(laterMessage, myRequest);
    Assert.assertNull(previous);
  }


  private static class LogicalSequence {
    public final ServerEntityRequest request;
    // The next message in the same key, which we will notify to retire when we retire.
    public LogicalSequence nextInKey;
    // The message which is explicitly waiting for us to retire before it can.
    public LogicalSequence deferNotify;
    // True if we are still waiting for the previous in our key to retire.
    public boolean isWaitingForPreviousInKey;
    // True if we are still waiting to be notified that the message to which we are deferring has completed.
    public boolean isWaitingForExplicitDefer;
    // True if retirement is complete (only used when stitching in the key).
    public boolean isRetired;
    
    public LogicalSequence(ServerEntityRequest request) {
      this.request = request;
    }
  }
}
