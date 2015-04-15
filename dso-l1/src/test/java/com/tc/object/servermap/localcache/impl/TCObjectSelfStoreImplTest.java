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
