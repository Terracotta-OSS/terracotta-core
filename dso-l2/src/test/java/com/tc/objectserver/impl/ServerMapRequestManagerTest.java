/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.heap.HeapKeyValueStorage;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.ServerMapRequestID;
import com.tc.object.dna.api.DNA;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.ObjectReferenceAddListener;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.stats.Stats;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    final ChannelStats channelStats = new ChannelStatsImpl(new CounterManagerImpl(), channelManager);
    final ServerMapRequestManagerImpl serverMapRequestManager = new ServerMapRequestManagerImpl(
                                                                                                objManager,
                                                                                                channelManager,
                                                                                                respondToServerTCMapSink,
                                                                                                managedObjectRequestSink,
                                                                                                null, channelStats);
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
    final ChannelStats channelStats = new ChannelStatsImpl(new CounterManagerImpl(), channelManager);
    final ServerMapRequestManagerImpl serverMapRequestManager = new ServerMapRequestManagerImpl(
                                                                                                objManager,
                                                                                                channelManager,
                                                                                                respondToServerMapSink,
                                                                                                managedObjectRequestSink,
                                                                                                null, channelStats);
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

  public void testPrefetch() throws Exception {
    final ObjectManager objManager = mock(ObjectManager.class);
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final ObjectID portableValue1ObjID = new ObjectID(1001);
    KeyValueStorage<Object, Object> references = new HeapKeyValueStorage<Object, Object>();
    references.put(portableKey1, portableValue1ObjID);
    PersistentObjectFactory persistentObjectFactory = mock(PersistentObjectFactory.class);
    when(persistentObjectFactory.getKeyValueStorage(mapID, true)).thenReturn(references);

    final Sink respondToServerMapSink = mock(Sink.class);
    final TestSink managedObjectRequestSink = new TestSink();
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    TestClientStateManager clientStateManager = new TestClientStateManager();
    final ChannelStats channelStats = new ChannelStatsImpl(new CounterManagerImpl(), channelManager);
    final ServerMapRequestManagerImpl serverMapRequestManager = new ServerMapRequestManagerImpl(
                                                                                                objManager,
                                                                                                channelManager,
                                                                                                respondToServerMapSink,
                                                                                                managedObjectRequestSink,
                                                                                                clientStateManager,
                                                                                                channelStats);
    ServerMapGetValueRequest mapGetValueRequest = new ServerMapGetValueRequest(requestID1,
                                                                               Collections.singleton(portableKey1));
    serverMapRequestManager.requestValues(clientID, mapID, Collections.singletonList(mapGetValueRequest));

    ConcurrentDistributedServerMapManagedObjectState managedObjectState = new TestCDSMManagedObjectState(0, mapID, persistentObjectFactory);
    ManagedObject managedObject = mock(ManagedObject.class);
    when(managedObject.getManagedObjectState()).thenReturn(managedObjectState);
    serverMapRequestManager.sendResponseFor(mapID, managedObject);

    ObjectRequestServerContextImpl context = (ObjectRequestServerContextImpl) managedObjectRequestSink.lastAdded;
    Assert.assertEquals(LOOKUP_STATE.SERVER_INITIATED_FORCED, context.getLookupState());
    Assert.assertTrue(clientStateManager.set.contains(portableValue1ObjID));
  }

  public class TestClientStateManager implements ClientStateManager {
    private final Set<ObjectID> set = new ObjectIDSet();

    @Override
    public Set<ObjectID> addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
      return null;
    }

    @Override
    public void addReference(NodeID nodeID, ObjectID objectID) {
      throw new ImplementMe();
    }

    @Override
    public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {
      set.addAll(oids);
      return oids;
    }

    @Override
    public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references,
                                                         NodeID clientID, Set<ObjectID> objectIDs,
                                                         Invalidations invalidationsForClient) {
      throw new ImplementMe();
    }

    @Override
    public Set<NodeID> getConnectedClientIDs() {
      throw new ImplementMe();
    }

    @Override
    public int getReferenceCount(NodeID nodeID) {
      throw new ImplementMe();
    }

    @Override
    public boolean hasReference(NodeID nodeID, ObjectID objectID) {
      throw new ImplementMe();
    }

    @Override
    public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
      throw new ImplementMe();

    }

    @Override
    public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
      throw new ImplementMe();

    }

    @Override
    public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
      throw new ImplementMe();

    }

    @Override
    public void shutdownNode(NodeID deadNode) {
      throw new ImplementMe();

    }

    @Override
    public boolean startupNode(NodeID nodeID) {
      throw new ImplementMe();
    }

    @Override
    public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
      throw new ImplementMe();

    }

  }

  private static class TestCDSMManagedObjectState extends ConcurrentDistributedServerMapManagedObjectState {

    protected TestCDSMManagedObjectState(long classId, ObjectID id, PersistentObjectFactory persistentObjectFactory) {
      super(classId, id, persistentObjectFactory);
    }

  }

  private static class TestSink implements Sink {
    private Object lastAdded;

    @Override
    public void add(EventContext context) {
      lastAdded = context;
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    @Override
    public void addMany(Collection contexts) {
      throw new ImplementMe();

    }

    @Override
    public void clear() {
      throw new ImplementMe();

    }

    @Override
    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();

    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();

    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void resetStats() {
      throw new ImplementMe();

    }

  }
}
