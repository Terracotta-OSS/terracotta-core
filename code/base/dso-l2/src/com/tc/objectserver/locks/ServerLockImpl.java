/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.objectserver.locks.context.LinkedServerLockContext;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public final class ServerLockImpl extends AbstractServerLock {
  private final static EnumSet<Type> SET_OF_GREEDY_HOLDERS = EnumSet.of(Type.GREEDY_HOLDER);

  private boolean                    isRecalled            = false;

  public ServerLockImpl(LockID lockID) {
    super(lockID);
  }

  @Override
  protected void requestLock(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout,
                             LockHelper helper) {
    ServerLockContext greedyHold = getGreedyHolder(cid);
    if (canAwardGreedilyOnTheClient(level, greedyHold)) {
      // Ignore if a client can fulfill request because it can
    } else if (isRecalled && type.equals(Type.TRY_PENDING) && timeout <= 0) {
      cannotAward(cid, tid, level, helper);
    } else {
      super.requestLock(cid, tid, level, type, timeout, helper);
    }
  }

  @Override
  protected void queue(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout, LockHelper helper) {
    if (!canAwardRequest(level) && hasGreedyHolders()) {
      recall(level, helper);
    }
    super.queue(cid, tid, level, type, timeout, helper);
  }

  @Override
  public boolean clearStateForNode(ClientID cid, LockHelper helper) {
    clearContextsForClient(cid, helper);

    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
    processPendingRequests(helper);
    return isEmpty();
  }

  @Override
  protected void reestablishLock(ClientServerExchangeLockContext cselc, LockHelper helper) {
    // if greedy request then award greedily and don't respond
    if (cselc.getThreadID().equals(ThreadID.VM_ID)) {
      awardLockGreedily(helper, createPendingContext((ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc
          .getState().getLockLevel(), helper), false);
    } else {
      super.reestablishLock(cselc, helper);
    }
  }

  /**
   * This method is responsible for processing pending requests. Awarding Write logic: If there are waiters present then
   * we do not grant a greedy lock to avoid starving waiters on other clients. This is because if a notify is called on
   * the client having greedy lock, then the local waiter will get notified and remote waiters will get starved.
   * 
   * @param helper
   */
  @Override
  protected void processPendingRequests(LockHelper helper) {
    if (isRecalled) { return; }

    ServerLockContext request = getNextRequestIfCanAward(helper);
    if (request == null) { return; }

    switch (request.getState().getLockLevel()) {
      case READ:
        add(request, helper);
        awardAllReadsGreedily(helper, request);
        break;
      case WRITE:
        if (hasWaiters()) {
          awardLock(helper, request);
        } else {
          awardLockGreedily(helper, request);
          // recall if it has pending requests from other clients
          if (hasPendingRequestsFromOtherClients(request.getClientID())) {
            if (hasPendingWrites()) {
              recall(ServerLockLevel.WRITE, helper);
            } else {
              recall(ServerLockLevel.READ, helper);
            }
          }
        }
        break;
    }
  }

  @Override
  protected void addHolder(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));

    switch (request.getState().getType()) {
      case GREEDY_HOLDER:
        this.addFirst(request);
        return;
      case HOLDER:
        SinglyLinkedListIterator<ServerLockContext> iter = iterator();
        while (iter.hasNext()) {
          switch (iter.next().getState().getType()) {
            case GREEDY_HOLDER:
              break;
            default:
              iter.addPrevious(request);
              return;
          }
        }

        this.addLast(request);
        break;
      default:
        throw new IllegalStateException("Only holders context should be passed " + request.getState());
    }
  }

  private void awardAllReadsGreedily(LockHelper helper, ServerLockContext request) {
    // fetch all the read requests and check if has write pending requests as well
    List<ServerLockContext> contexts = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    boolean hasPendingWrite = false;
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      if (context.isPending()) {
        switch (context.getState().getLockLevel()) {
          case READ:
            iterator.remove();
            contexts.add(context);
            break;
          case WRITE:
            hasPendingWrite = true;
            break;
        }
      }
    }

    ArrayList<ClientID> listOfClients = new ArrayList<ClientID>();
    for (ServerLockContext context : contexts) {
      if (!listOfClients.contains(context.getClientID())) {
        awardLockGreedily(helper, context);
        listOfClients.add(context.getClientID());
      }
    }

    if (hasPendingWrite) {
      recall(ServerLockLevel.WRITE, helper);
    }
  }

  private static boolean canAwardGreedilyOnTheClient(ServerLockLevel level, ServerLockContext holder) {
    return holder != null
           && (holder.getState().getLockLevel() == ServerLockLevel.WRITE || level == ServerLockLevel.READ);
  }

  /**
   * This method is called when a client gives up a greedy lock. A point to note is that we ignore wait contexts if
   * already present. The reason being that a greedy read lock is even when waiters are present, hence a duplicate wait
   * context can be present on the server.
   */
  @Override
  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    ServerLockContext greedyHolder = remove(cid, ThreadID.VM_ID, SET_OF_GREEDY_HOLDERS);

    if (greedyHolder == null) { throw new AssertionError("No Greedy Holder Exists For " + cid + " on " + lockID
                                                         + " Lock State: " + this.toString()); }

    recordLockReleaseStat(cid, ThreadID.VM_ID, helper);

    boolean hasGreedyReadHolder = false;

    for (ClientServerExchangeLockContext cselc : serverLockContexts) {
      switch (cselc.getState().getType()) {
        case GREEDY_HOLDER:
          // This is the case when a client holds a greedy write and a read recall is made
          // In such a case we would add this client as a GREEDY READ, process pending requests
          // and award all the read requests the lock they need.
          // However we need to make sure that the client side only sends GREEDY HOLDER (READ)
          // in its recallCommit message when it holds a greedy write lock.
          // Also a recallCommit message containing a GREEDY HOLDER (READ) cannot have any
          // other holders, pending or try pending contexts.
          // See ClientLockImpl for more details
          Assert.assertEquals(State.GREEDY_HOLDER_READ, cselc.getState());
          hasGreedyReadHolder = true;
          LinkedServerLockContext request = new LinkedServerLockContext(cid, ThreadID.VM_ID);
          request.setState(helper.getContextStateMachine(), State.PENDING_READ);
          awardLockGreedily(helper, request, false);
          processPendingRequests(helper);
          break;
        case HOLDER:
          Assert.assertFalse(hasGreedyReadHolder);
          awardLock(helper, createPendingContext(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), helper),
                    false);
          break;
        case PENDING:
          Assert.assertFalse(hasGreedyReadHolder);
          queue(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), Type.PENDING, -1, helper);
          break;
        case TRY_PENDING:
          Assert.assertFalse(hasGreedyReadHolder);
          if (cselc.timeout() <= 0) {
            cannotAward(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), helper);
          } else {
            queue(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), Type.TRY_PENDING, cselc.timeout(), helper);
          }
          break;
        case WAITER:
          ServerLockContext context = get((ClientID) cselc.getNodeID(), cselc.getThreadID());
          if (context != null) {
            Assert.assertTrue(context.isWaiter() || context.isPending());
          } else {
            ServerLockContext waiter = createWaiterAndScheduleTask(cselc, helper);
            addWaiter(waiter, helper);
          }
          break;
      }
    }

    if (hasGreedyHolders() && !isRecalled && hasPendingRequests()) {
      if (hasPendingWrites()) {
        recall(ServerLockLevel.WRITE, helper);
      } else {
        recall(ServerLockLevel.READ, helper);
      }
    }

    // Also check if the lock can be removed
    if (clearLockIfRequired(helper)) { return; }
    processPendingRequests(helper);
  }

  private void recall(ServerLockLevel level, LockHelper helper) {
    if (isRecalled) { return; }

    List<ServerLockContext> greedyHolders = getGreedyHolders();
    for (ServerLockContext greedyHolder : greedyHolders) {
      LockResponseContext lrc = LockResponseContextFactory.createLockRecallResponseContext(lockID, greedyHolder
          .getClientID(), greedyHolder.getThreadID(), level);
      helper.getLockSink().add(lrc);
      isRecalled = true;
    }

    recordLockHop(helper);
  }

  private void awardLockGreedily(LockHelper helper, ServerLockContext request) {
    awardLockGreedily(helper, request, true);
  }

  private void awardLockGreedily(LockHelper helper, ServerLockContext request, boolean toRespond) {
    State state = null;
    switch (request.getState().getLockLevel()) {
      case READ:
        state = State.GREEDY_HOLDER_READ;
        break;
      case WRITE:
        state = State.GREEDY_HOLDER_WRITE;
        break;
    }
    // remove holders (from the same client) who have given the lock non greedily till now
    removeNonGreedyHoldersAndPendingOfSameClient(request, helper);
    awardLock(helper, request, state, toRespond);
  }

  @Override
  protected void refuseTryRequestWithNoTimeout(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    ServerLockContext holder = getGreedyHolder(cid);
    if (hasGreedyHolders() && holder == null) {
      recall(level, helper);
    }
    if (!canAwardGreedilyOnTheClient(level, holder)) {
      cannotAward(cid, tid, level, helper);
    }
  }

  @Override
  protected ServerLockContext getNotifyHolder(ClientID cid, ThreadID tid) {
    ServerLockContext context = get(cid, tid);
    if (context == null) {
      context = get(cid, ThreadID.VM_ID);
    }
    return context;
  }

  @Override
  protected ServerLockContext remove(ClientID cid, ThreadID tid, EnumSet<Type> set) {
    ServerLockContext temp = super.remove(cid, tid, set);
    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
    return temp;
  }

  @Override
  protected ServerLockContext changeStateToHolder(ServerLockContext request, State state, LockHelper helper) {
    request = super.changeStateToHolder(request, state, helper);
    if (request.getState().getType() == Type.GREEDY_HOLDER) {
      request.setThreadID(ThreadID.VM_ID);
    }
    return request;
  }

  private boolean hasGreedyHolders() {
    if (!isEmpty() && getFirst().isGreedyHolder()) return true;
    return false;
  }

  private List<ServerLockContext> getGreedyHolders() {
    List<ServerLockContext> contexts = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
          contexts.add(context);
          break;
        default:
          return contexts;
      }
    }
    return contexts;
  }

  private ServerLockContext getGreedyHolder(ClientID cid) {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
          // can award greedily
          if (context.getClientID().equals(cid)) { return context; }
          break;
        default:
          return null;
      }
    }
    return null;
  }

  private void removeNonGreedyHoldersAndPendingOfSameClient(ServerLockContext context, LockHelper helper) {
    ClientID cid = context.getClientID();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext next = iterator.next();
      switch (next.getState().getType()) {
        case GREEDY_HOLDER:
          break;
        case TRY_PENDING:
          if (cid.equals(next.getClientID())) {
            cancelTryLockOrWaitTimer(next, helper);
            iterator.remove();
          }
          break;
        case PENDING:
        case HOLDER:
          if (cid.equals(next.getClientID())) {
            iterator.remove();
          }
          break;
        case WAITER:
          return;
      }
    }
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out = super.prettyPrint(out);
    out.print("isRecalled=" + isRecalled).flush();
    return out;
  }

  @Override
  public String toString() {
    String rv = super.toString();
    rv = rv + "\n" + "isRecalled=" + isRecalled;
    return rv;
  }
}
