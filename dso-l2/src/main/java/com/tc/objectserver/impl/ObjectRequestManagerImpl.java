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
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.RespondToObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
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
import java.util.SortedSet;
import java.util.Map.Entry;

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

  public ObjectRequestManagerImpl(final ObjectManager objectManager, final DSOChannelManager channelManager,
                                  final ClientStateManager stateManager, final Sink objectRequestSink,
                                  final Sink respondObjectRequestSink, final ObjectStatsRecorder objectStatsRecorder) {
    this.objectManager = objectManager;
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.respondObjectRequestSink = respondObjectRequestSink;
    this.objectRequestSink = objectRequestSink;
    this.objectRequestCache = new ObjectRequestCache(LOGGING_ENABLED);
    this.objectStatsRecorder = objectStatsRecorder;
  }

  public void requestObjects(final ObjectRequestServerContext requestContext) {
    splitAndRequestObjects(requestContext.getClientID(), requestContext.getRequestID(), requestContext
        .getRequestedObjectIDs(), requestContext.getRequestDepth(), requestContext.getLookupState(), requestContext
        .getRequestingThreadName());
  }

  private void splitAndRequestObjects(final ClientID clientID, final ObjectRequestID requestID,
                                      final SortedSet<ObjectID> ids, final int maxRequestDepth,
                                      final LOOKUP_STATE lookupState, final String requestingThreadName) {

    ObjectIDSet split = new ObjectIDSet();
    for (final Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      final ObjectID id = iter.next();
      split.add(id);
      if (split.size() >= SPLIT_SIZE || !iter.hasNext()) {
        basicRequestObjects(clientID, requestID, lookupState, requestingThreadName,
                            new RequestedObject(split, maxRequestDepth));
        split = new ObjectIDSet();
      }
    }

  }

  private void basicRequestObjects(final ClientID clientID, final ObjectRequestID requestID,
                                   final LOOKUP_STATE lookupState, final String requestingThreadName,
                                   final RequestedObject requestedObject) {

    LookupContext lookupContext = null;

    synchronized (this) {
      if (this.objectRequestCache.add(requestedObject, clientID)) {
        lookupContext = new LookupContext(clientID, requestID, requestedObject.getLookupIDSet(), requestedObject
            .getMaxDepth(), requestingThreadName, lookupState, this.objectRequestSink, this.respondObjectRequestSink);
      }
    }
    if (lookupContext != null) {
      this.objectManager.lookupObjectsAndSubObjectsFor(clientID, lookupContext, requestedObject.getMaxDepth());
    }
  }

  public void sendObjects(final ClientID requestedNodeID, final Collection objs, final ObjectIDSet requestedObjectIDs,
                          final ObjectIDSet missingObjectIDs, final LOOKUP_STATE lookupState, final int maxRequestDepth) {

    // Create ordered list of objects
    final LinkedList objectsInOrder = new LinkedList();
    final Set ids = new HashSet(Math.max((int) (objs.size() / .75f) + 1, 16));
    for (final Iterator i = objs.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      final ObjectID id = mo.getID();
      ids.add(id);
      if (requestedObjectIDs.contains(id)) {
        objectsInOrder.addLast(mo);
      } else {
        objectsInOrder.addFirst(mo);
      }
    }

    // Create map of clients and objects to be sent
    Set<ClientID> clientList = null;
    final long batchID = this.batchIDSequence.next();
    final RequestedObject reqObj = new RequestedObject(requestedObjectIDs, maxRequestDepth);
    synchronized (this) {
      clientList = this.objectRequestCache.remove(reqObj);
    }

    final Map<ClientID, Set<ObjectID>> clientNewIDsMap = new HashMap<ClientID, Set<ObjectID>>();
    final Map<ClientID, BatchAndSend> messageMap = new HashMap<ClientID, BatchAndSend>();

    for (final ClientID clientID : clientList) {
      try {
        // make batch and send object for each client.
        final MessageChannel channel = this.channelManager.getActiveChannel(clientID);
        messageMap.put(clientID, new BatchAndSend(channel, batchID));
        // get set of objects which are not present in the client out of the returned ones
        final Set newIds;
        if (lookupState.isServerInitiated() && lookupState.forceSend()) {
          newIds = ids;
        } else {
          newIds = this.stateManager.addReferences(clientID, ids);
        }
        clientNewIDsMap.put(clientID, newIds);
      } catch (final NoSuchChannelException e) {
        logger.warn("Not sending objects to client " + clientID + ": " + e);
      }
    }

    // send objects to each client
    if (!messageMap.isEmpty()) {
      final boolean requestDebug = this.objectStatsRecorder.getRequestDebug();

      for (final Entry<ClientID, Set<ObjectID>> entry : clientNewIDsMap.entrySet()) {
        final ClientID clientID = entry.getKey();
        final Set newIDs = entry.getValue();
        final BatchAndSend batchAndSend = messageMap.get(clientID);

        for (final Iterator iter = objectsInOrder.iterator(); iter.hasNext();) {
          final ManagedObject mo = (ManagedObject) iter.next();
          if (newIDs.contains(mo.getID())) {
            batchAndSend.sendObject(mo);
            if (requestDebug) {
              updateStats(mo);
            }
          }
        }
      }
      this.objectManager.releaseAllReadOnly(objectsInOrder);

      if (!missingObjectIDs.isEmpty()) {
        if (lookupState.isServerInitiated() && !lookupState.forceSend()) {
          // This is a possible case where changes are flying in and server is initiating some lookups and the lookups
          // go pending and in the meantime the changes made those looked up objects garbage and DGC removes those
          // objects. Now we don't want to send those missing objects to clients. Its not really an issue as the
          // clients should never lookup those objects, but still why send them ? Same is true for client prefetched
          // objects.
          logger.warn("Server Initiated lookup = " + lookupState + ". Ignoring Missing Objects : " + missingObjectIDs);
        } else {
          for (final Entry<ClientID, BatchAndSend> entry : messageMap.entrySet()) {
            final ClientID clientID = entry.getKey();
            final BatchAndSend batchAndSend = entry.getValue();
            logger.warn("Sending missing ids: " + missingObjectIDs.size() + " , to client: " + clientID);
            batchAndSend.sendMissingObjects(missingObjectIDs);
          }
        }
      }

      for (final BatchAndSend batchAndSend : messageMap.values()) {
        batchAndSend.flush();
      }
    } else {
      // no connected clients to send to
      this.objectManager.releaseAllReadOnly(objectsInOrder);
    }
  }

  protected synchronized int getTotalRequestedObjects() {
    return this.objectRequestCache.numberOfRequestedObjects();
  }

  protected synchronized int getObjectRequestCacheClientSize() {
    return this.objectRequestCache.clientSize();
  }

  private void updateStats(final ManagedObject mo) {
    String className = mo.getManagedObjectState().getClassName();
    if (className == null) {
      className = "UNKNOWN";
    }
    this.objectStatsRecorder.updateRequestStats(className);
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    synchronized (this) {
      out.indent().print("objectRequestCache: ").visit(this.objectRequestCache).flush();
    }
    return out;
  }

  protected static class RequestedObject {

    private final ObjectIDSet oidSet;
    private final int         depth;
    private final int         hashCode;

    RequestedObject(final ObjectIDSet oidSet, final int depth) {
      this.oidSet = oidSet;
      this.depth = depth;
      this.hashCode = oidSet.hashCode();
    }

    public ObjectIDSet getLookupIDSet() {
      return this.oidSet;
    }

    public int getMaxDepth() {
      return this.depth;
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof RequestedObject)) { return false; }
      final RequestedObject reqObj = (RequestedObject) obj;
      if (this.oidSet.equals(reqObj.getLookupIDSet()) && this.depth == reqObj.getMaxDepth()) { return true; }
      return false;
    }

    @Override
    public int hashCode() {
      return this.hashCode;
    }

    @Override
    public String toString() {
      return "RequestedObject [ " + this.oidSet + ", depth = " + this.depth + " ] ";
    }
  }

  protected static class ObjectRequestCache implements PrettyPrintable {

    private final Map<RequestedObject, LinkedHashSet<ClientID>> objectRequestMap = new HashMap<RequestedObject, LinkedHashSet<ClientID>>();

    private final boolean                                       verbose;

    private int                                                 multipleClientRequestCount;

    private int                                                 sameClientRequestCount;

    public ObjectRequestCache(final boolean verbose) {
      this.verbose = verbose;
    }

    // for tests
    protected int numberOfRequestedObjects() {
      int val = 0;
      for (final Object element : this.objectRequestMap.keySet()) {
        val += ((RequestedObject) element).getLookupIDSet().size();
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
      final Set<ClientID> clients = new LinkedHashSet<ClientID>();
      for (final LinkedHashSet<ClientID> linkedHashSet : this.objectRequestMap.values()) {
        clients.addAll(linkedHashSet);
      }
      return clients;
    }

    // TODO: put multiple clients, and single client request, print every 100 request
    public boolean add(final RequestedObject reqObjects, final ClientID clientID) {
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

    public boolean contains(final RequestedObject reqObj) {
      return this.objectRequestMap.containsKey(reqObj);
    }

    public Set<ClientID> getClientsForRequest(final RequestedObject reqObj) {
      return this.objectRequestMap.get(reqObj);
    }

    public Set<ClientID> remove(final RequestedObject reqObj) {
      return this.objectRequestMap.remove(reqObj);
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      out.duplicateAndIndent().indent().print(getClass().getName()).flush();
      out.duplicateAndIndent().indent().print("objectRequestMap").flush();
      for (final Entry<RequestedObject, LinkedHashSet<ClientID>> entry : this.objectRequestMap.entrySet()) {
        final StringBuilder strBuffer = new StringBuilder();
        strBuffer.append(entry.getKey());
        strBuffer.append(", Requested by: ");
        for (final ClientID clientID : entry.getValue()) {
          strBuffer.append(clientID + ", ");
        }
        out.duplicateAndIndent().indent().print(strBuffer.toString()).flush();
      }
      return out;
    }
  }

  protected static class BatchAndSend {

    private final MessageChannel     channel;
    private final long               batchID;

    private Integer                  sendCount  = 0;
    private Integer                  batches    = 0;
    private ObjectStringSerializer   serializer = new ObjectStringSerializerImpl();
    private TCByteBufferOutputStream out        = new TCByteBufferOutputStream();

    public BatchAndSend(final MessageChannel channel, final long batchID) {
      this.channel = channel;
      this.batchID = batchID;
    }

    public void sendObject(final ManagedObject m) {
      m.toDNA(this.out, this.serializer, DNAType.L1_FAULT);
      this.sendCount++;
      if (this.sendCount > 1000) {
        final RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) this.channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(this.out.toArray(), this.sendCount, this.serializer, this.batchID, this.batches++);
        responseMessage.send();
        this.sendCount = 0;
        this.serializer = new ObjectStringSerializerImpl();
        this.out = new TCByteBufferOutputStream();
      }
    }

    public void flush() {
      if (this.sendCount > 0) {
        final RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) this.channel
            .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
        responseMessage.initialize(this.out.toArray(), this.sendCount, this.serializer, this.batchID, 0);
        responseMessage.send();
        this.sendCount = 0;
        this.serializer = new ObjectStringSerializerImpl();
        this.out = new TCByteBufferOutputStream();
      }
    }

    public void sendMissingObjects(final Set missingObjectIDs) {

      if (missingObjectIDs.size() > 0) {
        final ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) this.channel
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
    private final LOOKUP_STATE    lookupState;
    private final Sink            respondObjectRequestSink;
    private final Sink            objectRequestSink;

    private ObjectIDSet           missingObjects;
    private Map                   objects;

    public LookupContext(final ClientID clientID, final ObjectRequestID requestID, final ObjectIDSet lookupIDs,
                         final int maxRequestDepth, final String requestingThreadName, final LOOKUP_STATE lookupState,
                         final Sink objectRequestSink, final Sink respondObjectRequestSink) {
      this.clientID = clientID;
      this.requestID = requestID;
      this.lookupIDs = lookupIDs;
      this.maxRequestDepth = maxRequestDepth;
      this.requestingThreadName = requestingThreadName;
      this.lookupState = lookupState;
      this.objectRequestSink = objectRequestSink;
      this.respondObjectRequestSink = respondObjectRequestSink;

    }

    public ObjectIDSet getLookupIDs() {
      return this.lookupIDs;
    }

    public ObjectIDSet getNewObjectIDs() {
      return new ObjectIDSet();
    }

    public void setResults(final ObjectManagerLookupResults results) {
      this.objects = results.getObjects();
      this.missingObjects = results.getMissingObjectIDs();

      if (results.getLookupPendingObjectIDs().size() > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("LookupPendingObjectIDs = " + results.getLookupPendingObjectIDs() + " , clientID = "
                       + this.clientID + " , requestID = " + this.requestID);
        }
        this.objectRequestSink.add(new ObjectRequestServerContextImpl(this.clientID, this.requestID, results
            .getLookupPendingObjectIDs(), this.requestingThreadName, -1, LOOKUP_STATE.SERVER_INITIATED));
      }
      final ResponseContext responseContext = new ResponseContext(this.clientID, this.objects.values(), this.lookupIDs,
                                                                  this.missingObjects, this.lookupState,
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

    public LOOKUP_STATE getLookupState() {
      return this.lookupState;
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
             + this.requestingThreadName + " , serverInitiated = " + this.lookupState
             + " , respondObjectRequestSink = " + this.respondObjectRequestSink + " ] ";
    }
  }

  protected static class ResponseContext implements RespondToObjectRequestContext {

    private final ClientID     requestedNodeID;
    private final Collection   objs;
    private final ObjectIDSet  requestedObjectIDs;
    private final ObjectIDSet  missingObjectIDs;
    private final LOOKUP_STATE lookupState;
    private final int          maxRequestDepth;

    public ResponseContext(final ClientID requestedNodeID, final Collection objs, final ObjectIDSet requestedObjectIDs,
                           final ObjectIDSet missingObjectIDs, final LOOKUP_STATE lookupState, final int maxDepth) {
      this.requestedNodeID = requestedNodeID;
      this.objs = objs;
      this.requestedObjectIDs = requestedObjectIDs;
      this.missingObjectIDs = missingObjectIDs;
      this.lookupState = lookupState;
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

    public LOOKUP_STATE getLookupState() {
      return this.lookupState;
    }

    public int getRequestDepth() {
      return this.maxRequestDepth;
    }

    @Override
    public String toString() {
      return "ResponseContext [ requestNodeID = " + this.requestedNodeID + " , objs.size = " + this.objs.size()
             + " , requestedObjectIDs = " + this.requestedObjectIDs + " , missingObjectIDs = " + this.missingObjectIDs
             + " , serverInitiated = " + this.lookupState + " ] ";
    }
  }

  // delegating all ObjectManagerMbean requests to the object manager
  public int getCachedObjectCount() {
    return this.objectManager.getCachedObjectCount();
  }

  public int getLiveObjectCount() {
    return this.objectManager.getLiveObjectCount();
  }

  public Iterator getRootNames() {
    return this.objectManager.getRootNames();
  }

  public Iterator getRoots() {
    return this.objectManager.getRoots();
  }

  public ManagedObjectFacade lookupFacade(final ObjectID id, final int limit) throws NoSuchObjectException {
    return this.objectManager.lookupFacade(id, limit);
  }

  public ObjectID lookupRootID(final String name) {
    return this.objectManager.lookupRootID(name);
  }
}
