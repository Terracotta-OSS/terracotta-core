/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.managedobject;

import org.terracotta.corestorage.KeyValueStorage;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.test.TCTestCase;

import java.util.Collections;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class MapManagedObjectStateTest extends TCTestCase {
  static {
    ManagedObjectStateFactory.disableSingleton(true);
  }

  private ObjectID oid;
  private PersistentObjectFactory persistentObjectFactory;
  private MapManagedObjectState mapManagedObjectState;
  private KeyValueStorage<Object, Object> keyValueStorage;
  private ApplyTransactionInfo applyTransactionInfo;
  private ManagedObjectChangeListener managedObjectChangeListener;

  @Override
  public void setUp() throws Exception {
    persistentObjectFactory = mock(PersistentObjectFactory.class);
    ManagedObjectChangeListenerProvider listenerProvider = mock(ManagedObjectChangeListenerProvider.class);
    managedObjectChangeListener = mock(ManagedObjectChangeListener.class);
    when(listenerProvider.getListener()).thenReturn(managedObjectChangeListener);
    ManagedObjectStateFactory.createInstance(listenerProvider, persistentObjectFactory);
    oid = new ObjectID(0);
    keyValueStorage = mock(KeyValueStorage.class);
    when(persistentObjectFactory.getKeyValueStorage(oid, true)).thenReturn(keyValueStorage);
    mapManagedObjectState = new MapManagedObjectState(0, oid, persistentObjectFactory);
    applyTransactionInfo = mock(ApplyTransactionInfo.class);
  }

  public void testUnknownLogicalAction() throws Exception {
    try {
      mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.SET_LAST_ACCESSED_TIME, new Object[0]);
      fail();
    } catch (AssertionError e) {
      // expected
    }
  }

  public void testPutLiterals() throws Exception {
    mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.PUT, new Object[] { "key", "value" });
    verify(keyValueStorage).put("key", "value");
    verify(managedObjectChangeListener, never()).changed(any(ObjectID.class), any(ObjectID.class), any(ObjectID.class));
  }

  public void testPutObjectKeyValue() throws Exception {
    ObjectID key = new ObjectID(1);
    ObjectID value = new ObjectID(1);
    mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.PUT, new Object[] { key, value });
    verify(keyValueStorage).put(key, value);
    verify(managedObjectChangeListener, times(2)).changed(eq(oid), (ObjectID)isNull(), or(eq(value), eq(key)));
  }

  public void testPutOverExistingKey() throws Exception {
    ObjectID oldOid = new ObjectID(1);
    ObjectID newOid = new ObjectID(2);
    when(keyValueStorage.get("key")).thenReturn(oldOid);
    mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.PUT, new Object[] { "key", newOid });
    verify(keyValueStorage).put("key", newOid);
  }

  public void testRemoveMissingKey() throws Exception {
    mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.REMOVE, new Object[] { "key" });
    verify(applyTransactionInfo, never()).deleteObject(any(ObjectID.class));
  }

  public void testRemoveKey() throws Exception {
    ObjectID key = new ObjectID(2);
    ObjectID value = new ObjectID(1);
    when(keyValueStorage.get(key)).thenReturn(value);
    mapManagedObjectState.applyLogicalAction(value, applyTransactionInfo, LogicalOperation.REMOVE, new Object[] { key });
    verify(keyValueStorage).remove(key);
  }

  public void testClear() throws Exception {
    ObjectID key = new ObjectID(1);
    ObjectID value = new ObjectID(2);
    when(keyValueStorage.keySet()).thenReturn(Collections.singleton((Object)key));
    when(keyValueStorage.values()).thenReturn(Collections.singleton((Object)value));
    mapManagedObjectState.applyLogicalAction(oid, applyTransactionInfo, LogicalOperation.CLEAR, new Object[0]);
    verify(keyValueStorage).clear();
  }

  public void testDestroy() throws Exception {
    mapManagedObjectState.destroy();
    verify(persistentObjectFactory).destroyKeyValueStorage(oid);
  }
}
