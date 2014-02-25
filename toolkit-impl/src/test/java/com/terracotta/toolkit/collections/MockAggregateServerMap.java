/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;

import com.tc.abortable.AbortedOperationException;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class MockAggregateServerMap extends AggregateServerMap {
  @Mock
  private InternalToolkitMap         anyServerMap;
  private final Map                  sharedMap = new ConcurrentHashMap();

  @Mock
  private L1ServerMapLocalCacheStore localCacheStore;
  @Mock
  private TCObjectServerMap          anyTcObjectServerMap;

  public MockAggregateServerMap(ToolkitObjectType type, SearchFactory searchBuilderFactory,
                                DistributedClusteredObjectLookup lookup, String name,
                                ToolkitObjectStripe[] stripeObjects, Configuration config, Callable schemaCreator,
                                ServerMapLocalStoreFactory serverMapLocalStoreFactory, PlatformService platformService,
                                ToolkitLock configMutationLock) {
    super(type, searchBuilderFactory, lookup, name, stripeObjects, config, schemaCreator, serverMapLocalStoreFactory,
          platformService, configMutationLock);
    MockitoAnnotations.initMocks(this);
    when(anyServerMap.put(any(), any(), anyInt(), anyInt(), anyInt())).thenAnswer(putAction());
    when(anyServerMap.get(any())).thenReturn(getAction());
    doAnswer(clearLocalCacheAction()).when(anyServerMap).clearLocalCache();

  }

  @Override
  protected L1ServerMapLocalCacheStore createLocalCacheStore() {
    return localCacheStore;
  }

  @Override
  protected InternalToolkitMap getAnyServerMap() {
    return anyServerMap;
  }

  @Override
  protected TCObjectServerMap getAnyTCObjectServerMap() {
    try {
      when(anyTcObjectServerMap.getAllSize((TCServerMap[]) any())).thenReturn((long) sharedMap.size());
    } catch (AbortedOperationException e) {
      // ignore
    }
    return anyTcObjectServerMap;
  }

  @Override
  protected InternalToolkitMap getServerMapForKey(Object key) {
    return getAnyServerMap();
  }

  private Answer clearLocalCacheAction() {
    return new Answer() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        sharedMap.clear();
        return null;
      }
    };

  }

  private Answer getAction() {
    return new Answer() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return sharedMap.get(args[0]);
      }
    };
  }

  private Answer putAction() {
    return new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return sharedMap.put(args[0], args[1]);
      }
    };
  }

}