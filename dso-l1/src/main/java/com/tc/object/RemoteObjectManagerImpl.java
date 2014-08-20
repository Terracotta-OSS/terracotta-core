/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.Util;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for any communications to the server for object retrieval and removal
 */
public class RemoteObjectManagerImpl implements RemoteObjectManager, PrettyPrintable {

  private static final long    RETRIEVE_WAIT_INTERVAL                    = 15000;
  private static final long    REMOVED_OBJECTS_SEND_NOW                  = 0;
  private static final long    CLEANUP_UNUSED_DNA_TIMER                  = 300000;

  private static final int     REMOVE_OBJECTS_THRESHOLD                  = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.L1_OBJECTMANAGER_REMOVED_OBJECTS_THRESHOLD,
                                                                                     10000);
  private static final long    REMOVED_OBJECTS_SEND_TIMER                = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getLong(TCPropertiesConsts.L1_OBJECTMANAGER_REMOVED_OBJECTS_SEND_TIMER,
                                                                                      5000);
  private static final int     MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY);
  private static final long    BATCH_LOOKUP_TIME_PERIOD                  = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD);
  private final static int     MAX_LRU                                   = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE);
  private final static boolean ENABLE_LOGGING                            = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getBoolean(TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED);

  private static enum State {
    PAUSED, RUNNING, REJOIN_IN_PROGRESS, STARTING, STOPPED
  }

  private static enum RemovedObjectsSendState {
    NOT_SCHEDULED, SCHEDULED_LATER, SCHEDULED_NOW
  }

  private final HashMap<String, ObjectID>          rootRequests             = new HashMap<String, ObjectID>();

  private final Map<ObjectID, DNA>                 dnaCache                 = new HashMap<ObjectID, DNA>();
  private final Map<ObjectID, ObjectLookupState>   objectLookupStates       = new HashMap<ObjectID, ObjectLookupState>();

  private final RequestRootMessageFactory          rrmFactory;
  private final RequestManagedObjectMessageFactory rmomFactory;
  private final LRUCache                           lru                      = new LRUCache();
  private final GroupID                            groupID;
  private final int                                defaultDepth;
  private final SessionManager                     sessionManager;
  private final TCLogger                           logger;

  private State                                    state                    = State.RUNNING;
  private ObjectIDSet                              removeObjects            = new BitSetObjectIDSet();
  private boolean                                  pendingSendTaskScheduled = false;
  private RemovedObjectsSendState                  removeTaskScheduled      = RemovedObjectsSendState.NOT_SCHEDULED;
  private long                                     objectRequestIDCounter   = 0;
  private long                                     hit                      = 0;
  private long                                     miss                     = 0;
  private final AbortableOperationManager          abortableOperationManager;

  private final Timer                              objectRequestTimer;

  public RemoteObjectManagerImpl(final GroupID groupID, final TCLogger logger,
                                 final RequestRootMessageFactory rrmFactory,
                                 final RequestManagedObjectMessageFactory rmomFactory, final int defaultDepth,
                                 final SessionManager sessionManager,
                                 final AbortableOperationManager abortableOperationManager,
                                 final TaskRunner taskRunner) {
    this.groupID = groupID;
    this.logger = logger;
    this.rrmFactory = rrmFactory;
    this.rmomFactory = rmomFactory;
    this.defaultDepth = defaultDepth;
    this.sessionManager = sessionManager;
    this.abortableOperationManager = abortableOperationManager;
    this.objectRequestTimer = taskRunner.newTimer("RemoteObjectManager Request Scheduler");
    this.objectRequestTimer.scheduleWithFixedDelay(new CleanupUnusedDNATask(),
        CLEANUP_UNUSED_DNA_TIMER, CLEANUP_UNUSED_DNA_TIMER, TimeUnit.MILLISECONDS);
  }

  @Override
  public synchronized void cleanup() {
    checkAndSetstate();
    rootRequests.clear();
    dnaCache.clear();
    objectLookupStates.clear();
    lru.clear();
    removeObjects = new BitSetObjectIDSet();
    pendingSendTaskScheduled = false;
    removeTaskScheduled = RemovedObjectsSendState.NOT_SCHEDULED;
  }

  private void checkAndSetstate() {
    throwExceptionIfNecessary(true);
    state = State.REJOIN_IN_PROGRESS;
    notifyAll();
  }

  private void throwExceptionIfNecessary(boolean throwExp) {
    if (state != State.PAUSED) {
      String message = "cleanup unexpected state: expected " + State.PAUSED + " but found " + state;
      if (throwExp) {
        throw new IllegalStateException(message);
      } else {
        logger.warn(message);
      }
    }
  }

  @Override
  public synchronized void shutdown(boolean fromShutdownHook) {
    state = State.STOPPED;
    objectRequestTimer.cancel();
    notifyAll();
  }

  private boolean isStopped() {
    return this.state == State.STOPPED;
  }

  private boolean isRejoinInProgress() {
    return this.state == State.REJOIN_IN_PROGRESS;
  }

  @Override
  public synchronized void pause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = State.PAUSED;
    // XXX:: We are clearing unmaterialized DNAs and removed objects here because on connect we are going to send
    // the list of objects present in this L1 from Client Object Manager anyways. We can't be clearing the removed
    // object IDs in unpause(), then you get MNK-835
    clear(GroupID.ALL_GROUPS);
    notifyAll();
  }

  @Override
  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    if (isStopped()) { return; }
    assertPausedOrRejoinInProgress("Attempt to init handshake while ");
    this.state = State.STARTING;
  }

  @Override
  public synchronized void unpause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotRunning("Attempt to unpause while not PAUSED");
    this.state = State.RUNNING;
    requestOutstanding();
    notifyAll();
  }

  @Override
  public synchronized void clear(GroupID gid) {
    this.lru.clear();
    this.dnaCache.clear();
    this.removeObjects.clear();
  }

  private void waitUntilRunningAbortable() throws AbortedOperationException {
    boolean isInterrupted = false;
    try {
      while (this.state != State.RUNNING) {
        if (isStopped()) { throw new TCNotRunningException(); }
        if (isRejoinInProgress()) { throw new PlatformRejoinException(); }
        try {
          wait();
        } catch (final InterruptedException e) {
          AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    try {
      while (this.state != State.RUNNING) {
        if (isStopped()) { throw new TCNotRunningException(); }
        if (isRejoinInProgress()) { throw new PlatformRejoinException(); }
        try {
          wait();
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private void assertPausedOrRejoinInProgress(final Object message) {
    State current = this.state;
    if (!(current == State.PAUSED || current == State.REJOIN_IN_PROGRESS)) { throw new AssertionError(message + ": "
                                                                                                      + current); }
  }

  private void assertNotPaused(final String message) {
    if (this.state == State.PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotRunning(final String message) {
    if (this.state == State.RUNNING) { throw new AssertionError(message + ": " + this.state); }
  }

  synchronized void requestOutstanding() {
    int lookupCount = 0;
    int rootCount = 0;
    for (final ObjectLookupState ols : this.objectLookupStates.values()) {
      if (!ols.isMissing() && !ols.isPending()) {
        sendRequestNow(ols);
        ++lookupCount;
      }
    }
    for (final Entry<String, ObjectID> e : this.rootRequests.entrySet()) {
      final String rootName = e.getKey();
      if (e.getValue().isNull()) {
        final RequestRootMessage rrm = createRootMessage(rootName);
        rrm.send();
        ++rootCount;
      }
    }
    this.logger.info("Sending Pending LookUp Requests lookups " + lookupCount + " roots " + rootCount);
  }

  @Override
  public synchronized void preFetchObject(final ObjectID id) throws AbortedOperationException {
    waitUntilRunningAbortable();
    if (this.dnaCache.containsKey(id) || this.objectLookupStates.containsKey(id)) { return; }
    final ObjectLookupState ols = new ObjectLookupState(getNextRequestID(), id, this.defaultDepth);
    ols.makePrefetchRequest();
    sendRequest(ols);
  }

  @Override
  public DNA retrieve(final ObjectID id) throws AbortedOperationException {
    return basicRetrieve(id, this.defaultDepth);
  }

  @Override
  public DNA retrieve(final ObjectID id, final int depth) throws AbortedOperationException {
    return basicRetrieve(id, depth);
  }

  public synchronized DNA basicRetrieve(final ObjectID id, final int depth)
      throws AbortedOperationException {
    boolean isInterrupted = false;
    if (id.getGroupID() != this.groupID.toInt()) { throw new AssertionError("Looking up in the wrong Remote Manager : "
                                                                            + this.groupID + " id : " + id
                                                                            + " depth : " + depth); }
    boolean inMemory = true;
    long startTime = System.currentTimeMillis();
    long totalTime = 0;

    DNA dna;
    try {
      while ((dna = this.dnaCache.remove(id)) == null) {
        waitUntilRunningAbortable();
        ObjectLookupState ols = this.objectLookupStates.get(id);
        if (ols == null) {
          ols = new ObjectLookupState(getNextRequestID(), id, depth);
          ols.makeLookupRequest();
          sendRequest(ols);
        } else if (ols.isMissing()) {
          this.objectLookupStates.remove(id);
          throw new TCObjectNotFoundException(id.toString());
        } else if (ols.isPrefetch()) {
          ols.makeLookupRequest();
        }

        inMemory = false;
        final long current = System.currentTimeMillis();
        if (current - startTime >= RETRIEVE_WAIT_INTERVAL) {
          totalTime += current - startTime;
          startTime = current;
          this.logger.warn("Still waiting for " + totalTime + " ms to retrieve " + id + " depth : " + depth);
        }
        try {
          wait(RETRIEVE_WAIT_INTERVAL);
        } catch (final InterruptedException e) {
          AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          isInterrupted = true;
        }
      }
    } finally {
      this.objectLookupStates.remove(id);
      this.lru.remove(id);
      Util.selfInterruptIfNeeded(isInterrupted);
    }
    increamentStatsAndLogIfNecessary(inMemory);
    return dna;
  }

  private void increamentStatsAndLogIfNecessary(final boolean inMemory) {
    if (inMemory) {
      this.hit++;
    } else {
      this.miss++;
    }
    if (ENABLE_LOGGING && ((this.hit + this.miss) % 1000 == 0)) {
      this.logger.info("Cache Hit : Miss ratio = " + this.hit + "  : " + this.miss);
    }

  }

  private ObjectRequestID getNextRequestID() {
    return new ObjectRequestID(this.objectRequestIDCounter++);
  }

  /**
   * basicRetrieve and preFetch can call this method, the lookupState gets added to the map here.
   */
  private void sendRequest(final ObjectLookupState lookupState) {
    final ObjectLookupState old = this.objectLookupStates.put(lookupState.getLookupID(), lookupState);
    Assert.assertNull(old);
    final int size = this.objectLookupStates.size();
    if (size % 5000 == 4999) {
      this.logger.warn("Too many pending requests in the system : objectLookup states size : " + size
                       + " dna Cache size : " + this.dnaCache.size());
    }
    if (size <= MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY) {
      sendRequestNow(lookupState);
    } else {
      scheduleRequestForLater(lookupState);
    }
  }

  private void scheduleRequestForLater(final ObjectLookupState ctxt) {
    ctxt.makePending();
    if (!pendingSendTaskScheduled) {
      // one-shot delayed action
      objectRequestTimer.schedule(new SendPendingRequestsTask(),
          BATCH_LOOKUP_TIME_PERIOD, TimeUnit.MILLISECONDS);
      pendingSendTaskScheduled = true;
    }
  }

  public synchronized void sendPendingRequests() {
    waitUntilRunning();
    this.pendingSendTaskScheduled = false;
    final HashMap<Integer, ObjectIDSet> segregatedPending = getPendingRequestSegregated();
    if (!segregatedPending.isEmpty()) {
      sendSegregatedPendingRequests(segregatedPending);
    }
  }

  private void sendSegregatedPendingRequests(final HashMap<Integer, ObjectIDSet> segregatedPending) {
    for (final Entry<Integer, ObjectIDSet> e : segregatedPending.entrySet()) {
      final int requestDepth = e.getKey();
      final ObjectIDSet oids = e.getValue();
      sendRequestNow(getNextRequestID(), oids, requestDepth);
    }
  }

  private HashMap<Integer, ObjectIDSet> getPendingRequestSegregated() {
    final HashMap<Integer, ObjectIDSet> segregatedPending = new HashMap<Integer, ObjectIDSet>();
    for (final ObjectLookupState ols : this.objectLookupStates.values()) {
      if (ols.isPending()) {
        ols.makeUnPending();
        final Integer key = ols.getRequestDepth();
        ObjectIDSet oids = segregatedPending.get(key);
        if (oids == null) {
          oids = new BitSetObjectIDSet();
          segregatedPending.put(key, oids);
        }
        addRequestedObjectIDsTo(ols, oids);
      }
    }
    return segregatedPending;
  }

  private void sendRequestNow(final ObjectLookupState ctxt) {
    final Set<ObjectID> oids = addRequestedObjectIDsTo(ctxt, new HashSet<ObjectID>());
    sendRequestNow(ctxt.getRequestID(), oids, ctxt.getRequestDepth());
  }

  private Set<ObjectID> addRequestedObjectIDsTo(final ObjectLookupState ctxt, final Set<ObjectID> oids) {
    oids.add(ctxt.getLookupID());
    return oids;
  }

  private void sendRequestNow(final ObjectRequestID requestID, final Set<ObjectID> oids, final int requestDepth) {
    final RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(requestID, oids, requestDepth);
    rmom.send();
  }
  
  private RequestManagedObjectMessage createRequestManagedObjectMessage(final ObjectRequestID requestID,
                                                                        final Set<ObjectID> oids, final int requestDepth) {
    final RequestManagedObjectMessage rmom = this.rmomFactory.newRequestManagedObjectMessage(this.groupID);
    if (this.removeObjects.isEmpty()) {
      rmom.initialize(requestID, new BitSetObjectIDSet(oids), requestDepth, TCCollections.EMPTY_OBJECT_ID_SET);
    } else {
      rmom.initialize(requestID, new BitSetObjectIDSet(oids), requestDepth, this.removeObjects);
      this.removeObjects = new BitSetObjectIDSet();
    }
    return rmom;
  }

  @Override
  public synchronized ObjectID retrieveRootID(final String name, GroupID gid) {

    waitUntilRunning();
    if (!this.rootRequests.containsKey(name)) {
      final RequestRootMessage rrm = createRootMessage(name);
      this.rootRequests.put(name, ObjectID.NULL_ID);
      rrm.send();
    }

    boolean isInterrupted = false;
    try {
      while (ObjectID.NULL_ID.equals(this.rootRequests.get(name))) {
        waitUntilRunning();
        try {
          if (ObjectID.NULL_ID.equals(this.rootRequests.get(name))) {
            wait();
          }
        } catch (final InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }

    return (this.rootRequests.containsKey(name) ? this.rootRequests.get(name) : ObjectID.NULL_ID);
  }

  private RequestRootMessage createRootMessage(final String name) {
    final RequestRootMessage rrm = this.rrmFactory.newRequestRootMessage(this.groupID);
    rrm.initialize(name);
    return rrm;
  }

  @Override
  public synchronized void addRoot(final String name, final ObjectID id, final NodeID nodeID) {
    waitUntilRunning();
    if (id.isNull()) {
      this.rootRequests.remove(name);
    } else {
      this.rootRequests.put(name, id);
    }
    notifyAll();
  }

  @Override
  public synchronized void addAllObjects(final SessionID sessionID, final long batchID, final Collection dnas,
                                         final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring DNA added from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    this.lru.clearOneUnrequestedDNABatch();
    this.lru.add(batchID, dnas);
    for (Object obj : dnas) {
      final DNA dna = (DNA) obj;
      // The server should not send us any objects that the server thinks we still have.
      if (this.removeObjects.contains(dna.getObjectID())) {
        // formatting
        throw new AssertionError("Server sent us an object that is present in the removed set - " + dna.getObjectID()
            + " , removed set = " + this.removeObjects);
      }
      basicAddObject(dna);
    }
    notifyAll();
  }

  @Override
  public synchronized void objectsNotFoundFor(final SessionID sessionID, final long batchID, final Set missingOIDs,
                                              final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring Missing Object IDs " + missingOIDs + " from a different session: " + sessionID + ", "
                       + this.sessionManager);
      return;
    }
    for (Object missingOID : missingOIDs) {
      final ObjectID oid = (ObjectID) missingOID;
      final ObjectLookupState ols = this.objectLookupStates.get(oid);
      this.logger.debug("Received Missing Object ID from server : " + oid + " ObjectLookup State : " + ols);
      if (ols == null) {
        /**
         * DEV-5697 : This is possible if prefetch from the server and the look from the client are racing and the
         * object gets removed, DGCed before the lookup from the client is actually processed.
         */
        continue;
      } else if (ols.isPrefetch()) {
        // Ignoring prefetch requests, as it could made under incorrect locking, reset the data structures
        this.objectLookupStates.remove(oid);
      } else {
        ols.makeMissingObject();
      }
    }
    notifyAll();
  }

  @Override
  public synchronized void addObject(final DNA dna) {
    if (!this.removeObjects.contains(dna.getObjectID())) {
      basicAddObject(dna);
    }
    notifyAll();
  }

  @Override
  public synchronized void cleanOutObject(final DNA dna) {
    removed(dna.getObjectID());
  }

  // Used only for testing
  synchronized int getDNACacheSize() {
    return this.lru.size();
  }

  private void basicAddObject(final DNA dna) {
    final ObjectID id = dna.getObjectID();
    this.dnaCache.put(id, dna);
    final ObjectLookupState ols = this.objectLookupStates.get(id);
    if (ols != null && ols.isPrefetch()) {
      // Prefetched requests are removed from the lookupState map so it can be removed from the cache if it is not used
      // within a certain time.
      this.objectLookupStates.remove(id);
    }
  }

  /**
   * Do not wait here for any reason as this method is called under the ClientObjectManager lock and waiting here could
   * cause deadlocks on restarts. Since you are not allowed to wait here (waitUntilRunning) don't send any messages to
   * the server from this method too as it can end-up in the server even before the connection is fully handshaked.
   */
  @Override
  public synchronized void removed(final ObjectID id) {

    if (isStopped()) { return; }
    if (objectLookupStates.containsKey(id)) {
      logger.warn("Not removing object " + id + " as it is being looked up : " + objectLookupStates.get(id));
      return;
    }
    dnaCache.remove(id);
    removeObjects.add(id);
    if (removeObjects.size() >= REMOVE_OBJECTS_THRESHOLD
        && removeTaskScheduled != RemovedObjectsSendState.SCHEDULED_NOW) {
      objectRequestTimer.schedule(new RemovedObjectTask(), REMOVED_OBJECTS_SEND_NOW, TimeUnit.MILLISECONDS);
      removeTaskScheduled = RemovedObjectsSendState.SCHEDULED_NOW;
    } else if (removeObjects.size() == 1 && removeTaskScheduled == RemovedObjectsSendState.NOT_SCHEDULED) {
      objectRequestTimer.schedule(new RemovedObjectTask(), REMOVED_OBJECTS_SEND_TIMER, TimeUnit.MILLISECONDS);
      removeTaskScheduled = RemovedObjectsSendState.SCHEDULED_LATER;
    }
  }

  public synchronized void sendRemovedObjects() {
    waitUntilRunning();
    this.removeTaskScheduled = RemovedObjectsSendState.NOT_SCHEDULED;
    if (!this.removeObjects.isEmpty()) {
      sendRequestNow(getNextRequestID(), TCCollections.EMPTY_OBJECT_ID_SET, -1);
    }
  }

  @Override
  public synchronized boolean isInDNACache(final ObjectID id) {
    return this.dnaCache.get(id) != null;
  }

  public synchronized void clearAllUnrequestedDNABatches() {
    waitUntilRunning();
    this.lru.clearAllUnrequestedDNABatches();
  }

  private class SendPendingRequestsTask implements Runnable {
    @Override
    public void run() {
      try {
        sendPendingRequests();
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling scheduled task");
      } catch (PlatformRejoinException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName());
      }
    }
  }

  private class RemovedObjectTask implements Runnable {
    @Override
    public void run() {
      try {
        sendRemovedObjects();
      } catch (PlatformRejoinException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName());
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling timer task");
      }
    }
  }

  private class CleanupUnusedDNATask implements Runnable {
    @Override
    public void run() {
      try {
        clearAllUnrequestedDNABatches();
      } catch (PlatformRejoinException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName());
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling timer task");
      }
    }
  }

  private static final class ObjectLookupState extends LookupStateTransitionAdaptor {

    private final ObjectRequestID requestID;
    private final long            timestamp;
    private final int             depth;
    private final ObjectID        lookupID;

    ObjectLookupState(final ObjectRequestID requestID, final ObjectID id, final int depth) {
      this.lookupID = id;
      this.timestamp = System.currentTimeMillis();
      this.requestID = requestID;
      this.depth = depth;
    }

    public ObjectID getLookupID() {
      return this.lookupID;
    }

    public ObjectRequestID getRequestID() {
      return this.requestID;
    }

    public int getRequestDepth() {
      return this.depth;
    }

    @Override
    public String toString() {
      return getClass().getName() + "[" + new Date(this.timestamp) + ", requestID =" + this.requestID + ", lookupID ="
             + this.lookupID + ", depth = " + this.depth + ", state = " + getState()
             + "]";
    }
  }

  private class LRUCache {
    private final HashMap<ObjectID, Long>            lruOids = new HashMap<ObjectID, Long>();
    private final LinkedHashMap<Long, LRUCacheEntry> batches = new LinkedHashMap<Long, LRUCacheEntry>();

    public int size() {
      return this.lruOids.size();
    }

    public void clear() {
      this.lruOids.clear();
      this.batches.clear();
    }

    public void add(final long batchID, final Collection objs) {
      final Set<ObjectID> oids = getOrCreateSetForBatch(batchID);
      for (Object obj : objs) {
        final DNA dna = (DNA) obj;
        final ObjectID oid = dna.getObjectID();
        oids.add(oid);
        final Long old = this.lruOids.put(oid, batchID);
        if (old != null) {
          throw new AssertionError("Old Entry present for :" + dna + " oid : " + oid + " old " + old
              + " batch " + batchID + " objs " + objs);
        }
      }
    }

    private Set<ObjectID> getOrCreateSetForBatch(final Long batchID) {
      LRUCacheEntry entry = this.batches.get(batchID);
      if (entry == null) {
        entry = new LRUCacheEntry();
        this.batches.put(batchID, entry);
      }
      return entry.getOids();
    }

    private Set<ObjectID> getSetForBatch(Long batchID) {
      LRUCacheEntry entry = this.batches.get(batchID);
      return entry.getOids();
    }

    public void remove(final ObjectID id) {
      final Long batchID = this.lruOids.remove(id);
      if (batchID == null) { return; }

      final Set<ObjectID> oids = getSetForBatch(batchID);
      oids.remove(id);
      if (oids.isEmpty()) {
        this.batches.remove(batchID);
      }
    }

    public void clearOneUnrequestedDNABatch() {
      if (this.batches.isEmpty() || this.batches.size() <= MAX_LRU) { return; }
      final Set<ObjectID> oidsToRemove = removeFirstBatchToClear();

      removeOids(oidsToRemove);
      if (ENABLE_LOGGING) {
        RemoteObjectManagerImpl.this.logger.info("DNA LRU remove 1 batch containing " + oidsToRemove.size() + " DNAs");
      }
    }

    private void removeOids(Set<ObjectID> oidsToRemove) {
      for (final ObjectID oid : oidsToRemove) {
        this.lruOids.remove(oid);
        if (!RemoteObjectManagerImpl.this.objectLookupStates.containsKey(oid)) {
          removed(oid);
        }
      }
    }

    public void clearAllUnrequestedDNABatches() {
      int removed = 0;
      for (Iterator<Entry<Long, LRUCacheEntry>> i = this.batches.entrySet().iterator(); i.hasNext();) {
        LRUCacheEntry batch = i.next().getValue();
        if (!batch.getAndSetAccessed(false)) {
          // Not accessed for two cycles - remove
          i.remove();
          removeOids(batch.getOids());
          removed++;
        }
      }
      if (ENABLE_LOGGING) {
        RemoteObjectManagerImpl.this.logger.info("DNA LRU remove " + removed + " batch containing  many DNAs");
      }
    }

    private Set<ObjectID> removeFirstBatchToClear() {
      final Iterator<Entry<Long, LRUCacheEntry>> i = this.batches.entrySet().iterator();
      final LRUCacheEntry batchToRemove = i.next().getValue();
      i.remove();
      return batchToRemove.getOids(); // already removed, doesn't matter if accessed is set
    }
  }

  private static final class LRUCacheEntry {

    private final HashSet<ObjectID> oidsSet;
    private boolean                 accessed;

    private LRUCacheEntry() {
      this.oidsSet = new HashSet<ObjectID>();
      this.accessed = true;
    }

    public boolean getAndSetAccessed(boolean b) {
      boolean old = this.accessed;
      this.accessed = b;
      return old;
    }

    public Set<ObjectID> getOids() {
      this.accessed = true;
      return this.oidsSet;
    }
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.duplicateAndIndent().indent().print(getClass().getSimpleName()).flush();
    out.duplicateAndIndent().indent().print(this.groupID).flush();
    out.duplicateAndIndent().indent().print(this.state).flush();
    out.duplicateAndIndent().indent().print("dnaCache:").visit(this.dnaCache).flush();

    // printing this.objectLookupStates.toString() as PrettyPrinter prints the size of the map otherwise
    out.duplicateAndIndent().indent().print("lookupstates:").print(this.objectLookupStates.toString()).flush();
    return out;
  }

}
