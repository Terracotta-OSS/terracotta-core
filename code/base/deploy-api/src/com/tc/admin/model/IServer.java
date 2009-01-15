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
import com.tc.stats.DSOClassInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public interface IServer extends IClusterNode, ManagedObjectFacadeProvider {
  static final IServer[] NULL_SET                    = {};

  static final String    PROP_CONNECTED              = "connected";
  static final String    PROP_CONNECT_ERROR          = "connectError";
  static final String    PROP_LOCK_STATS_TRACE_DEPTH = "lockStatsTraceDepth";
  static final String    PROP_LOCK_STATS_ENABLED     = "lockStatsEnabled";

  static final String    POLLED_ATTR_CACHE_MISS_RATE = "CacheMissRate";

  IClusterModel getClusterModel();

  boolean isActiveCoordinator();

  boolean isAutoConnect();

  void setAutoConnect(boolean autoConnect);

  void setHost(String host);

  void setPort(int jmxPort);

  String[] getConnectionCredentials();

  Map<String, Object> getConnectionEnvironment();

  JMXConnector getJMXConnector();

  void setJMXConnector(JMXConnector jmxc) throws IOException;

  void setConnectionCredentials(String[] creds);

  void refreshCachedCredentials();

  String getName();

  String getHostAddress();

  String getCanonicalHostName();

  Integer getDSOListenPort();

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

  String getStatsExportServletURI();

  String getStatsExportServletURI(String sessionId);

  void addServerLogListener(ServerLogListener logListener);

  void removeServerLogListener(ServerLogListener logListener);

  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;

  DSOClassInfo[] getClassInfo();

  GCStats[] getGCStats();

  void addDGCListener(DGCListener listener);

  void removeDGCListener(DGCListener listener);

  void runGC();

  Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                       TimeUnit unit);

  int getLiveObjectCount();

  boolean isDBBackupSupported();

  void addDBBackupListener(DBBackupListener listener);

  void removeDBBackupListener(DBBackupListener listener);

  void backupDB() throws IOException;

  void backupDB(String path) throws IOException;

  boolean isDBBackupRunning();

  String getDefaultDBBackupPath();

  boolean isDBBackupEnabled();

  String getDBHome();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  boolean isLockProfilingSupported();

  int getLockProfilerTraceDepth();

  void setLockProfilerTraceDepth(int traceDepth);

  boolean isLockProfilingEnabled();

  void setLockProfilingEnabled(boolean lockStatsEnabled);

  Collection<LockSpec> getLockSpecs();

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
