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
package com.tc.object;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.object.servermap.ExpirableMapEntry;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.platform.PlatformService;

import java.util.Map;

/**
 * @author tim
 */
public class TCObjectServerMapImplTest {
  private ClientObjectManager clientObjectManager;
  private RemoteServerMapManager serverMapManager;
  private ObjectID objectID;
  private L1ServerMapLocalCacheManager globalLocalCacheManager;
  private PlatformService              platformService;
  private ServerMapLocalCache serverMapLocalCache;

  @Before
  public void setUp() throws Exception {
    platformService = mock(PlatformService.class);
    clientObjectManager = mock(ClientObjectManager.class);
    serverMapManager = mock(RemoteServerMapManager.class);
    objectID = new ObjectID(1);
    globalLocalCacheManager = mock(L1ServerMapLocalCacheManager.class);
    serverMapLocalCache = mock(ServerMapLocalCache.class);
    when(globalLocalCacheManager.getOrCreateLocalCache(any(ObjectID.class), any(ClientObjectManager.class),
                                                       any(PlatformService.class), anyBoolean(),
                                                       any(L1ServerMapLocalCacheStore.class),
                                                       any(PinnedEntryFaultCallback.class)))
        .thenReturn(serverMapLocalCache);
  }

  @Test
  public void testGetAllVersioned() throws Exception {
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager);
    when(clientObjectManager.lookup(objectID)).thenReturn(tcObjectServerMap);
    when(clientObjectManager.lookup(new ObjectID(2))).thenReturn(tcObjectServerMap);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        Map<Object, Object> result = (Map<Object, Object>)invocation.getArguments()[1];
        result.put("foo", new CompoundResponse(new ObjectID(3), 1, 2, 3, 4, 5));
        result.put("bar", new CompoundResponse(new ObjectID(3), 1, 2, 3, 4, 4));
        result.put("baz", new CompoundResponse(ObjectID.NULL_ID, 0, 0, 0, 0, 0));
        return null;
      }
    }).when(serverMapManager).getMappingForAllKeys(anyMap(), anyMap());

    ExpirableMapEntry expirableMapEntry = mock(ExpirableMapEntry.class);
    when(clientObjectManager.lookupObjectQuiet(new ObjectID(3))).thenReturn(expirableMapEntry);

    SetMultimap<ObjectID, Object> request = HashMultimap.create();
    request.put(objectID, "foo");
    request.put(new ObjectID(2), "bar");
    request.put(objectID, "baz");

    Map<Object, VersionedObject> result = tcObjectServerMap.getAllVersioned(request);

    assertThat(result, hasEntry((Object) "foo", new VersionedObject(expirableMapEntry, 5)));
    assertThat(result, hasEntry((Object) "bar", new VersionedObject(expirableMapEntry, 4)));
    assertThat(result, hasEntry((Object) "baz", null));
  }

  @Test
  public void testCleanupReplaceOnTimeout() throws Exception {
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager) {
      @Override
      public boolean logicalInvokeWithResult(final LogicalOperation method, final Object[] parameters) throws AbortedOperationException {
        throw new AbortedOperationException();
      }
    };
    tcObjectServerMap.setupLocalStore(mock(L1ServerMapLocalCacheStore.class), mock(PinnedEntryFaultCallback.class));
    TCObjectSelf value = mock(TCObjectSelf.class);
    when(clientObjectManager.lookupOrCreate(eq(value), any(GroupID.class))).thenReturn(value);

    try {
      tcObjectServerMap.doLogicalReplaceUnlocked(null, "foo", "bar", value, null);
      fail("Did not get a timeout");
    } catch (AbortedOperationException e) {
      // expected
    }
    verify(globalLocalCacheManager).removeTCObjectSelfTemp(value, true);
    verify(serverMapLocalCache).removeFromLocalCache("foo");
  }

  @Test
  public void testCleanupOnRemoveTimeout() throws Exception {
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager) {
      @Override
      public boolean logicalInvokeWithResult(final LogicalOperation method, final Object[] parameters) throws AbortedOperationException {
        throw new AbortedOperationException();
      }
    };
    tcObjectServerMap.setupLocalStore(mock(L1ServerMapLocalCacheStore.class), mock(PinnedEntryFaultCallback.class));

    try {
      tcObjectServerMap.doLogicalRemoveUnlocked(null, "foo", "bar", null);
      fail("Did not get a timeout");
    } catch (AbortedOperationException e) {
      // expected
    }
    verify(serverMapLocalCache).removeFromLocalCache("foo");
  }

  @Test
  public void testCleanupOnPutIfAbsentTimeout() throws Exception {
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager) {
      @Override
      public boolean logicalInvokeWithResult(final LogicalOperation method, final Object[] parameters) throws AbortedOperationException {
        throw new AbortedOperationException();
      }
    };
    tcObjectServerMap.setupLocalStore(mock(L1ServerMapLocalCacheStore.class), mock(PinnedEntryFaultCallback.class));
    TCObjectSelf value = mock(TCObjectSelf.class);
    when(clientObjectManager.lookupOrCreate(eq(value), any(GroupID.class))).thenReturn(value);

    try {
      tcObjectServerMap.doLogicalPutIfAbsentUnlocked(null, "foo", value, null);
      fail("Did not get a timeout");
    } catch (AbortedOperationException e) {
      // expected
    }
    verify(globalLocalCacheManager).removeTCObjectSelfTemp(value, true);
  }

  @Test
  public void testExpireWithOID() throws Exception {
    final ObjectID oid = new ObjectID(1);
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager) {
      @Override
      public void logicalInvoke(final LogicalOperation method, final Object[] parameters) {
        assertThat(parameters[1], is((Object) oid));
      }
    };

    TCObjectSelf value = mock(TCObjectSelf.class);
    when(value.getObjectID()).thenReturn(oid);

    tcObjectServerMap.doLogicalExpire("foo", "bar", value);
    tcObjectServerMap.doLogicalExpireUnlocked(null, "bar", value);
  }

  @Test
  public void testSetLastAccessedTimeOID() throws Exception {
    final ObjectID oid = new ObjectID(1);
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(platformService, clientObjectManager,
        serverMapManager, objectID, null, mock(TCClass.class), false, globalLocalCacheManager) {
      @Override
      public void logicalInvoke(final LogicalOperation method, final Object[] parameters) {
        assertThat(parameters[1], is((Object) oid));
      }
    };

    TCObjectSelf value = mock(TCObjectSelf.class);
    when(value.getObjectID()).thenReturn(oid);

    tcObjectServerMap.doLogicalSetLastAccessedTime("foo", value, 1234L);
  }
}
