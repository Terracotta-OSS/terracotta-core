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