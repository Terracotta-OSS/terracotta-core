/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.util.State;
import com.tc.util.Util;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class is a kludge but I think it will do the trick for now. It is responsible for any communications to the
 * server for object retrieval and removal
 * 
 * @author steve
 */
public class RemoteObjectManagerImpl implements RemoteObjectManager {

  private static final State                       PAUSED                    = new State("PAUSED");
  private static final State                       STARTING                  = new State("STARTING");
  private static final State                       RUNNING                   = new State("RUNNING");

  private final LinkedHashMap                      rootRequests              = new LinkedHashMap();
  private final Map                                dnaRequests               = new THashMap();
  private final Set                                removeObjects             = new THashSet(100, 0.8f);
  private final Map                                outstandingObjectRequests = new THashMap();
  private final Map                                outstandingRootRequests   = new THashMap();
  private long                                     objectRequestIDCounter    = 0;
  private final ObjectRequestMonitor               requestMonitor;
  private final ChannelIDProvider                  cip;
  private final RequestRootMessageFactory          rrmFactory;
  private final RequestManagedObjectMessageFactory rmomFactory;
  private final DNALRU                             lruDNA                    = new DNALRU();
  private final static int                         MAX_LRU                   = 60;
  private final int                                defaultDepth;
  private State                                    state                     = RUNNING;
  private final SessionManager                     sessionManager;
  private final TCLogger                           logger;
  private static final int                         REMOVE_OBJECTS_THRESHOLD  = 10000;

  public RemoteObjectManagerImpl(TCLogger logger, ChannelIDProvider cip, RequestRootMessageFactory rrmFactory,
                                 RequestManagedObjectMessageFactory rmomFactory, ObjectRequestMonitor requestMonitor,
                                 int defaultDepth, SessionManager sessionManager) {
    this.logger = logger;
    this.cip = cip;
    this.rrmFactory = rrmFactory;
    this.rmomFactory = rmomFactory;
    this.requestMonitor = requestMonitor;
    this.defaultDepth = defaultDepth;
    this.sessionManager = sessionManager;
  }

  public synchronized void pause() {
    assertNotPaused("Attempt to pause while PAUSED");
    state = PAUSED;
    notifyAll();
  }

  public synchronized void starting() {
    assertPaused("Attempt to start while not PAUSED");
    state = STARTING;
    notifyAll();
  }

  public synchronized void unpause() {
    assertStarting("Attempt to unpause while not STARTING");
    state = RUNNING;
    notifyAll();
  }

