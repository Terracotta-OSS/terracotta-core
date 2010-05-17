/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

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
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.Util;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

/**
 * This class is responsible for any communications to the server for object retrieval and removal
 */
public class RemoteObjectManagerImpl implements RemoteObjectManager, PrettyPrintable {

  private static final long    RETRIEVE_WAIT_INTERVAL                    = 15000;
  private static final int     REMOVE_OBJECTS_THRESHOLD                  = 10000;
  private static final long    REMOVED_OBJECTS_SEND_TIMER                = 30000;

  private static final int     MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(
                                                                                     TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY);
  private static final long    BATCH_LOOKUP_TIME_PERIOD                  = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(
                                                                                     TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD);
  private final static int     MAX_LRU                                   = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(
                                                                                     TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE);
  private final static boolean ENABLE_LOGGING                            = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getBoolean(
                                                                                         TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED);

  private static enum State {
    PAUSED, RUNNING, STARTING, STOPPED
  }

  private final HashMap<String, ObjectID>          rootRequests             = new HashMap<String, ObjectID>();

  private final Map<ObjectID, DNA>                 dnaCache                 = new HashMap<ObjectID, DNA>();
  private final Map<ObjectID, ObjectLookupState>   objectLookupStates       = new HashMap<ObjectID, ObjectLookupState>();

  private final Timer                              objectRequestTimer       = new Timer(
                                                                                        "RemoteObjectManager Request Scheduler",
                                                                                        true);

  private final RequestRootMessageFactory          rrmFactory;
  private final RequestManagedObjectMessageFactory rmomFactory;
  private final LRUCache                           lru                      = new LRUCache();
  private final GroupID                            groupID;
  private final int                                defaultDepth;
  private final SessionManager                     sessionManager;
  private final TCLogger                           logger;

  private State                                    state                    = State.RUNNING;
  private ObjectIDSet                              removeObjects            = new ObjectIDSet();

  private boolean                                  pendingSendTaskScheduled = false;
  private boolean                                  removeTaskScheduled      = false;
  private long                                     objectRequestIDCounter   = 0;
  private long                                     hit                      = 0;
  private long                                     miss                     = 0;

  public RemoteObjectManagerImpl(final GroupID groupID, final TCLogger logger,
                                 final RequestRootMessageFactory rrmFactory,
                                 final RequestManagedObjectMessageFactory rmomFactory, final int defaultDepth,
                                 final SessionManager sessionManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.rrmFactory = rrmFactory;
    this.rmomFactory = rmomFactory;
    this.defaultDepth = defaultDepth;
    this.sessionManager = sessionManager;
  }

  public synchronized void shutdown() {
    this.state = State.STOPPED;
  }

