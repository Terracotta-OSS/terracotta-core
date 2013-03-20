/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.hamcrest.Matcher;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerMapRequestManagerTest extends TestCase {

  private ObjectManager objectManager;
  private Sink responseSink;
  private Sink managedObjectRequestSink;
  private DSOChannelManager channelManager;
  private ChannelStats channelStats;
  private ClientStateManager clientStateManager;
  private ServerMapRequestManager serverMapRequestManager;

  @Override
  public void setUp() throws Exception {
    objectManager = mock(ObjectManager.class);
    responseSink = mock(Sink.class);
    managedObjectRequestSink = mock(Sink.class);
    channelManager = mock(DSOChannelManager.class);
    channelStats = mock(ChannelStats.class);
    clientStateManager = mock(ClientStateManager.class);
    serverMapRequestManager = new ServerMapRequestManagerImpl(objectManager, channelManager, responseSink,
        managedObjectRequestSink, clientStateManager, channelStats);
  }

  public void tests() {
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey = "key1";
    final ObjectID portableValue = new ObjectID(0);
    final ArrayList requests = new ArrayList();
    Set<Object> keys = new HashSet<Object>();
    keys.add(portableKey);
    requests.add(new ServerMapGetValueRequest(requestID, keys));
    serverMapRequestManager.requestValues(clientID, mapID, requests);
    final Set<ObjectID> lookupIDs = new HashSet<ObjectID>();
    lookupIDs.add(mapID);

    final ArgumentCaptor<ServerMapRequestContext> requestContextArg = ArgumentCaptor
        .forClass(ServerMapRequestContext.class);

    verify(objectManager, atLeastOnce()).lookupObjectsFor(eq(clientID), requestContextArg.capture());
    assertEquals(clientID, requestContextArg.getValue().getClientID());
    assertEquals(lookupIDs, requestContextArg.getValue().getLookupIDs());

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey)).thenReturn(new CDSMValue(portableValue, 0, 0, 0, 0));
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

    verify(objectManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);

    final ArrayList responses = new ArrayList();
    ServerMapGetValueResponse response = new ServerMapGetValueResponse(requestID);
    response.put(portableKey, portableValue);
    responses.add(response);
    verify(message, atLeastOnce()).initializeGetValueResponse(mapID, responses);

    verify(message, atLeastOnce()).send();

  }

  public void testMultipleKeysRequests() {
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ServerMapRequestID requestID2 = new ServerMapRequestID(1);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final ObjectID portableValue1 = new ObjectID(0);
    final Object portableKey2 = "key2";
    final ObjectID portableValue2 = new ObjectID(1);
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

    verify(objectManager, atMost(1)).lookupObjectsFor(eq(clientID), requestContextArg.capture());
    assertEquals(clientID, requestContextArg.getValue().getClientID());
    assertEquals(lookupIDs, requestContextArg.getValue().getLookupIDs());

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey1)).thenReturn(new CDSMValue(portableValue1, 0, 0, 0, 0));
    when(mos.getValueForKey(portableKey2)).thenReturn(new CDSMValue(portableValue2, 0, 0, 0, 0));

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

    verify(objectManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);

    final ArgumentCaptor<Collection> responsesArg = ArgumentCaptor.forClass(Collection.class);

    final ArrayList responses = new ArrayList();
    verify(message, atLeastOnce()).initializeGetValueResponse(eq(mapID), responsesArg.capture());

    ServerMapGetValueResponse response1 = new ServerMapGetValueResponse(requestID1);
    response1.put(portableKey1, portableValue1);
    ServerMapGetValueResponse response2 = new ServerMapGetValueResponse(requestID2);
    response2.put(portableKey2, portableValue2);

    responses.add(response1);
    responses.add(response2);

    verify(message, atLeastOnce()).send();
    assertTrue(responsesArg.getValue().contains(response1));
    assertTrue(responsesArg.getValue().contains(response2));

  }

  public void testPrefetch() throws Exception {
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final ObjectID portableValue1ObjID = new ObjectID(1001);
    when(clientStateManager.addReferences(eq(clientID), (Set<ObjectID>)argThat(hasItem(portableValue1ObjID)))).thenReturn(Collections
        .singleton(portableValue1ObjID));

    ServerMapGetValueRequest mapGetValueRequest = new ServerMapGetValueRequest(requestID1,
                                                                               Collections.singleton(portableKey1));
    serverMapRequestManager.requestValues(clientID, mapID, Collections.singletonList(mapGetValueRequest));

    ConcurrentDistributedServerMapManagedObjectState managedObjectState = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(managedObjectState.getValueForKey(portableKey1)).thenReturn(new CDSMValue(portableValue1ObjID, 0, 0, 0, 0));
    ManagedObject managedObject = mock(ManagedObject.class);
    when(managedObject.getManagedObjectState()).thenReturn(managedObjectState);
    serverMapRequestManager.sendResponseFor(mapID, managedObject);

    verify(managedObjectRequestSink).add(argThat(hasState(LOOKUP_STATE.SERVER_INITIATED_FORCED)));
    verify(clientStateManager).addReferences(eq(clientID), (Set<ObjectID>)argThat(hasItem(portableValue1ObjID)));
  }

  private static Matcher<ObjectRequestServerContextImpl> hasState(final LOOKUP_STATE state) {
    return new ArgumentMatcher<ObjectRequestServerContextImpl>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof ObjectRequestServerContextImpl) {
          return state == ((ObjectRequestServerContextImpl)o).getLookupState();
        } else {
          return false;
        }
      }
    };
  }
}
