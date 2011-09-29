/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
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
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.impl.MockSerializedEntry;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.Assert;

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
    Mockito.when(objectManager.lookup(oid)).thenReturn(serializedEntry);
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
                                                                                       new NullRuntimeLogger(),
                                                                                       SampledCounter.NULL_SAMPLED_COUNTER,
                                                                                       store);
    Collection changes = Collections.singletonList(new MyDna(oid, 200));
    Assert.assertEquals(100, serializedEntry.getLastAccessedTime());
    transactionManager.apply(TxnType.NORMAL, null, changes, Collections.EMPTY_MAP);
    Assert.assertEquals(200, serializedEntry.getLastAccessedTime());

    MyMockSerializedEntry entryFetched = (MyMockSerializedEntry) store.getById(oid);
    Assert.assertEquals(200, serializedEntry.getLastAccessedTime());
    Assert.assertTrue(entryFetched.equals(serializedEntry));
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

    public int getArraySize() {
      return 0;
    }

    public DNACursor getCursor() {
      return null;
    }

    public String getDefiningLoaderDescription() {
      return null;
    }

    public ObjectID getObjectID() throws DNAException {
      return oid;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return null;
    }

    public String getTypeName() {
      return null;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public boolean isDelta() {
      return true;
    }

  }

  private class MyTCObjectSelfStore implements TCObjectSelfStore {
    private final HashMap<ObjectID, byte[]> oidtoSerialzed = new HashMap<ObjectID, byte[]>();

    public void addAllObjectIDs(Set oids) {
      throw new ImplementMe();

    }

    public void addAllObjectIDsToValidate(Invalidations invalidations) {
      throw new ImplementMe();

    }

    public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                   Object tcoself, boolean isNew) {
      updateLocalCache(((TCObjectSelf) tcoself).getObjectID(), (TCObjectSelf) tcoself);
      return true;
    }

    public void addTCObjectSelfTemp(TCObjectSelf obj) {
      throw new ImplementMe();

    }

    public boolean contains(ObjectID objectID) {
      throw new ImplementMe();
    }

    public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
      throw new ImplementMe();

    }

    public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
      throw new ImplementMe();

    }

    public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
      throw new ImplementMe();

    }

    public void removeTCObjectSelfTemp(TCObjectSelf value, boolean notifyServer) {
      throw new ImplementMe();

    }

    public int size() {
      throw new ImplementMe();
    }

    public void updateLocalCache(ObjectID oid, TCObjectSelf tcoself) {
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
          oidtoSerialzed.put(((MyMockSerializedEntry) tcoself).getObjectID(), b);
        }
      } catch (Throwable t) {
        throw new AssertionError(t);
      }
    }

    public Object getById(ObjectID oid) {
      try {
        ByteArrayInputStream bais = new ByteArrayInputStream(oidtoSerialzed.get(oid));
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
  }
}
