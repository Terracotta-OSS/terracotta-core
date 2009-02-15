/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNA;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.TCCollections;
import com.tc.util.Util;

import gnu.trove.THashMap;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class is responsible for any communications to the server for object retrieval and removal
 */
public class RemoteObjectManagerImpl implements RemoteObjectManager, ClientHandshakeCallback {

  private static final State                       PAUSED                    = new State("PAUSED");
  private static final State                       RUNNING                   = new State("RUNNING");

  private final LinkedHashMap                      rootRequests              = new LinkedHashMap();
  private final Map                                dnaRequests               = new HashMap();
  private final Map                                outstandingObjectRequests = new HashMap();
  private final Map                                outstandingRootRequests   = new HashMap();
  private final Set                                missingObjectIDs          = new HashSet();
  private long                                     objectRequestIDCounter    = 0;
  private final ObjectRequestMonitor               requestMonitor;
  private final ClientIDProvider                   cip;
  private final RequestRootMessageFactory          rrmFactory;
  private final RequestManagedObjectMessageFactory rmomFactory;
  private final DNALRU                             lruDNA                    = new DNALRU();
  private final static int                         MAX_LRU                   = TCPropertiesImpl
                                                                                 .getProperties()
                                                                                 .getInt(
                                                                                         TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE);
  private final static boolean                     ENABLE_LOGGING            = TCPropertiesImpl
                                                                                 .getProperties()
                                                                                 .getBoolean(
                                                                                             TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED);
  private final GroupID                            groupID;
  private final int                                defaultDepth;
  private State                                    state                     = RUNNING;
  private ObjectIDSet                              removeObjects             = new ObjectIDSet();
  private final SessionManager                     sessionManager;
  private final TCLogger                           logger;
  private static final int                         REMOVE_OBJECTS_THRESHOLD  = 10000;
  private long                                     hit                       = 0;
  private long                                     miss                      = 0;

  public RemoteObjectManagerImpl(GroupID groupID, TCLogger logger, ClientIDProvider cip,
                                 RequestRootMessageFactory rrmFactory, RequestManagedObjectMessageFactory rmomFactory,
                                 ObjectRequestMonitor requestMonitor, int defaultDepth, SessionManager sessionManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.cip = cip;
    this.rrmFactory = rrmFactory;
    this.rmomFactory = rmomFactory;
    this.requestMonitor = requestMonitor;
    this.defaultDepth = defaultDepth;
    this.sessionManager = sessionManager;
  }

  public synchronized void pause(NodeID remote, int disconnected) {
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = PAUSED;
    // XXX:: We are clearing unmaterialized DNAs and removed objects here because on connect we are going to send
    // the list of objects present in this L1 from Client Object Manager anyways. We can't be clearing the removed
    // object IDs in unpause(), then you get MNK-835
    clear();
    notifyAll();
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    // NOP
  }

  public synchronized void unpause(NodeID remote, int disconnected) {
    assertPaused("Attempt to unpause while not PAUSED");
    this.state = RUNNING;
    requestOutstanding();
    notifyAll();
  }

