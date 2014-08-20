/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.CompoundResponse;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.context.ServerMapRequestPrefetchObjectsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ServerMapRequestManagerTest extends TestCase {

  private ObjectManager objectManager;
  private Sink responseSink;
  private Sink prefetchSink;
  private DSOChannelManager channelManager;
  private ChannelStats channelStats;
  private ClientStateManager clientStateManager;
  private ServerMapRequestManagerImpl serverMapRequestManager;

  @Override
  public void setUp() throws Exception {
    objectManager = mock(ObjectManager.class);
    responseSink = mock(Sink.class);
    prefetchSink = mock(Sink.class);
    channelManager = mock(DSOChannelManager.class);
    channelStats = mock(ChannelStats.class);
    clientStateManager = mock(ClientStateManager.class);
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_REQUEST_PREFETCH_ENABLED, "false");
    serverMapRequestManager = new ServerMapRequestManagerImpl(objectManager, channelManager, responseSink, prefetchSink,
         clientStateManager, channelStats);
  }

  public void tests() {
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey = "key1";
    final ObjectID portableValue = new ObjectID(0);
    final ArrayList requests = new ArrayList();
    final long version = 10;
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
    when(mos.getValueForKey(portableKey)).thenReturn(new CDSMValue(portableValue, 0, 0, 0, 0, version));
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

    ArgumentCaptor<ServerMapRequestPrefetchObjectsContext> capture = ArgumentCaptor.forClass(ServerMapRequestPrefetchObjectsContext.class);
    verify(prefetchSink, atLeastOnce()).add(capture.capture());

    ServerMapGetValueResponse msg = capture.getValue().getAnswers().iterator().next();
    assertEquals(((CompoundResponse)msg.getValues().get(portableKey)).getData(),portableValue);
    assertEquals(((CompoundResponse) msg.getValues().get(portableKey)).getVersion(), version);
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
    
    final Set<Object> dataSet = new HashSet<Object>();
    dataSet.add(portableKey1);
    dataSet.add(portableKey2);
    dataSet.add(portableValue1);
    dataSet.add(portableValue2);
    
    Set<Object> keys1 = new HashSet<Object>();
    keys1.add(portableKey1);
    request1.add(new ServerMapGetValueRequest(requestID1, keys1));
    final ArrayList request2 = new ArrayList();
    Set<Object> keys2 = new HashSet<Object>();
    keys2.add(portableKey2);
    request2.add(new ServerMapGetValueRequest(requestID2, keys2));
    
    when(clientStateManager.hasReference(any(NodeID.class),any(ObjectID.class))).thenReturn(Boolean.TRUE);
    
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

    verify(objectManager, atLeastOnce()).lookupObjectsFor(any(NodeID.class),any(ObjectManagerResultsContext.class));
    
    ArgumentCaptor<ServerMapRequestPrefetchObjectsContext> capture = ArgumentCaptor.forClass(ServerMapRequestPrefetchObjectsContext.class);

    final ArrayList responses = new ArrayList();

    ServerMapGetValueResponse response1 = new ServerMapGetValueResponse(requestID1);
    response1.put(portableKey1, portableValue1);
    ServerMapGetValueResponse response2 = new ServerMapGetValueResponse(requestID2);
    response2.put(portableKey2, portableValue2);

    responses.add(response1);
    responses.add(response2);

    verify(prefetchSink, atLeastOnce()).add(capture.capture());
    assertTrue(capture.getValue().getAnswers().contains(response1));
    assertTrue(capture.getValue().getAnswers().contains(response2));
    
  }
    
  public void testPrefetch() throws Exception {
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final ObjectID portableValue1ObjID = new ObjectID(1001);
    when(clientStateManager.addReference(eq(clientID), any(ObjectID.class))).thenReturn(true);

    ServerMapGetValueRequest mapGetValueRequest = new ServerMapGetValueRequest(requestID1,
                                                                               Collections.singleton(portableKey1));
    serverMapRequestManager.requestValues(clientID, mapID, Collections.singletonList(mapGetValueRequest));

    ConcurrentDistributedServerMapManagedObjectState managedObjectState = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(managedObjectState.getValueForKey(portableKey1)).thenReturn(new CDSMValue(portableValue1ObjID, 0, 0, 0, 0));
    ManagedObject managedObject = mock(ManagedObject.class);
    when(managedObject.getManagedObjectState()).thenReturn(managedObjectState);
    serverMapRequestManager.sendResponseFor(mapID, managedObject);
    
    verify(objectManager).lookupObjectsFor(eq(clientID), any(ServerMapRequestPrefetchObjectsContext.class));
  }
}
