/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfImpl;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.ConcurrentHashMap;

public class TCObjectSelfStoreImplTest extends TCTestCase {
  // test for CRQ-263, getObjectByID method stucks in a tight loop on interrupted exception
  public void testInterruptDuringGetByID() throws Exception {
    ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback> dummyCacheMap = new ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback>();

    final TCObjectSelfStore store = new TCObjectSelfStoreImpl(dummyCacheMap);
    TCObjectSelfImpl tcObjectSelfImpl = new TCObjectSelfImpl();
    tcObjectSelfImpl.initializeTCObject(new ObjectID(1), Mockito.mock(TCClass.class), true);
    final TCObjectSelfCallback selfCallback = Mockito.mock(TCObjectSelfCallback.class);
    store.initializeTCObjectSelfStore(selfCallback);
    store.addTCObjectSelf(Mockito.mock(L1ServerMapLocalCacheStore.class),
                          Mockito.mock(AbstractLocalCacheStoreValue.class), tcObjectSelfImpl, true);
    Thread objectLookupThread = new Thread() {
      @Override
      public void run() {
        synchronized (selfCallback) {
          store.getById(new ObjectID(1));
        }
      }
    };
    objectLookupThread.start();
    ThreadUtil.reallySleep(1000);
    objectLookupThread.interrupt();
    ThreadUtil.reallySleep(1000);
    store.removeTCObjectSelf(tcObjectSelfImpl);
    objectLookupThread.join();
  }
}
