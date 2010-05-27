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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    try {
      final List<ServerMapRequestContext> requestList = this.requestQueue.remove(mapID);

      if (requestList == null) { throw new AssertionError("Looked up : " + managedObject
                                                          + " But no request pending for it : " + this.requestQueue); }

      for (final ServerMapRequestContext request : requestList) {

        final ServerMapRequestType requestType = request.getRequestType();
        switch (requestType) {
          case GET_SIZE:
            sendResponseForGetSize(mapID, request, csmState);
            break;
          case GET_VALUE_FOR_KEY:
            gatherResponseForGetValue(mapID, request, csmState, results);
            break;
          default:
            throw new AssertionError("Unknown request type : " + requestType);
        }
      }
    } finally {
      // TODO::FIXME::Release as soon as possible
      this.objectManager.releaseReadOnly(managedObject);
    }
    if (!results.isEmpty()) {
      sendResponseForGetValue(mapID, results);
    }
  }

  private void gatherResponseForGetValue(final ObjectID mapID, final ServerMapRequestContext request,
                                         final ConcurrentDistributedServerMapManagedObjectState csmState,
                                         final Map<ClientID, Collection<ServerMapGetValueResponse>> results) {
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
      preFetchPortableValueIfNeeded(mapID, portableValue, clientID);
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

  private void preFetchPortableValueIfNeeded(final ObjectID mapID, final Object portableValue, final ClientID clientID) {
    if (portableValue instanceof ObjectID) {
      final ObjectID valueID = (ObjectID) portableValue;
      if (valueID.isNull()) { return; }
      if (mapID.getGroupID() != valueID.getGroupID()) {
        // TODO::FIX for AA
        // Not in this server
        return;
      }
      final ObjectIDSet lookupIDs = new ObjectIDSet();
      lookupIDs.add(valueID);
      this.managedObjectRequestSink.add(new ObjectRequestServerContextImpl(clientID, ObjectRequestID.NULL_ID,
                                                                           lookupIDs, Thread.currentThread().getName(),
                                                                           1, true));
    }
  }

  private final static class ServerMapRequestQueue {

    private final Map<ObjectID, List<ServerMapRequestContext>> serverMapRequestMap = new HashMap<ObjectID, List<ServerMapRequestContext>>();

    public synchronized boolean add(final ServerMapRequestContext context) {
      boolean newEntry = false;
      final ObjectID mapID = context.getServerTCMapID();
      List<ServerMapRequestContext> requestList = this.serverMapRequestMap.get(mapID);
      if (requestList == null) {
        requestList = new ArrayList<ServerMapRequestContext>();
        this.serverMapRequestMap.put(mapID, requestList);
        newEntry = true;
      }
      requestList.add(context);
      return newEntry;
    }

    public synchronized List<ServerMapRequestContext> remove(final ObjectID mapID) {
      return this.serverMapRequestMap.remove(mapID);
    }
  }

}
