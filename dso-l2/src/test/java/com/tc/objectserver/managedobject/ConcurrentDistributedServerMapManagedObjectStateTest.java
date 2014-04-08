/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.terracotta.corestorage.KeyValueStorage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.event.ClientChannelMonitor;
import com.tc.objectserver.event.MutationEventPublisher;
import com.tc.objectserver.impl.SamplingType;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.server.ServerEventType;
import com.tc.test.TCTestCase;
import com.tc.util.Events;

import java.util.HashSet;
import java.util.Set;

public class ConcurrentDistributedServerMapManagedObjectStateTest extends TCTestCase {
  private static final ClientID                            CLIENT_ID = new ClientID(1);

  static {
    ManagedObjectStateFactory.disableSingleton(true);
  }

  private ObjectID oid;
  private PersistentObjectFactory persistentObjectFactory;
  private KeyValueStorage<Object, Object> keyValueStorage;
  private ConcurrentDistributedServerMapManagedObjectState state;
  private ManagedObjectChangeListener changeListener;
  private ApplyTransactionInfo applyTransactionInfo;
  private MutationEventPublisher mutationEventPublisher;
  private ClientChannelMonitor                             clientChannelMonitor;

  @Override
  public void setUp() throws Exception {
    oid = new ObjectID(1);
    keyValueStorage = mock(KeyValueStorage.class);
    persistentObjectFactory = mock(PersistentObjectFactory.class);
    when(persistentObjectFactory.getKeyValueStorage(oid, true)).thenReturn(keyValueStorage);
    state = new ConcurrentDistributedServerMapManagedObjectState(0, oid, persistentObjectFactory);
    setInvalidateOnChange(state, true);

    changeListener = mock(ManagedObjectChangeListener.class);
    ManagedObjectChangeListenerProvider listenerProvider = when(mock(ManagedObjectChangeListenerProvider.class).getListener())
        .thenReturn(changeListener)
        .getMock();
    ManagedObjectStateFactory.createInstance(listenerProvider, persistentObjectFactory);

    applyTransactionInfo = searchableApplyInfo();
  }

