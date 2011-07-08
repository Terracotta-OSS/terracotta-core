/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.management.lock.stats.LockSpec;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.stats.api.DSOClassInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;

public interface IServer extends IClusterNode, ManagedObjectFacadeProvider {
  static final IServer[] NULL_SET                                = {};

  static final String    PROP_CONNECTED                          = "connected";
  static final String    PROP_CONNECT_ERROR                      = "connectError";
  static final String    PROP_LOCK_STATS_TRACE_DEPTH             = "lockStatsTraceDepth";
  static final String    PROP_LOCK_STATS_ENABLED                 = "lockStatsEnabled";

  static final String    POLLED_ATTR_CACHE_MISS_RATE             = "CacheMissRate";
  static final String    POLLED_ATTR_FAULTED_RATE                = "L2DiskFaultRate";
  static final String    POLLED_ATTR_ONHEAP_FLUSH_RATE           = "OnHeapFlushRate";
  static final String    POLLED_ATTR_ONHEAP_FAULT_RATE           = "OnHeapFaultRate";
  static final String    POLLED_ATTR_OFFHEAP_FLUSH_RATE          = "OffHeapFlushRate";
  static final String    POLLED_ATTR_OFFHEAP_FAULT_RATE          = "OffHeapFaultRate";
  static final String    POLLED_ATTR_LOCK_RECALL_RATE            = "GlobalLockRecallRate";
  static final String    POLLED_ATTR_BROADCAST_RATE              = "BroadcastRate";
  static final String    POLLED_ATTR_TRANSACTION_SIZE_RATE       = "TransactionSizeRate";
  static final String    POLLED_ATTR_PENDING_TRANSACTIONS_COUNT  = "PendingTransactionsCount";
  static final String    POLLED_ATTR_CACHED_OBJECT_COUNT         = "CachedObjectCount";
  static final String    POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT = "OffheapObjectCachedCount";

  boolean isActiveCoordinator();

  boolean isAutoConnect();

  void setAutoConnect(boolean autoConnect);

  void setHost(String host);

  void setPort(int jmxPort);

  String[] getConnectionCredentials();

  Map<String, Object> getConnectionEnvironment();

  JMXConnector getJMXConnector();

  void setJMXConnector(JMXConnector jmxc) throws IOException;

  <T> T getMBeanProxy(ObjectName on, Class<T> mbeanType);

  boolean removeNotificationListener(ObjectName on, NotificationListener listener) throws IOException,
      InstanceNotFoundException, ListenerNotFoundException;

  boolean addNotificationListener(ObjectName on, NotificationListener listener) throws IOException,
      InstanceNotFoundException;

  Set<ObjectName> queryNames(ObjectName on, QueryExp query) throws IOException;

  void setConnectionCredentials(String[] creds);

  void refreshCachedCredentials();

  void clearConnectionCredentials();

  String getName();

  String getHostAddress();

  String getCanonicalHostName();

  Integer getDSOListenPort();

  Integer getDSOGroupPort();

  String getPersistenceMode();

  String getFailoverMode();

  String getConnectionStatusString();

  boolean isConnected();

  boolean hasConnectError();

  Exception getConnectError();

  String getConnectErrorMessage();

  String getConnectErrorMessage(Exception e);

  boolean isStarted();

  boolean isActive();

  boolean testIsActive();

  boolean isPassiveUninitialized();

  boolean isPassiveStandby();

  long getStartTime();

  long getActivateTime();

  IServer[] getClusterServers();

  IServerGroup[] getClusterServerGroups();

  IProductVersion getProductInfo();

  void doShutdown();

  IClient[] getClients();

  void addClientConnectionListener(ClientConnectionListener listener);

  void removeClientConnectionListener(ClientConnectionListener listener);

  IBasicObject[] getRoots();

  void addRootCreationListener(RootCreationListener listener);

  void removeRootCreationListener(RootCreationListener listener);

  Map getServerStatistics();

  Map<IClient, Map<String, Object>> getPrimaryClientStatistics();

  Map<IClient, Long> getClientTransactionRates();

  Number[] getDSOStatistics(String[] names);

  Map<IClient, Long> getAllPendingTransactionsCount();

  Map<IClient, Integer> getClientLiveObjectCount();

  boolean isResidentOnClient(IClient client, ObjectID oid);

  String getStatsExportServletURI();

  String getStatsExportServletURI(String sessionId);

  void addServerLogListener(ServerLogListener logListener);

  void removeServerLogListener(ServerLogListener logListener);

  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

  DSOClassInfo[] getClassInfo();

  GCStats[] getGCStats();

  List<TerracottaOperatorEvent> getOperatorEvents();

  void addDGCListener(DGCListener listener);

  void addTerracottaOperatorEventsListener(TerracottaOperatorEventsListener listener);

  void removeDGCListener(DGCListener listener);

  void removeTerracottaOperatorEventsListener(TerracottaOperatorEventsListener listener);

  void runGC();

  void setAttribute(ObjectName on, String attrName, Object attrValue) throws Exception;

  Map<ObjectName, Exception> setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue) throws Exception;

  Map<ObjectName, Exception> setAttribute(String attrName, Map<ObjectName, Object> attrMap) throws Exception;

  Object getAttribute(ObjectName on, String attrName) throws Exception;

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                       TimeUnit unit);

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit, Object[] args,
                                 String[] sigs);

  Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, Object[] args, String[] sigs);

  int getLiveObjectCount();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  boolean isLockProfilingSupported();

  int getLockProfilerTraceDepth();

  void setLockProfilerTraceDepth(int traceDepth);

  boolean isLockProfilingEnabled();

  void setLockProfilingEnabled(boolean lockStatsEnabled);

  Collection<LockSpec> getLockSpecs() throws InterruptedException;

  boolean isClusterStatsSupported();

  void startupClusterStats();

  String[] getSupportedClusterStats();

  void clearAllClusterStats();

  void clearClusterStatsSession(String sessionId);

  void startClusterStatsSession(String sessionId, String[] statsToRecord, long samplePeriodMillis);

  void endCurrentClusterStatsSession();

  void captureClusterStat(String sraName);

  String[] getAllClusterStatsSessions();

  boolean isActiveClusterStatsSession();

  String getActiveClusterStatsSession();

  void addClusterStatsListener(IClusterStatsListener listener);

  void removeClusterStatsListener(IClusterStatsListener listener);

  void disconnect();

  void splitbrain();

  void setFaultDebug(boolean faultDebug);

  boolean getFaultDebug();

  void setRequestDebug(boolean requestDebug);

  boolean getRequestDebug();

  void setFlushDebug(boolean flushDebug);

  boolean getFlushDebug();

  boolean getBroadcastDebug();

  void setBroadcastDebug(boolean broadcastDebug);

  boolean getCommitDebug();

  void setCommitDebug(boolean commitDebug);
}
