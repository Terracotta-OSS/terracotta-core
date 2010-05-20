/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.cache.CachedItemStore;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.GetSizeServerMapRequestMessage;
import com.tc.object.msg.GetValueServerMapRequestMessage;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RemoteServerMapManagerImpl implements RemoteServerMapManager {

  // TODO::Make its own property
  private static final int                                               MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(
                                                                                                                               TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY);
  private static final long                                              BATCH_LOOKUP_TIME_PERIOD                  = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(
                                                                                                                               TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD);

  private final GroupID                                                  groupID;
  private final ServerMapMessageFactory                                  smmFactory;
  private final TCLogger                                                 logger;
  private final SessionManager                                           sessionManager;
  private final Map<ServerMapRequestID, AbstractServerMapRequestContext> outstandingRequests                       = new HashMap<ServerMapRequestID, AbstractServerMapRequestContext>();

  private final Timer                                                    requestTimer                              = new Timer(
                                                                                                                               "RemoteServerMapManager Request Scheduler",
                                                                                                                               true);

  private State                                                          state                                     = State.RUNNING;
  private long                                                           requestIDCounter                          = 0;
  private boolean                                                        pendingSendTaskScheduled                  = false;

  private final CachedItemStore                                          cachedItems                               = new CachedItemStore(
                                                                                                                                         16384,
                                                                                                                                         0.75f,
                                                                                                                                         1024);

  private static enum State {
    PAUSED, RUNNING, STARTING, STOPPED
  }

  public RemoteServerMapManagerImpl(final GroupID groupID, final TCLogger logger,
                                    final ServerMapMessageFactory smmFactory, final SessionManager sessionManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.smmFactory = smmFactory;
    this.sessionManager = sessionManager;
  }

  public synchronized Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    assertSameGroupID(oid);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createLookupValueRequestContext(oid, portableKey);
    context.makeLookupRequest();
    sendRequest(context);
    return waitForResult(context);
  }

  private void assertSameGroupID(final ObjectID oid) {
    if (oid.getGroupID() != this.groupID.toInt()) { throw new AssertionError(
                                                                             "Looking up in the wrong Remote Manager : "
                                                                                 + this.groupID + " id : " + oid); }
  }

  public synchronized int getSize(final ObjectID mapId) {
    assertSameGroupID(mapId);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createGetSizeRequestContext(mapId);
    context.makeLookupRequest();
    sendRequestNow(context); // Get size requests are not batched for now
    return (Integer) waitForResult(context);
  }

  private Object waitForResult(final AbstractServerMapRequestContext context) {
    boolean isInterrupted = false;
    Object result;
    while (true) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
      result = context.getResult();
      if (result != null) {
        removeRequestContext(context);
        break;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return result;
  }

  private void sendRequest(final AbstractServerMapRequestContext context) {
    final int size = this.outstandingRequests.size();
    if (size % 5000 == 4999) {
      this.logger.warn("Too many pending requests in the system : objectLookup states size : " + size);
    }
    if (size <= MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY) {
      sendRequestNow(context);
    } else {
      scheduleRequestForLater(context);
    }

  }

  private void scheduleRequestForLater(final AbstractServerMapRequestContext context) {
    context.makePending();
    if (!this.pendingSendTaskScheduled) {
      this.requestTimer.schedule(new SendPendingRequestsTimer(), BATCH_LOOKUP_TIME_PERIOD);
      this.pendingSendTaskScheduled = true;
    }
  }

  private class SendPendingRequestsTimer extends TimerTask {
    @Override
    public void run() {
      sendPendingRequests();
    }
  }

  /**
   * Only GET_VALUE_FOR_KEY requests are batched, its a little ugly to assume that here. Needs some refactoring.
   */
  public synchronized void sendPendingRequests() {
    waitUntilRunning();
    this.pendingSendTaskScheduled = false;
    final ServerMapRequestMessage msg = this.smmFactory
        .newServerTCMapRequestMessage(this.groupID, ServerMapRequestType.GET_VALUE_FOR_KEY);
    initializeMessageWithPendingRequests(msg);
    if (msg.getRequestCount() != 0) {
      msg.send();
    }
  }

  private void initializeMessageWithPendingRequests(final ServerMapRequestMessage msg) {
    for (final AbstractServerMapRequestContext context : this.outstandingRequests.values()) {
      if (context.isPending()) {
        if (context.getRequestType() == ServerMapRequestType.GET_SIZE) {
          // Only GET_VALUE_FOR_KEY Requests are batched here
          throw new AssertionError("Get Size requests are not batched so it should never be pending : " + context);
        }
        context.makeUnPending();
        context.initializeMessage(msg);
      }
    }
  }

  private void sendRequestNow(final AbstractServerMapRequestContext context) {
    final ServerMapRequestMessage msg = this.smmFactory.newServerTCMapRequestMessage(this.groupID, context
        .getRequestType());
    context.initializeMessage(msg);
    msg.send();
  }

  private AbstractServerMapRequestContext createGetSizeRequestContext(final ObjectID mapId) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetSizeServerMapRequestContext context = new GetSizeServerMapRequestContext(requestID, mapId, this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  synchronized void requestOutstanding() {
    for (final AbstractServerMapRequestContext context : this.outstandingRequests.values()) {
      sendRequestNow(context);
    }
  }

  private void removeRequestContext(final AbstractServerMapRequestContext context) {
    final Object old = this.outstandingRequests.remove(context.getRequestID());
    if (old != context) { throw new AssertionError("Removed wrong context. context = " + context + " old = " + old); }
  }

  private AbstractServerMapRequestContext createLookupValueRequestContext(final ObjectID oid, final Object portableKey) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetValueServerMapRequestContext context = new GetValueServerMapRequestContext(requestID, oid, portableKey,
                                                                                        this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  private AbstractServerMapRequestContext getRequestContext(final ServerMapRequestID requestID) {
    return this.outstandingRequests.get(requestID);
  }

  public synchronized void addResponseForKeyValueMapping(final SessionID sessionID, final ObjectID mapID,
                                                         final Collection<ServerMapGetValueResponse> responses,
                                                         final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring response for ServerMap :  " + mapID + " ,  responses :" + responses.size()
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    for (final ServerMapGetValueResponse r : responses) {
      setResultForRequest(sessionID, mapID, r.getRequestID(), r.getValue(), nodeID);
    }
    notifyAll();
  }

  public synchronized void addResponseForGetSize(final SessionID sessionID, final ObjectID mapID,
                                                 final ServerMapRequestID requestID, final Integer size,
                                                 final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring response for ServerMap :  " + mapID + " , " + requestID + " , size : " + size
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    setResultForRequest(sessionID, mapID, requestID, size, nodeID);
    notifyAll();
  }

  private void setResultForRequest(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                   final Object result, final NodeID nodeID) {
    final AbstractServerMapRequestContext context = getRequestContext(requestID);
    if (context != null) {
      context.setResult(mapID, result);
    } else {
      this.logger.warn("Server Map Request Context is null for " + mapID + " request ID : " + requestID + " result : "
                       + result);
    }
  }

  public void addCachedItemForLock(final LockID lockID, final CachedItem item) {
    this.cachedItems.add(lockID, item);
  }

  public void removeCachedItemForLock(final LockID lockID, final CachedItem item) {
    this.cachedItems.remove(lockID, item);
  }

  public void flush(final LockID lockID) {
    this.cachedItems.flush(lockID);
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (this.state != State.RUNNING) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = State.PAUSED;
    notifyAll();
  }

  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    if (isStopped()) { return; }
    assertPaused("Attempt to init handshake while not PAUSED");
    this.state = State.STARTING;
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotRunning("Attempt to unpause while not PAUSED");
    this.state = State.RUNNING;
    requestOutstanding();
    notifyAll();
  }

  public synchronized void shutdown() {
    this.state = State.STOPPED;
  }

  private boolean isStopped() {
    return this.state == State.STOPPED;
  }

  private void assertPaused(final String message) {
    if (this.state != State.PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotPaused(final String message) {
    if (this.state == State.PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotRunning(final String message) {
    if (this.state == State.RUNNING) { throw new AssertionError(message + ": " + this.state); }
  }

  private ServerMapRequestID getNextRequestID() {
    return new ServerMapRequestID(this.requestIDCounter++);
  }

  private static abstract class AbstractServerMapRequestContext extends LookupStateTransitionAdaptor {

    // protected final static TCLogger logger = TCLogging.getLogger(AbstractServerMapRequestContext.class);

    protected final ObjectID             oid;
    protected final GroupID              groupID;
    protected final ServerMapRequestID   requestID;
    protected final ServerMapRequestType requestType;
    protected Object                     result;

    public AbstractServerMapRequestContext(final ServerMapRequestType requestType, final ServerMapRequestID requestID,
                                           final ObjectID mapID, final GroupID groupID) {

      this.requestType = requestType;
      this.requestID = requestID;
      this.oid = mapID;
      this.groupID = groupID;
    }

    public ServerMapRequestID getRequestID() {
      return this.requestID;
    }

    public ServerMapRequestType getRequestType() {
      return this.requestType;
    }

    public void setResult(final ObjectID mapID, final Object result) {
      if (!this.oid.equals(mapID)) { throw new AssertionError("Wrong request to response : this map id : " + this.oid
                                                              + " response is for : " + mapID + " type : "
                                                              + getRequestType()); }
      this.result = result;
    }

    public Object getResult() {
      return this.result;
    }

    @Override
    public int hashCode() {
      return this.requestID.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) { return true; }
      if (!(o instanceof AbstractServerMapRequestContext)) { return false; }
      final AbstractServerMapRequestContext other = (AbstractServerMapRequestContext) o;
      return (this.requestID.equals(other.requestID) && this.requestType.equals(other.requestType)
              && this.oid.equals(other.oid) && this.groupID.equals(other.groupID));
    }

    public abstract void initializeMessage(ServerMapRequestMessage requestMessage);

  }

  private class GetSizeServerMapRequestContext extends AbstractServerMapRequestContext {

    public GetSizeServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                          final GroupID groupID) {
      super(ServerMapRequestType.GET_SIZE, requestID, mapID, groupID);
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetSizeServerMapRequestMessage) requestMessage).initializeGetSizeRequest(this.requestID, this.oid);
    }

  }

  private class GetValueServerMapRequestContext extends AbstractServerMapRequestContext {

    private final Object portableKey;

    public GetValueServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                           final Object portableKey, final GroupID groupID) {
      super(ServerMapRequestType.GET_VALUE_FOR_KEY, requestID, mapID, groupID);
      this.portableKey = portableKey;
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetValueServerMapRequestMessage) requestMessage).addGetValueRequestTo(this.requestID, this.oid,
                                                                              this.portableKey);
    }

  }
}
