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
package com.tc.objectserver.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.locks.context.LinkedServerLockContext;
import com.tc.objectserver.locks.context.SingleServerLockContext;
import com.tc.objectserver.locks.context.WaitServerLockContext;
import com.tc.objectserver.locks.timer.LockTimer.LockTimerContext;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.SinglyLinkedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.TimerTask;

/**
 * This class extends SinglyLinkedList which stores ServerLockContext. The ServerLockContexts are placed in the order of
 * greedy holders, pending requests, try lock requests and then waiters.
 */
public abstract class AbstractServerLock extends SinglyLinkedList<ServerLockContext> implements ServerLock,
    StateDumpable {
  private final static EnumSet<Type> SET_OF_TRY_PENDING_OR_WAITERS = EnumSet.of(Type.TRY_PENDING, Type.WAITER);
  private final static EnumSet<Type> SET_OF_WAITERS                = EnumSet.of(Type.WAITER);
  private final static EnumSet<Type> SET_OF_HOLDERS                = EnumSet.of(Type.HOLDER, Type.GREEDY_HOLDER);

  protected final static Logger logger = LoggerFactory.getLogger(AbstractServerLock.class);
  protected final LockID lockID;

  public AbstractServerLock(LockID lockID) {
    this.lockID = lockID;
  }

  @Override
  public void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    validateAndGetNumberOfPending(cid, tid, level);
    requestLock(cid, tid, level, Type.PENDING, -1, helper);
  }

  @Override
  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    validateAndGetNumberOfPending(cid, tid, level);

    if (timeout <= 0 && !canAwardRequest(level)) {
      refuseTryRequestWithNoTimeout(cid, tid, level, helper);
      return;
    }

    requestLock(cid, tid, level, Type.TRY_PENDING, timeout, helper);
  }

  @Override
  public void queryLock(ClientID cid, ThreadID tid, LockHelper helper) {
    List<ClientServerExchangeLockContext> holdersAndWaiters = new ArrayList<>();
    int pendingCount = 0;

    for (ServerLockContext context : this) {
      ClientServerExchangeLockContext cselc = null;
      Type type = context.getState().getType();
      switch (type) {
        case GREEDY_HOLDER:
        case HOLDER:
          cselc = new ClientServerExchangeLockContext(lockID, context.getClientID(), context.getThreadID(), context
              .getState());
          holdersAndWaiters.add(cselc);
          break;
        case WAITER:
          cselc = new ClientServerExchangeLockContext(lockID, context.getClientID(), context.getThreadID(), context
              .getState(), ((WaitServerLockContext) context).getTimeout());
          holdersAndWaiters.add(cselc);
          break;
        case PENDING:
        case TRY_PENDING:
          pendingCount++;
          break;
        default:
          throw new AssertionError(type);
      }
    }

    LockResponseContext lrc = LockResponseContextFactory.createLockQueriedResponseContext(this.lockID, cid, tid, this.holderLevel(), holdersAndWaiters, pendingCount);
    helper.getLockSink().addMultiThreaded(lrc);
  }

  @Override
  public void interrupt(ClientID cid, ThreadID tid, LockHelper helper) {
    // check if waiters are present
    ServerLockContext context = remove(cid, tid, SET_OF_WAITERS);
    if (context == null) {
      logger.warn("Cannot interrupt: " + cid + "," + tid + " is not waiting.");
      return;
    }

    moveWaiterToPending(context, helper);
    processPendingRequests(helper);
  }

  @Override
  public NotifiedWaiters notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo,
                                LockHelper helper) throws TCIllegalMonitorStateException {
    ServerLockContext holder = getNotifyHolder(cid, tid);
    validateWaitNotifyState(cid, tid, holder, helper);

    List<ServerLockContext> waiters = removeWaiters(action);
    for (ServerLockContext waiter : waiters) {
      moveWaiterToPending(waiter, helper);
      ClientServerExchangeLockContext cselc = new ClientServerExchangeLockContext(lockID, waiter.getClientID(), waiter
          .getThreadID(), State.WAITER);
      addNotifiedWaitersTo.addNotification(cselc);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Notify " + cid + " " + tid + " " + addNotifiedWaitersTo);
    }

    return addNotifiedWaitersTo;
  }

  @Override
  public void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) {
    moveFromHolderToWaiter(cid, tid, timeout, helper);
    processPendingRequests(helper);
  }

  /**
   * This method should be called to release a lock. One important thing to note is that this method should not be
   * called while trying to release a greedy lock (recall commit is the way to go for that). We ignore requests for
   * which context could not be found. The reason for that is avoid a race between a client and the server where a
   * client might initiate an unlock while a greedy lock award is in flight to the client.
   * <p>
   * Scenario explained below: <br>
   * Client 1 is holding greedy write and receives recall request for read lock. It only has read holders so it sends
   * the following... <br>
   * Recall Commit: Thread[1], HOLDER_READ; Thread[2], PENDING_READ; - this can happen when a write lock was held by
   * Thread[1] but has just been unlocked (but before this thread grabbed the read lock) <br>
   * The server receives this and notices that the Thread 2 request is pending, and therefore awards a greedy read to
   * Client 1.<br>
   * While the greedy award is in flight to Client 1, Thread[1] does an unlock, sees that the lock is free (no greedy
   * hold) and therefore does a remote unlock on the server
   * 
   * @param cid - client id requesting
   * @param tid - thread id requesting to be unlocked
   * @param helper
   */
  @Override
  public void unlock(ClientID cid, ThreadID tid, LockHelper helper) {
    // remove current hold
    ServerLockContext context = remove(cid, tid, SET_OF_HOLDERS);

    if (context == null) { return; }
    Assert.assertTrue(context.isHolder());

    if (clearLockIfRequired(helper)) { return; }
    processPendingRequests(helper);
  }

  @Override
  public void reestablishState(ClientServerExchangeLockContext cselc, LockHelper helper) {
    Assert.assertFalse(checkDuplicate((ClientID) cselc.getNodeID(), cselc.getThreadID()));
    switch (cselc.getState().getType()) {
      case GREEDY_HOLDER:
      case HOLDER:
        if (!canAwardRequest(cselc.getState().getLockLevel())) { throw new AssertionError(
                                                                                          "Lock could not be awarded as it is already held "
                                                                                              + cselc); }
        reestablishLock(cselc, helper);
        break;
      case WAITER:
        ServerLockContext context = createWaiterAndScheduleTask(cselc, helper);
        addWaiter(context, helper);
        break;
      //$CASES-OMITTED$
      default:
        throw new IllegalArgumentException("Called with wrong type = " + cselc.getState().getType());
    }
  }

  @Override
  public LockMBean getMBean(DSOChannelManager channelManager) {
    List<ServerLockContextBean> contextsPresent = new ArrayList<>();
    SinglyLinkedListIterator<ServerLockContext> contexts = iterator();
    while (contexts.hasNext()) {
      ServerLockContext context = contexts.next();
      ServerLockContextBean clonedContext = null;
      String client = channelManager.getChannelAddress(context.getClientID());
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
        case PENDING:
          clonedContext = new ServerLockContextBean(client, context.getThreadID(), context.getState());
          break;
        case TRY_PENDING:
        case WAITER:
          clonedContext = new ServerLockContextBean(client, context.getThreadID(), context.getState(),
                                                    ((WaitServerLockContext) context).getTimeout());
          break;
        default:
          logger.warn("unexpected lock type: " + context.getState().getType()); 
          break;
      }
      
      if (clonedContext != null) {
        contextsPresent.add(clonedContext);
      }
    }
    LockMBean bean = new LockMBeanImpl(lockID, contextsPresent
        .toArray(new ServerLockContextBean[contextsPresent.size()]));

    return bean;
  }

  @Override
  public LockID getLockID() {
    return lockID;
  }

  @Override
  public boolean clearStateForNode(ClientID cid, LockHelper helper) {
    clearContextsForClient(cid, helper);
    processPendingRequests(helper);

    return isEmpty();
  }

  @Override
  public void timerTimeout(LockTimerContext lockTimerContext) {
    ClientID cid = lockTimerContext.getClientID();
    ThreadID tid = lockTimerContext.getThreadID();
    LockHelper helper = lockTimerContext.getHelper();

    // Ignore contexts for which time out could not be canceled or which got removed
    ServerLockContext context = remove(cid, tid, SET_OF_TRY_PENDING_OR_WAITERS);
    if (context == null) { return; }

    if (context.isWaiter()) {
      waitTimeout(context, helper);
    } else {
      tryLockTimeout(context, helper);
    }
  }

  @Override
  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    // NO-OP
  }

  private void tryLockTimeout(ServerLockContext context, LockHelper helper) {
    Assert.assertTrue(context.isTryPending());
    cannotAward(context.getClientID(), context.getThreadID(), context.getState().getLockLevel(), helper);
    processPendingRequests(helper);
  }

  private void waitTimeout(ServerLockContext context, LockHelper helper) {
    Assert.assertTrue(context.isWaiter());
    // Add a wait Timeout message
    LockResponseContext lrc = LockResponseContextFactory.createLockWaitTimeoutResponseContext(this.lockID, context.getClientID(), context.getThreadID(), context.getState().getLockLevel());
    helper.getLockSink().addMultiThreaded(lrc);
    lock(context.getClientID(), context.getThreadID(), ServerLockLevel.WRITE, helper);
  }

  protected ServerLockContext createWaiterAndScheduleTask(ClientServerExchangeLockContext cselc, LockHelper helper) {
    WaitServerLockContext context = createWaitOrTryPendingServerLockContext((ClientID) cselc.getNodeID(), cselc
        .getThreadID(), cselc.getState(), cselc.timeout(), helper);
    if (cselc.timeout() > 0) {
      LockTimerContext ltc = new LockTimerContext(lockID, cselc.getThreadID(), (ClientID) cselc.getNodeID(), helper);
      TimerTask task = helper.getLockTimer().scheduleTimer(helper.getTimerCallback(), cselc.timeout(), ltc);
      context.setTimerTask(task);
    }
    return context;
  }

  protected boolean clearLockIfRequired(LockHelper helper) {
    if (isEmpty()) {
      LockStore store = helper.getLockStore();
      store.remove(lockID);
      return true;
    }
    return false;
  }

  protected void reestablishLock(ClientServerExchangeLockContext cselc, LockHelper helper) {
    awardLock(helper, createPendingContext((ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState()
        .getLockLevel(), helper), false);
  }

  protected void moveFromHolderToWaiter(ClientID cid, ThreadID tid, long timeout, LockHelper helper) {
    ServerLockContext holder = remove(cid, tid, SET_OF_HOLDERS);
    validateWaitNotifyState(cid, tid, holder, helper);

    WaitServerLockContext waiter = createWaitOrTryPendingServerLockContext(cid, tid, State.WAITER, timeout, helper);
    if (timeout > 0) {
      LockTimerContext ltc = new LockTimerContext(lockID, tid, cid, helper);
      TimerTask task = helper.getLockTimer().scheduleTimer(helper.getTimerCallback(), timeout, ltc);
      waiter.setTimerTask(task);
    }
    addWaiter(waiter, helper);
  }

  private void validateWaitNotifyState(ClientID cid, ThreadID tid, ServerLockContext holder, LockHelper helper) {
    if (holder == null) {
      throw new TCIllegalMonitorStateException("No holder present for when trying to wait/notify " + cid + "," + tid
                                               + " for lock = " + toString());
    } else if (holder.getState() != State.HOLDER_WRITE && holder.getState() != State.GREEDY_HOLDER_WRITE) {
      String message = "Holder not in correct state while wait/notify " + lockID + " " + holder;
      throw new TCIllegalMonitorStateException(message);
    }
  }

  protected void queue(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout, LockHelper helper) {
    switch (type) {
      case TRY_PENDING:
        WaitServerLockContext waitContext = createTryPendingServerLockContext(cid, tid, level, timeout, helper);
        if (timeout > 0) {
          LockTimerContext ltc = new LockTimerContext(lockID, tid, cid, helper);
          TimerTask task;
          try {
            task = helper.getLockTimer().scheduleTimer(helper.getTimerCallback(), timeout, ltc);
          } catch (IllegalArgumentException e) {
            if (timeout + System.currentTimeMillis() < 0) {
              queue(cid, tid, level, Type.PENDING, -1, helper);
              return;
            }
            throw e;
          }
          waitContext.setTimerTask(task);
        }
        addTryPending(waitContext, helper);
        break;
      case PENDING:
        ServerLockContext pendingContext = createPendingContext(cid, tid, level, helper);
        addPending(pendingContext, helper);
        break;
      //$CASES-OMITTED$
      default:
        throw new IllegalStateException("Only pending and try pending state should be passed = " + type);
    }
  }

  protected void requestLock(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout,
                             LockHelper helper) {
    queue(cid, tid, level, type, timeout, helper);
    processPendingRequests(helper);
  }

  protected int validateAndGetNumberOfPending(ClientID cid, ThreadID tid, ServerLockLevel reqLevel) {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    int noOfPendingRequests = 0;
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
          if (isUpgradeRequest(cid, tid, reqLevel, context)) { throw new TCLockUpgradeNotSupportedError(
                                                                                                        "Lock upgrade is not supported."
                                                                                                            + context
                                                                                                            + " lock = "
                                                                                                            + lockID); }
          if (isAlreadyHeldBySameContext(cid, tid, reqLevel, context)) { throw new AssertionError(
                                                                                                  "Client requesting already held lock!"
                                                                                                      + context
                                                                                                      + " lock = "
                                                                                                      + lockID); }
          break;
        case PENDING:
        case TRY_PENDING:
          noOfPendingRequests++;
          break;
        case WAITER:
          if (context.getClientID().equals(cid) && context.getThreadID().equals(tid)) { throw new AssertionError(
                                                                                                                 "This thread is already in wait state for "
                                                                                                                     + lockID); }
          break;
        default:
      }
    }
    return noOfPendingRequests;
  }

  protected void clearContextsForClient(ClientID cid, LockHelper helper) {
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();

    // clear contexts and cancel timer tasks
    while (iter.hasNext()) {
      ServerLockContext context = iter.next();
      if (context.getClientID().equals(cid)) {
        iter.remove();
        switch (context.getState().getType()) {
          case WAITER:
          case TRY_PENDING:
            WaitServerLockContext waitContext = (WaitServerLockContext) context;
            if (waitContext.getTimerTask() != null) {
              waitContext.getTimerTask().cancel();
            }
            break;
          //$CASES-OMITTED$
          default:
        }
      }
    }
  }

  private boolean isUpgradeRequest(ClientID cid, ThreadID tid, ServerLockLevel reqLevel, ServerLockContext holder) {
    if (reqLevel == ServerLockLevel.WRITE && this.isRead() && holder.getClientID().equals(cid)
        && holder.getThreadID().equals(tid)) { return true; }
    return false;
  }

  protected ServerLockContext getNotifyHolder(ClientID cid, ThreadID tid) {
    return get(cid, tid);
  }

  private boolean isAlreadyHeldBySameContext(ClientID cid, ThreadID tid, ServerLockLevel reqLevel,
                                             ServerLockContext context) {
    if (reqLevel == context.getState().getLockLevel() && context.getClientID().equals(cid)
        && context.getThreadID().equals(tid)) { return true; }
    return false;
  }

  protected void moveWaiterToPending(ServerLockContext waiter, LockHelper helper) {
    cancelTryLockOrWaitTimer(waiter, helper);
    // Add a pending request
    queue(waiter.getClientID(), waiter.getThreadID(), waiter.getState().getLockLevel(), Type.PENDING, -1, helper);
  }

  protected abstract void processPendingRequests(LockHelper helper);

  protected void awardLock(LockHelper helper, ServerLockContext request) {
    awardLock(helper, request, true);
  }

  protected void awardLock(LockHelper helper, ServerLockContext request, boolean toRespond) {
    State state = null;
    ServerLockLevel lockLevel = request.getState().getLockLevel();
    switch (lockLevel) {
      case READ:
        state = State.HOLDER_READ;
        break;
      case WRITE:
        state = State.HOLDER_WRITE;
        break;
      default:
        throw new AssertionError(lockLevel);
    }
    awardLock(helper, request, state, toRespond);
  }

  /**
   * Assumption that this context has already been removed from the list
   */
  protected void awardLock(LockHelper helper, ServerLockContext request, State state, boolean toRespond) {
    // add this request to the front of the list
    cancelTryLockOrWaitTimer(request, helper);
    request = changeStateToHolder(request, state, helper);
    addHolder(request, helper);

    if (toRespond) {
      // create a lock response context and add it to the sink
      LockResponseContext lrc = LockResponseContextFactory.createLockAwardResponseContext(lockID,
                                                                                          request.getClientID(),
                                                                                          request.getThreadID(),
                                                                                          request.getState()
                                                                                              .getLockLevel());
      helper.getLockSink().addMultiThreaded(lrc);
    }
  }

  protected void refuseTryRequestWithNoTimeout(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    cannotAward(cid, tid, level, helper);
  }

  protected void cannotAward(ClientID cid, ThreadID tid, ServerLockLevel requestedLockLevel, LockHelper helper) {
    LockResponseContext lrc = LockResponseContextFactory.createLockRejectedResponseContext(this.lockID, cid, tid,
                                                                                           requestedLockLevel);
    helper.getLockSink().addMultiThreaded(lrc);
  }

  protected void add(ServerLockContext request, LockHelper helper) {
    switch (request.getState().getType()) {
      case HOLDER:
      case GREEDY_HOLDER:
        addHolder(request, helper);
        break;
      case WAITER:
        addWaiter(request, helper);
        break;
      case PENDING:
        addPending(request, helper);
        break;
      case TRY_PENDING:
        addTryPending(request, helper);
        break;
      default:
        throw new AssertionError(request.getState().getType());
    }
  }

  protected void addHolder(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));
    this.addFirst(request);
  }

  protected void addTryPending(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    // I'm not sure this is a sensible thing to assert - you are racing with the client on
    // try lock requests...
    // Assert.assertFalse(checkDuplicate(request));

    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      Type type = iter.next().getState().getType();
      switch (type) {
        case GREEDY_HOLDER:
        case HOLDER:
        case PENDING:
        case TRY_PENDING:
          break;
        case WAITER:
          iter.addPrevious(request);
          return;
        default:
          throw new AssertionError(type);
      }
    }

    this.addLast(request);
  }

  protected void addPending(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    if (checkDuplicate(request)) {
      logger.debug("Ignoring existing Request " + request + " in Lock " + lockID);
      return;
    }

    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      Type type = iter.next().getState().getType();
      switch (type) {
        case GREEDY_HOLDER:
        case HOLDER:
        case PENDING:
        case TRY_PENDING:
          break;
        case WAITER:
          iter.addPrevious(request);
          return;
        default:
          throw new AssertionError(type);
      }
    }

    this.addLast(request);
  }

  protected void addWaiter(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    // This has been commented out because there is a possibility in restarts that this can happen.
    // consider steps:
    // 1) ClientLock trying to do remote wait and get inside synchronization
    // 2) Then that thread doesn't somehow doesn't get CPU cycles
    // 3) Server crashes and transport is established again
    // 4) Then remote wait gets called and wait request gets queued in L2 Lock Manager
    // 5) Client handshake also sends this to "Wait" context to the server
    // Hence duplicate.
    // Assert.assertFalse(checkDuplicate(request));
    if (checkDuplicate(request)) {
      logger.info("Ignoring adding of waiter for same context " + request);
      return;
    }

    this.addLast(request);
  }

  /**
   * This list that is being maintained for contexts can have SingleServerLockContext, LinkedServerLockContext,
   * WaitSingleServerLockContext and WaitLinkedServerLockContext. In case the size of the queue is 1, a
   * SingleServerLockContext is present in the list. This has been done to save space. In order to continue adding more
   * elements to the list, a change to LinkedServerLockContext is required.]
   * 
   * @param helper
   */
  protected void preStepsForAdd(LockHelper helper) {
    if (isEmpty() || (!isEmpty() && (getFirst().getNext() != null || getFirst() instanceof LinkedServerLockContext))) { return; }

    // Since there is only 1 element in the list, a change is required.
    SingleServerLockContext context = (SingleServerLockContext) removeFirst();
    LinkedServerLockContext newContext = null;
    Type type = context.getState().getType();
    switch (type) {
      case GREEDY_HOLDER:
      case HOLDER:
      case PENDING:
        newContext = new LinkedServerLockContext(context.getClientID(), context.getThreadID());
        newContext.setState(helper.getContextStateMachine(), context.getState());
        break;
      case TRY_PENDING:
      case WAITER:
        throw new AssertionError("waiters/try pending are all linked server contexts");
      default:
        throw new AssertionError(type);
    }
    this.addFirst(newContext);
  }

  protected boolean checkDuplicate(ClientID cid, ThreadID tid) {
    return checkDuplicate(new SingleServerLockContext(cid, tid));
  }

  protected boolean checkDuplicate(ServerLockContext context) {
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      ServerLockContext temp = iter.next();
      if (context.equals(temp)) { return true; }
    }
    return false;
  }

  protected boolean canAwardRequest(ServerLockLevel requestLevel) {
    switch (requestLevel) {
      case READ:
        if (!hasHolders() || isRead()) { return true; }
        return false;
      case WRITE:
        return !hasHolders();
      default:
        throw new AssertionError(requestLevel);
    }
  }

  protected ServerLockContext getNextRequestIfCanAward(LockHelper helper) {
    // Fetch the next pending context
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      ServerLockContext request = iter.next();
      switch (request.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (canAwardRequest(request.getState().getLockLevel())) {
            iter.remove();
            return request;
          }
          return null;
        case WAITER:
          return null;
        //$CASES-OMITTED$
        default:
      }
    }
    return null;
  }

  protected void cancelTryLockOrWaitTimer(ServerLockContext request, LockHelper helper) {
    if (request.isTryPending() || request.isWaiter()) {
      WaitServerLockContext waitRequest = (WaitServerLockContext) request;
      if (waitRequest.getTimerTask() != null) {
        waitRequest.getTimerTask().cancel();
      }
    }
  }

  // Contexts methods
  protected ServerLockContext changeStateToHolder(ServerLockContext request, State state, LockHelper helper) {
    request = changeFromWaitContextIfRequired(request, helper);
    request.setState(helper.getContextStateMachine(), state);
    Assert.assertTrue(request.isHolder());
    return request;
  }

  private ServerLockContext changeFromWaitContextIfRequired(ServerLockContext request, LockHelper helper) {
    switch (request.getState().getType()) {
      case WAITER:
      case TRY_PENDING:
        request = createSingleOrLinkedServerLockContext(request.getClientID(), request.getThreadID(), request
            .getState(), helper);
        break;
      //$CASES-OMITTED$
      default:
    }
    return request;
  }

  protected WaitServerLockContext createTryPendingServerLockContext(ClientID cid, ThreadID tid, ServerLockLevel level,
                                                                    long timeout, LockHelper helper) {
    State state = null;
    switch (level) {
      case READ:
        state = State.TRY_PENDING_READ;
        break;
      case WRITE:
        state = State.TRY_PENDING_WRITE;
        break;
      default:
        throw new AssertionError(level);
    }

    return createWaitOrTryPendingServerLockContext(cid, tid, state, timeout, helper);
  }

  protected ServerLockContext createPendingContext(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    State state = null;
    switch (level) {
      case READ:
        state = State.PENDING_READ;
        break;
      case WRITE:
        state = State.PENDING_WRITE;
        break;
      default:
        throw new AssertionError(level);
    }
    return createSingleOrLinkedServerLockContext(cid, tid, state, helper);
  }

  private ServerLockContext createSingleOrLinkedServerLockContext(ClientID cid, ThreadID tid, State state,
                                                                  LockHelper helper) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new SingleServerLockContext(cid, tid);
    } else {
      context = new LinkedServerLockContext(cid, tid);
    }
    context.setState(helper.getContextStateMachine(), state);
    return context;
  }

  protected WaitServerLockContext createWaitOrTryPendingServerLockContext(ClientID cid, ThreadID tid, State state,
                                                                          long timeout, LockHelper helper) {
    WaitServerLockContext context = new WaitServerLockContext(cid, tid, timeout);
    context.setState(helper.getContextStateMachine(), state);
    return context;
  }

  // Helper methods
  protected boolean hasHolders() {
    if (!isEmpty() && getFirst().isHolder()) { return true; }
    return false;
  }

  protected boolean hasOnlyReadHolders() {
    return isRead();
  }

  protected boolean hasWaiters() {
    if (!isEmpty() && getLast().isWaiter()) { return true; }
    return false;
  }

  protected boolean hasPendingRequests() {
    for (ServerLockContext context : this) {
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          return true;
        case WAITER:
          return false;
        //$CASES-OMITTED$
        default:
      }
    }
    return false;
  }

  protected boolean hasPendingWrites() {
    for (ServerLockContext context : this) {
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (context.getState().getLockLevel() == ServerLockLevel.WRITE) { return true; }
          break;
        case WAITER:
          return false;
        //$CASES-OMITTED$
        default:
      }
    }
    return false;
  }

  protected List<ServerLockContext> removeAllPendingReadRequests(LockHelper helper) {
    List<ServerLockContext> requests = new ArrayList<>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (context.getState().getLockLevel() == ServerLockLevel.READ) {
            iterator.remove();
            requests.add(context);
          }
          break;
        case WAITER:
          return requests;
        //$CASES-OMITTED$
        default:
      }
    }
    return requests;
  }

  protected boolean hasPendingRequestsFromOtherClients(ClientID cid) {
    for (ServerLockContext context : this) {
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (!context.getClientID().equals(cid)) { return true; }
          break;
        case WAITER:
          return false;
        //$CASES-OMITTED$
        default:
      }
    }
    return false;
  }

  protected int getNoOfPendingRequests() {
    int count = 0;
    for (ServerLockContext context : this) {
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          count++;
          break;
        case WAITER:
          return count;
        //$CASES-OMITTED$
        default:
      }
    }
    return count;
  }

  protected ServerLockLevel holderLevel() {
    if (!hasHolders()) { return null; }
    ServerLockContext holder = getFirst();
    return holder.getState().getLockLevel();
  }

  protected boolean isRead() {
    if (holderLevel() == ServerLockLevel.READ) { return true; }
    return false;
  }

  protected boolean isWrite() {
    if (holderLevel() == ServerLockLevel.WRITE) { return true; }
    return false;
  }

  protected ServerLockContext remove(ClientID cid, ThreadID tid, EnumSet<Type> set) {
    ServerLockContext temp = null;
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      temp = iter.next();
      if (temp.getClientID().equals(cid) && temp.getThreadID().equals(tid) && set.contains(temp.getState().getType())) {
        iter.remove();
        return temp;
      }
    }
    return null;
  }

  protected ServerLockContext get(ClientID cid, ThreadID tid) {
    for (ServerLockContext temp : this) {
      if (temp.getClientID().equals(cid) && temp.getThreadID().equals(tid)) { return temp; }
    }
    return null;
  }

  private List<ServerLockContext> removeWaiters(NotifyAction action) {
    List<ServerLockContext> contexts = new ArrayList<>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case WAITER:
          iterator.remove();
          contexts.add(context);
          if (action == NotifyAction.ONE) { return contexts; }
          break;
        //$CASES-OMITTED$
        default:
      }
    }
    return contexts;
  }

  @Override
  public void addStateTo(final StateDumpCollector stateDumpCollector) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode componentState = mapper.createObjectNode();
      componentState.put("Lock Info", String.valueOf(lockID));

      ArrayNode contextNodes = mapper.createArrayNode();
      SinglyLinkedListIterator<ServerLockContext> iter = iterator();
      while (iter.hasNext()) {
        contextNodes.add(iter.next().toString());
      }
      componentState.set("Contexts", contextNodes);
      stateDumpCollector.addState(StateDumpCollector.JSON_STATE_KEY, mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(componentState));
    } catch (Exception e) {
      stateDumpCollector.addState("exception", e.getLocalizedMessage());
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Lock Info");
    builder.append("\n");
    builder.append(lockID);
    builder.append("\n");
    builder.append("Contexts [ ");
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      builder.append(iter.next().toString());
      if (iter.hasNext()) {
        builder.append(" , ");
      }
    }
    builder.append(" ]");
    return builder.toString();
  }
}
