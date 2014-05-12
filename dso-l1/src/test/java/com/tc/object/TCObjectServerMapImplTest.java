package com.tc.object;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.tc.object.bytecode.Manager;
import com.tc.object.servermap.ExpirableMapEntry;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class TCObjectServerMapImplTest {
  private Manager manager;
  private ClientObjectManager clientObjectManager;
  private RemoteServerMapManager serverMapManager;
  private ObjectID objectID;
  private L1ServerMapLocalCacheManager globalLocalCacheManager;

  @Before
  public void setUp() throws Exception {
    manager = mock(Manager.class);
    clientObjectManager = mock(ClientObjectManager.class);
    serverMapManager = mock(RemoteServerMapManager.class);
    objectID = new ObjectID(1);
    globalLocalCacheManager = mock(L1ServerMapLocalCacheManager.class);
  }

  @Test
  public void testGetAllVersioned() throws Exception {
    TCObjectServerMap tcObjectServerMap = new TCObjectServerMapImpl(manager, clientObjectManager,
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
}
