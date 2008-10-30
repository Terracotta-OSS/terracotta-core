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
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.ObjectRequestServerContext;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectRequestManagerImpl implements ObjectRequestManager, ServerTransactionListener {

  public static final int                SPLIT_SIZE           = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(
                                                                          TCPropertiesConsts.L2_OBJECTMANAGER_OBJECT_REQUEST_SPLIT_SIZE);
  public static final boolean            LOGGING_ENABLED      = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getBoolean(
                                                                              TCPropertiesConsts.L2_OBJECTMANAGER_OBJECT_REQUEST_LOGGING_ENABLED);

  private final static TCLogger          logger               = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final static State             INIT                 = new State("INITIAL");
  private final static State             STARTING             = new State("STARTING");
  private final static State             STARTED              = new State("STARTED");

  private final ObjectManager            objectManager;
  private final ServerTransactionManager transactionManager;

  private final List                     pendingRequests      = new LinkedList();
  private final Set                      resentTransactionIDs = new HashSet();
  private final Sink                     respondObjectRequestSink;
  private final Sink                     objectRequestSink;

  private volatile State                 state                = INIT;
  private DSOChannelManager              channelManager;
  private ClientStateManager             stateManager;
  private Sequence                       batchIDSequence      = new SimpleSequence();
  private ObjectRequestCache             objectRequestCache;

  private ObjectStatsRecorder            objectStatsRecorder;

  public ObjectRequestManagerImpl(ObjectManager objectManager, DSOChannelManager channelManager,
                                  ClientStateManager stateManager, ServerTransactionManager transactionManager,
                                  Sink objectRequestSink, Sink respondObjectRequestSink,
                                  ObjectStatsRecorder objectStatsRecorder) {
    this.objectManager = objectManager;
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.transactionManager = transactionManager;
    this.respondObjectRequestSink = respondObjectRequestSink;
    this.objectRequestSink = objectRequestSink;
    this.objectRequestCache = new ObjectRequestCache(LOGGING_ENABLED);
    this.objectStatsRecorder = objectStatsRecorder;
    transactionManager.addTransactionListener(this);
  }

  public synchronized void transactionManagerStarted(Set cids) {
    state = STARTING;
    objectManager.start();
    moveToStartedIfPossible();
  }

  private void moveToStartedIfPossible() {
    if (state == STARTING && resentTransactionIDs.isEmpty()) {
      state = STARTED;
      transactionManager.removeTransactionListener(this);
      processPending();
    }
  }

  public void requestObjects(ClientID clientID, ObjectRequestID requestID, ObjectIDSet ids, int maxRequestDepth,
                             boolean serverInitiated, String requestingThreadName) {
    synchronized (this) {
      if (state != STARTED) {
        LookupContext lookupContext = new LookupContext(clientID, requestID, ids, maxRequestDepth,
                                                        requestingThreadName, serverInitiated, objectRequestSink,
                                                        respondObjectRequestSink);
        pendingRequests.add(lookupContext);
        if (logger.isDebugEnabled()) {
          logger.debug("RequestObjectManager is not started, lookup has been added to pending request: "
                       + lookupContext);
        }
        return;
      }
    }
    splitAndRequestObjects(clientID, requestID, ids, maxRequestDepth, serverInitiated, requestingThreadName);
  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {

    basicSendObjects(requestedNodeID, objs, requestedObjectIDs, missingObjectIDs, isServerInitiated, maxRequestDepth);
  }

  public synchronized void addResentServerTransactionIDs(Collection sTxIDs) {
    if (state != INIT) { throw new AssertionError("Cant add Resent transactions after start up ! " + sTxIDs.size()
                                                  + "Txns : " + state); }
    resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + resentTransactionIDs.size());
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    return;
  }

  public synchronized void clearAllTransactionsFor(NodeID client) {
    if (state == STARTED) return;
    for (Iterator iter = resentTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) iter.next();
      if (stxID.getSourceID().equals(client)) {
        iter.remove();
      }
    }
    moveToStartedIfPossible();
  }

  private void processPending() {
    logger.info("Processing Pending Lookups = " + pendingRequests.size());
    for (Iterator iter = pendingRequests.iterator(); iter.hasNext();) {
      LookupContext lookupContext = (LookupContext) iter.next();
      logger.info("Processing pending Looking up : " + lookupContext);
      splitAndRequestObjects(lookupContext.getRequestedNodeID(), lookupContext.getRequestID(), lookupContext
          .getLookupIDs(), lookupContext.getMaxRequestDepth(), lookupContext.isServerInitiated(), lookupContext
          .getRequestingThreadName());
    }
  }

  public synchronized void transactionApplied(ServerTransactionID stxID) {
    resentTransactionIDs.remove(stxID);
    moveToStartedIfPossible();
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
      if (objectRequestCache.add(reqObj, clientID)) {
        lookupContext = new LookupContext(clientID, requestID, split, maxRequestDepth, requestingThreadName,
                                          serverInitiated, objectRequestSink, respondObjectRequestSink);
      }
    }

    if (lookupContext != null) {
      objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, maxRequestDepth);
    }
  }

  private void basicSendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                                ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {

    Map<ClientID, BatchAndSend> messageMap = new HashMap<ClientID, BatchAndSend>();

    /**
     * will contain the object which are not present in the client out of the returned ones
     */
    Map<ClientID, Set<ObjectID>> clientNewIDsMap = new HashMap<ClientID, Set<ObjectID>>();

    LinkedList objectsInOrder = new LinkedList();
    try {

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

      RequestedObject reqObj = null;
      Set<ClientID> clientList = null;
      long batchID = batchIDSequence.next();

      reqObj = new RequestedObject(requestedObjectIDs, maxRequestDepth);

      synchronized (this) {
        clientList = this.objectRequestCache.remove(reqObj);
      }

      // prepare clients
      for (Iterator iter = clientList.iterator(); iter.hasNext();) {
        ClientID clientID = (ClientID) iter.next();

        Set newIds = stateManager.addReferences(clientID, ids);
        clientNewIDsMap.put(clientID, newIds);

        // make batch and send object for each client.
        MessageChannel channel = channelManager.getActiveChannel(clientID);
        messageMap.put(clientID, new BatchAndSend(channel, batchID));
      }

      boolean requestDebug = objectStatsRecorder.getRequestDebug();

      for (Iterator<Map.Entry<ClientID, Set<ObjectID>>> i = clientNewIDsMap.entrySet().iterator(); i.hasNext();) {
        Map.Entry<ClientID, Set<ObjectID>> entry = i.next();
        ClientID clientID = entry.getKey();
        Set newIDs = entry.getValue();
        BatchAndSend batchAndSend = messageMap.get(clientID);

        final boolean isLast = !i.hasNext();

        for (Iterator iter = objectsInOrder.iterator(); iter.hasNext();) {
          ManagedObject mo = (ManagedObject) iter.next();
          if (newIDs.contains(mo.getID())) {
            batchAndSend.sendObject(mo);
            if (requestDebug) updateStats(mo);
          }
          if (isLast) {
            objectManager.releaseReadOnly(mo);
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
            logger.warn("Sending missing ids: + " + missingObjectIDs.size() + " , to client: " + clientID);
            batchAndSend.sendMissingObjects(missingObjectIDs);
          }
        }
      }

    } catch (NoSuchChannelException e) {
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {
        objectManager.releaseReadOnly((ManagedObject) i.next());
      }
    } finally {
      for (Iterator<BatchAndSend> iterator = messageMap.values().iterator(); iterator.hasNext();) {
        BatchAndSend batchAndSend = iterator.next();
        batchAndSend.flush();
      }
    }
  }

  protected synchronized int getTotalRequestedObjects() {
    return objectRequestCache.numberOfRequestedObjects();
  }

  protected synchronized int getObjectRequestCacheClientSize() {
    return objectRequestCache.clientSize();
  }

  private void updateStats(ManagedObject mo) {
    String className = mo.getManagedObjectState().getClassName();
    if (className == null) className = "UNKNOWN";
    objectStatsRecorder.updateRequestStats(className);
  }

  protected static class RequestedObject {

    private final ObjectIDSet oidSet;

    private final int         depth;

    public RequestedObject(ObjectIDSet oidSet, int depth) {
      this.oidSet = oidSet;
      this.depth = depth;
    }

    public ObjectIDSet getOIdSet() {
      return oidSet;
    }

    public int getDepth() {
      return depth;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof RequestedObject)) { return false; }
      RequestedObject reqObj = (RequestedObject) obj;
      if (this.oidSet.equals(reqObj.getOIdSet()) && this.depth == reqObj.getDepth()) { return true; }
      return false;
    }

    public int hashCode() {
      return this.oidSet.hashCode();
    }

    public String toString() {
      return "RequestedObject [ " + oidSet + ", depth = " + depth + " ] ";
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
      return objectRequestMap.size();
    }

    // for tests
    protected int clientSize() {
      return clients().size();
    }

    // for tests
    protected Set<ClientID> clients() {
      Set<ClientID> clients = new LinkedHashSet<ClientID>();
      for (Iterator<LinkedHashSet<ClientID>> i = objectRequestMap.values().iterator(); i.hasNext();) {
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
          if (verbose && (++sameClientRequestCount % 100) == 0) {
            // The count keeps track of how many requests were made from the same client that has already been requested
            logger.info("[ObjectRequestCache] same client already made requests. total same client request optimized: "
                        + sameClientRequestCount);
          }
        } else {
          if (verbose && (++multipleClientRequestCount % 100) == 0) {
            // The count keeps track of how many requests were made from the multiple clients that has already been
            // requested
            logger
                .info("[ObjectRequestCache] multiple clients already made requests. total multiple clients request optimized: "
                      + multipleClientRequestCount);
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
      m.toDNA(out, serializer);
      sendCount++;
      if (sendCount > 1000) {
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, batches++);
        responseMessage.send();
        sendCount = 0;
        serializer = new ObjectStringSerializer();
        out = new TCByteBufferOutputStream();
      }
    }

    public void flush() {
      if (sendCount > 0) {
        RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, 0);
        responseMessage.send();
        sendCount = 0;
        serializer = new ObjectStringSerializer();
        out = new TCByteBufferOutputStream();
      }
    }

    public void sendMissingObjects(Set missingObjectIDs) {

      if (missingObjectIDs.size() > 0) {
        ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) channel
            .createMessage(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE);
        notFound.initialize(missingObjectIDs, batchID);
        notFound.send();
      }
    }

    protected int getSendCount() {
      return sendCount;
    }

    protected int getBatches() {
      return batches;
    }

    protected ObjectStringSerializer getSerializer() {
      return serializer;
    }

    protected TCByteBufferOutputStream getOut() {
      return out;
    }

  }

  protected static class LookupContext implements ObjectManagerResultsContext {

    private final ClientID        clientID;
    private final ObjectRequestID requestID;
    private final ObjectIDSet     lookupIDs;
    private final ObjectIDSet     missingObjects = new ObjectIDSet();
    private final int             maxRequestDepth;
    private final String          requestingThreadName;
    private final boolean         serverInitiated;
    private final Sink            respondObjectRequestSink;
    private final Sink            objectRequestSink;
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
      return lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      return new ObjectIDSet();
    }

    public void missingObject(ObjectID oid) {
      missingObjects.add(oid);
    }

    public void setResults(ObjectManagerLookupResults results) {
      objects = results.getObjects();
      if (results.getLookupPendingObjectIDs().size() > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("LookupPendingObjectIDs = " + results.getLookupPendingObjectIDs() + " , clientID = " + clientID
                       + " , requestID = " + requestID);
        }
        objectRequestSink.add(new ObjectRequestServerContext(this.clientID, this.requestID, results
            .getLookupPendingObjectIDs(), this.requestingThreadName));
      }
      ResponseContext responseContext = new ResponseContext(this.clientID, this.objects.values(), this.lookupIDs,
                                                            this.missingObjects, this.serverInitiated,
                                                            this.maxRequestDepth);
      respondObjectRequestSink.add(responseContext);
      if (logger.isDebugEnabled()) {
        logger.debug("Adding to respondSink , clientID = " + clientID + " , requestID = " + requestID + " "
                     + responseContext);
      }
    }

    public ObjectRequestID getRequestID() {
      return requestID;
    }

    public int getMaxRequestDepth() {
      return maxRequestDepth;
    }

    public boolean updateStats() {
      return false;
    }

    public boolean isServerInitiated() {
      return serverInitiated;
    }

    public ClientID getRequestedNodeID() {
      return clientID;
    }

    public String getRequestingThreadName() {
      return requestingThreadName;
    }

    @Override
    public String toString() {
      return "Lookup Context [ clientID = " + clientID + " , requestID = " + requestID + " , ids = " + lookupIDs
             + " , lookedup objects.size() = " + (objects != null ? objects.size() : 0) + " , missingObjects  = "
             + missingObjects + " , maxRequestDepth = " + maxRequestDepth + " , requestingThreadName = "
             + requestingThreadName + " , serverInitiated = " + serverInitiated + " , respondObjectRequestSink = "
             + respondObjectRequestSink + " ] ";
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
      return requestedNodeID;
    }

    public Collection getObjs() {
      return objs;
    }

    public ObjectIDSet getRequestedObjectIDs() {
      return requestedObjectIDs;
    }

    public ObjectIDSet getMissingObjectIDs() {
      return missingObjectIDs;
    }

    public boolean isServerInitiated() {
      return serverInitiated;
    }

    public int getRequestDepth() {
      return maxRequestDepth;
    }

    @Override
    public String toString() {

      return "ResponseContext [ requestNodeID = " + requestedNodeID + " , objs.size = " + objs.size()
             + " , requestedObjectIDs = " + requestedObjectIDs + " , missingObjectIDs = " + missingObjectIDs
             + " , serverInitiated = " + serverInitiated + " ] ";
    }

  }

}
