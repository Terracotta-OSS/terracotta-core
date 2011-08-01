/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.GetAllKeysServerMapRequestMessage;
import com.tc.object.msg.GetAllSizeServerMapRequestMessage;
import com.tc.object.msg.GetValueServerMapRequestMessage;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class RemoteServerMapManagerImpl implements RemoteServerMapManager {

  // TODO::Make its own property
  private static final int                                               MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(TCPropertiesConsts.L1_SERVERMAPMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY);
  private static final long                                              BATCH_LOOKUP_TIME_PERIOD                  = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(TCPropertiesConsts.L1_SERVERMAPMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD);

  private static final String                                            SIZE_KEY                                  = "SIZE_KEY";
  private static final String                                            ALL_KEYS                                  = "ALL-KEYS";

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

  // private final Sink ttiTTLEvitionSink;
  private final L1ServerMapLocalCacheManager                             globalLocalCacheManager;

  private static enum State {
    PAUSED, RUNNING, STARTING, STOPPED
  }

  public RemoteServerMapManagerImpl(final GroupID groupID, final TCLogger logger,
                                    final ServerMapMessageFactory smmFactory, final SessionManager sessionManager,
                                    final Sink ttiTTLEvitionSink, L1ServerMapLocalCacheManager globalLocalCacheManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.smmFactory = smmFactory;
    this.sessionManager = sessionManager;
    // this.ttiTTLEvitionSink = ttiTTLEvitionSink;
    this.globalLocalCacheManager = globalLocalCacheManager;
  }

  /**
   * TODO: Maybe change to getValue()
   */
  public synchronized Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    assertSameGroupID(oid);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createLookupValueRequestContext(oid,
                                                                                    Collections.singleton(portableKey));
    context.makeLookupRequest();
    sendRequest(context);
    Map<Object, Object> result = waitForResult(context);
    return result.get(portableKey);
  }

  public synchronized void getMappingForAllKeys(final Map<ObjectID, Set<Object>> mapIdToKeysMap, Map<Object, Object> rv) {
    Set<AbstractServerMapRequestContext> contextsToWaitFor = new HashSet<AbstractServerMapRequestContext>();
    for (Entry<ObjectID, Set<Object>> entry : mapIdToKeysMap.entrySet()) {
      ObjectID mapId = entry.getKey();
      Set<Object> keys = entry.getValue();
      assertSameGroupID(mapId);
      waitUntilRunning();
      final AbstractServerMapRequestContext context = createLookupValueRequestContext(mapId, keys);
      context.makeLookupRequest();
      contextsToWaitFor.add(context);
      sendRequest(context);
    }

    waitForResults(contextsToWaitFor, rv);
  }

  public synchronized Set getAllKeys(ObjectID mapID) {
    assertSameGroupID(mapID);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createGetAllKeysRequestContext(mapID);
    context.makeLookupRequest();
    sendRequestNow(context);
    Map<Object, Object> result = waitForResult(context);
    Assert.assertTrue(result.containsKey(ALL_KEYS));
    return (Set) result.get(ALL_KEYS);
  }

  private void assertSameGroupID(final ObjectID oid) {
    if (oid.getGroupID() != this.groupID.toInt()) { throw new AssertionError(
                                                                             "Looking up in the wrong Remote Manager : "
                                                                                 + this.groupID + " id : " + oid); }
  }

  public synchronized long getAllSize(final ObjectID[] mapIDs) {
    for (ObjectID mapId : mapIDs) {
      assertSameGroupID(mapId);
    }
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createGetAllSizeRequestContext(mapIDs);
    context.makeLookupRequest();
    sendRequestNow(context);
    Map<Object, Object> result = waitForResult(context);
    Assert.assertTrue(result.containsKey(SIZE_KEY));
    return (Long) result.get(SIZE_KEY);
  }

  private Map<Object, Object> waitForResult(final AbstractServerMapRequestContext context) {
    boolean isInterrupted = false;
    try {
      while (true) {
        try {
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
        if (context.isMissing()) {
          removeRequestContext(context);
          throw new TCObjectNotFoundException(context.getMapID().toString());
        }
        Map<Object, Object> result = context.getResult();
        if (result != null) {
          removeRequestContext(context);
          return result;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private void waitForResults(Set<AbstractServerMapRequestContext> contextsToWaitFor, Map<Object, Object> rv) {
    boolean isInterrupted = false;
    try {
      while (true) {
        try {
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
        for (Iterator<AbstractServerMapRequestContext> iterator = contextsToWaitFor.iterator(); iterator.hasNext();) {
          AbstractServerMapRequestContext context = iterator.next();
          if (context.isMissing()) {
            removeRequestContext(context);
            iterator.remove();
            throw new TCObjectNotFoundException(context.getMapID().toString());
          }
          Map<Object, Object> result = context.getResult();
          if (result != null) {
            removeRequestContext(context);
            iterator.remove();
            rv.putAll(result);
          }
        }
        if (contextsToWaitFor.isEmpty()) { return; }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
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
      try {
        sendPendingRequests();
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getClass().getName()
                    + " while trying to send pending requests. Cancelling timer task.");
        this.cancel();
      }
    }
  }

  /**
   * Only GET_VALUE_FOR_KEY requests are batched, its a little ugly to assume that here. Needs some refactoring.
   */
  public synchronized void sendPendingRequests() {
    waitUntilRunning();
    this.pendingSendTaskScheduled = false;
    final ServerMapRequestMessage msg = this.smmFactory
        .newServerMapRequestMessage(this.groupID, ServerMapRequestType.GET_VALUE_FOR_KEY);
    initializeMessageWithPendingRequests(msg);
    if (msg.getRequestCount() != 0) {
      msg.send();
    }
  }

  private void initializeMessageWithPendingRequests(final ServerMapRequestMessage msg) {
    for (final AbstractServerMapRequestContext context : this.outstandingRequests.values()) {
      if (context.isPending()) {
        if (context.getRequestType() != ServerMapRequestType.GET_VALUE_FOR_KEY) {
          // Only GET_VALUE_FOR_KEY Requests are batched here
          throw new AssertionError(context.getRequestType()
                                   + " requests are not batched so it should never be pending : " + context);
        }
        context.makeUnPending();
        context.initializeMessage(msg);
      }
    }
  }

  private void sendRequestNow(final AbstractServerMapRequestContext context) {
    final ServerMapRequestMessage msg = this.smmFactory.newServerMapRequestMessage(this.groupID,
                                                                                   context.getRequestType());
    context.initializeMessage(msg);
    msg.send();
  }

  private AbstractServerMapRequestContext createGetAllSizeRequestContext(final ObjectID[] maps) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetAllSizeServerMapRequestContext context = new GetAllSizeServerMapRequestContext(requestID, maps,
                                                                                            this.groupID);
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

  private AbstractServerMapRequestContext createLookupValueRequestContext(final ObjectID oid,
                                                                          final Set<Object> portableKeys) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetValueServerMapRequestContext context = new GetValueServerMapRequestContext(requestID, oid, portableKeys,
                                                                                        this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  private AbstractServerMapRequestContext createGetAllKeysRequestContext(final ObjectID mapID) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetAllKeysServerMapRequestContext context = new GetAllKeysServerMapRequestContext(requestID, mapID,
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
      setResultForRequest(sessionID, mapID, r.getRequestID(), r.getValues(), nodeID);
    }
    notifyAll();
  }

  public synchronized void addResponseForGetAllSize(final SessionID sessionID, final GroupID gID,
                                                    final ServerMapRequestID requestID, final Long size,
                                                    final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring response for ServerMap :  " + requestID + " , size : " + size
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    Map<Object, Object> sizeMap = new HashMap<Object, Object>();
    sizeMap.put(SIZE_KEY, size);
    setResultForRequest(sessionID, ObjectID.NULL_ID, requestID, sizeMap, nodeID);
    notifyAll();
  }

  public synchronized void addResponseForGetAllKeys(final SessionID sessionID, final ObjectID mapID,
                                                    final ServerMapRequestID requestID, final Set keys,
                                                    final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring response for ServerMap :  " + mapID + " , " + requestID + " , keys.size : "
                       + keys.size() + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    Map<Object, Object> allKeysMap = new HashMap<Object, Object>();
    allKeysMap.put(ALL_KEYS, keys);
    setResultForRequest(sessionID, mapID, requestID, allKeysMap, nodeID);
    notifyAll();
  }

  public synchronized void objectNotFoundFor(final SessionID sessionID, final ObjectID mapID,
                                             final ServerMapRequestID requestID, final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring Missing Object IDs " + mapID + " from a different session: " + sessionID + ", "
                       + this.sessionManager);
      return;
    }
    final AbstractServerMapRequestContext context = this.outstandingRequests.get(requestID);
    context.makeMissingObject();
    notifyAll();
  }

  private void setResultForRequest(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                   final Map<Object, Object> rv, final NodeID nodeID) {
    final AbstractServerMapRequestContext context = getRequestContext(requestID);
    if (context != null) {
      context.setResult(mapID, rv);
    } else {
      this.logger.warn("Server Map Request Context is null for " + mapID + " request ID : " + requestID + " result : "
                       + rv);
    }
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    try {
      while (this.state != State.RUNNING) {
        try {
          if (isStopped()) { throw new TCNotRunningException(); }
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
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
    globalLocalCacheManager.addAllObjectIDsToValidate(handshakeMessage.getObjectIDsToValidate());
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
    this.requestTimer.cancel();
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
    protected Map<Object, Object>        result;

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

    public void setResult(final ObjectID mapID, final Map<Object, Object> rv) {
      if (!this.oid.equals(mapID)) { throw new AssertionError("Wrong request to response : this map id : " + this.oid
                                                              + " response is for : " + mapID + " type : "
                                                              + getRequestType()); }
      this.result = rv;
    }

    public Map<Object, Object> getResult() {
      return this.result;
    }

    @Override
    public int hashCode() {
      return this.requestID.hashCode();
    }

    public ObjectID getMapID() {
      return this.oid;
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

  private class GetValueServerMapRequestContext extends AbstractServerMapRequestContext {

    private final Set<Object> portableKeys;

    public GetValueServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                           final Set<Object> portableKeys, final GroupID groupID) {
      super(ServerMapRequestType.GET_VALUE_FOR_KEY, requestID, mapID, groupID);
      this.portableKeys = portableKeys;
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetValueServerMapRequestMessage) requestMessage).addGetValueRequestTo(this.requestID, this.oid,
                                                                              this.portableKeys);
    }

  }

  private class GetAllKeysServerMapRequestContext extends AbstractServerMapRequestContext {

    public GetAllKeysServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                             final GroupID groupID) {
      super(ServerMapRequestType.GET_ALL_KEYS, requestID, mapID, groupID);
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetAllKeysServerMapRequestMessage) requestMessage).initializeSnapshotRequest(this.requestID, this.oid);
    }

  }

  private class GetAllSizeServerMapRequestContext extends AbstractServerMapRequestContext {
    private final ObjectID[] mapIDs;

    public GetAllSizeServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID[] mapIDs,
                                             final GroupID groupID) {
      super(ServerMapRequestType.GET_SIZE, requestID, ObjectID.NULL_ID, groupID);
      this.mapIDs = mapIDs;
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetAllSizeServerMapRequestMessage) requestMessage).initializeGetAllSizeRequest(this.requestID, this.mapIDs);
    }

  }

  /**
   * Flush all entries for invalidated objectId's
   */
  public void processInvalidations(Invalidations invalidations) {
    // NOTE: if this impl changes, check RemoteServerMapManagerGroupImpl
    Set<ObjectID> mapIDs = invalidations.getMapIds();
    for (ObjectID mapID : mapIDs) {
      ObjectIDSet set = invalidations.getObjectIDSetForMapId(mapID);
      globalLocalCacheManager.removeEntriesForObjectId(mapID, set);
    }
  }

  /**
   * Flush all local entries corresponding for the lock that is about to be flushed
   */
  public void preTransactionFlush(LockID lockID, ServerLockLevel level) {
    // NOTE: if this impl changes, check RemoteServerMapManagerGroupImpl
    if (lockID == null) { throw new AssertionError("ID cannot be null"); }
    if (level == ServerLockLevel.WRITE) {
      this.globalLocalCacheManager.removeEntriesForLockId(lockID);
      // TODO: remove all TCObjectSelf from here
    }
  }

}
