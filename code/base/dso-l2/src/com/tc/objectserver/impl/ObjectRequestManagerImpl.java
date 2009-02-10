/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ObjectRequestManagerImpl implements ObjectRequestManager {

  public static final int           SPLIT_SIZE      = TCPropertiesImpl
                                                        .getProperties()
                                                        .getInt(
                                                                TCPropertiesConsts.L2_OBJECTMANAGER_OBJECT_REQUEST_SPLIT_SIZE);
  public static final boolean       LOGGING_ENABLED = TCPropertiesImpl
                                                        .getProperties()
                                                        .getBoolean(
                                                                    TCPropertiesConsts.L2_OBJECTMANAGER_OBJECT_REQUEST_LOGGING_ENABLED);

  private final static TCLogger     logger          = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final ObjectManager       objectManager;

  private final Sink                respondObjectRequestSink;
  private final Sink                objectRequestSink;

  private final DSOChannelManager   channelManager;
  private final ClientStateManager  stateManager;
  private final Sequence            batchIDSequence = new SimpleSequence();
  private final ObjectRequestCache  objectRequestCache;

  private final ObjectStatsRecorder objectStatsRecorder;

  public ObjectRequestManagerImpl(ObjectManager objectManager, DSOChannelManager channelManager,
                                  ClientStateManager stateManager, Sink objectRequestSink,
                                  Sink respondObjectRequestSink, ObjectStatsRecorder objectStatsRecorder) {
    this.objectManager = objectManager;
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.respondObjectRequestSink = respondObjectRequestSink;
    this.objectRequestSink = objectRequestSink;
    this.objectRequestCache = new ObjectRequestCache(LOGGING_ENABLED);
    this.objectStatsRecorder = objectStatsRecorder;
  }

  public void requestObjects(ObjectRequestServerContext requestContext) {
    splitAndRequestObjects(requestContext.getClientID(), requestContext.getRequestID(), requestContext
        .getRequestedObjectIDs(), requestContext.getRequestDepth(), requestContext.isServerInitiated(), requestContext
        .getRequestingThreadName());
  }

  private void splitAndRequestObjects(ClientID clientID, ObjectRequestID requestID, ObjectIDSet ids,
                                      int maxRequestDepth, boolean serverInitiated, String requestingThreadName) {
    ObjectIDSet split = new ObjectIDSet();

    for (Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      split.add(iter.next());
      if (split.size() >= SPLIT_SIZE || !iter.hasNext()) {
        basicRequestObjects(clientID, requestID, maxRequestDepth, serverInitiated, requestingThreadName, split);
        split = new ObjectIDSet();
      }
    }

  }

  private void basicRequestObjects(ClientID clientID, ObjectRequestID requestID, int maxRequestDepth,
                                   boolean serverInitiated, String requestingThreadName, ObjectIDSet split) {

    LookupContext lookupContext = null;

    synchronized (this) {
      RequestedObject reqObj = new RequestedObject(split, maxRequestDepth);
      if (this.objectRequestCache.add(reqObj, clientID)) {
        lookupContext = new LookupContext(clientID, requestID, split, maxRequestDepth, requestingThreadName,
                                          serverInitiated, this.objectRequestSink, this.respondObjectRequestSink);
      }
    }

    if (lookupContext != null) {
      this.objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, maxRequestDepth);
    }
  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {

    // Create ordered list of objects
    LinkedList objectsInOrder = new LinkedList();
    Set ids = new HashSet(Math.max((int) (objs.size() / .75f) + 1, 16));
    for (Iterator i = objs.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      ids.add(mo.getID());
      if (requestedObjectIDs.contains(mo.getID())) {
        objectsInOrder.addLast(mo);
      } else {
        objectsInOrder.addFirst(mo);
      }
    }

    // Create map of clients and objects to be sent
    Set<ClientID> clientList = null;
    long batchID = this.batchIDSequence.next();
    RequestedObject reqObj = new RequestedObject(requestedObjectIDs, maxRequestDepth);
    synchronized (this) {
      clientList = this.objectRequestCache.remove(reqObj);
    }

    Map<ClientID, Set<ObjectID>> clientNewIDsMap = new HashMap<ClientID, Set<ObjectID>>();
    Map<ClientID, BatchAndSend> messageMap = new HashMap<ClientID, BatchAndSend>();

    for (Iterator iter = clientList.iterator(); iter.hasNext();) {
      ClientID clientID = (ClientID) iter.next();
      try {
        // make batch and send object for each client.
        MessageChannel channel = this.channelManager.getActiveChannel(clientID);
        messageMap.put(clientID, new BatchAndSend(channel, batchID));
        // get set of objects which are not present in the client out of the returned ones
        Set newIds = this.stateManager.addReferences(clientID, ids);
        clientNewIDsMap.put(clientID, newIds);
      } catch (NoSuchChannelException e) {
        logger.warn("Not sending objects to client " + clientID + ": " + e);
      }
    }

    // send objects to each client
    if (!messageMap.isEmpty()) {
      boolean requestDebug = this.objectStatsRecorder.getRequestDebug();

      for (Iterator<Map.Entry<ClientID, Set<ObjectID>>> i = clientNewIDsMap.entrySet().iterator(); i.hasNext();) {
        Map.Entry<ClientID, Set<ObjectID>> entry = i.next();
        final boolean isLast = !i.hasNext();
        ClientID clientID = entry.getKey();
        Set newIDs = entry.getValue();
        BatchAndSend batchAndSend = messageMap.get(clientID);

        for (Iterator iter = objectsInOrder.iterator(); iter.hasNext();) {
          ManagedObject mo = (ManagedObject) iter.next();
          if (newIDs.contains(mo.getID())) {
            batchAndSend.sendObject(mo);
            if (requestDebug) {
              updateStats(mo);
            }
          }
          if (isLast) {
            this.objectManager.releaseReadOnly(mo);
          }
        }
      }

      if (!missingObjectIDs.isEmpty()) {
        if (isServerInitiated) {
          // This is a possible case where changes are flying in and server is initiating some lookups and the lookups
          // go pending and in the meantime the changes made those looked up objects garbage and DGC removes those
          // objects. Now we dont want to send those missing objects to clients. Its not really an issue as the
          // clients should never lookup those objects, but still why send them ?
          logger.warn("Server Initiated lookup. Ignoring Missing Objects : " + missingObjectIDs);
        } else {
          for (Iterator<Map.Entry<ClientID, BatchAndSend>> missingIterator = messageMap.entrySet().iterator(); missingIterator
              .hasNext();) {
            Map.Entry<ClientID, BatchAndSend> entry = missingIterator.next();
            ClientID clientID = entry.getKey();
            BatchAndSend batchAndSend = entry.getValue();
            logger.warn("Sending missing ids: " + missingObjectIDs.size() + " , to client: " + clientID);
            batchAndSend.sendMissingObjects(missingObjectIDs);
          }
        }
      }

      for (Iterator<BatchAndSend> iterator = messageMap.values().iterator(); iterator.hasNext();) {
        BatchAndSend batchAndSend = iterator.next();
        batchAndSend.flush();
      }
    } else {
      // no connected clients to send to
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {
        this.objectManager.releaseReadOnly((ManagedObject) i.next());
      }
    }
  }

  protected synchronized int getTotalRequestedObjects() {
    return this.objectRequestCache.numberOfRequestedObjects();
  }

  protected synchronized int getObjectRequestCacheClientSize() {
    return this.objectRequestCache.clientSize();
  }

  private void updateStats(ManagedObject mo) {
    String className = mo.getManagedObjectState().getClassName();
    if (className == null) {
      className = "UNKNOWN";
    }
    this.objectStatsRecorder.updateRequestStats(className);
  }

  protected static class RequestedObject {

    private final ObjectIDSet oidSet;

    private final int         depth;

    public RequestedObject(ObjectIDSet oidSet, int depth) {
      this.oidSet = oidSet;
      this.depth = depth;
    }

    public ObjectIDSet getOIdSet() {
      return this.oidSet;
    }

    public int getDepth() {
      return this.depth;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RequestedObject)) { return false; }
      RequestedObject reqObj = (RequestedObject) obj;
      if (this.oidSet.equals(reqObj.getOIdSet()) && this.depth == reqObj.getDepth()) { return true; }
      return false;
    }

    @Override
    public int hashCode() {
      return this.oidSet.hashCode();
    }

    @Override
    public String toString() {
      return "RequestedObject [ " + this.oidSet + ", depth = " + this.depth + " ] ";
    }
  }

  protected static class ObjectRequestCache {

    private final Map<RequestedObject, LinkedHashSet<ClientID>> objectRequestMap = new HashMap<RequestedObject, LinkedHashSet<ClientID>>();

    private final boolean                                       verbose;

    private int                                                 multipleClientRequestCount;

    private int                                                 sameClientRequestCount;

    public ObjectRequestCache(boolean verbose) {
      this.verbose = verbose;
    }

    // for tests
    protected int numberOfRequestedObjects() {
      int val = 0;
      for (Iterator iter = this.objectRequestMap.keySet().iterator(); iter.hasNext();) {
        val += ((RequestedObject) iter.next()).getOIdSet().size();
      }
      return val;
    }

    // for tests
    protected int cacheSize() {
      return this.objectRequestMap.size();
    }

    // for tests
    protected int clientSize() {
      return clients().size();
    }

    // for tests
    protected Set<ClientID> clients() {
      Set<ClientID> clients = new LinkedHashSet<ClientID>();
      for (Iterator<LinkedHashSet<ClientID>> i = this.objectRequestMap.values().iterator(); i.hasNext();) {
        clients.addAll(i.next());
      }
      return clients;
    }

    // TODO: put multiple clients, and single client request, print every 100 request
    public boolean add(RequestedObject reqObjects, ClientID clientID) {
      // check already been requested.

      LinkedHashSet<ClientID> clientList = this.objectRequestMap.get(reqObjects);
      if (clientList == null) {
        clientList = new LinkedHashSet<ClientID>();
        clientList.add(clientID);
        this.objectRequestMap.put(reqObjects, clientList);
        return true;
      } else {

        if (clientList.add(clientID)) {
          if (this.verbose && (++this.sameClientRequestCount % 100) == 0) {
            // The count keeps track of how many requests were made from the same client that has already been requested
            logger.info("[ObjectRequestCache] same client already made requests. total same client request optimized: "
                        + this.sameClientRequestCount);
          }
        } else {
          if (this.verbose && (++this.multipleClientRequestCount % 100) == 0) {
            // The count keeps track of how many requests were made from the multiple clients that has already been
            // requested
            logger
                .info("[ObjectRequestCache] multiple clients already made requests. total multiple clients request optimized: "
                      + this.multipleClientRequestCount);
          }
        }
        return false;
      }
    }

    public boolean contains(RequestedObject reqObj) {
      return this.objectRequestMap.containsKey(reqObj);
    }

    public Set<ClientID> getClientsForRequest(RequestedObject reqObj) {
      return this.objectRequestMap.get(reqObj);
    }

    public Set<ClientID> remove(RequestedObject reqObj) {
      return this.objectRequestMap.remove(reqObj);
    }
  }

  protected static class BatchAndSend {

    private final MessageChannel     channel;
    private final long               batchID;

    private Integer                  sendCount  = 0;
    private Integer                  batches    = 0;
    private ObjectStringSerializer   serializer = new ObjectStringSerializer();
    private TCByteBufferOutputStream out        = new TCByteBufferOutputStream();

    public BatchAndSend(MessageChannel channel, long batchID) {
      this.channel = channel;
      this.batchID = batchID;
    }

    public void sendObject(ManagedObject m) {
      m.toDNA(this.out, this.serializer);
      this.sendCount++;
      if (this.sendCount > 1000) {
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) this.channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(this.out.toArray(), this.sendCount, this.serializer, this.batchID, this.batches++);
        responseMessage.send();
        this.sendCount = 0;
        this.serializer = new ObjectStringSerializer();
        this.out = new TCByteBufferOutputStream();
      }
    }

    public void flush() {
      if (this.sendCount > 0) {
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) this.channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(this.out.toArray(), this.sendCount, this.serializer, this.batchID, 0);
        responseMessage.send();
        this.sendCount = 0;
        this.serializer = new ObjectStringSerializer();
        this.out = new TCByteBufferOutputStream();
      }
    }

    public void sendMissingObjects(Set missingObjectIDs) {

      if (missingObjectIDs.size() > 0) {
        ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) this.channel
            .createMessage(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE);
        notFound.initialize(missingObjectIDs, this.batchID);
        notFound.send();
      }
    }

    protected int getSendCount() {
      return this.sendCount;
    }

    protected int getBatches() {
      return this.batches;
    }

    protected ObjectStringSerializer getSerializer() {
      return this.serializer;
    }

    protected TCByteBufferOutputStream getOut() {
      return this.out;
    }

  }

  protected static class LookupContext implements ObjectManagerResultsContext {

    private final ClientID        clientID;
    private final ObjectRequestID requestID;
    private final ObjectIDSet     lookupIDs;
    private final int             maxRequestDepth;
    private final String          requestingThreadName;
    private final boolean         serverInitiated;
    private final Sink            respondObjectRequestSink;
    private final Sink            objectRequestSink;

    private ObjectIDSet           missingObjects;
    private Map                   objects;

    public LookupContext(ClientID clientID, ObjectRequestID requestID, ObjectIDSet lookupIDs, int maxRequestDepth,
                         String requestingThreadName, boolean serverInitiated, Sink objectRequestSink,
                         Sink respondObjectRequestSink) {
      this.clientID = clientID;
      this.requestID = requestID;
      this.lookupIDs = lookupIDs;
      this.maxRequestDepth = maxRequestDepth;
      this.requestingThreadName = requestingThreadName;
      this.serverInitiated = serverInitiated;
      this.objectRequestSink = objectRequestSink;
      this.respondObjectRequestSink = respondObjectRequestSink;

    }

    public ObjectIDSet getLookupIDs() {
      return this.lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      return new ObjectIDSet();
    }

    public void setResults(ObjectManagerLookupResults results) {
      this.objects = results.getObjects();
      this.missingObjects = results.getMissingObjectIDs();

      if (results.getLookupPendingObjectIDs().size() > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("LookupPendingObjectIDs = " + results.getLookupPendingObjectIDs() + " , clientID = "
                       + this.clientID + " , requestID = " + this.requestID);
        }
        this.objectRequestSink.add(new ObjectRequestServerContextImpl(this.clientID, this.requestID, results
            .getLookupPendingObjectIDs(), this.requestingThreadName, -1, true));
      }
      ResponseContext responseContext = new ResponseContext(this.clientID, this.objects.values(), this.lookupIDs,
                                                            this.missingObjects, this.serverInitiated,
                                                            this.maxRequestDepth);
      this.respondObjectRequestSink.add(responseContext);
      if (logger.isDebugEnabled()) {
        logger.debug("Adding to respondSink , clientID = " + this.clientID + " , requestID = " + this.requestID + " "
                     + responseContext);
      }
    }

    public ObjectRequestID getRequestID() {
      return this.requestID;
    }

    public int getMaxRequestDepth() {
      return this.maxRequestDepth;
    }

    public boolean updateStats() {
      return true;
    }

    public boolean isServerInitiated() {
      return this.serverInitiated;
    }

    public ClientID getRequestedNodeID() {
      return this.clientID;
    }

    public String getRequestingThreadName() {
      return this.requestingThreadName;
    }

    @Override
    public String toString() {
      return "Lookup Context@" + System.identityHashCode(this) + "[ clientID = " + this.clientID + " , requestID = "
             + this.requestID + " , ids = " + this.lookupIDs + " , lookedup objects.size() = "
             + (this.objects != null ? this.objects.size() : 0) + " , missingObjects  = " + this.missingObjects
             + " , maxRequestDepth = " + this.maxRequestDepth + " , requestingThreadName = "
             + this.requestingThreadName + " , serverInitiated = " + this.serverInitiated
             + " , respondObjectRequestSink = " + this.respondObjectRequestSink + " ] ";
    }
  }

  protected static class ResponseContext implements RespondToObjectRequestContext {

    private final ClientID    requestedNodeID;
    private final Collection  objs;
    private final ObjectIDSet requestedObjectIDs;
    private final ObjectIDSet missingObjectIDs;
    private final boolean     serverInitiated;
    private final int         maxRequestDepth;

    public ResponseContext(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                           ObjectIDSet missingObjectIDs, boolean serverInitiated, int maxDepth) {
      this.requestedNodeID = requestedNodeID;
      this.objs = objs;
      this.requestedObjectIDs = requestedObjectIDs;
      this.missingObjectIDs = missingObjectIDs;
      this.serverInitiated = serverInitiated;
      this.maxRequestDepth = maxDepth;
    }

    public ClientID getRequestedNodeID() {
      return this.requestedNodeID;
    }

    public Collection getObjs() {
      return this.objs;
    }

    public ObjectIDSet getRequestedObjectIDs() {
      return this.requestedObjectIDs;
    }

    public ObjectIDSet getMissingObjectIDs() {
      return this.missingObjectIDs;
    }

    public boolean isServerInitiated() {
      return this.serverInitiated;
    }

    public int getRequestDepth() {
      return this.maxRequestDepth;
    }

    @Override
    public String toString() {

      return "ResponseContext [ requestNodeID = " + this.requestedNodeID + " , objs.size = " + this.objs.size()
             + " , requestedObjectIDs = " + this.requestedObjectIDs + " , missingObjectIDs = " + this.missingObjectIDs
             + " , serverInitiated = " + this.serverInitiated + " ] ";
    }

  }

}
