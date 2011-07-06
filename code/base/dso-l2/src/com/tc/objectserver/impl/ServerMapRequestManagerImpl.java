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
import com.tc.object.msg.GetAllKeysServerMapResponseMessage;
import com.tc.object.msg.GetAllSizeServerMapResponseMessage;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.msg.ObjectNotFoundServerMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.ServerMapGetAllSizeHelper;
import com.tc.objectserver.context.ServerMapRequestAllKeysContext;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.context.ServerMapRequestSizeContext;
import com.tc.objectserver.context.ServerMapRequestValueContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
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

    final ServerMapRequestValueContext requestContext = new ServerMapRequestValueContext(clientID, mapID, requests,
                                                                                         this.respondToServerTCMapSink);
    processRequest(clientID, requestContext);
  }

  public void requestSize(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                          ServerMapGetAllSizeHelper helper) {
    final ServerMapRequestSizeContext requestContext = new ServerMapRequestSizeContext(requestID, clientID, mapID,
                                                                                       this.respondToServerTCMapSink,
                                                                                       helper);
    processRequest(clientID, requestContext);
  }

  public void requestAllKeys(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID) {
    final ServerMapRequestAllKeysContext requestContext = new ServerMapRequestAllKeysContext(
                                                                                             requestID,
                                                                                             clientID,
                                                                                             mapID,
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
    final ConcurrentDistributedServerMapManagedObjectState cdsmState = (ConcurrentDistributedServerMapManagedObjectState) state;

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
            sendResponseForGetAllSize(mapID, (ServerMapRequestSizeContext) request, cdsmState);
            break;
          case GET_ALL_KEYS:
            sendResponseForGetAllKeys(mapID, (ServerMapRequestAllKeysContext) request, cdsmState);
            break;
          case GET_VALUE_FOR_KEY:
            gatherResponseForGetValue(mapID, (ServerMapRequestValueContext) request, cdsmState, results, prefetches);
            break;
          default:
            throw new AssertionError("Unknown request type : " + requestType);
        }
      }
    } finally {
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
      final ServerMapRequestID requestID = request.getRequestID();
      final ServerMapRequestType requestType = request.getRequestType();
      final ClientID clientID = request.getClientID();

      final MessageChannel channel = getActiveChannel(clientID);
      if (channel == null) {
        logger.error("no Active Channel, cannot sent ObjectNotFound message for mapID: " + mapID + " for client "
                     + clientID);
        return;
      }

      if (requestType == ServerMapRequestType.GET_VALUE_FOR_KEY) {
        sendMissingObjectReponseForGetValueRequests(mapID, request, requestType, channel);
      } else {
        initializeAndSendObjectNotFoundMessage(mapID, requestID, requestType, channel);
      }
    }
  }

  private void initializeAndSendObjectNotFoundMessage(ObjectID mapID, final ServerMapRequestID requestID,
                                                      final ServerMapRequestType requestType,
                                                      final MessageChannel channel) {
    final ObjectNotFoundServerMapResponseMessage notFound = (ObjectNotFoundServerMapResponseMessage) channel
        .createMessage(TCMessageType.OBJECT_NOT_FOUND_SERVER_MAP_RESPONSE_MESSAGE);
    notFound.initialize(mapID, requestID, requestType);
    notFound.send();
  }

  private void sendMissingObjectReponseForGetValueRequests(ObjectID mapID, final ServerMapRequestContext request,
                                                           final ServerMapRequestType requestType,
                                                           final MessageChannel channel) {
    ServerMapRequestValueContext valueRequest = (ServerMapRequestValueContext) request;
    Collection<ServerMapGetValueRequest> getValueRequests = valueRequest.getValueRequests();
    for (ServerMapGetValueRequest getValueRequest : getValueRequests) {
      final ServerMapRequestID getValueRequestID = getValueRequest.getRequestID();
      initializeAndSendObjectNotFoundMessage(mapID, getValueRequestID, requestType, channel);
    }
  }

  private void gatherResponseForGetValue(final ObjectID mapID, final ServerMapRequestValueContext request,
                                         final ConcurrentDistributedServerMapManagedObjectState cdsmState,
                                         final Map<ClientID, Collection<ServerMapGetValueResponse>> results,
                                         final Map<ClientID, ObjectIDSet> prefetches) {
    final ClientID clientID = request.getClientID();
    Collection<ServerMapGetValueResponse> responses = results.get(clientID);
    if (responses == null) {
      responses = new ArrayList<ServerMapGetValueResponse>();
      results.put(clientID, responses);
    }
    for (final ServerMapGetValueRequest r : request.getValueRequests()) {
      Object portableValue = cdsmState.getValueForKey(r.getKey());
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

  private void sendResponseForGetAllSize(final ObjectID mapID, final ServerMapRequestSizeContext request,
                                         final ConcurrentDistributedServerMapManagedObjectState cdsmState) {
    final ServerMapRequestID requestID = request.getRequestID();
    final ClientID clientID = request.getClientID();
    final Integer size = cdsmState.getSize();

    ServerMapGetAllSizeHelper helper = request.getServerMapGetAllSizeHelper();
    synchronized (helper) {
      helper.addSize(mapID, size);
      if (helper.isDone()) {

        final MessageChannel channel = getActiveChannel(clientID);
        if (channel == null) { return; }

        final GetAllSizeServerMapResponseMessage responseMessage = (GetAllSizeServerMapResponseMessage) channel
            .createMessage(TCMessageType.GET_ALL_SIZE_SERVER_MAP_RESPONSE_MESSAGE);
        responseMessage.initializeGetAllSizeResponse(helper.getGroupID(), requestID, helper.getTotalSize());
        responseMessage.send();
      }
    }
  }

  private void sendResponseForGetAllKeys(final ObjectID mapID, final ServerMapRequestAllKeysContext request,
                                         final ConcurrentDistributedServerMapManagedObjectState cdsmState) {
    final ServerMapRequestID requestID = request.getRequestID();
    final ClientID clientID = request.getClientID();

    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) { return; }

    final GetAllKeysServerMapResponseMessage responseMessage = (GetAllKeysServerMapResponseMessage) channel
        .createMessage(TCMessageType.GET_ALL_KEYS_SERVER_MAP_RESPONSE_MESSAGE);

    responseMessage.initializeGetAllKeysResponse(mapID, requestID, cdsmState.getAllKeys());
    responseMessage.send();
  }

  private MessageChannel getActiveChannel(final ClientID clientID) {
    try {
      return this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      this.logger.warn("Client " + clientID + " disconnect before sending Response for ServerMap Request ");
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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print("requestQueue: ").flush();
    out.visit(this.requestQueue).flush();
    return out;
  }

  private final static class ServerMapRequestQueue implements PrettyPrintable {

    private final TCConcurrentMultiMap<ObjectID, ServerMapRequestContext> requests = new TCConcurrentMultiMap<ObjectID, ServerMapRequestContext>();

    public boolean add(final ServerMapRequestContext context) {
      final ObjectID mapID = context.getServerTCMapID();
      return this.requests.add(mapID, context);
    }

    public Collection<ServerMapRequestContext> remove(final ObjectID mapID) {
      return this.requests.removeAll(mapID);
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.visit(this.requests).flush();
      return out;
    }
  }

}