  public synchronized void clear() {
    if (state != STARTING) throw new AssertionError("Attempt to clear while not STARTING: " + state);
    lruDNA.clear();
    for (Iterator i = dnaRequests.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      if (e.getValue() != null) {
        i.remove();
      }
    }
    removeObjects.clear();
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (state != RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private void assertPaused(Object message) {
    if (state != PAUSED) throw new AssertionError(message + ": " + state);
  }

  private void assertStarting(Object message) {
    if (state != STARTING) throw new AssertionError(message + ": " + state);
  }

  private void assertNotPaused(Object message) {
    if (state == PAUSED) throw new AssertionError(message + ": " + state);
  }

  public synchronized void requestOutstanding() {
    assertStarting("Attempt to request outstanding object requests while not STARTING");
    for (Iterator i = outstandingObjectRequests.values().iterator(); i.hasNext();) {
      RequestManagedObjectMessage rmom = createRequestManagedObjectMessage((ObjectRequestContext) i.next(),
                                                                           Collections.EMPTY_SET);
      rmom.send();
    }
    for (Iterator i = outstandingRootRequests.values().iterator(); i.hasNext();) {
      RequestRootMessage rrm = createRootMessage((String) i.next());
      rrm.send();
    }
  }

  public DNA retrieve(ObjectID id) {
    return retrieve(id, defaultDepth);
  }

  public synchronized DNA retrieve(ObjectID id, int depth) {
    boolean isInterrupted = false;
    
    ObjectRequestContext ctxt = new ObjectRequestContextImpl(this.cip.getChannelID(),
                                                             new ObjectRequestID(objectRequestIDCounter++), id, depth);
    while (!dnaRequests.containsKey(id) || dnaRequests.get(id) == null) {
      waitUntilRunning();
      if (!dnaRequests.containsKey(id)) {
        sendRequest(ctxt);
      } else if (!outstandingObjectRequests.containsKey(id)) {
        outstandingObjectRequests.put(id, ctxt);
      }

      if (dnaRequests.get(id) == null) {
        try {
          wait();
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    lruDNA.remove(id);
    return (DNA) dnaRequests.remove(id);
  }

  private void sendRequest(ObjectRequestContext ctxt) {
    Set tr = new HashSet(removeObjects);
    RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(ctxt, tr);
    removeObjects.clear();
    ObjectID id = null;
    for (Iterator i = ctxt.getObjectIDs().iterator(); i.hasNext();) {
      id = (ObjectID) i.next();
      dnaRequests.put(id, null);
    }
    // XXX:: This is a little weird that we add only the last ObjectID to the outstandingObjectRequests map
    // when we add all the list of ObjectIDs to dnaRequests. This is done so that we only send the request once
    // on resend. Since the only way we request for more than one ObjectID in 1 message is when someone initiate
    // non-blocking lookups. So if we loose those requests on restart it is still ok.
    this.outstandingObjectRequests.put(id, ctxt);
    rmom.send();
    requestMonitor.notifyObjectRequest(ctxt);
  }

  private RequestManagedObjectMessage createRequestManagedObjectMessage(ObjectRequestContext ctxt, Set removed) {
    RequestManagedObjectMessage rmom = rmomFactory.newRequestManagedObjectMessage();
    Set requestedObjectIDs = ctxt.getObjectIDs();
    rmom.initialize(ctxt, requestedObjectIDs, removed);
    return rmom;
  }

  public synchronized ObjectID retrieveRootID(String name) {

    if (!rootRequests.containsKey(name)) {
      RequestRootMessage rrm = createRootMessage(name);
      rootRequests.put(name, ObjectID.NULL_ID);
      outstandingRootRequests.put(name, name);
      rrm.send();
    }

    boolean isInterrupted = false;
    while (ObjectID.NULL_ID.equals(rootRequests.get(name))) {
      waitUntilRunning();
      try {
        if (ObjectID.NULL_ID.equals(rootRequests.get(name))) {
          wait();
        }
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);

    return (ObjectID) (rootRequests.containsKey(name) ? rootRequests.get(name) : ObjectID.NULL_ID);
  }

  private RequestRootMessage createRootMessage(String name) {
    RequestRootMessage rrm = rrmFactory.newRequestRootMessage();
    rrm.initialize(name);
    return rrm;
  }

  public synchronized void addRoot(String name, ObjectID id) {
    waitUntilRunning();
    if (id.isNull()) {
      rootRequests.remove(name);
    } else {
      rootRequests.put(name, id);
    }
    Object rootName = outstandingRootRequests.remove(name);
    if (rootName == null) {
      // This is possible in some restart scenario
      logger.warn("A root was added that was not found in the outstanding requests. root name = " + name + " " + id);
    }
    notifyAll();
  }

  public synchronized void addAllObjects(SessionID sessionID, long batchID, Collection dnas) {
    waitUntilRunning();
    if (!sessionManager.isCurrentSession(sessionID)) {
      logger.warn("Ignoring DNA added from a different session: " + sessionID + ", " + sessionManager);
      return;
    }
    lruDNA.clearUnrequestedDNA();
    lruDNA.add(batchID, dnas);
    for (Iterator i = dnas.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      // The server should not send us any objects that the server thinks we still have.
      if (removeObjects.contains(dna.getObjectID())) {
        // formatting
        throw new AssertionError("Server sent us an object that is present in the removed set - " + dna.getObjectID()
                                 + " , removed set = " + removeObjects);
      }
      basicAddObject(dna);
    }
    notifyAll();
  }

  // Used only for testing
  synchronized void addObject(DNA dna) {
    if (!removeObjects.contains(dna.getObjectID())) basicAddObject(dna);
    notifyAll();
  }

  private void basicAddObject(DNA dna) {
    dnaRequests.put(dna.getObjectID(), dna);
    outstandingObjectRequests.remove(dna.getObjectID());
  }

  public synchronized void removed(ObjectID id) {
    dnaRequests.remove(id);
    removeObjects.add(id);
    if (removeObjects.size() > REMOVE_OBJECTS_THRESHOLD) {
      ObjectRequestContext ctxt = new ObjectRequestContextImpl(this.cip.getChannelID(),
                                                               new ObjectRequestID(objectRequestIDCounter++),
                                                               Collections.EMPTY_SET, -1);
      Set tr = new HashSet(removeObjects);
      RequestManagedObjectMessage rmom = createRequestManagedObjectMessage(ctxt, tr);
      removeObjects.clear();
      rmom.send();
    }
  }

  public class ObjectRequestContextImpl implements ObjectRequestContext {

    private final long            timestamp;

    private final Set             objectIDs;

    private final ObjectRequestID requestID;

    private final ChannelID       channelID;

    private final int             depth;

    private ObjectRequestContextImpl(ChannelID channelID, ObjectRequestID requestID, ObjectID objectID, int depth) {
      this(channelID, requestID, new HashSet(), depth);
      this.objectIDs.add(objectID);
    }

    private ObjectRequestContextImpl(ChannelID channelID, ObjectRequestID requestID, Set objectIDs, int depth) {
      this.timestamp = System.currentTimeMillis();
      this.channelID = channelID;
      this.requestID = requestID;
      this.objectIDs = objectIDs;
      this.depth = depth;
    }

    public ChannelID getChannelID() {
      return this.channelID;
    }

    public ObjectRequestID getRequestID() {
      return this.requestID;
    }

    public Set getObjectIDs() {
      return this.objectIDs;
    }

    public int getRequestDepth() {
      return this.depth;
    }

    public String toString() {
      return getClass().getName() + "[" + new Date(timestamp) + ", requestID =" + requestID + ", objectIDs ="
             + objectIDs + ", depth = " + depth + "]";
    }
  }

  private class DNALRU {
    private LinkedHashMap dnas = new LinkedHashMap();

    public synchronized int size() {
      return dnas.size();
    }

    public synchronized void clear() {
      dnas.clear();
    }

    public synchronized void add(long batchID, Collection objs) {
      Long key = new Long(batchID);
      Map m = (Map) dnas.get(key);
      if (m == null) {
        // XXX:: We are creating a Map with initial size equals objs.size() but there could be more to come !
        // Revisit !!
        m = new THashMap(objs.size(), 0.8f);
        dnas.put(key, m);
      }
      for (Iterator i = objs.iterator(); i.hasNext();) {
        DNA dna = (DNA) i.next();
        m.put(dna.getObjectID(), dna);
      }
    }

    public synchronized void remove(ObjectID id) {
      for (Iterator i = dnas.values().iterator(); i.hasNext();) {
        Map m = (Map) i.next();
        if (m.remove(id) != null) {
          // found !!!
          break;
        }
      }
    }

    public synchronized void clearUnrequestedDNA() {
      if (dnas.size() > MAX_LRU) {
        Iterator dnaMapIterator = dnas.values().iterator();
        Map dnaMap = (Map) dnaMapIterator.next();
        for (Iterator i = dnaMap.keySet().iterator(); i.hasNext();) {
          ObjectID id = (ObjectID) i.next();
          if (!outstandingObjectRequests.containsKey(id)) {
            // only include this ID in the removed set if this DNA has never left the request map.
            // If it has left the map, this client is actually be referencing this object
            if (dnaRequests.containsKey(id)) {
              removed(id);
            }
          }
        }
        dnaMapIterator.remove();
      }
    }
  }

}
