/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT_LOCAL;
import static com.tc.server.ServerEventType.REMOVE_LOCAL;
import static com.terracotta.toolkit.config.ConfigUtil.distributeInStripes;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.search.ToolkitAttributeExtractorInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal.LOCK_STRATEGY;
import org.terracotta.toolkit.rejoin.RejoinException;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SearchException;
import org.terracotta.toolkit.search.SearchQueryResultSet;
import org.terracotta.toolkit.search.ToolkitSearchQuery;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeType;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;
import org.terracotta.toolkit.store.ToolkitStore;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.search.SearchRequestIDGenerator;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;
import com.tc.platform.PlatformService;
import com.tc.search.SearchRequestID;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;
import com.tc.util.Util;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.bulkload.BufferBackend;
import com.terracotta.toolkit.bulkload.BufferedOperation;
import com.terracotta.toolkit.cluster.TerracottaClusterInfo;
import com.terracotta.toolkit.collections.map.ServerMap.GetType;
import com.terracotta.toolkit.collections.map.ToolkitMapAggregateSet.ClusteredMapAggregateEntrySet;
import com.terracotta.toolkit.collections.map.ToolkitMapAggregateSet.ClusteredMapAggregateKeySet;
import com.terracotta.toolkit.collections.map.ToolkitMapAggregateSet.ClusteredMapAggregatedValuesCollection;
import com.terracotta.toolkit.collections.servermap.L1ServerMapLocalCacheStoreImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfigParameters;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.config.ConfigChangeListener;
import com.terracotta.toolkit.config.ImmutableConfiguration;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.object.DestroyApplicator;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializedMapValue;
import com.terracotta.toolkit.object.serialization.SerializedMapValueParameters;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.search.SearchableEntity;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;
import com.terracotta.toolkit.type.DistributedToolkitType;
import com.terracottatech.search.SearchBuilder.Search;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AggregateServerMap<K, V> implements DistributedToolkitType<InternalToolkitMap<K, V>>,
    ToolkitCacheInternal<K,V>, ToolkitStore<K,V>, ConfigChangeListener, ValuesResolver<K, V>, SearchableEntity,
    BufferBackend<K, V>, ServerEventDestination {
  private static final TCLogger                                            LOGGER                             = TCLogging
                                                                                                                  .getLogger(AggregateServerMap.class);

  public static final int                                                  DEFAULT_MAX_SIZEOF_DEPTH           = 1000;

  private static final String                                              EHCACHE_GETALL_BATCH_SIZE_PROPERTY = "ehcache.getAll.batchSize";
  private static final int                                                 DEFAULT_GETALL_BATCH_SIZE          = 1000;
  private final static String                                              SNAPSHOT_TXN_LOCK_ID               = "snapshot_txn_lock";
  private final static List<ToolkitObjectType>                             VALID_TYPES                        = Arrays
                                                                                                                  .asList(ToolkitObjectType.STORE,
                                                                                                                      ToolkitObjectType.CACHE);

  private final int                                                        getAllBatchSize;

  protected volatile InternalToolkitMap<K, V>[]                            serverMaps;
  protected final String                                                   name;
  protected final UnclusteredConfiguration                                 config;
  protected final CopyOnWriteArrayList<ToolkitCacheListener<K>>            listeners;
  private volatile ToolkitObjectStripe<InternalToolkitMap<K, V>>[]         stripeObjects;
  private final Consistency                                                consistency;
  private final TimeSource                                                 timeSource;
  private final SearchFactory                                              searchBuilderFactory;
  private final ServerMapLocalStoreFactory                                 serverMapLocalStoreFactory;
  private final TerracottaClusterInfo                                      clusterInfo;
  private final PlatformService                                            platformService;
  private final SearchRequestIDGenerator                                   searchReqIdGenerator;
  private final ToolkitLock configMutationLock;
  private final Callable<ToolkitMap<String, String>>                       schemaCreator;
  private final DistributedClusteredObjectLookup<InternalToolkitMap<K, V>> lookup;
  private final ToolkitObjectType                                          toolkitObjectType;
  private final L1ServerMapLocalCacheStore<K, V>                           localCacheStore;
  private final PinnedEntryFaultCallback                                   pinnedEntryFaultCallback;
  private volatile boolean                                                 lookupSuccessfulAfterRejoin;
  private final AtomicReference<ToolkitMap<String, String>>                attrSchema                         = new AtomicReference<ToolkitMap<String, String>>();
  private final LOCK_STRATEGY                                              lockStrategy;
  private volatile ToolkitAttributeExtractor                               attributeExtractor;
  private final CopyOnWriteArraySet<VersionUpdateListener<K, V>>           versionUpdateListeners;
  private final ToolkitLock                                                concurrentLock;
  private final TaskRunner                                                 taskRunner;
  private final Timer timer;

  protected int getTerracottaProperty(String propName, int defaultValue) {
    try {
      return platformService.getTCProperties().getInt(propName, defaultValue);
    } catch (UnsupportedOperationException e) {
      // for unit-tests
      return defaultValue;
    }
  }

  public AggregateServerMap(ToolkitObjectType type, SearchFactory searchBuilderFactory,
                            DistributedClusteredObjectLookup<InternalToolkitMap<K, V>> lookup, String name,
                            ToolkitObjectStripe<InternalToolkitMap<K, V>>[] stripeObjects, Configuration config,
                            Callable<ToolkitMap<String, String>> schemaCreator,
                            ServerMapLocalStoreFactory serverMapLocalStoreFactory, PlatformService platformService,
                            ToolkitLock configMutationLock) {
    this.toolkitObjectType = type;
    this.searchBuilderFactory = searchBuilderFactory;
    this.lookup = lookup;
    this.platformService = platformService;
    this.configMutationLock = configMutationLock;
    this.clusterInfo = new TerracottaClusterInfo(platformService);
    this.getAllBatchSize = getTerracottaProperty(EHCACHE_GETALL_BATCH_SIZE_PROPERTY, DEFAULT_GETALL_BATCH_SIZE);
    this.serverMapLocalStoreFactory = serverMapLocalStoreFactory;
    Preconditions.checkArgument(isValidType(type), "Type has to be one of %s but was %s", VALID_TYPES, type);

    this.name = name;
    Preconditions.checkNotNull(schemaCreator);
    this.schemaCreator = schemaCreator;
    this.listeners = new CopyOnWriteArrayList<ToolkitCacheListener<K>>();
    this.versionUpdateListeners = new CopyOnWriteArraySet<VersionUpdateListener<K, V>>();

    this.config = new UnclusteredConfiguration(config);
    this.consistency = Consistency.valueOf((String)InternalCacheConfigurationType.CONSISTENCY
        .getExistingValueOrException(config));
    localCacheStore = createLocalCacheStore();
    pinnedEntryFaultCallback = new PinnedEntryFaultCallbackImpl(this);
    searchReqIdGenerator = new SearchRequestIDGenerator();
    this.timeSource = new SystemTimeSource();
    this.lockStrategy = getLockStrategyFromConfig(config);
    setupStripeObjects(stripeObjects);
    concurrentLock = ToolkitLockingApi.createConcurrentTransactionLock("CONCURRENT_LOCK_FOR_BULKLOAD", platformService);
    taskRunner = platformService.getTaskRunner();
    timer = taskRunner.newTimer();
  }

  private void setupStripeObjects(ToolkitObjectStripe<InternalToolkitMap<K, V>>[] stripeObjects) {
    this.stripeObjects = stripeObjects;
    List<InternalToolkitMap<K, V>> list = new ArrayList<InternalToolkitMap<K, V>>();
    for (ToolkitObjectStripe<InternalToolkitMap<K, V>> stripeObject : stripeObjects) {
      for (InternalToolkitMap<K, V> serverMap : stripeObject) {
        serverMap.setLockStrategy(lockStrategy);
        list.add(serverMap);
      }
    }
    initializeLocalCache(list);
    this.serverMaps = list.toArray(new ServerMap[list.size()]);
    for (ToolkitObjectStripe stripeObject : stripeObjects) {
      stripeObject.addConfigChangeListener(this);
    }
    // logging only when its different than LONG_LOCK_STRATEGY
    if (lockStrategy != LOCK_STRATEGY.LONG_LOCK_STRATEGY) {
      LOGGER.info("cache or store " + name + " created with " + lockStrategy);
    }
  }

  private LOCK_STRATEGY getLockStrategyFromConfig(Configuration configuration) {
    String value = (String) configuration.getObjectOrNull(ConfigFieldsInternal.LOCK_STRATEGY_NAME);
    if (value != null) { return LOCK_STRATEGY.valueOf(value); }
    return LOCK_STRATEGY.LONG_LOCK_STRATEGY;
  }

  private static boolean isValidType(ToolkitObjectType toolkitObjectType) {
    for (ToolkitObjectType validType : VALID_TYPES) {
      if (validType == toolkitObjectType) return true;
    }
    return false;
  }

  private void initializeLocalCache(List<InternalToolkitMap<K, V>> serverMapsParam) {
    boolean localCacheEnabled = (Boolean) InternalCacheConfigurationType.LOCAL_CACHE_ENABLED
        .getValueIfExistsOrDefault(config);
    for (InternalToolkitMap<K, V> serverMap : serverMapsParam) {
      serverMap.initializeLocalCache(localCacheStore, pinnedEntryFaultCallback, localCacheEnabled);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void rejoinStarted() {
    // nothing to do
  }

  @Override
  public void rejoinCompleted() {
    getAnyServerMap().cleanLocalState();
    ToolkitObjectStripe<InternalToolkitMap<K, V>>[] objects = lookup.lookupStripeObjects(name, this.toolkitObjectType,
                                                                                         config);
    if (objects != null) {
      setupStripeObjects(objects);
      lookupSuccessfulAfterRejoin = true;
    } else {
      lookupSuccessfulAfterRejoin = false;
    }

    // handles re-join scenario by re-registering server event listener if needed
    resendEventRegistrations();

    if (attributeExtractor != null) {
      registerServerMapAttributeExtractor();
    }
  }

  @Override
  public void resendEventRegistrations() {
    if (!listeners.isEmpty()) {
      registerServerEventListener(EnumSet.of(EVICT, EXPIRE), true);
    }

    if (!versionUpdateListeners.isEmpty()) {
      registerServerEventListener(EnumSet.of(PUT_LOCAL, REMOVE_LOCAL), true);
    }
  }

  protected boolean isLookupSuccessfulAfterRejoin() {
    return lookupSuccessfulAfterRejoin;
  }

  protected L1ServerMapLocalCacheStore<K, V> createLocalCacheStore() {
    ServerMapLocalStore<K, V> smLocalStore = serverMapLocalStoreFactory
        .getOrCreateServerMapLocalStore(getLocalStoreConfig());
    return new L1ServerMapLocalCacheStoreImpl<K, V>(smLocalStore);
  }

  private ServerMapLocalStoreConfig getLocalStoreConfig() {
    return new ServerMapLocalStoreConfigParameters().populateFrom(config, this.name).buildConfig();
  }

  protected int getServerMapIndexForKey(Object key) {
    return Math.abs(key.hashCode() % serverMaps.length);
  }

  protected InternalToolkitMap<K, V> getServerMapForKey(Object key) {
    Preconditions.checkNotNull(key, "Key cannot be null");
    return serverMaps[getServerMapIndexForKey(key)];
  }

  protected InternalToolkitMap<K, V> getAnyServerMap() {
    return serverMaps[0];
  }

  protected TCObjectServerMap getAnyTCObjectServerMap() {
    final InternalToolkitMap<K, V> e = getAnyServerMap();
    if (e == null || e.__tc_managed() == null) { throw new UnsupportedOperationException("Map is not shared ServerMap"); }
    return (TCObjectServerMap) e.__tc_managed();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return getServerMapForKey(key).createLockForKey(key);
  }

  @Override
  public int size() {
    // wait and then tell me more accurate size
    waitForAllCurrentTransactionsToComplete();
    return getSize();
  }

  protected void waitForAllCurrentTransactionsToComplete() {
    try {
      platformService.waitForAllCurrentTransactionsToComplete();
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  private int getSize() {
    long sum;
    try {
      sum = getAnyTCObjectServerMap().getAllSize(serverMaps);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    // copy the way CHM does if overflow integer
    if (sum > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) sum;
    }
  }

  @Override
  public boolean isEmpty() {
    return this.size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return getServerMapForKey(key).containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(Object key) {
    return getServerMapForKey(key).get(key);
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    return getServerMapForKey(key).get(key, valueOid);
  }

  @Override
  public V remove(Object key) {
    return getServerMapForKey(key).remove(key);
  }

  @Override
  public void clear() {
    doClear();
    waitForAllCurrentTransactionsToComplete();
  }

  private void doClear() {
    for (InternalToolkitMap<K, V> map : serverMaps) {
      map.clear();
    }
  }

  @Override
  public void clearVersioned() {
    for (InternalToolkitMap<K, V> map : serverMaps) {
      map.clearVersioned();
    }
    waitForAllCurrentTransactionsToComplete();
    clearLocalCache();
  }

  @Override
  public Set<K> keySet() {
    return new ClusteredMapAggregateKeySet<K, V>(this);
  }

  @Override
  public Collection<V> values() {
    return new ClusteredMapAggregatedValuesCollection<K, V>(this);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new ClusteredMapAggregateEntrySet<K, V>(this);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return getServerMapForKey(key).remove(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return getServerMapForKey(key).replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return getServerMapForKey(key).replace(key, value);
  }

  @Override
  public void removeNoReturn(Object key) {
    getServerMapForKey(key).removeNoReturn(key);
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    getServerMapForKey(key).removeNoReturnVersioned(key, version);
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return getServerMapForKey(key).unsafeLocalGet(key);
  }

  @Override
  public V unlockedGet(Object key, boolean quiet) {
    return getServerMapForKey(key).unlockedGet((K) key, quiet);
  }

  @Override
  public void putNoReturn(K key, V value) {
    putNoReturn(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public int localSize() {
    return getAnyServerMap().localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return getAnyServerMap().localKeySet();
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return getServerMapForKey(key).containsLocalKey(key);
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    // synchronize not to have duplicate listeners
    synchronized (listeners) {
      if (!listeners.contains(listener)) {
        if (listeners.isEmpty()) {
          registerServerEventListener(EnumSet.of(EVICT, EXPIRE), false);
        }
        listeners.add(listener);
      }
    }
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    synchronized (listeners) {
      if (listeners.contains(listener)) {
        if (listeners.size() == 1) {
          unregisterServerEventListener(EnumSet.of(EVICT, EXPIRE));
        }
        listeners.remove(listener);
      }
    }
  }

  private void registerServerEventListener(Set<ServerEventType> eventTypes, boolean skipRejoinChecks) {
    // For routing incoming events
    platformService.registerServerEventListener(this, eventTypes);

    // Send registrations to server
    for (InternalToolkitMap<K, V> serverMap : serverMaps) {
      serverMap.registerListener(eventTypes, skipRejoinChecks);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Server event listener has been registered for cache: "
                   + getName() + ". Notification types: " + eventTypes);
    }
  }

  private void unregisterServerEventListener(Set<ServerEventType> eventTypes) {
    // For routing incoming events
    platformService.unregisterServerEventListener(this, eventTypes);

    // Send registrations to server
    for (InternalToolkitMap<K, V> serverMap : serverMaps) {
      serverMap.unregisterListener(eventTypes);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Server event listener has been unregistered for cache: "
                   + getName() + ". Notification types: " + eventTypes);
    }
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    getServerMapForKey(key).putNoReturn(key, value, (int) createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return getServerMapForKey(key).putIfAbsent(key, value, (int) createTimeInSecs, customMaxTTISeconds,
                                               customMaxTTLSeconds);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return getServerMapForKey(key).putIfAbsent(key, value);
  }

  @Override
  public SearchQueryResultSet executeQuery(ToolkitSearchQuery query) {
    SearchQueryResultSet results = null;
    SearchRequestID reqId = searchReqIdGenerator.getNextRequestID();
    boolean doSnapshot = Search.BATCH_SIZE_UNLIMITED != query.getResultPageSize();
    try {
      if (doSnapshot) takeSnapshotForSearchQuery(reqId);
      results = searchBuilderFactory.createSearchExecutor(getName(), getToolkitObjectType(), this,
          getAnyServerMap().isEventual(), platformService)
          .executeQuery(query, reqId);
      
    } finally {
      if (results == null && doSnapshot) closeResultSet(reqId);
    }
    return results;
  }

  @Override
  public void closeResultSet(SearchRequestID queryId) {
    for (ToolkitObjectStripe<InternalToolkitMap<K, V>> stripe : stripeObjects) {
      Iterator<InternalToolkitMap<K, V>> itr = stripe.iterator();
      InternalToolkitMap<K, V> svrMap = itr.next();
      svrMap.releaseSnapshot(queryId);
    }
  }

  public void setApplyDestroyCallback(DestroyApplicator destroyCallback) {
    getAnyServerMap().setApplyDestroyCallback(destroyCallback);
  }

  @Override
  public void destroy() {
    // Wait due to search index destroy working globally across all segments only once,
    // therefore allowing for races between pending txns and index destroy
    if (attributeExtractor != null) waitForAllCurrentTransactionsToComplete();
    for (InternalToolkitMap serverMap : serverMaps) {
      serverMap.destroy();
    }
    try {
      getSearchSchema().destroy();
    } catch (Exception e) {
      throw new ToolkitRuntimeException(e);
    }
  }

  @Override
  public void disposeLocally() {
    // Need to wait for all transactions to complete since there could still be in-flight transactions dependent on the
    // local cache.
    try {
      waitForAllCurrentTransactionsToComplete();
    } catch (TCNotRunningException e) {
      LOGGER.info("Ignoring " + TCNotRunningException.class.getName()
                  + " while waiting for all current txns to complete");
    } finally {
      try {
        getAnyServerMap().disposeLocally();
      } catch (TCNotRunningException e) {
        LOGGER.info("Ignoring " + TCNotRunningException.class.getName() + " while destroying local cache");
      }
    }
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return doGetAll(keys, false);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    return doGetAll(keys, true);
  }

  private Multimap<Integer, Entry> createBatchsForServerMap(Map map) {
    Multimap<Integer, Entry> batches = ArrayListMultimap.create();
    for (Object o : map.entrySet()) {
      Entry entry = (Entry)o;
      batches.put(getServerMapIndexForKey(entry.getKey()), entry);
    }
    return batches;
  }

  private Map<InternalToolkitMap, Map> createBatchesPerServerMap(Map map) {
    Map<InternalToolkitMap, Map> batches = new HashMap<InternalToolkitMap, Map>();
    for (Object o : map.entrySet()) {
      Entry e = (Entry) o;
      InternalToolkitMap serverMap = getServerMapForKey(e.getKey());
      Map batch = batches.get(serverMap);
      if (batch == null) {
        batch = new HashMap();
        batches.put(serverMap, batch);
      }
      batch.put(e.getKey(), e.getValue());
    }
    return batches;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    if (map == null || map.isEmpty()) { return; }
    if (getAnyServerMap().isEventual()) {
      Multimap<Integer, Entry> batchsForServerMap = createBatchsForServerMap(map);
      for (Entry<Integer, Collection<Entry>> batch : batchsForServerMap.asMap().entrySet()) {
        concurrentLock.lock();
        try {
          // for single serverMap
          for (Entry e : batch.getValue()) {
            serverMaps[batch.getKey()].unlockedPutNoReturn((K) e.getKey(), (V) e.getValue(), timeSource.nowInSeconds(),
                ToolkitConfigFields.DEFAULT_MAX_TTI_SECONDS,
                ToolkitConfigFields.DEFAULT_MAX_TTL_SECONDS);
          }
        } finally {
          concurrentLock.unlock();
        }

      }
    } else {
      for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
        putNoReturn(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    if (keys == null || keys.isEmpty()) { return; }
    for (K key : keys) {
      removeNoReturn(key);
    }
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    synchronized (versionUpdateListeners) {
      if (versionUpdateListeners.isEmpty()) {
        registerServerEventListener(EnumSet.of(PUT_LOCAL, REMOVE_LOCAL), false);
      }
      versionUpdateListeners.add(listener);
    }
  }

  @Override
  public void unregisterVersionUpdateListener(final VersionUpdateListener listener) {
    synchronized (versionUpdateListeners) {
      versionUpdateListeners.remove(listener);
      if (versionUpdateListeners.isEmpty()) {
        unregisterServerEventListener(EnumSet.of(PUT_LOCAL, REMOVE_LOCAL));
      }
    }
  }

  private Map<K, V> doGetAll(final Collection<? extends K> keys, boolean quiet) {
    if (keys == null || keys.isEmpty()) { return Collections.emptyMap(); }
    if (isExplicitLocked()) { throw new UnsupportedOperationException(); }
    switch (consistency) {
      case STRONG:
      case SYNCHRONOUS_STRONG:
        Map<K, V> rv = new HashMap<K, V>();
        if (quiet) {
          for (K key : keys) {
            rv.put(key, getQuiet(key));
          }
        } else {
          for (K key : keys) {
            rv.put(key, get(key));
          }
        }
        return rv;
      case EVENTUAL:
        return unlockedGetAll((Collection<K>) keys, quiet);
    }
    throw new UnsupportedOperationException("Unknown consistency - " + consistency);
  }

  Map<K, V> getAllInternal(Set<K> keys, boolean quiet) {
    final SetMultimap<ObjectID, K> mapIdToKeysMap = divideKeysIntoServerMaps(keys);
    TCObjectServerMap tcObjectServerMap = getAnyTCObjectServerMap();
    Map<K, V> rv = null;
    try {
      rv = tcObjectServerMap.getAllValuesUnlocked(mapIdToKeysMap);
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }

    for (Entry<K, V> entry : rv.entrySet()) {
      V nonExpiredValue = getServerMapForKey(entry.getKey()).checkAndGetNonExpiredValue(entry.getKey(),
                                                                                        entry.getValue(),
                                                                                        GetType.UNLOCKED, quiet);
      entry.setValue(nonExpiredValue);
    }
    return rv;
  }

  private SetMultimap<ObjectID, K> divideKeysIntoServerMaps(Set<K> keys) {
    SetMultimap<ObjectID, K> mapIdToKeys = HashMultimap.create();
    for (K key : keys) {
      InternalToolkitMap<K, V> serverMap = getServerMapForKey(key);
      assertKeyLiteral(key);
      TCObject tcObject = serverMap.__tc_managed();
      if (tcObject == null) { throw new UnsupportedOperationException(
                                                                      "unlockedGetAll is not supported in a non-shared ServerMap"); }
      mapIdToKeys.put(tcObject.getObjectID(), key);
    }
    return mapIdToKeys;
  }

  private void takeSnapshotForSearchQuery(SearchRequestID reqId) {
    if (platformService.isExplicitlyLocked()) throw new SearchException(
                                                                        "Paged search queries inside explicit lock scope not supported");

    final CountDownLatch complete = new CountDownLatch(stripeObjects.length);
    for (ToolkitObjectStripe<InternalToolkitMap<K, V>> stripe : stripeObjects) {
      Iterator<InternalToolkitMap<K, V>> itr = stripe.iterator();
      InternalToolkitMap<K, V> svrMap = itr.next();

      ToolkitLockingApi.lock(SNAPSHOT_TXN_LOCK_ID, ToolkitLockTypeInternal.CONCURRENT, platformService);
      platformService.addTransactionCompleteListener(new TransactionCompleteListener() {

        @Override
        public void transactionComplete(TransactionID txnID) {
          complete.countDown();
        }

        @Override
        public void transactionAborted(TransactionID txnID) {
          complete.countDown();

        }
      });
      try {
        svrMap.takeSnapshot(reqId);
        while (itr.hasNext()) {
          svrMap = itr.next();
          svrMap.addSelfToTxn();
        }
      } finally {
        try {
          ToolkitLockingApi.unlock(SNAPSHOT_TXN_LOCK_ID, ToolkitLockTypeInternal.CONCURRENT, platformService);
        } catch (PlatformRejoinException e) {
          throw new RejoinException(e);
        }
      }

    }

    try {
      complete.await();
    } catch (InterruptedException e) {
      throw new ToolkitRuntimeException(e);
    }
  }

  public void assertKeyLiteral(K key) {
    if (!LiteralValues.isLiteralInstance(key)) {
      //
      throw new UnsupportedOperationException("Only literal keys are supported - key: " + key);
    }
  }

  @Override
  public V getQuiet(Object key) {
    return getServerMapForKey(key).get(key, true);
  }

  @Override
  public Configuration getConfiguration() {
    return new ImmutableConfiguration(config);
  }

  @Override
  public void configChanged(final String fieldChanged, final Serializable changedValue) {
    Serializable newValue = changedValue;
    InternalCacheConfigurationType type = InternalCacheConfigurationType.getTypeFromConfigString(fieldChanged);

    if (type == null || type.isClusterWideConfig()) {
      if (fieldChanged.equals(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME)) {
        int maxTotalCount = 0;
        for (ToolkitObjectStripe stripe : stripeObjects) {
          maxTotalCount += stripe.getConfiguration().getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME);
        }
        newValue = maxTotalCount;
      } else {
        for (InternalToolkitMap sm : serverMaps) {
          sm.setConfigFieldInternal(fieldChanged, newValue);
        }
      }
      config.setObject(fieldChanged, newValue);
    }

  }

  @Override
  public void setConfigField(final String fieldChanged, final Serializable changedValue) {
    configMutationLock.lock();
    try {
      // to prevent user from manually setting a wrong configuration option
      validateField(fieldChanged);
      Serializable newValue = changedValue;
      config.setObject(fieldChanged, newValue);
      // set config changes ServerMap
      int[] values = null;
      for (int i = 0; i < this.serverMaps.length; i++) {
        if (ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME.equals(fieldChanged)) {
          if (values == null) {
            values = distributeInStripes((Integer) newValue, this.serverMaps.length);
          }
          newValue = ((Integer) changedValue) < 0 ? -1 : values[i];
        }
        serverMaps[i].setConfigField(fieldChanged, newValue);
      }

      // set the config field in ClusteredObjectStripeImpl
      for (ToolkitObjectStripe<InternalToolkitMap<K, V>> stripe : this.stripeObjects) {
        if (ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME.equals(fieldChanged)) {
          int maxTotalCount = 0;
          for (InternalToolkitMap<K, V> sm : stripe) {
            maxTotalCount += sm.getMaxCountInCluster();
          }
          newValue = maxTotalCount < 0 ? -1 : maxTotalCount;
        }
        stripe.setConfigField(fieldChanged, newValue);
      }
    } finally {
      configMutationLock.unlock();
    }
  }

  private void validateField(final String fieldChanged) {
    final InternalCacheConfigurationType type = InternalCacheConfigurationType.getTypeFromConfigString(fieldChanged);
    Preconditions.checkArgument(InternalCacheConfigurationType.getConfigsFor(toolkitObjectType).contains(type),
                                "%s does not support configuration option '%s'", toolkitObjectType, fieldChanged);
    Preconditions
        .checkArgument(type.isDynamicChangeAllowed(), "Dynamic change not allowed for field: %s", fieldChanged);
  }

  @Override
  public boolean isDestroyed() {
    throw new UnsupportedOperationException();
  }

  private boolean isExplicitLocked() {
    return false;
  }

  @Override
  public void unlockedPutNoReturn(K key, V value, int createTimeInSecs, int customTTISeconds, int customTTLSeconds) {
    getServerMapForKey(key).unlockedPutNoReturn(key, value, createTimeInSecs, customTTISeconds, customTTLSeconds);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                                           final int customTTISeconds, final int customTTLSeconds) {
    getServerMapForKey(key).unlockedPutNoReturnVersioned(key, value, version, createTimeInSecs, customTTISeconds,
                                                         customTTLSeconds);
  }

  @Override
  public void unlockedRemoveNoReturn(Object key) {
    getServerMapForKey(key).unlockedRemoveNoReturn(key);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    getServerMapForKey(key).unlockedRemoveNoReturnVersioned(key, version);
  }

  public void unlockedClear() {
    for (InternalToolkitMap<K, V> map : serverMaps) {
      map.unlockedClear();
    }
  }

  @Override
  public void clearLocalCache() {
    getAnyServerMap().clearLocalCache();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return getAnyServerMap().localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return getAnyServerMap().localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return getAnyServerMap().localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return getAnyServerMap().localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return getAnyServerMap().containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return getAnyServerMap().containsKeyLocalOffHeap(key);
  }

  @Override
  public void putVersioned(K key, V value, long version) {
    putVersioned(key, value, version, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
        ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version) {
    getServerMapForKey(key).putIfAbsentVersioned(key, value, version, timeSource.nowInSeconds(),
                 ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                                        int customMaxTTLSeconds) {
    getServerMapForKey(key).putIfAbsentVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds,
                                                      customMaxTTLSeconds);
  }

  @Override
  public void putVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                           int customMaxTTLSeconds) {
    getServerMapForKey(key).putVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds,
        customMaxTTLSeconds);
  }

  @Override
  public V put(K key, V value) {
    return put(key, value, timeSource.nowInSeconds(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
        ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return getServerMapForKey(key).put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set keys) {
    Map<Object, Set<ClusterNode>> map = new HashMap<Object, Set<ClusterNode>>();
    for (Map m : serverMaps) {
      Map<K, Set<ClusterNode>> nodesWithKeys = clusterInfo.getNodesWithKeys(m, keys);
      for (Entry<K, Set<ClusterNode>> entry : nodesWithKeys.entrySet()) {
        Set<ClusterNode> clusterNodeSet = map.get(entry.getKey());
        if (clusterNodeSet == null) {
          clusterNodeSet = new HashSet<ClusterNode>();
          map.put(entry.getKey(), clusterNodeSet);
        }
        clusterNodeSet.addAll(entry.getValue());
      }
    }
    return map;
  }

  @Override
  public Iterator<InternalToolkitMap<K, V>> iterator() {
    return new AggregateServerMapIterator<InternalToolkitMap<K, V>>(this.serverMaps);
  }

  @Override
  public String getDestinationName() {
    return getName();
  }

  @Override
  public void handleServerEvent(final ServerEvent event) {
    final ServerEventType type = event.getType();
    if (type == EVICT || type == EXPIRE) {
      doHandleEvictions(event);
    } else if (type == PUT_LOCAL || type == REMOVE_LOCAL) {
      doHandleVersionUpdates((VersionedServerEvent) event);
    }
  }

  private void doHandleEvictions(final ServerEvent event) {
    for (final ToolkitCacheListener listener : listeners) {
      try {
        switch (event.getType()) {
          case EVICT:
            listener.onEviction(event.getKey());
            break;
          case EXPIRE:
            listener.onExpiration(event.getKey());
            break;
          default:
            throw new IllegalStateException("unexpected ServerEvent in doHandleEvictions " + event.getType());
        }
      } catch(TCNotRunningException e){
        //Exception Ignored
      } catch (Throwable t){
          // Catch throwable here since the eviction listener will ultimately call user code.
          // That way we do not cause an unhandled exception to be thrown in a stage thread, bringing
          // down the L1.
          LOGGER.error("Cache listener threw an exception", t);
      }
    }
  }

  private void doHandleVersionUpdates(final VersionedServerEvent event) {
    final Object key = event.getKey();
    final long version = event.getVersion();
    final ServerEventType type = event.getType();
    switch (type) {
      case PUT_LOCAL:
        CustomLifespanVersionedServerEvent customLifespanEvent = (CustomLifespanVersionedServerEvent) event;
        final int creationTimeInSeconds = customLifespanEvent.getCreationTimeInSeconds();
        final int timeToIdle = customLifespanEvent.getTimeToIdle();
        final int timeToLive = customLifespanEvent.getTimeToLive();
        V value;

        try {
          value = deserializeValue(key, event.getValue());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        for (final VersionUpdateListener<K, V> listener : versionUpdateListeners) {
          listener.onLocalPut((K) key, value, version, creationTimeInSeconds, timeToIdle, timeToLive,
                              getServerMapIndexForKey(key));
        }
        break;
      case REMOVE_LOCAL:
        for (final VersionUpdateListener<K, V> listener : versionUpdateListeners) {
          listener.onLocalRemove((K) key, version, getServerMapIndexForKey(key));
        }
        break;
      default:
        throw new IllegalStateException("unexpected ServerEvent in doHandleVersionUpdates " + type);
    }
  }

  private V deserializeValue(final Object key, final byte[] value) throws IOException, ClassNotFoundException {
    final int now = new SystemTimeSource().nowInSeconds();
    final SerializedMapValueParameters<V> params = new SerializedMapValueParameters<V>()
        .createTime(now).lastAccessedTime(now).serialized(value);
    final SerializationStrategy serializationStrategy = platformService
        .lookupRegisteredObjectByName(TerracottaToolkit.TOOLKIT_SERIALIZER_REGISTRATION_NAME,
            SerializationStrategy.class);
    final boolean compressionEnabled = serverMaps[0].isCompressionEnabled();

    return (V) new SerializedMapValue(params).getDeserializedValue(
        serializationStrategy, compressionEnabled, localCacheStore, key, false);
  }

  private static class AggregateServerMapIterator<E> implements Iterator<E> {
    private final E[] array;
    private int       index = 0;

    public AggregateServerMapIterator(E[] array) {
      this.array = array;
    }

    @Override
    public boolean hasNext() {
      return index < array.length;
    }

    @Override
    public E next() {
      if (!hasNext()) { throw new NoSuchElementException(); }
      index++;
      return array[index - 1];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor<K, V> attrExtractor) {
    // This race is okay to have, the only reason for the conditional is to avoid calling call() below
    attributeExtractor = attrExtractor;
    if (attrSchema.get() == null) {
      try {
        attrSchema.compareAndSet(null, getSearchSchema());
      } catch (Exception e) {
        throw new ToolkitRuntimeException(e);
      }
    }
    registerServerMapAttributeExtractor();
  }

  private ToolkitMap<String, String> getSearchSchema() throws Exception {
    ToolkitMap<String, String> schema;
    if (attributeExtractor instanceof ToolkitAttributeExtractorInternal) {
      schema = ((ToolkitAttributeExtractorInternal) attributeExtractor).createAttributeMap();
    } else {
      schema = schemaCreator.call();
    }
    return schema;
  }

  private void registerServerMapAttributeExtractor() {
    if (attributeExtractor instanceof ToolkitAttributeExtractorInternal) {
      ToolkitAttributeExtractorInternal<K, V> intExtr = (ToolkitAttributeExtractorInternal<K, V>) attributeExtractor;
      Map<String, Class<?>> initialSchema = intExtr.getInitialTypeSchema();
      Map<String, String> configSchema = new HashMap<String, String>(initialSchema.size());

      for (Map.Entry<String, Class<?>> attrType : initialSchema.entrySet()) {
        ToolkitAttributeType t = ToolkitAttributeType.typeFor(attrType.getValue());
        if (t == null) throw new ToolkitRuntimeException(
                                                         String.format("Attribute %s is of unknown type %s",
                                                                       attrType.getKey(), attrType.getValue().getName()));
        String typeName = (t == ToolkitAttributeType.ENUM ? attrType.getValue().getName() : t.name());
        configSchema.put(attrType.getKey(), typeName);
      }
      ToolkitMap<String, String> schema = attrSchema.get();
      if (schema.isEmpty()) schema.putAll(configSchema);
      // Attempt to prevent classloader leaks by clearing the initial schema map
      initialSchema.clear();
    }
    for (InternalToolkitMap serverMap : this.serverMaps) {
      serverMap.registerAttributeExtractor(attributeExtractor);
      ((ServerMap) serverMap).setSearchAttributeTypes(attrSchema.get());
    }
  }

  private static class PinnedEntryFaultCallbackImpl implements PinnedEntryFaultCallback {

    private final WeakReference<AggregateServerMap> serverMap;

    public PinnedEntryFaultCallbackImpl(AggregateServerMap serverMap) {
      this.serverMap = new WeakReference<AggregateServerMap>(serverMap);
    }

    @Override
    public void unlockedGet(Object key) {
      AggregateServerMap serverMapLocal = serverMap.get();
      if (serverMapLocal != null) {
        serverMapLocal.unlockedGet(key, true);
      }

    }

    @Override
    public void get(Object key) {
      AggregateServerMap serverMapLocal = serverMap.get();
      if (serverMapLocal != null) {
        serverMapLocal.get(key);
      }
    }
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return searchBuilderFactory.createQueryBuilder(this, getToolkitObjectType());
  }

  protected ToolkitObjectType getToolkitObjectType() {
    return this.toolkitObjectType;
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return Collections.unmodifiableMap(new GetAllCustomMap(keys, this, quiet, getAllBatchSize));
  }

  @Override
  public boolean isBulkLoadEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void drain(final Map<K, BufferedOperation<V>> buffer) {
    Map<InternalToolkitMap, Map> batches = createBatchesPerServerMap(buffer);
    Collection<Future<?>> futures = new ArrayList<Future<?>>(batches.size());
    for (final Entry<InternalToolkitMap, Map> entry : batches.entrySet()) {
      futures.add(timer.schedule(new Runnable() {
        @Override
        public void run() {
          try {
            entry.getKey().drain(entry.getValue());
          } catch (RejoinException e) {
            LOGGER.warn("Got a rejoin while draining. Dumping the batch.");
          } catch (TCNotRunningException e) {
            LOGGER.debug("Got a TCNotRunningException while draining. Ignoring it.", e);
          }
        }
      }, 0, TimeUnit.MILLISECONDS));
    }
    boolean interrupted = false;
    for (Future<?> future : futures) {
      try {
        while (true) {
          try {
            future.get();
            break;
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } catch (ExecutionException e) {
        LOGGER.error("Error draining batch", e);
        Throwables.propagate(e);
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }

  @Override
  public Set<K> keySetForSegment(int segmentIndex) {
    final int segmentCount = serverMaps.length;
    Preconditions.checkArgument(segmentIndex >= 0 && segmentIndex < segmentCount,
                                "Segment index must be in the range [0..%s)", segmentCount);

    return serverMaps[segmentIndex].keySet();
  }

  @Override
  public VersionedValue<V> getVersionedValue(Object key) {
    return getServerMapForKey(key).getVersionedValue(key);
  }

  @Override
  public Map<K, VersionedValue<V>> getAllVersioned(final Collection<K> keys) {
    return getAnyServerMap().getAllVersioned(divideKeysIntoServerMaps(Sets.newHashSet(keys)));
  }

  @Override
  public void quickClear() {
    doClear();
  }

  @Override
  public int quickSize() {
    return getSize();
  }

  @Override
  public boolean remove(Object key, Object value, ToolkitValueComparator<V> comparator) {
    return getServerMapForKey(key).remove(key, value, comparator);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue, ToolkitValueComparator<V> comparator) {
    return getServerMapForKey(key).replace(key, oldValue, newValue, comparator);
  }

  @Override
  public BufferedOperation<V> createBufferedOperation(final BufferedOperation.Type type, final K key, final V value,
                                                      final long version, final int createTimeInSecs,
                                                      final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    return getServerMapForKey(key).createBufferedOperation(type, key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }
}
