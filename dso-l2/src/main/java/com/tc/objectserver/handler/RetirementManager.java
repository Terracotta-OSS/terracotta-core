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
 */
package com.tc.objectserver.handler;

import com.tc.objectserver.api.Retiree;
import com.tc.tracing.Trace;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import org.terracotta.entity.EntityMessage;

import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
  private final Map<MessageIdentity, LogicalSequence> currentlyRunning;
  private final Map<MessageIdentity, EntityMessage> waitingForDeferredRegistration;
  private final Map<MessageIdentity, EntityMessage> inflightServerMessages;

  public RetirementManager() {
    this.currentlyRunning = new ConcurrentHashMap<>();
    this.waitingForDeferredRegistration = new ConcurrentHashMap<>();
    this.inflightServerMessages = new ConcurrentHashMap<>();
  }
  
  public void registerServerMessage(EntityMessage msg) {
    inflightServerMessages.put(id(msg), msg);
  }
  
  public boolean hasServerInflightMessages() {
    return !this.inflightServerMessages.isEmpty();
  }
  
  public boolean isMessageRunning(EntityMessage invokeMessage) {
    return this.currentlyRunning.containsKey(id(invokeMessage));
  }
  
  public void holdMessage(EntityMessage invokeMessage) {
    if (this.currentlyRunning.computeIfPresent(id(invokeMessage), (m, ls)->ls.hold()) == null) {
      throw new IllegalStateException("message already retired");
    }
  }
  
  public boolean releaseMessage(EntityMessage invokeMessage) {
    // must be non-null so compute.  retireMessage
    // outside the synchronized block if the message is complete and heldCount is zero
    return this.currentlyRunning.get(id(invokeMessage)).release().isRetireable();
  }
  
  private void removeMessage(EntityMessage invoke) {
    currentlyRunning.remove(id(invoke));
    waitingForDeferredRegistration.remove(id(invoke));
  }

  public void registerWithMessage(EntityMessage invokeMessage, int concurrencyKey, Retiree retiree) {
    LogicalSequence newWrapper = new LogicalSequence(invokeMessage, concurrencyKey);

    EntityMessage deferred = removeWaitingForDeferred(invokeMessage);
    if (null != deferred) {
      LogicalSequence ls = getCurrentlyRunning(deferred);
      ls.retirementDeferredBy(invokeMessage, newWrapper);
    }

    newWrapper.updateWithRetiree(retiree);
    LogicalSequence previous = messageIsRunning(invokeMessage, newWrapper);
    // We can't find something else there.
    Assert.assertNull(previous);
  }

  private void removeInflightServerMessage(EntityMessage msg) {
    inflightServerMessages.remove(id(msg));
  }
  /**
   * This returns a list because it is possible to return a sequence of queued up retirements:  completedMessage may unblock
   * an earlier retirement which is followed by a logical sequence of operations which couldn't retire until it did.
   *
   * @param completedMessage
   * @return
   */
  private Deque<LogicalSequence> retireForCompletion(EntityMessage completedMessage) {
    removeInflightServerMessage(completedMessage);
    Deque<LogicalSequence> toRetire = new LinkedList<>();
    //  must be non-null if called
    LogicalSequence ls = this.currentlyRunning.get(id(completedMessage));
    if (ls.complete()) {
      toRetire.add(ls);
    }
    
    return toRetire;
  }
  
  boolean testingIsRetireable(EntityMessage msg) {
    return this.currentlyRunning.get(id(msg)).isRetireable();
  }
  
  List<Retiree> testingRetireForCompletion(EntityMessage completedMessage) {
    LogicalSequence seq = this.currentlyRunning.get(id(completedMessage));
    return traverseDependencyGraph(retireForCompletion(completedMessage));
  }

  private List<Retiree> traverseDependencyGraph(Deque<LogicalSequence> requestStack) {
    List<Retiree> toRetire = new LinkedList<>();
//  it is assumed heere that any message that is completed 
//  is not having new deferments added to it so synchronization is not needed

    while(!requestStack.isEmpty()) {
      LogicalSequence currentRequest = requestStack.pop();

      // proceed if current request is completed
      if(currentRequest.isRetireable()) {
        // We can retire.
        toRetire.add(currentRequest.response);
        removeMessage(currentRequest.entityMessage);
        currentRequest.retire();
        if (currentRequest.deferNotify != null) {
          currentRequest.deferNotify.entityMessageCompleted(currentRequest.entityMessage);
          requestStack.push(currentRequest.deferNotify);
          currentRequest.deferNotify = null;
        }
      }
    }
    return toRetire;
  }
  
  private LogicalSequence messageIsRunning(EntityMessage msg, LogicalSequence ls) {
    return this.currentlyRunning.put(id(msg), ls);
  }
  
  private LogicalSequence getCurrentlyRunning(EntityMessage msg) {
    return this.currentlyRunning.get(id(msg));
  }
  
  private EntityMessage setWaitingForDeferred(EntityMessage toDefer, EntityMessage deferring) {
    return this.waitingForDeferredRegistration.put(id(toDefer), deferring);
  }
  
  private EntityMessage removeWaitingForDeferred(EntityMessage invoked) {
    return this.waitingForDeferredRegistration.remove(id(invoked));
  }

  public void deferRetirement(EntityMessage invokeMessageToDefer, EntityMessage laterMessage) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Deferring retirement for " + invokeMessageToDefer + " until " + laterMessage + " is finished");
    }
    
    LogicalSequence myRequest = getCurrentlyRunning(invokeMessageToDefer);
    LogicalSequence laterRequest = getCurrentlyRunning(laterMessage);
    
    if (laterRequest == null) {
      EntityMessage multiDefer = setWaitingForDeferred(laterMessage, invokeMessageToDefer);
      myRequest.retirementDeferredBy(laterMessage, null);
      // We can only defer by currently running messages.
      Assert.assertNull(multiDefer);
    } else {
      myRequest.retirementDeferredBy(laterMessage, laterRequest);
    }
  }

  /**
   * This method purely exists for verifying that nothing has been lost (since anything remaining in this object when the
   *  entity is destroyed would indicate a serious bug and possibly hung clients).
   */
  public void entityWasDestroyed() {
    Assert.assertTrue(this.currentlyRunning.isEmpty());
    // Note that we don't assert mostRecentRegisteredToKey is empty since it is fixed-size and always contains the most
    //  recent LogicalSequence, per-key (just so they aren't explicitly life-cycled from outside).
    Assert.assertTrue(this.waitingForDeferredRegistration.isEmpty());
  }
  
  public Map<String, Object> getState() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("running", this.currentlyRunning.entrySet().stream().collect(Collectors.toMap(entry->entry.getKey().toString(), entry->entry.getKey().toString(), (one, two)->one, LinkedHashMap::new)));
    map.put("waitingForDeferredRegistration", this.waitingForDeferredRegistration.entrySet().stream().collect(Collectors.toMap(entry->entry.getKey().toString(), entry->entry.getKey().toString(), (one, two)->one, LinkedHashMap::new)));
    return map;
  }
  
  public void retireMessage(EntityMessage message) {
    Deque<LogicalSequence> sequence = retireForCompletion(message);
    List<Retiree> readyToRetire = traverseDependencyGraph(sequence);
    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    // order matters here, this should be a reverse chain of the original deferment chain
    for (Retiree toRetire : readyToRetire) {
      if (null != toRetire) {
        if (Trace.isTraceEnabled()) {
          Trace.activeTrace().log("Retiring message with trace id " + toRetire.getTraceID());
        }
        chain = chain.thenCompose((t)->toRetire.retired());
      }
    }
  }
  
  private static class LogicalSequence {
    // Corresponding entity message
    public final EntityMessage entityMessage;
    // concurrency key
    public final int concurrencyKey;
    // The thing to be retired
    private Retiree response;
    // The message which is explicitly waiting for us to retire before it can.
    private LogicalSequence deferNotify;
    // True if the request is completed
    private final SetOnceFlag isCompleted = new SetOnceFlag();
    // True if retirement is complete (only used when stitching in the key).
    private final SetOnceFlag isRetired = new SetOnceFlag();
    
    private int heldCount = 0;
        
    private final Map<EntityMessage,LogicalSequence> entityMessagesDeferringRetirement = new IdentityHashMap<>();

    public LogicalSequence(EntityMessage entityMessage, int concurrency) {
      this.entityMessage = entityMessage;
      this.concurrencyKey = concurrency;
    }

    public LogicalSequence updateWithRetiree(Retiree response) {
      this.response = response;
      return this;
    }

    public synchronized void retirementDeferredBy(EntityMessage entityMessage, LogicalSequence ls) {
      // just add this entityMessage to waiting set
//      Assert.assertFalse(isCompleted.isSet());
      entityMessagesDeferringRetirement.put(entityMessage,ls);
      if (ls != null) {
        Assert.assertNull(ls.deferNotify);
        ls.deferNotify = this;
      }
    }

    public synchronized void entityMessageCompleted(EntityMessage entityMessage) {
      // remove entityMessage from waiting set and return status for asserting
      entityMessagesDeferringRetirement.remove(entityMessage);
    }

    public synchronized boolean isWaitingForExplicitDefer() {
      boolean messagesWaiting = !entityMessagesDeferringRetirement.isEmpty();
      // true if waiting set size is not zero
      return messagesWaiting || this.heldCount > 0;
    }

    public synchronized boolean isWaitingForExplicitDeferOf(EntityMessage entityMessage) {
      return entityMessagesDeferringRetirement.containsKey(entityMessage);
    }
    //  synchronized by the caller
    public LogicalSequence hold() {
      heldCount += 1;
      return this;
    }
    //  synchronized by the caller
    public LogicalSequence release() {
      heldCount -= 1;
      Assert.assertTrue(heldCount >= 0);
      return this;
    }
    
    public void retire() {
      isRetired.attemptSet();
    }
    
    public boolean complete() {
      isCompleted.attemptSet();
      return !isWaitingForExplicitDefer();
    }
    
    public boolean isRetireable() {
      return isCompleted.isSet() && !isWaitingForExplicitDefer();
    }
    
    @Override
    public String toString() {
      return "LogicalSequence{" + "response=" + response + ", entityMessage=" + 
          entityMessage + ", deferNotify=" + deferNotify + ", isCompleted=" + 
          isCompleted + ", isRetired=" + isRetired + ", entityMessagesDeferringRetirement=" + 
          entityMessagesDeferringRetirement + ", heldCount=" + 
          heldCount + '}';
    }
  }
  
  private static MessageIdentity id(EntityMessage target) {
    return new MessageIdentity(target);
  }
  
  private static class MessageIdentity {
    private final EntityMessage target;

    public MessageIdentity(EntityMessage target) {
      this.target = target;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof MessageIdentity) ? target == ((MessageIdentity)obj).target : false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this.target);
    }

    @Override
    public String toString() {
      return target.toString();
    }
  }
}
