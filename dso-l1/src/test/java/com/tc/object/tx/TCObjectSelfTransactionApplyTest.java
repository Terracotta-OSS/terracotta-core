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
package com.tc.object.tx;

import org.mockito.Mockito;

import com.tc.abortable.NullAbortableOperationManager;
import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.impl.MockSerializedEntry;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import junit.framework.TestCase;

public class TCObjectSelfTransactionApplyTest extends TestCase {

  public void testApply() throws Exception {
    ObjectID oid = new ObjectID(1001);
    MyMockSerializedEntry serializedEntry = new MyMockSerializedEntry(oid, "1001".getBytes(), null);
    serializedEntry.setLastAccessedTime(100);
    ClientObjectManager objectManager = Mockito.mock(ClientObjectManager.class);
    Mockito.when(objectManager.lookupQuiet(oid)).thenReturn(serializedEntry);
    TCObjectSelfStore store = new MyTCObjectSelfStore();
    store.addTCObjectSelf(null, null, serializedEntry, false);

    ClientTransactionManagerImpl transactionManager = new ClientTransactionManagerImpl(
                                                                                       Mockito
                                                                                           .mock(ClientIDProvider.class),
                                                                                       objectManager,
                                                                                       Mockito
                                                                                           .mock(ClientTransactionFactory.class),
                                                                                       Mockito
                                                                                           .mock(ClientLockManager.class),
                                                                                       Mockito
                                                                                           .mock(RemoteTransactionManager.class),
                                                                                       SampledCounter.NULL_SAMPLED_COUNTER,
                                                                                       store,
                                                                                       new NullAbortableOperationManager());
    Collection changes = Collections.singletonList(new MyDna(oid, 200));
    Assert.assertEquals(100, serializedEntry.getLastAccessedTime());
    transactionManager.apply(TxnType.NORMAL, null, changes, Collections.EMPTY_MAP);
    Assert.assertEquals(100, serializedEntry.getLastAccessedTime());

    MyMockSerializedEntry entryFetched = (MyMockSerializedEntry) store.getById(oid);
    Assert.assertNull(entryFetched);
  }

  private static class MyMockSerializedEntry extends MockSerializedEntry {
    public MyMockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz) {
      super(id, array, tcClazz);
    }

    @Override
    public void hydrate(DNA dna, boolean force, WeakReference peer) {
      if (dna instanceof MyDna) {
        setLastAccessedTime(((MyDna) dna).getLastAccessed());
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MyMockSerializedEntry other = (MyMockSerializedEntry) obj;
      if (!Arrays.equals(getSerializedBytes(), other.getSerializedBytes())) return false;
      if (getLastAccessedTime() != other.getLastAccessedTime()) return false;
      return true;
    }

  }

  private static class MyDna implements DNA {
    private final ObjectID oid;
    private final int      lastAccessed;

    public MyDna(ObjectID oid, int lastAccessed) {
      this.oid = oid;
      this.lastAccessed = lastAccessed;
    }

    public int getLastAccessed() {
      return this.lastAccessed;
    }

    @Override
    public int getArraySize() {
      return 0;
    }

    @Override
    public DNACursor getCursor() {
      return null;
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return oid;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return null;
    }

    @Override
    public String getTypeName() {
      return null;
    }

    @Override
    public long getVersion() {
      return 0;
    }

    @Override
    public boolean hasLength() {
      return false;
    }

    @Override
    public boolean isDelta() {
      return true;
    }
  }

  private class MyTCObjectSelfStore implements TCObjectSelfStore {
    private final HashMap<ObjectID, byte[]> oidtoSerialized = new HashMap<ObjectID, byte[]>();

    @Override
    public void cleanup() {
      throw new ImplementMe();
    }

    @Override
    public void addAllObjectIDs(Set oids) {
      throw new ImplementMe();

    }

    @Override
    public ObjectIDSet getObjectIDsToValidate(final NodeID remoteNode) {
      throw new ImplementMe();
    }

    @Override
    public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                   Object tcoself, boolean isNew) {
      try {
        if (tcoself instanceof MyMockSerializedEntry) {
          MyMockSerializedEntry entry = (MyMockSerializedEntry) tcoself;
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos);
          oos.writeLong(entry.getLastAccessedTime());
          oos.writeInt(entry.getSerializedBytes().length);
          oos.write(entry.getSerializedBytes());
          oos.flush();

          byte[] b = baos.toByteArray();
          oidtoSerialized.put(((MyMockSerializedEntry) tcoself).getObjectID(), b);
        }
        return true;
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public void addTCObjectSelfTemp(TCObjectSelf obj) {
      throw new ImplementMe();

    }

    @Override
    public boolean contains(ObjectID objectID) {
      throw new ImplementMe();
    }

    @Override
    public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
      throw new ImplementMe();

    }

    @Override
    public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
      throw new ImplementMe();

    }

    @Override
    public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
      throw new ImplementMe();

    }

    @Override
    public void removeTCObjectSelfTemp(TCObjectSelf value, boolean notifyServer) {
      throw new ImplementMe();

    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void removeObjectById(ObjectID oid) {
      try {
        oidtoSerialized.remove(oid);
      } catch (Throwable t) {
        throw new AssertionError(t);
      }
    }

    @Override
    public Object getById(ObjectID oid) {
      try {
        byte[] serialized = oidtoSerialized.get(oid);
        if (serialized == null) { return null; }
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bais);
        long lastAccessedTime = ois.readLong();
        int len = ois.readInt();
        byte[] b = new byte[len];
        ois.readFully(b);
        MyMockSerializedEntry entry = new MyMockSerializedEntry(oid, b, null);
        entry.setLastAccessedTime(lastAccessedTime);
        return entry;
      } catch (Throwable t) {
        throw new AssertionError(t);
      }
    }

    @Override
    public void shutdown(boolean fromShutdownHook) {
      throw new ImplementMe();

    }

    @Override
    public void removeTCObjectSelf(TCObjectSelf self) {
      throw new ImplementMe();

    }

    @Override
    public void rejoinInProgress(boolean rejoinInProgress) {
      throw new ImplementMe();

    }
  }
}
