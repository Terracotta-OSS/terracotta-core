/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.object.msg.GetSizeServerMapResponseMessage;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.msg.ObjectNotFoundServerMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ServerMapRequestManagerImpl implements ServerMapRequestManager {

  private final TCLogger              logger       = TCLogging.getLogger(ServerMapRequestManagerImpl.class);
  private final ObjectManager         objectManager;
  private final DSOChannelManager     channelManager;
  private final Sink                  respondToServerTCMapSink;
  private final Sink                  managedObjectRequestSink;
  private final ServerMapRequestQueue requestQueue = new ServerMapRequestQueue();

  public ServerMapRequestManagerImpl(final ObjectManager objectManager, final DSOChannelManager channelManager,
                                     final Sink respondToServerTCMapSink, final Sink managedObjectRequestSink) {
    this.channelManager = channelManager;
    this.objectManager = objectManager;
    this.respondToServerTCMapSink = respondToServerTCMapSink;
    this.managedObjectRequestSink = managedObjectRequestSink;
  }

  public void requestValues(final ClientID clientID, final ObjectID mapID,
                            final Collection<ServerMapGetValueRequest> requests) {

    final ServerMapRequestContext requestContext = new ServerMapRequestContext(clientID, mapID, requests,
                                                                               this.respondToServerTCMapSink);
    processRequest(clientID, requestContext);
  }

  public void requestSize(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID) {
    final ServerMapRequestContext requestContext = new ServerMapRequestContext(requestID, clientID, mapID,
                                                                               this.respondToServerTCMapSink);
    processRequest(clientID, requestContext);
  }

  private void processRequest(final ClientID clientID, final ServerMapRequestContext requestContext) {
    if (this.requestQueue.add(requestContext)) {
      this.objectManager.lookupObjectsFor(clientID, requestContext);
    }
  }

  public void sendResponseFor(final ObjectID mapID, final ManagedObject managedObject) {
    final ManagedObjectState state = managedObject.getManagedObjectState();

    if (!(state instanceof ConcurrentDistributedServerMapManagedObjectState)) {
      // Formatter
      throw new AssertionError("Server Map " + mapID
                               + " is not a ConcurrentDistributedServerMapManagedObjectState, state is of class type: "
                               + state.getClassName());
    }
    final ConcurrentDistributedServerMapManagedObjectState csmState = (ConcurrentDistributedServerMapManagedObjectState) state;

    final Map<ClientID, Collection<ServerMapGetValueResponse>> results = new HashMap<ClientID, Collection<ServerMapGetValueResponse>>();
    final Map<ClientID, ObjectIDSet> prefetches = new HashMap<ClientID, ObjectIDSet>();
    try {
      final Collection<ServerMapRequestContext> requests = this.requestQueue.remove(mapID);

      if (requests.isEmpty()) { throw new AssertionError("Looked up : " + managedObject
                                                         + " But no request pending for it : " + this.requestQueue); }

      for (final ServerMapRequestContext request : requests) {

        final ServerMapRequestType requestType = request.getRequestType();
        switch (requestType) {
          case GET_SIZE:
            sendResponseForGetSize(mapID, request, csmState);
            break;
          case GET_VALUE_FOR_KEY:
            gatherResponseForGetValue(mapID, request, csmState, results, prefetches);
            break;
          default:
            throw new AssertionError("Unknown request type : " + requestType);
        }
      }
    } finally {
      // TODO::FIXME::Release as soon as possible
      this.objectManager.releaseReadOnly(managedObject);
    }
    if (!prefetches.isEmpty()) {
      preFetchPortableValue(prefetches);
    }
    if (!results.isEmpty()) {
      sendResponseForGetValue(mapID, results);
    }
  }

  public void sendMissingObjectResponseFor(ObjectID mapID) {
    final Collection<ServerMapRequestContext> requests = this.requestQueue.remove(mapID);
    
    for (final ServerMapRequestContext request : requests) {
      final ServerMapRequestID requestID = request.getSizeRequestID();
      final ServerMapRequestType requestType = request.getRequestType();
      final ClientID clientID = request.getClientID();

      final MessageChannel channel = getActiveChannel(clientID);
      if (channel == null) {
        logger.error("no Active Channel, cannot sent ObjectNotFound message for mapID: " + mapID + " for client " + clientID);
        return; 
      }

      final ObjectNotFoundServerMapResponseMessage notFound = (ObjectNotFoundServerMapResponseMessage) channel
          .createMessage(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE);
      notFound.initialize(mapID, requestID, requestType);
      notFound.send();
    }
  }

  private void gatherResponseForGetValue(final ObjectID mapID, final ServerMapRequestContext request,
                                         final ConcurrentDistributedServerMapManagedObjectState csmState,
                                         final Map<ClientID, Collection<ServerMapGetValueResponse>> results,
                                         final Map<ClientID, ObjectIDSet> prefetches) {
    final ClientID clientID = request.getClientID();
    Collection<ServerMapGetValueResponse> responses = results.get(clientID);
    if (responses == null) {
      responses = new ArrayList<ServerMapGetValueResponse>();
      results.put(clientID, responses);
    }
    for (final ServerMapGetValueRequest r : request.getValueRequests()) {
      Object portableValue = csmState.getValueForKey(r.getKey());
      // Null Value is not supported in CDSM
      portableValue = (portableValue == null ? ObjectID.NULL_ID : portableValue);
      responses.add(new ServerMapGetValueResponse(r.getRequestID(), portableValue));
      gatherPreFetchPortableValues(prefetches, clientID, portableValue);
    }
  }

  private void gatherPreFetchPortableValues(final Map<ClientID, ObjectIDSet> prefetches, final ClientID clientID,
                                            final Object portableValue) {
    if (portableValue instanceof ObjectID) {
      final ObjectID valueID = (ObjectID) portableValue;
      if (valueID.isNull()) { return; }
      ObjectIDSet lookupIDs = prefetches.get(clientID);
      if (lookupIDs == null) {
        lookupIDs = new ObjectIDSet();
        prefetches.put(clientID, lookupIDs);
      }
      lookupIDs.add(valueID);
    }
  }

  private void sendResponseForGetValue(final ObjectID mapID,
                                       final Map<ClientID, Collection<ServerMapGetValueResponse>> results) {
    for (final Entry<ClientID, Collection<ServerMapGetValueResponse>> e : results.entrySet()) {
      final ClientID clientID = e.getKey();
      final MessageChannel channel = getActiveChannel(clientID);
      if (channel == null) {
        this.logger.info("Client " + clientID + " is not active : Ignoring sending response for getValue() ");
        return;
      }
      final GetValueServerMapResponseMessage responseMessage = (GetValueServerMapResponseMessage) channel
          .createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);
      responseMessage.initializeGetValueResponse(mapID, e.getValue());
      responseMessage.send();
    }

  }

  private void sendResponseForGetSize(final ObjectID mapID, final ServerMapRequestContext request,
                                      final ConcurrentDistributedServerMapManagedObjectState csmState) {
    final ServerMapRequestID requestID = request.getSizeRequestID();
    final ClientID clientID = request.getClientID();
    final Integer size = csmState.getSize();

    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) { return; }

    final GetSizeServerMapResponseMessage responseMessage = (GetSizeServerMapResponseMessage) channel
        .createMessage(TCMessageType.GET_SIZE_SERVER_MAP_RESPONSE_MESSAGE);
    responseMessage.initializeGetSizeResponse(mapID, requestID, size);
    responseMessage.send();
  }

  private MessageChannel getActiveChannel(final ClientID clientID) {
    try {
      return this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      this.logger.warn("Client " + clientID + " disconnect before sending Response for ServeMap Request ");
      return null;
    }
  }

  private void preFetchPortableValue(final Map<ClientID, ObjectIDSet> prefetches) {
    for (final Entry<ClientID, ObjectIDSet> element : prefetches.entrySet()) {
      this.managedObjectRequestSink.add(new ObjectRequestServerContextImpl(element.getKey(), ObjectRequestID.NULL_ID,
                                                                           element.getValue(), Thread.currentThread()
                                                                               .getName(), 1, true));
    }
  }

  private final static class ServerMapRequestQueue {

    private final TCConcurrentMultiMap<ObjectID, ServerMapRequestContext> requests = new TCConcurrentMultiMap<ObjectID, ServerMapRequestContext>();

    public boolean add(final ServerMapRequestContext context) {
      final ObjectID mapID = context.getServerTCMapID();
      return this.requests.add(mapID, context);
    }

    public Collection<ServerMapRequestContext> remove(final ObjectID mapID) {
      return this.requests.removeAll(mapID);
    }
  }

}