  private boolean isStopped() {
    return this.state == State.STOPPED;
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = State.PAUSED;
    // XXX:: We are clearing unmaterialized DNAs and removed objects here because on connect we are going to send
    // the list of objects present in this L1 from Client Object Manager anyways. We can't be clearing the removed
    // object IDs in unpause(), then you get MNK-835
    clear();
    notifyAll();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
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

  public synchronized void clear() {
    this.lru.clear();
    this.dnaCache.clear();
    this.removeObjects.clear();
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (this.state != State.RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
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

  synchronized void requestOutstanding() {
    for (ObjectLookupState ols : this.objectLookupStates.values()) {
      if (!ols.isMissing() && !ols.isPending()) {
        sendRequestNow(ols);
      }
    }
    for (Entry<String, ObjectID> e : this.rootRequests.entrySet()) {
      String rootName = e.getKey();
      if (e.getValue().isNull()) {
        RequestRootMessage rrm = createRootMessage(rootName);
        rrm.send();
      }
    }
  }

  public synchronized void preFetchObject(ObjectID id) {
    if (this.dnaCache.containsKey(id) || this.objectLookupStates.containsKey(id)) { return; }
    ObjectLookupState ols = new ObjectLookupState(getNextRequestID(), id, this.defaultDepth, ObjectID.NULL_ID);
    ols.makePrefetchRequest();
    sendRequest(ols);
  }

  public DNA retrieve(final ObjectID id) {
    return basicRetrieve(id, this.defaultDepth, ObjectID.NULL_ID);
  }

  public DNA retrieveWithParentContext(final ObjectID id, final ObjectID parentContext) {
    return basicRetrieve(id, this.defaultDepth, parentContext);
  }

  public DNA retrieve(final ObjectID id, final int depth) {
    return basicRetrieve(id, depth, ObjectID.NULL_ID);
  }

  public synchronized DNA basicRetrieve(final ObjectID id, final int depth, final ObjectID parentContext) {
    boolean isInterrupted = false;
    if (id.getGroupID() != this.groupID.toInt()) { throw new AssertionError("Looking up in the wrong Remote Manager : "
                                                                            + this.groupID + " id : " + id
                                                                            + " depth : " + depth + " parent : "
                                                                            + parentContext); }
    boolean inMemory = true;
    long startTime = System.currentTimeMillis();
    long totalTime = 0;

    DNA dna;
    while ((dna = this.dnaCache.remove(id)) == null) {
      waitUntilRunning();
      ObjectLookupState ols = this.objectLookupStates.get(id);
      if (ols == null) {
        ols = new ObjectLookupState(getNextRequestID(), id, depth, parentContext);
        ols.makeLookupRequest();
        sendRequest(ols);
      } else if (ols.isMissing()) {
        this.objectLookupStates.remove(id);
        throw new TCObjectNotFoundException(id.toString());
      } else if (ols.isPrefetch()) {
        ols.makeLookupRequest();
      }

      inMemory = false;
      long current = System.currentTimeMillis();
      if (current - startTime >= RETRIEVE_WAIT_INTERVAL) {
        totalTime += current - startTime;
        startTime = current;
        this.logger.warn("Still waiting for " + totalTime + " ms to retrieve " + id + " depth : " + depth
                         + " parent : " + parentContext);
      }
      try {
        wait(RETRIEVE_WAIT_INTERVAL);
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    this.objectLookupStates.remove(id);
    this.lru.remove(id);
    Util.selfInterruptIfNeeded(isInterrupted);
    increamentStatsAndLogIfNecessary(inMemory);
    return dna;
  }

  private void increamentStatsAndLogIfNecessary(boolean inMemory) {
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
    ObjectLookupState old = this.objectLookupStates.put(lookupState.getLookupID(), lookupState);
    Assert.assertNull(old);
    int size = this.objectLookupStates.size();
    if (size % 5000 == 4999) {
      logger.warn("Too many pending requests in the system : objectLookup states size : " + size + " dna Cache size : "
                  + dnaCache.size());
    }
    if (size <= MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY) {
      sendRequestNow(lookupState);
    } else {
      scheduleRequestForLater(lookupState);
    }
  }

  private void scheduleRequestForLater(ObjectLookupState ctxt) {
    ctxt.makePending();
    if (!this.pendingSendTaskScheduled) {
      this.objectRequestTimer.schedule(new SendPendingRequestsTimer(), BATCH_LOOKUP_TIME_PERIOD);
      this.pendingSendTaskScheduled = true;
    }
  }

  public synchronized void sendPendingRequests() {
    waitUntilRunning();
    this.pendingSendTaskScheduled = false;
    HashMap<Integer, ObjectIDSet> segregatedPending = getPendingRequestSegregated();
    if (!segregatedPending.isEmpty()) {
      sendSegregatedPendingRequests(segregatedPending);
    }
  }

  private void sendSegregatedPendingRequests(HashMap<Integer, ObjectIDSet> segregatedPending) {
    for (Entry<Integer, ObjectIDSet> e : segregatedPending.entrySet()) {
      int requestDepth = e.getKey().intValue();
      ObjectIDSet oids = e.getValue();
      sendRequestNow(getNextRequestID(), oids, requestDepth);
    }
  }

  private HashMap<Integer, ObjectIDSet> getPendingRequestSegregated() {
    HashMap<Integer, ObjectIDSet> segregatedPending = new HashMap<Integer, ObjectIDSet>();
    for (ObjectLookupState ols : this.objectLookupStates.values()) {
      if (ols.isPending()) {
        ols.makeUnPending();
        Integer key = new Integer(ols.getRequestDepth());
        ObjectIDSet oids = segregatedPending.get(key);
        if (oids == null) {
          oids = new ObjectIDSet();
          segregatedPending.put(key, oids);
        }
        addRequestedObjectIDsTo(ols, oids);
      }
    }
    return segregatedPending;
  }

  private void sendRequestNow(ObjectLookupState ctxt) {
    Set<ObjectID> oids = addRequestedObjectIDsTo(ctxt, new HashSet<ObjectID>());
    sendRequestNow(ctxt.getRequestID(), oids, ctxt.getRequestDepth());
  }

  private Set<ObjectID> addRequestedObjectIDsTo(ObjectLookupState ctxt, Set<ObjectID> oids) {
    oids.add(ctxt.getLookupID());
    ObjectID parent = ctxt.getParentID();
    if (!parent.isNull()) {
      // XXX:: This is a hacky way to let the L2 know about the parent but works for now.
      oids.add(parent);
    }
    return oids;
  }

  private void sendRequestNow(ObjectRequestID requestID, Set<ObjectID> oids, int requestDepth) {
    RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(requestID, oids, requestDepth);
    rmom.send();
  }

  private RequestManagedObjectMessage createRequestManagedObjectMessage(ObjectRequestID requestID, Set<ObjectID> oids,
                                                                        int requestDepth) {
    RequestManagedObjectMessage rmom = this.rmomFactory.newRequestManagedObjectMessage(this.groupID);
    if (this.removeObjects.isEmpty()) {
      rmom.initialize(requestID, oids, requestDepth, TCCollections.EMPTY_OBJECT_ID_SET);
    } else {
      rmom.initialize(requestID, oids, requestDepth, this.removeObjects);
      this.removeObjects = new ObjectIDSet();
    }
    return rmom;
  }

  public synchronized ObjectID retrieveRootID(final String name) {

    if (!this.rootRequests.containsKey(name)) {
      RequestRootMessage rrm = createRootMessage(name);
      this.rootRequests.put(name, ObjectID.NULL_ID);
      rrm.send();
    }

    boolean isInterrupted = false;
    while (ObjectID.NULL_ID.equals(this.rootRequests.get(name))) {
      waitUntilRunning();
      try {
        if (ObjectID.NULL_ID.equals(this.rootRequests.get(name))) {
          wait();
        }
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);

    return (this.rootRequests.containsKey(name) ? this.rootRequests.get(name) : ObjectID.NULL_ID);
  }

  private RequestRootMessage createRootMessage(final String name) {
    RequestRootMessage rrm = this.rrmFactory.newRequestRootMessage(this.groupID);
    rrm.initialize(name);
    return rrm;
  }

  public synchronized void addRoot(final String name, final ObjectID id, final NodeID nodeID) {
    waitUntilRunning();
    if (id.isNull()) {
      this.rootRequests.remove(name);
    } else {
      this.rootRequests.put(name, id);
    }
    notifyAll();
  }

  public synchronized void addAllObjects(final SessionID sessionID, final long batchID, final Collection dnas,
                                         final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring DNA added from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    this.lru.clearUnrequestedDNA();
    this.lru.add(batchID, dnas);
    for (Iterator i = dnas.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
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

  public synchronized void objectsNotFoundFor(final SessionID sessionID, final long batchID, final Set missingOIDs,
                                              final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring Missing Object IDs " + missingOIDs + " from a different session: " + sessionID + ", "
                       + this.sessionManager);
      return;
    }
    for (Iterator i = missingOIDs.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      ObjectLookupState ols = this.objectLookupStates.get(oid);
      this.logger.warn("Received Missing Object ID from server : " + oid + " ObjectLookup State : " + ols);
      if (ols.isPrefetch()) {
        // Ignoring prefetch requests, as it could made under incorrect locking, reset the data structures
        this.objectLookupStates.remove(oid);
      } else {
        ols.makeMissingObject();
      }
    }
    notifyAll();
  }

  // Used only for testing
  synchronized void addObject(final DNA dna) {
    if (!this.removeObjects.contains(dna.getObjectID())) {
      basicAddObject(dna);
    }
    notifyAll();
  }

  // Used only for testing
  synchronized int getDNACacheSize() {
    return this.lru.size();
  }

  private void basicAddObject(final DNA dna) {
    ObjectID id = dna.getObjectID();
    this.dnaCache.put(id, dna);
    ObjectLookupState ols = this.objectLookupStates.get(id);
    if (ols != null && ols.isPrefetch()) {
      // Prefetched requests are removed from the lookupState map so it can be removed from the cache if it is not used
      // within a certain time.
      this.objectLookupStates.remove(id);
    }
  }

  public synchronized void removed(final ObjectID id) {
    this.dnaCache.remove(id);
    this.removeObjects.add(id);
    if (this.removeObjects.size() >= REMOVE_OBJECTS_THRESHOLD) {
      sendRequestNow(getNextRequestID(), TCCollections.EMPTY_OBJECT_ID_SET, -1);
    } else if (this.removeObjects.size() == 1 && !this.removeTaskScheduled) {
      this.objectRequestTimer.schedule(new RemovedObjectTimer(), REMOVED_OBJECTS_SEND_TIMER);
      this.removeTaskScheduled = true;

    }
  }

  public synchronized void sendRemovedObjects() {
    this.removeTaskScheduled = false;
    if (!this.removeObjects.isEmpty()) {
      sendRequestNow(getNextRequestID(), TCCollections.EMPTY_OBJECT_ID_SET, -1);
    }
  }

  public synchronized boolean isInDNACache(final ObjectID id) {
    return this.dnaCache.get(id) != null;
  }

  private class SendPendingRequestsTimer extends TimerTask {
    @Override
    public void run() {
      RemoteObjectManagerImpl.this.sendPendingRequests();
    }
  }

  private class RemovedObjectTimer extends TimerTask {
    @Override
    public void run() {
      RemoteObjectManagerImpl.this.sendRemovedObjects();
    }
  }

  private static final class ObjectLookupState {

    private final ObjectRequestID requestID;
    private final long            timestamp;
    private final int             depth;
    private final ObjectID        lookupID;
    private final ObjectID        parent;
    private LookupState           state = LookupState.UNINITALIZED;

    ObjectLookupState(final ObjectRequestID requestID, final ObjectID id, final int depth, final ObjectID parent) {
      this.lookupID = id;
      this.parent = parent;
      this.timestamp = System.currentTimeMillis();
      this.requestID = requestID;
      this.depth = depth;
    }

    public ObjectID getParentID() {
      return this.parent;
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

    public void makeLookupRequest() {
      this.state = this.state.makeLookupRequest();
    }

    public void makePrefetchRequest() {
      this.state = this.state.makePrefetchRequest();
    }

    public void makePending() {
      this.state = this.state.makePending();
    }

    public void makeUnPending() {
      this.state = this.state.makeUnPending();
    }

    public void makeMissingObject() {
      this.state = this.state.makeMissingObject();
    }

    public boolean isPrefetch() {
      return this.state.isPrefetch();
    }

    public boolean isMissing() {
      return this.state.isMissing();
    }

    public boolean isPending() {
      return this.state.isPending();
    }

    @Override
    public String toString() {
      return getClass().getName() + "[" + new Date(this.timestamp) + ", requestID =" + this.requestID + ", lookupID ="
             + this.lookupID + ", parent = " + this.parent + ", depth = " + this.depth + ", state = " + this.state
             + "]";
    }
  }

  private class LRUCache {
    private final HashMap<ObjectID, Long>            lruOids = new HashMap<ObjectID, Long>();
    private final LinkedHashMap<Long, Set<ObjectID>> batches = new LinkedHashMap<Long, Set<ObjectID>>();

    public int size() {
      return this.lruOids.size();
    }

    public void clear() {
      this.lruOids.clear();
      this.batches.clear();
    }

    public void add(final long batchID, final Collection objs) {
      Long batch = new Long(batchID);
      Set<ObjectID> oids = getOrCreateSetForBatch(batch);
      for (Iterator i = objs.iterator(); i.hasNext();) {
        DNA dna = (DNA) i.next();
        ObjectID oid = dna.getObjectID();
        oids.add(oid);
        Long old = this.lruOids.put(oid, batch);
        if (old != null) { throw new AssertionError("Old Entry present for :" + dna + " old : " + old); }
      }
    }

    private Set<ObjectID> getOrCreateSetForBatch(Long batchID) {
      Set<ObjectID> oids = this.batches.get(batchID);
      if (oids == null) {
        oids = new HashSet<ObjectID>();
        this.batches.put(batchID, oids);
      }
      return oids;
    }

    public void remove(final ObjectID id) {
      Long batchID = this.lruOids.remove(id);
      if (batchID == null) { return; }

      Set<ObjectID> oids = this.batches.get(batchID);
      oids.remove(id);
      if (oids.isEmpty()) {
        this.batches.remove(batchID);
      }
    }

    public void clearUnrequestedDNA() {
      if (this.batches.isEmpty() || this.batches.size() <= MAX_LRU) { return; }
      Set<ObjectID> oidsToRemove = removeFirstBatchToClear();

      for (ObjectID oid : oidsToRemove) {
        if (!RemoteObjectManagerImpl.this.objectLookupStates.containsKey(oid)) {
          removed(oid);
        }
        this.lruOids.remove(oid);
      }

      if (ENABLE_LOGGING) {
        RemoteObjectManagerImpl.this.logger.info("DNA LRU remove 1 batch containing " + oidsToRemove.size() + " DNAs");
      }
    }

    private Set<ObjectID> removeFirstBatchToClear() {
      Iterator<Entry<Long, Set<ObjectID>>> i = this.batches.entrySet().iterator();
      Set<ObjectID> oidsToRemove = i.next().getValue();
      i.remove();
      return oidsToRemove;
    }
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.duplicateAndIndent().indent().print(getClass().getSimpleName()).flush();
    out.duplicateAndIndent().indent().print(this.groupID).flush();
    out.duplicateAndIndent().indent().print("dnaCache:").visit(this.dnaCache).flush();
    StringBuilder strBuffer = new StringBuilder();
    out.duplicateAndIndent().indent().print("pending objects:").print(strBuffer.toString()).flush();

    //printing this.objectLookupStates.toString() as PrettyPrinter prints the size of the map otherwise
    out.duplicateAndIndent().indent().print("lookupstates:").print(this.objectLookupStates.toString()).flush();
    return out;
  }
}
