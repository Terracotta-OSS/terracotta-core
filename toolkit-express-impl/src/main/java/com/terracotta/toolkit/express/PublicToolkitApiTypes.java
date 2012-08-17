/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import org.terracotta.toolkit.InvalidToolkitConfigException;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitCapability;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.api.ToolkitFactoryService;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields.PinningStore;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.AbstractConfiguration;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.SupportedConfigurationType;
import org.terracotta.toolkit.events.ToolkitNotificationEvent;
import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.cache.TimestampedValue;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheMetaDataCallback;
import org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata;
import org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData;
import org.terracotta.toolkit.internal.cluster.OutOfBandClusterListener;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.meta.MetaData;
import org.terracotta.toolkit.internal.search.SearchBuilder;
import org.terracotta.toolkit.internal.search.SearchException;
import org.terracotta.toolkit.internal.search.SearchQueryResultSet;
import org.terracotta.toolkit.internal.search.SearchResult;
import org.terracotta.toolkit.internal.store.ToolkitCacheConfigBuilderInternal;
import org.terracotta.toolkit.internal.store.ToolkitStoreConfigBuilderInternal;
import org.terracotta.toolkit.internal.store.ToolkitStoreConfigFieldsInternal;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;
import org.terracotta.toolkit.object.Destroyable;
import org.terracotta.toolkit.object.ToolkitLockedObject;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;
import org.terracotta.toolkit.tck.TCKStrict;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

abstract class PublicToolkitApiTypes {

  private static final Set<String> PUBLIC_TOOLKIT_TYPE_CLASS_NAMES;
  static {
    Set<String> tmpSet = new HashSet<String>();
    tmpSet.add(AbstractConfiguration.class.getName());
    tmpSet.add(ClusterEvent.Type.class.getName());
    tmpSet.add(ClusterEvent.class.getName());
    tmpSet.add(ClusterInfo.class.getName());
    tmpSet.add(ClusterListener.class.getName());
    tmpSet.add(ClusterNode.class.getName());
    tmpSet.add(Configuration.class.getName());
    tmpSet.add("org.terracotta.toolkit.store.ConfigurationImpl");
    tmpSet.add(Consistency.class.getName());
    tmpSet.add(Destroyable.class.getName());
    tmpSet.add(EntryWithMetaData.class.getName());
    tmpSet.add(InvalidToolkitConfigException.class.getName());
    tmpSet.add(MetaData.class.getName());
    tmpSet.add(NotSerializableRuntimeException.class.getName());
    tmpSet.add(OperatorEventLevel.class.getName());
    tmpSet.add(OutOfBandClusterListener.class.getName());
    tmpSet.add(PinningStore.class.getName());
    tmpSet.add(SearchBuilder.class.getName());
    tmpSet.add(SearchException.class.getName());
    tmpSet.add(SearchQueryResultSet.class.getName());
    tmpSet.add(SearchResult.class.getName());
    tmpSet.add("org.terracotta.toolkit.SecretProvider");
    tmpSet.add(SupportedConfigurationType.class.getName());
    tmpSet.add("org.terracotta.toolkit.config.SupportedConfigurationType$1");
    tmpSet.add("org.terracotta.toolkit.config.SupportedConfigurationType$2");
    tmpSet.add("org.terracotta.toolkit.config.SupportedConfigurationType$3");
    tmpSet.add("org.terracotta.toolkit.config.SupportedConfigurationType$4");
    tmpSet.add(TCKStrict.class.getName());
    tmpSet.add(TerracottaL1Instance.class.getName());
    tmpSet.add(TimestampedValue.class.getName());
    tmpSet.add(Toolkit.class.getName());
    tmpSet.add(ToolkitAtomicLong.class.getName());
    tmpSet.add(ToolkitBarrier.class.getName());
    tmpSet.add(ToolkitBlockingQueue.class.getName());
    tmpSet.add(ToolkitCache.class.getName());
    tmpSet.add(ToolkitCacheConfigBuilder.class.getName());
    tmpSet.add(ToolkitCacheConfigBuilderInternal.class.getName());
    tmpSet.add(ToolkitCacheConfigFields.class.getName());
    tmpSet.add(ToolkitCacheListener.class.getName());
    tmpSet.add(ToolkitCacheMetaDataCallback.class.getName());
    tmpSet.add(ToolkitCacheWithMetadata.class.getName());
    tmpSet.add(ToolkitCapability.class.getName());
    tmpSet.add(ToolkitFactory.class.getName());
    tmpSet.add("org.terracotta.toolkit.ToolkitFactory$ToolkitFactoryServiceLookup$1");
    tmpSet.add("org.terracotta.toolkit.ToolkitFactory$ToolkitTypeSubNameTuple");
    tmpSet.add("org.terracotta.toolkit.ToolkitFactory$ToolkitFactoryServiceLookup");
    tmpSet.add(ToolkitFactoryService.class.getName());
    tmpSet.add(ToolkitInstantiationException.class.getName());
    tmpSet.add(ToolkitInternal.class.getName());
    tmpSet.add(ToolkitList.class.getName());
    tmpSet.add(ToolkitLock.class.getName());
    tmpSet.add(ToolkitLockedObject.class.getName());
    tmpSet.add(ToolkitLockType.class.getName());
    tmpSet.add(ToolkitLockTypeInternal.class.getName());
    tmpSet.add("org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal$1");
    tmpSet.add(ToolkitLogger.class.getName());
    tmpSet.add(ToolkitMap.class.getName());
    tmpSet.add(ToolkitStoreConfigBuilder.class.getName());
    tmpSet.add(ToolkitStoreConfigBuilderInternal.class.getName());
    tmpSet.add("org.terracotta.toolkit.store.ToolkitStoreConfigBuilder$ConfigFieldMapping");
    tmpSet.add(ToolkitStoreConfigFields.class.getName());
    tmpSet.add(ToolkitStoreConfigFieldsInternal.class.getName());
    tmpSet.add(ToolkitNotificationListener.class.getName());
    tmpSet.add(ToolkitNotificationEvent.class.getName());
    tmpSet.add(ToolkitNotifier.class.getName());
    tmpSet.add(ToolkitObject.class.getName());
    tmpSet.add(ToolkitReadWriteLock.class.getName());
    tmpSet.add(ToolkitRuntimeException.class.getName());
    tmpSet.add(ToolkitSet.class.getName());
    tmpSet.add(ToolkitSortedSet.class.getName());
    tmpSet.add(ToolkitStore.class.getName());
    tmpSet.add(ToolkitCacheInternal.class.getName());

    PUBLIC_TOOLKIT_TYPE_CLASS_NAMES = Collections.unmodifiableSet(tmpSet);
  }

  protected static boolean isClassPublicToolkitApiType(Class klass) {
    return isClassPublicToolkitApiType(klass.getName());
  }

  protected static boolean isClassPublicToolkitApiType(String className) {
    return PUBLIC_TOOLKIT_TYPE_CLASS_NAMES.contains(className);
  }

  protected static Set<String> getPublicToolkitApiTypes() {
    return PUBLIC_TOOLKIT_TYPE_CLASS_NAMES;
  }
}
