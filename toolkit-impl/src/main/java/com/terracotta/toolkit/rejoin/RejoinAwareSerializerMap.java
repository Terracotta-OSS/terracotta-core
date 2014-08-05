/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import com.google.common.base.Preconditions;
import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.terracotta.toolkit.object.serialization.SerializerMap;
import com.terracotta.toolkit.object.serialization.SerializerMapImpl;
import com.terracotta.toolkit.roots.impl.RootsUtil;
import com.terracotta.toolkit.roots.impl.RootsUtil.RootObjectCreator;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

public class RejoinAwareSerializerMap<K, V> implements SerializerMap<K, V>, RejoinLifecycleListener {
  private volatile SerializerMap<K, V> delegateMap;
  private final PlatformService        platformService;

  public RejoinAwareSerializerMap(PlatformService plateformService) {
    super();
    this.platformService = plateformService;
    this.delegateMap = lookupOrCreate();
  }

  @Override
  public V put(K key, V value) {
    return delegateMap.put(key, value);
  }

  @Override
  public V get(K key) {
    return delegateMap.get(key);
  }

  @Override
  public V localGet(K key) {
    return delegateMap.localGet(key);
  }

  @Override
  public void onRejoinStart() {
    this.delegateMap = ToolkitInstanceProxy.newRejoinInProgressProxy("SerializerMap", SerializerMap.class);
  }

  @Override
  public void onRejoinComplete() {
    SerializerMapImpl<K, V> afterRejoin = lookupOrCreate();
    Preconditions.checkNotNull(afterRejoin);
    this.delegateMap = afterRejoin;

  }

  private SerializerMapImpl lookupOrCreate() {
    return RootsUtil.lookupOrCreateRootInGroup(platformService, new GroupID(0),
                                               ToolkitTypeConstants.SERIALIZER_MAP_ROOT_NAME,
                                               new RootObjectCreator<SerializerMapImpl>() {
                                                 @Override
                                                 public SerializerMapImpl create() {
                                                   return new SerializerMapImpl(platformService);
                                                 }
                                               });
  }

}
