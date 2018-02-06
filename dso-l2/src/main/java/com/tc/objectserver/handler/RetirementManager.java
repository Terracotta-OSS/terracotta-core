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

import com.tc.objectserver.api.Retiree;
import com.tc.tracing.Trace;
import com.tc.util.Assert;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

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
    this.currentlyRunning = new IdentityHashMap<>();
    this.waitingForDeferredRegistration = new IdentityHashMap<>();
    this.mostRecentRegisteredToKey = new HashMap<>();
  }
  
  public synchronized boolean isMessageRunning(EntityMessage invokeMessage) {
    return this.currentlyRunning.containsKey(invokeMessage);
  }
  
  public synchronized void holdMessage(EntityMessage invokeMessage) {
    if (this.currentlyRunning.computeIfPresent(invokeMessage, (m, ls)->ls.hold()) == null) {
      throw new IllegalStateException("message already retired");
    }
  }
  
  public synchronized boolean releaseMessage(EntityMessage invokeMessage) {
    // must be non-null so compute.  retireMessage
    // outside the synchronized block if the message is complete and heldCount is zero
    return this.currentlyRunning.compute(invokeMessage, (m, ls)->ls.release()).isRetireable();
  }

  public synchronized void registerWithMessage(EntityMessage invokeMessage, int concurrencyKey, Retiree retiree) {
    LogicalSequence newWrapper = new LogicalSequence(invokeMessage, concurrencyKey);
    // if concurrencyKey is UNIVERSAL_KEY, then current request doesn't need to wait for other requests running on
    // UNIVERSAL_KEY
    if(concurrencyKey != ConcurrencyStrategy.UNIVERSAL_KEY) {
      // See if there is anything for this key
      LogicalSequence lastInKey = this.mostRecentRegisteredToKey.put(concurrencyKey, newWrapper);
      if ((null != lastInKey) && (!lastInKey.isRetired)) {
        lastInKey.nextInKey = newWrapper;
        newWrapper.isWaitingForPreviousInKey = true;
      }
    }

    LogicalSequence toUpdateWithReference = waitingForDeferredRegistration.remove(invokeMessage);
    if (null != toUpdateWithReference) {
      Assert.assertTrue(toUpdateWithReference.isWaitingForExplicitDeferOf(invokeMessage));
      newWrapper.deferNotify = toUpdateWithReference;
    }

    newWrapper.updateWithRetiree(retiree);
    LogicalSequence previous = this.currentlyRunning.put(invokeMessage, newWrapper);
    // We can't find something else there.
    Assert.assertNull(previous);
  }

  /**
   * This returns a list because it is possible to return a sequence of queued up retirements:  completedMessage may unblock
   * an earlier retirement which is followed by a logical sequence of operations which couldn't retire until it did.
   *
   * @param completedMessage
   * @return
   */
  synchronized List<Retiree> retireForCompletion(EntityMessage completedMessage) {
    List<Retiree> toRetire = new ArrayList<>();
    //  must be non-null if called
    this.currentlyRunning.compute(completedMessage, (m,ls)->{
      if (ls.heldCount > 0) {
        ls.isCompleted = true;
        return ls;
      } else {
        ls.isCompleted = true;
        traverseDependencyGraph(toRetire, ls);
        return null;
      }
    });
    return toRetire;
  }

  private void traverseDependencyGraph(List<Retiree> toRetire, LogicalSequence completedRequest) {
    Stack<LogicalSequence> requestStack = new Stack<>();
    requestStack.add(completedRequest);

    while(!requestStack.isEmpty()) {
      LogicalSequence currentRequest = requestStack.pop();
      Assert.assertFalse(currentRequest.isRetired);

      // proceed if current request is completed
      if(currentRequest.isCompleted) {
        // See if we are still waiting for anyone.
        if (!currentRequest.isWaitingForExplicitDefer() && !currentRequest.isWaitingForPreviousInKey) {
          // We can retire.
          toRetire.add(currentRequest.response);
          currentRequest.isRetired = true;
          this.mostRecentRegisteredToKey.remove(currentRequest.concurrencyKey, currentRequest);
          // since current request is retired, we can unblock next request on same concurrency key if any
          if (currentRequest.nextInKey != null) {
            currentRequest.nextInKey.isWaitingForPreviousInKey = false;
            requestStack.push(currentRequest.nextInKey);
            currentRequest.nextInKey = null;
          }
        }

        // since current request is completed, we can unblock any request waiting on this request if any
        if (currentRequest.deferNotify != null) {
          currentRequest.deferNotify.entityMessageCompleted(currentRequest.entityMessage);
          requestStack.push(currentRequest.deferNotify);
          currentRequest.deferNotify = null;
        }
      }
    }
  }

  public synchronized void deferRetirement(EntityMessage invokeMessageToDefer, EntityMessage laterMessage) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Deferring retirement for " + invokeMessageToDefer + " until " + laterMessage + " is finished");
    }
    
    LogicalSequence myRequest = this.currentlyRunning.get(invokeMessageToDefer);
    
    if (myRequest == null) {
      myRequest = this.waitingForDeferredRegistration.get(invokeMessageToDefer);
      // We can only defer by currently running messages.
      Assert.assertNotNull(myRequest);
    }

    myRequest.retirementDeferredBy(laterMessage);
        
    LogicalSequence previous = this.waitingForDeferredRegistration.put(laterMessage, myRequest);
    Assert.assertNull(previous);
  }

  /**
   * This method purely exists for verifying that nothing has been lost (since anything remaining in this object when the
   *  entity is destroyed would indicate a serious bug and possibly hung clients).
   */
  public synchronized void entityWasDestroyed() {
    Assert.assertTrue(this.currentlyRunning.isEmpty());
    // Note that we don't assert mostRecentRegisteredToKey is empty since it is fixed-size and always contains the most
    //  recent LogicalSequence, per-key (just so they aren't explicitly life-cycled from outside).
    Assert.assertTrue(this.waitingForDeferredRegistration.isEmpty());
  }
  
  public synchronized Map<String, Object> getState() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("running", this.currentlyRunning.entrySet().stream().collect(Collectors.toMap(entry->entry.getKey().toString(), entry->entry.getKey().toString(), (one, two)->one, LinkedHashMap::new)));
    map.put("waitingForDeferredRegistration", this.waitingForDeferredRegistration.entrySet().stream().collect(Collectors.toMap(entry->entry.getKey().toString(), entry->entry.getKey().toString(), (one, two)->one, LinkedHashMap::new)));
    map.put("mostRecentRegisteredToKey", this.mostRecentRegisteredToKey.entrySet().stream().collect(Collectors.toMap(entry->entry.getKey().toString(), entry->entry.getKey().toString(), (one, two)->one, LinkedHashMap::new)));
    return map;
  }
  
  public void retireMessage(EntityMessage message) {
    List<Retiree> readyToRetire = retireForCompletion(message);
    for (Retiree toRetire : readyToRetire) {
      if (null != toRetire) {
        if (Trace.isTraceEnabled()) {
          Trace.activeTrace().log("Retiring message with trace id " + toRetire.getTraceID());
        }
        // if not, retire the message
        toRetire.retired();
      }
    }
  }
  
  private static class LogicalSequence {
    // Corresponding entity message
    public final EntityMessage entityMessage;
    // The next message in the same key, which we will notify to retire when we retire.
    public LogicalSequence nextInKey;
    // concurrency key
    public final int concurrencyKey;
    // The thing to be retired
    private Retiree response;
    // The message which is explicitly waiting for us to retire before it can.
    public LogicalSequence deferNotify;
    // True if we are still waiting for the previous in our key to retire.
    public boolean isWaitingForPreviousInKey;
    // True if the request is completed
    public boolean isCompleted;
    // True if retirement is complete (only used when stitching in the key).
    public boolean isRetired;
    
    public int heldCount = 0;
    
    private final Map<EntityMessage,EntityMessage> entityMessagesDeferringRetirement = new IdentityHashMap<>();

    public LogicalSequence(EntityMessage entityMessage, int concurrency) {
      this.entityMessage = entityMessage;
      this.concurrencyKey = concurrency;
    }

    public LogicalSequence updateWithRetiree(Retiree response) {
      this.response = response;
      return this;
    }

    public void retirementDeferredBy(EntityMessage entityMessage) {
      // just add this entityMessage to waiting set
      entityMessagesDeferringRetirement.put(entityMessage,entityMessage);
    }

    public void entityMessageCompleted(EntityMessage entityMessage) {
      // remove entityMessage from waiting set and return status for asserting
      entityMessagesDeferringRetirement.remove(entityMessage);
    }

    public boolean isWaitingForExplicitDefer() {
      // true if waiting set size is not zero
      return !entityMessagesDeferringRetirement.isEmpty() || this.heldCount > 0;
    }

    public boolean isWaitingForExplicitDeferOf(EntityMessage entityMessage) {
      return entityMessagesDeferringRetirement.containsKey(entityMessage);
    }
    
    public LogicalSequence hold() {
      heldCount += 1;
      return this;
    }

    public LogicalSequence release() {
      heldCount -= 1;
      Assert.assertTrue(heldCount >= 0);
      return this;
    }
    
    public boolean isRetireable() {
      return heldCount == 0 && isCompleted;
    }
    
    @Override
    public String toString() {
      return "LogicalSequence{" + "response=" + response + ", entityMessage=" + 
          entityMessage + ", nextInKey=" + nextInKey + ", deferNotify=" + deferNotify + 
          ", isWaitingForPreviousInKey=" + isWaitingForPreviousInKey + ", isCompleted=" + 
          isCompleted + ", isRetired=" + isRetired + ", entityMessagesDeferringRetirement=" + 
          entityMessagesDeferringRetirement + ", heldCount=" + 
          heldCount + '}';
    }
  }
}