  public synchronized void clear() {
    this.lruDNA.clear();
    for (Iterator i = this.dnaRequests.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      if (e.getValue() != null) {
        i.remove();
      }
    }
    this.removeObjects.clear();
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (this.state != RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private void assertPaused(Object message) {
    if (this.state != PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotPaused(Object message) {
    if (this.state == PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  synchronized void requestOutstanding() {
    for (Iterator i = this.outstandingObjectRequests.values().iterator(); i.hasNext();) {
      RequestManagedObjectMessage rmom = createRequestManagedObjectMessage((ObjectRequestContext) i.next());
      rmom.send();
    }
    for (Iterator i = this.outstandingRootRequests.values().iterator(); i.hasNext();) {
      RequestRootMessage rrm = createRootMessage((String) i.next());
      rrm.send();
    }
  }

  public DNA retrieve(ObjectID id) {
    return basicRetrieve(id, this.defaultDepth, ObjectID.NULL_ID);
  }

  public DNA retrieveWithParentContext(ObjectID id, ObjectID parentContext) {
    return basicRetrieve(id, this.defaultDepth, parentContext);
  }

  public DNA retrieve(ObjectID id, int depth) {
    return basicRetrieve(id, depth, ObjectID.NULL_ID);
  }

  public synchronized DNA basicRetrieve(ObjectID id, int depth, ObjectID parentContext) {
    boolean isInterrupted = false;
    if (id.getGroupID() != this.groupID.toInt()) {
      //
      throw new AssertionError("Looking up in the wrong Remote Manager : " + this.groupID + " id : " + id + " depth : "
                               + depth + " parent : " + parentContext);
    }
    ObjectRequestContext ctxt = new ObjectRequestContextImpl(this.cip.getClientID(),
                                                             new ObjectRequestID(this.objectRequestIDCounter++), id,
                                                             depth, parentContext);
    boolean inMemory = true;
    while (!this.dnaRequests.containsKey(id) || this.dnaRequests.get(id) == null || this.missingObjectIDs.contains(id)) {
      waitUntilRunning();
      if (this.missingObjectIDs.contains(id)) {
        throw new TCObjectNotFoundException(id.toString(), this.missingObjectIDs);
      } else if (!this.dnaRequests.containsKey(id)) {
        inMemory = false;
        sendRequest(ctxt);
      } else if (!this.outstandingObjectRequests.containsKey(id)) {
        this.outstandingObjectRequests.put(id, ctxt);
      }

      if (this.dnaRequests.get(id) == null) {
        try {
          wait();
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    this.lruDNA.remove(id);
    if (inMemory) {
      this.hit++;
    } else {
      this.miss++;
    }
    if (ENABLE_LOGGING && ((this.hit + this.miss) % 1000 == 0)) {
      this.logger.info("Cache Hit : Miss ratio = " + this.hit + "  : " + this.miss);
    }
    return (DNA) this.dnaRequests.remove(id);
  }

  private void sendRequest(ObjectRequestContext ctxt) {
    RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(ctxt);
    ObjectID id = null;
    for (Iterator i = ctxt.getRequestedObjectIDs().iterator(); i.hasNext();) {
      id = (ObjectID) i.next();
      this.dnaRequests.put(id, null);
    }
    // XXX:: This is a little weird that we add only the last ObjectID to the outstandingObjectRequests map
    // when we add all the list of ObjectIDs to dnaRequests. This is done so that we only send the request once
    // on resend. Since the only way we request for more than one ObjectID in 1 message is when someone initiate
    // non-blocking lookups. So if we loose those requests on restart it is still ok.
    this.outstandingObjectRequests.put(id, ctxt);
    rmom.send();
    this.requestMonitor.notifyObjectRequest(ctxt);
  }

  private RequestManagedObjectMessage createRequestManagedObjectMessage(ObjectRequestContext ctxt) {
    RequestManagedObjectMessage rmom = this.rmomFactory.newRequestManagedObjectMessage(this.groupID);
    Set<ObjectID> requestedObjectIDs = ctxt.getRequestedObjectIDs();
    if (this.removeObjects.isEmpty()) {
      rmom.initialize(ctxt, requestedObjectIDs, TCCollections.EMPTY_OBJECT_ID_SET);
    } else {
      rmom.initialize(ctxt, requestedObjectIDs, this.removeObjects);
      this.removeObjects = new ObjectIDSet();
    }
    return rmom;
  }

  public synchronized ObjectID retrieveRootID(String name) {

    if (!this.rootRequests.containsKey(name)) {
      RequestRootMessage rrm = createRootMessage(name);
      this.rootRequests.put(name, ObjectID.NULL_ID);
      this.outstandingRootRequests.put(name, name);
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

    return (ObjectID) (this.rootRequests.containsKey(name) ? this.rootRequests.get(name) : ObjectID.NULL_ID);
  }

  private RequestRootMessage createRootMessage(String name) {
    RequestRootMessage rrm = this.rrmFactory.newRequestRootMessage(this.groupID);
    rrm.initialize(name);
    return rrm;
  }

  public synchronized void addRoot(String name, ObjectID id, NodeID nodeID) {
    waitUntilRunning();
    if (id.isNull()) {
      this.rootRequests.remove(name);
    } else {
      this.rootRequests.put(name, id);
    }
    Object rootName = this.outstandingRootRequests.remove(name);
    if (rootName == null) {
      // This is possible in some restart scenario
      this.logger.warn("A root was added that was not found in the outstanding requests. root name = " + name + " "
                       + id);
    }
    notifyAll();
  }

  public synchronized void addAllObjects(SessionID sessionID, long batchID, Collection dnas, NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring DNA added from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    this.lruDNA.clearUnrequestedDNA();
    this.lruDNA.add(batchID, dnas);
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

  public synchronized void objectsNotFoundFor(SessionID sessionID, long batchID, Set missingOIDs, NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring Missing Object IDs " + missingOIDs + " from a different session: " + sessionID + ", "
                       + this.sessionManager);
      return;
    }
    this.logger.warn("Received Missing Object IDs from server : " + missingOIDs);
    this.missingObjectIDs.addAll(missingOIDs);
    notifyAll();
  }

  // Used only for testing
  synchronized void addObject(DNA dna) {
    if (!this.removeObjects.contains(dna.getObjectID())) {
      basicAddObject(dna);
    }
    notifyAll();
  }

  // Used only for testing
  synchronized int getDNACacheSize() {
    return this.lruDNA.size();
  }

  private void basicAddObject(DNA dna) {
    this.dnaRequests.put(dna.getObjectID(), dna);
    this.outstandingObjectRequests.remove(dna.getObjectID());
  }

  public synchronized void removed(ObjectID id) {
    this.dnaRequests.remove(id);
    this.removeObjects.add(id);
    if (this.removeObjects.size() >= REMOVE_OBJECTS_THRESHOLD) {
      ObjectRequestContext ctxt = new ObjectRequestContextImpl(this.cip.getClientID(),
                                                               new ObjectRequestID(this.objectRequestIDCounter++),
                                                               new ObjectIDSet(), -1);
      RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(ctxt);
      rmom.send();
    }
  }

  public class ObjectRequestContextImpl implements ObjectRequestContext {

    private final long            timestamp;

    private final ObjectIDSet     objectIDs;

    private final ObjectRequestID requestID;

    private final ClientID        clientID;

    private final int             depth;

    private ObjectRequestContextImpl(ClientID clientID, ObjectRequestID requestID, ObjectID objectID, int depth,
                                     ObjectID parentContext) {
      this(clientID, requestID, new ObjectIDSet(), depth);
      this.objectIDs.add(objectID);
      // XXX:: This is a hack for now. This parent context could be exposed to the L2 to make it more elegant.
      if (!parentContext.isNull()) {
        this.objectIDs.add(parentContext);
      }
    }

    private ObjectRequestContextImpl(ClientID clientID, ObjectRequestID requestID, ObjectIDSet objectIDs, int depth) {
      this.timestamp = System.currentTimeMillis();
      this.clientID = clientID;
      this.requestID = requestID;
      this.objectIDs = objectIDs;
      this.depth = depth;
    }

    public ClientID getClientID() {
      return this.clientID;
    }

    public ObjectRequestID getRequestID() {
      return this.requestID;
    }

    public ObjectIDSet getRequestedObjectIDs() {
      return this.objectIDs;
    }

    public int getRequestDepth() {
      return this.depth;
    }

    @Override
    public String toString() {
      return getClass().getName() + "[" + new Date(this.timestamp) + ", requestID =" + this.requestID + ", objectIDs ="
             + this.objectIDs + ", depth = " + this.depth + "]";
    }
  }

  private class DNALRU {
    // TODO:: These two data structure can be merged to one with into a LinkedHashMap with some marker object to
    // identify buckets
    private final LinkedHashMap dnas         = new LinkedHashMap();
    private final HashMap       oids2BatchID = new HashMap();

    public synchronized int size() {
      return this.dnas.size();
    }

    public synchronized void clear() {
      this.dnas.clear();
      this.oids2BatchID.clear();
    }

    public synchronized void add(long batchID, Collection objs) {
      Long key = new Long(batchID);
      Map m = (Map) this.dnas.get(key);
      if (m == null) {
        m = new THashMap(objs.size() * 2, 0.8f);
        this.dnas.put(key, m);
      }
      for (Iterator i = objs.iterator(); i.hasNext();) {
        DNA dna = (DNA) i.next();
        m.put(dna.getObjectID(), dna);
        this.oids2BatchID.put(dna.getObjectID(), key);
      }
    }

    public synchronized void remove(ObjectID id) {
      Long batchID = (Long) this.oids2BatchID.remove(id);
      if (batchID != null) {
        Map m = (Map) this.dnas.get(batchID);
        Object dna = m.remove(id);
        Assert.assertNotNull(dna);
        if (m.isEmpty()) {
          this.dnas.remove(batchID);
        }
      }
    }

    public synchronized void clearUnrequestedDNA() {
      if (this.dnas.size() > MAX_LRU) {
        Iterator dnaMapIterator = this.dnas.values().iterator();
        Map dnaMap = (Map) dnaMapIterator.next();
        int removedDNACount = dnaMap.size();
        for (Iterator i = dnaMap.keySet().iterator(); i.hasNext();) {
          ObjectID id = (ObjectID) i.next();
          if (!RemoteObjectManagerImpl.this.outstandingObjectRequests.containsKey(id)) {
            // only include this ID in the removed set if this DNA has never left the request map.
            // If it has left the map, this client is actually be referencing this object
            if (RemoteObjectManagerImpl.this.dnaRequests.containsKey(id)) {
              removed(id);
            }
          }
          this.oids2BatchID.remove(id);
        }
        dnaMapIterator.remove();
        if (ENABLE_LOGGING) {
          RemoteObjectManagerImpl.this.logger.info("DNA LRU remove 1 map containing " + removedDNACount + " DNAs");
        }
      }
    }
  }
}