  public void testCapacityEvictionWhenOverLimit() throws Exception {
    when(keyValueStorage.size()).thenReturn(115L);
    state.applyPhysicalAction(new PhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_COUNT_IN_CLUSTER_FIELDNAME,
        100, false), oid, applyTransactionInfo);
    state.applyPhysicalAction(new PhysicalAction(ConcurrentDistributedServerMapManagedObjectState.EVICTION_ENABLED_FIELDNAME, true, false),
        oid, applyTransactionInfo);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT, new Object[] { "a", oid });
    verify(applyTransactionInfo).initiateEvictionFor(any(ObjectID.class));
  }

  public void testNoCapacityEvictionWhenUnderLimit() throws Exception {
    when(keyValueStorage.size()).thenReturn(114L);
    state.applyPhysicalAction(new PhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_COUNT_IN_CLUSTER_FIELDNAME,
        100, false), oid, applyTransactionInfo);
    state.applyPhysicalAction(new PhysicalAction(ConcurrentDistributedServerMapManagedObjectState.EVICTION_ENABLED_FIELDNAME, true, false),
        oid, applyTransactionInfo);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT, new Object[] { "a", oid });
    verify(applyTransactionInfo, never()).initiateEvictionFor(any(ObjectID.class));
  }

  public void testL2SyncDehyrdate() throws Exception {
    CDSMValue value1 = new CDSMValue(new ObjectID(2), 2, 2, 3, 4);
    CDSMValue value2 = new CDSMValue(new ObjectID(3), 3, 3, 2, 1);
    when(keyValueStorage.keySet()).thenReturn(new HashSet<Object>(asList("key1", "key2")));
    when(keyValueStorage.get("key1")).thenReturn(value1);
    when(keyValueStorage.get("key2")).thenReturn(value2);

    DNAWriter dnaWriter = mock(DNAWriter.class);
    state.dehydrate(oid, dnaWriter, DNAType.L2_SYNC);

    verify(dnaWriter, times(10)).addPhysicalAction(anyString(), any());
    verify(dnaWriter).addLogicalAction(SerializationUtil.PUT, new Object[] { "key1", value1.getObjectID(), value1.getCreationTime(),
        value1.getLastAccessedTime(), value1.getTimeToIdle(), value1.getTimeToLive() });
    verify(dnaWriter).addLogicalAction(SerializationUtil.PUT, new Object[] { "key2", value2.getObjectID(), value2.getCreationTime(),
        value2.getLastAccessedTime(), value2.getTimeToIdle(), value2.getTimeToLive() });
  }

  public void testL1FaultDehydrate() throws Exception {
    DNAWriter dnaWriter = mock(DNAWriter.class);
    state.dehydrate(oid, dnaWriter, DNAType.L1_FAULT);
    verify(dnaWriter, times(10)).addPhysicalAction(anyString(), any());
    verify(dnaWriter, never()).addLogicalAction(anyInt(), any(Object[].class));
  }

  public void testPut() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(2), 0, 0, 0, 0);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT, new Object[] { key, value.getObjectID() });
    verify(keyValueStorage).put(key, value);
    verify(applyTransactionInfo, never()).deleteObject(any(ObjectID.class));
    verify(applyTransactionInfo).recordValue(value.getObjectID(), false);
    verify(applyTransactionInfo, never()).invalidate(any(ObjectID.class), any(ObjectID.class));

    CDSMValue oldValue = new CDSMValue(new ObjectID(2), 0, 1, 2, 3);
    when(keyValueStorage.containsKey(key)).thenReturn(true);
    when(keyValueStorage.get(key)).thenReturn(oldValue);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT, new Object[] { key, value.getObjectID() });
    verify(keyValueStorage).put(key, value);
    verify(applyTransactionInfo).deleteObject(oldValue.getObjectID());
    verify(applyTransactionInfo).recordValue(value.getObjectID(), true);
    verify(applyTransactionInfo).invalidate(oid, oldValue.getObjectID());
  }

  public void testRemove() throws Exception {
    Object key = "key";
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE, new Object[] { key });
    verify(applyTransactionInfo, never()).deleteObject(any(ObjectID.class));
    verify(applyTransactionInfo, never()).invalidate(any(ObjectID.class), any(ObjectID.class));

    CDSMValue oldValue = new CDSMValue(new ObjectID(2), 0, 1, 3, 4);
    when(keyValueStorage.containsKey(key)).thenReturn(true);
    when(keyValueStorage.get(key)).thenReturn(oldValue);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE, new Object[] { key });
    verify(keyValueStorage, times(2)).remove(key);
    verify(applyTransactionInfo).deleteObject(oldValue.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, oldValue.getObjectID());
  }

  public void testRemoveIfEqual() throws Exception {
    ObjectID valueOid = new ObjectID(1);
    Object key = "key";
    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(oid, 0, 0, 0, 0));

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { key, new ObjectID(2) });
    verify(keyValueStorage, never()).remove(key);
    verify(applyTransactionInfo, never()).deleteObject(valueOid);
    verify(applyTransactionInfo, never()).invalidate(oid, valueOid);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { key, valueOid });
    verify(keyValueStorage).remove(key);
    verify(applyTransactionInfo).deleteObject(valueOid);
    verify(applyTransactionInfo).invalidate(oid, valueOid);
  }

  public void testReplace() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(2), 0, 0, 0, 0);

    // doesn't exist yet
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE, new Object[] { key, value.getObjectID() });
    verify(keyValueStorage, never()).put(key, value);
    verify(applyTransactionInfo).deleteObject(value.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value.getObjectID());

    // now it does
    CDSMValue oldValue = new CDSMValue(new ObjectID(3), 0, 0, 0, 0);
    when(keyValueStorage.get(key)).thenReturn(oldValue);
    when(keyValueStorage.containsKey(key)).thenReturn(true);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE, new Object[] { key, value.getObjectID() });
    value.setVersion(1L);
    verify(keyValueStorage).put(key, value);
    verify(applyTransactionInfo).deleteObject(oldValue.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, oldValue.getObjectID());
  }

  public void testReplaceIfEqual() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(2), 0, 0, 0, 0);

    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(new ObjectID(1), 0, 0, 0, 0));
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE_IF_VALUE_EQUAL, new Object[] { key, new ObjectID(3), value
        .getObjectID() });
    verify(keyValueStorage, never()).put(key, value);
    verify(applyTransactionInfo).deleteObject(value.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value.getObjectID());
    verify(applyTransactionInfo).recordValue(new ObjectID(2), false);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE_IF_VALUE_EQUAL, new Object[] { key, new ObjectID(1), value
        .getObjectID() });
    value.setVersion(1L);
    verify(keyValueStorage).put(key, value);
    verify(applyTransactionInfo).deleteObject(new ObjectID(1));
    verify(applyTransactionInfo).invalidate(oid, new ObjectID(1));
    verify(applyTransactionInfo).recordValue(new ObjectID(2), true);
  }

  public void testPutIfAbsent() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(1), 0, 0, 0, 0);
    when(keyValueStorage.containsKey(key)).thenReturn(true);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT_IF_ABSENT, new Object[] { key, value.getObjectID() });
    verify(keyValueStorage, never()).put(key, value);
    verify(applyTransactionInfo).deleteObject(value.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value.getObjectID());
    verify(applyTransactionInfo).recordValue(new ObjectID(1), true);

    when(keyValueStorage.containsKey(key)).thenReturn(false);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT_IF_ABSENT, new Object[] { key, value.getObjectID() });
    verify(keyValueStorage).put(key, value);
    verify(applyTransactionInfo).recordValue(new ObjectID(1), false);
  }


  public void testPutIfAbsentVersioned() throws Exception {
    Object key = "key";
    CDSMValue newValue = new CDSMValue(new ObjectID(1), 0, 0, 0, 0, 5);

    // when the key mapping was present
    CDSMValue oldValue = new CDSMValue(new ObjectID(1), 0, 0, 0, 0, 6);
    when(keyValueStorage.get(key)).thenReturn(oldValue);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT_IF_ABSENT_VERSIONED, new Object[] {
        key, newValue.getObjectID(), 0L, 0L, 0L, 0L, 5L });
    verify(keyValueStorage, never()).put(key, newValue);
    verify(applyTransactionInfo).deleteObject(newValue.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, newValue.getObjectID());

    // when the key mapping was absent
    when(keyValueStorage.get(key)).thenReturn(null);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT_IF_ABSENT_VERSIONED, new Object[] { key,
        newValue.getObjectID(), 0L, 0L, 0L, 0L, 5L });
    verify(keyValueStorage).put(key, newValue);

  }

  public void testClearVersioned() throws Exception {
    final Set<Object> keys = ImmutableSet.<Object>of(0, 1, 2);
    final CDSMValue value1 = new CDSMValue(new ObjectID(0), 0, 0, 0, 0, 0);
    final CDSMValue value2 = new CDSMValue(new ObjectID(1), 0, 0, 0, 0, 1);
    final CDSMValue value3 = new CDSMValue(new ObjectID(2), 0, 0, 0, 0, 2);
    when(keyValueStorage.keySet()).thenReturn(keys);
    when(keyValueStorage.get(0)).thenReturn(value1);
    when(keyValueStorage.get(1)).thenReturn(value2);
    when(keyValueStorage.get(2)).thenReturn(value3);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.CLEAR_VERSIONED, ArrayUtils.EMPTY_OBJECT_ARRAY);

    verify(applyTransactionInfo).deleteObject(value1.getObjectID());
    verify(applyTransactionInfo).deleteObject(value2.getObjectID());
    verify(applyTransactionInfo).deleteObject(value3.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value1.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value2.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value3.getObjectID());

    verify(mutationEventPublisher, times(3)).publishEvent(any(Set.class), eq(ServerEventType.REMOVE), any(), any(CDSMValue.class), anyString());
    verify(mutationEventPublisher, never()).publishEvent(any(Set.class), eq(ServerEventType.REMOVE_LOCAL), any(), any(CDSMValue.class), anyString());
  }

  public void testMustDeleteObjectsOnClearAndSendEvents() throws Exception {
    final Set<Object> keys = ImmutableSet.<Object>of(0, 1, 2);
    final CDSMValue value1 = new CDSMValue(new ObjectID(0), 0, 0, 0, 0, 0);
    final CDSMValue value2 = new CDSMValue(new ObjectID(1), 0, 0, 0, 0, 1);
    final CDSMValue value3 = new CDSMValue(new ObjectID(2), 0, 0, 0, 0, 2);
    when(keyValueStorage.keySet()).thenReturn(keys);
    when(keyValueStorage.get(0)).thenReturn(value1);
    when(keyValueStorage.get(1)).thenReturn(value2);
    when(keyValueStorage.get(2)).thenReturn(value3);

    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.CLEAR, ArrayUtils.EMPTY_OBJECT_ARRAY);

    verify(applyTransactionInfo).deleteObject(value1.getObjectID());
    verify(applyTransactionInfo).deleteObject(value2.getObjectID());
    verify(applyTransactionInfo).deleteObject(value3.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value1.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value2.getObjectID());
    verify(applyTransactionInfo).invalidate(oid, value3.getObjectID());

    verify(mutationEventPublisher, times(3)).publishEvent(any(Set.class), eq(ServerEventType.REMOVE), any(), any(CDSMValue.class), anyString());
    verify(mutationEventPublisher, times(3)).publishEvent(any(Set.class), eq(ServerEventType.REMOVE_LOCAL), any(), any(CDSMValue.class),
        anyString());
  }

  public void testSetLastAccessedTime() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(1), 0, 0, 0, 0);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.SET_LAST_ACCESSED_TIME, new Object[] { key, value
        .getObjectID(), 42L });
    verify(keyValueStorage, never()).put(key, value);

    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(new ObjectID(1), 0, 0, 0, 0));
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.SET_LAST_ACCESSED_TIME, new Object[] { key, value
        .getObjectID(), 42L });
    value.setLastAccessedTime(42);
    verify(keyValueStorage).put(key, value);
  }

  public void testPutWithExpiry() throws Exception {
    Object key = "key";
    ObjectID valueOid = new ObjectID(1);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT, new Object[] { key, valueOid, 1L, 2L, 3L, 4L });
    verify(keyValueStorage).put(key, new CDSMValue(valueOid, 1L, 2L, 3L, 4L));
  }

  public void testPutIfAbsentWithExpiry() throws Exception {
    Object key = "key";
    ObjectID valueOid = new ObjectID(1);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.PUT_IF_ABSENT, new Object[] { key, valueOid, 1L, 2L, 3L, 4L });
    verify(keyValueStorage).put(key, new CDSMValue(valueOid, 1L, 2L, 3L, 4L));
  }

  public void testReplaceWithExpiry() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(2), 2, 2, 3, 4);
    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(new ObjectID(1), 3, 3, 2, 1));
    when(keyValueStorage.containsKey(key)).thenReturn(true);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE, new Object[] { key,
        value.getObjectID(), value.getCreationTime(), value.getLastAccessedTime(), value.getTimeToIdle(), value.getTimeToLive() });
    value.setVersion(1L);
    verify(keyValueStorage).put(key, value);
  }

  public void testReplaceIfEqualWithExpiry() throws Exception {
    Object key = "key";
    CDSMValue value = new CDSMValue(new ObjectID(2), 2, 2, 3, 4);
    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(new ObjectID(1), 0, 0, 0, 0));
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REPLACE_IF_VALUE_EQUAL, new Object[] { key,
        new ObjectID(1), value.getObjectID(), value.getCreationTime(), value.getLastAccessedTime(),
        value.getTimeToIdle(), value.getTimeToLive() });
    value.setVersion(1L);
    verify(keyValueStorage).put(key, value);
  }

  public void testShouldSendOperationCountChangeEventOnEachPut() throws Exception {
    final TestDNACursor cursor = createOperationRateDNACursor();
    final OperationCountChangeEventListener listener = new OperationCountChangeEventListener();
    state.getOperationEventBus().register(listener);
    state.apply(new ObjectID(3), cursor, new ApplyTransactionInfo());
    assertEquals(200, listener.count);
  }

  public void testEvictionEvent() throws Exception {
    ObjectID valueID = new ObjectID(1);
    String key = "foo";
    when(applyTransactionInfo.isEviction()).thenReturn(true);
    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(valueID, 1, 1, 1, 1, 1));
    state.getRandomSamples(1, mock(ClientObjectReferenceSet.class), SamplingType.FOR_EVICTION);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { key, valueID });

    verify(mutationEventPublisher).publishEvent(new HashSet(), ServerEventType.EVICT, key,
                                                new CDSMValue(ObjectID.NULL_ID), null);
  }

  public void testExpirationEvent() throws Exception {
    ObjectID valueID = new ObjectID(1);
    String key = "foo";
    when(applyTransactionInfo.isEviction()).thenReturn(true);
    when(keyValueStorage.get(key)).thenReturn(new CDSMValue(valueID, 1, 1, 1, 1, 1));
    state.getRandomSamples(1, mock(ClientObjectReferenceSet.class), SamplingType.FOR_EXPIRATION);
    state.applyLogicalAction(oid, applyTransactionInfo, SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { key, valueID });

    verify(mutationEventPublisher).publishEvent(new HashSet(), ServerEventType.EXPIRE, key,
                                                new CDSMValue(ObjectID.NULL_ID), null);
  }

  public static final class OperationCountChangeEventListener {
    private int count;

    @Subscribe
    public void writeOperationCountEvent(Events.WriteOperationCountChangeEvent event) {
      this.count += event.getDelta();
    }
  }

  private TestDNACursor createOperationRateDNACursor() {

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "bob", false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCK_TYPE_FIELDNAME, 1, false);


    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { "key-" + (1000 + i), new ObjectID(1000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT_IF_ABSENT, new Object[] { "key-" + (2000 + i), new ObjectID(2000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { "key-" + (1000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.REPLACE, new Object[] { "key-" + (2000 + i), new ObjectID(5000 + i) });
    }

    return cursor;
  }

  private ApplyTransactionInfo searchableApplyInfo() {
    return searchableApplyInfoForNode(CLIENT_ID);
  }

  private ApplyTransactionInfo searchableApplyInfoForNode(NodeID nodeID) {
    final ApplyTransactionInfo info = mock(ApplyTransactionInfo.class);
    when(info.isActiveTxn()).thenReturn(true);
    when(info.isSearchEnabled()).thenReturn(true);
    ServerTransactionID stxID = mock(ServerTransactionID.class);
    when(stxID.getSourceID()).thenReturn(nodeID);
    when(info.getServerTransactionID()).thenReturn(stxID);

    mutationEventPublisher = mock(MutationEventPublisher.class);
    when(info.getMutationEventPublisher()).thenReturn(mutationEventPublisher);

    clientChannelMonitor = mock(ClientChannelMonitor.class);
    when(info.getClientChannelMonitor()).thenReturn(clientChannelMonitor);
    return info;
  }

  private void setInvalidateOnChange(ConcurrentDistributedServerMapManagedObjectState state, boolean invalidateOnChange) {
    state.applyPhysicalAction(new PhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE_FIELDNAME,
        invalidateOnChange, false), ObjectID.NULL_ID, searchableApplyInfo());
  }

  public void testRegisterServerEventListener() throws Exception {
    ClientID client11 = new ClientID(11);
    ClientID client22 = new ClientID(22);

    state.applyLogicalAction(oid, searchableApplyInfoForNode(client11),
                            SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal(), ServerEventType.REMOVE_LOCAL.ordinal() });
    state.applyLogicalAction(oid, searchableApplyInfoForNode(client22),
                             SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal() });
    
    Assert.assertEquals(Sets.newHashSet(client11, client22), state.getRegisteredClients(ServerEventType.PUT_LOCAL));
    Assert.assertEquals(Sets.newHashSet(client11), state.getRegisteredClients(ServerEventType.REMOVE_LOCAL));
  }

  public void testUnregisterServerEventListener() throws Exception {
    ClientID client11 = new ClientID(11);
    ClientID client22 = new ClientID(22);

    state.applyLogicalAction(oid, searchableApplyInfoForNode(client11),
                            SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal(), ServerEventType.REMOVE_LOCAL.ordinal() });
    state.applyLogicalAction(oid, searchableApplyInfoForNode(client22),
                             SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal() });
    
    state.applyLogicalAction(oid, searchableApplyInfoForNode(client11),
                             SerializationUtil.UNREGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal() });
    
    Assert.assertEquals(Sets.newHashSet(client22), state.getRegisteredClients(ServerEventType.PUT_LOCAL));
    Assert.assertEquals(Sets.newHashSet(client11), state.getRegisteredClients(ServerEventType.REMOVE_LOCAL));
  }

  public void testRelayedRegisterServerEventListener() throws Exception {
    ClientID client11 = new ClientID(11);
    ClientID client22 = new ClientID(22);

    state.applyLogicalAction(oid, searchableApplyInfoForNode(new ServerID()),
                             SerializationUtil.REGISTER_SERVER_EVENT_LISTENER_PASSIVE,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal(), 11L, 22L });
    state.applyLogicalAction(oid, searchableApplyInfoForNode(new ServerID()),
                             SerializationUtil.REGISTER_SERVER_EVENT_LISTENER_PASSIVE,
                             new Object[] { ServerEventType.REMOVE_LOCAL.ordinal(), 11L });
    
    Assert.assertEquals(Sets.newHashSet(client11, client22), state.getRegisteredClients(ServerEventType.PUT_LOCAL));
    Assert.assertEquals(Sets.newHashSet(client11), state.getRegisteredClients(ServerEventType.REMOVE_LOCAL));
  }
  
  public void testRemoveEventListeningClient() throws Exception {
    ClientID client11 = new ClientID(11);
    ClientID client22 = new ClientID(22);

    state.applyLogicalAction(oid, searchableApplyInfoForNode(client11),
                            SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal(), ServerEventType.REMOVE_LOCAL.ordinal() });
    state.applyLogicalAction(oid, searchableApplyInfoForNode(client22),
                             SerializationUtil.REGISTER_SERVER_EVENT_LISTENER,
                             new Object[] { ServerEventType.PUT_LOCAL.ordinal() });
    
    state.applyLogicalAction(oid, searchableApplyInfoForNode(client11),
                             SerializationUtil.REMOVE_EVENT_LISTENING_CLIENT, new Object[] { 11L });

    Assert.assertEquals(Sets.newHashSet(client22), state.getRegisteredClients(ServerEventType.PUT_LOCAL));
    Assert.assertEquals(Sets.newHashSet(), state.getRegisteredClients(ServerEventType.REMOVE_LOCAL));
  }
  
}
