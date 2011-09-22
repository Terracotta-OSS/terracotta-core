/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ServerMapRequestManagerTest extends TestCase {

  public void tests() {
    final ObjectManager objManager = mock(ObjectManager.class);
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey = "key1";
    final Object portableValue = "value1";
    final Sink respondToServerTCMapSink = mock(Sink.class);
    final Sink managedObjectRequestSink = mock(Sink.class);
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    final ServerMapRequestManagerImpl serverMapRequestManager = new ServerMapRequestManagerImpl(
                                                                                                objManager,
                                                                                                channelManager,
                                                                                                respondToServerTCMapSink,
                                                                                                managedObjectRequestSink,
                                                                                                null);
    final ArrayList requests = new ArrayList();
    Set<Object> keys = new HashSet<Object>();
    keys.add(portableKey);
    requests.add(new ServerMapGetValueRequest(requestID, keys));
    serverMapRequestManager.requestValues(clientID, mapID, requests);
    final Set<ObjectID> lookupIDs = new HashSet<ObjectID>();
    lookupIDs.add(mapID);

    final ArgumentCaptor<ServerMapRequestContext> requestContextArg = ArgumentCaptor
        .forClass(ServerMapRequestContext.class);

    verify(objManager, atLeastOnce()).lookupObjectsFor(Matchers.eq(clientID), requestContextArg.capture());
    assertEquals(clientID, requestContextArg.getValue().getClientID());
    assertEquals(lookupIDs, requestContextArg.getValue().getLookupIDs());

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey)).thenReturn(portableValue);
    when(mo.getManagedObjectState()).thenReturn(mos);
    final MessageChannel messageChannel = mock(MessageChannel.class);
    try {
      when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }
    final GetValueServerMapResponseMessage message = mock(GetValueServerMapResponseMessage.class);
    when(messageChannel.createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE)).thenReturn(message);

    serverMapRequestManager.sendResponseFor(mapID, mo);

    verify(mo, atLeastOnce()).getManagedObjectState();

    verify(objManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);

    final ArrayList responses = new ArrayList();
    Map<Object, Object> values = new HashMap<Object, Object>();
    values.put(portableKey, portableValue);
    responses.add(new ServerMapGetValueResponse(requestID, values));
    verify(message, atLeastOnce()).initializeGetValueResponse(mapID, responses);

    verify(message, atLeastOnce()).send();

  }

  public void testMultipleKeysRequests() {
    final ObjectManager objManager = mock(ObjectManager.class);
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ServerMapRequestID requestID2 = new ServerMapRequestID(1);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final Object portableValue1 = "value1";
    final Object portableKey2 = "key2";
    final Object portableValue2 = "value2";
    final Sink respondToServerMapSink = mock(Sink.class);
    final Sink managedObjectRequestSink = mock(Sink.class);
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    final ServerMapRequestManagerImpl serverMapRequestManager = new ServerMapRequestManagerImpl(
                                                                                                objManager,
                                                                                                channelManager,
                                                                                                respondToServerMapSink,
                                                                                                managedObjectRequestSink,
                                                                                                null);
    final ArrayList request1 = new ArrayList();
    Set<Object> keys1 = new HashSet<Object>();
    keys1.add(portableKey1);
    request1.add(new ServerMapGetValueRequest(requestID1, keys1));
    final ArrayList request2 = new ArrayList();
    Set<Object> keys2 = new HashSet<Object>();
    keys2.add(portableKey2);
    request2.add(new ServerMapGetValueRequest(requestID2, keys2));
    serverMapRequestManager.requestValues(clientID, mapID, request1);
    serverMapRequestManager.requestValues(clientID, mapID, request2);

    final Set<ObjectID> lookupIDs = new HashSet<ObjectID>();
    lookupIDs.add(mapID);

    final ArgumentCaptor<ServerMapRequestContext> requestContextArg = ArgumentCaptor
        .forClass(ServerMapRequestContext.class);

    verify(objManager, atMost(1)).lookupObjectsFor(Matchers.eq(clientID), requestContextArg.capture());
    assertEquals(clientID, requestContextArg.getValue().getClientID());
    assertEquals(lookupIDs, requestContextArg.getValue().getLookupIDs());

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey1)).thenReturn(portableValue1);
    when(mos.getValueForKey(portableKey2)).thenReturn(portableValue2);

    when(mo.getManagedObjectState()).thenReturn(mos);
    final MessageChannel messageChannel = mock(MessageChannel.class);
    try {
      when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }
    final GetValueServerMapResponseMessage message = mock(GetValueServerMapResponseMessage.class);
    when(messageChannel.createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE)).thenReturn(message);

    serverMapRequestManager.sendResponseFor(mapID, mo);

    verify(mo, atMost(1)).getManagedObjectState();

    verify(objManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);

    final ArgumentCaptor<Collection> responsesArg = ArgumentCaptor.forClass(Collection.class);

    final ArrayList responses = new ArrayList();
    verify(message, atLeastOnce()).initializeGetValueResponse(Matchers.eq(mapID), responsesArg.capture());

    Map<Object, Object> values1 = new HashMap<Object, Object>();
    values1.put(portableKey1, portableValue1);
    Map<Object, Object> values2 = new HashMap<Object, Object>();
    values2.put(portableKey2, portableValue2);
    ServerMapGetValueResponse response2 = new ServerMapGetValueResponse(requestID1, values1);
    ServerMapGetValueResponse response1 = new ServerMapGetValueResponse(requestID2, values2);

    responses.add(response1);
    responses.add(response2);

    verify(message, atLeastOnce()).send();
    assertTrue(responsesArg.getValue().contains(response1));
    assertTrue(responsesArg.getValue().contains(response2));

  }

}
