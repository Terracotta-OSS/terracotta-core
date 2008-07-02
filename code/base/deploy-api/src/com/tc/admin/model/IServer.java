/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.stats.DSOClassInfo;
import com.tc.stats.DSOMBean;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;

public interface IServer extends IClusterNode {
  static final String PROP_CONNECTED     = "connected";
  static final String PROP_CONNECT_ERROR = "connectError";

  boolean isAutoConnect();

  void setAutoConnect(boolean autoConnect);

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

  String getProductVersion();

  String getProductBuildID();

  String getProductLicense();

  String getProductCopyright();
  
  String getConnectionStatusString();
  
  boolean isConnected();

  boolean hasConnectError();

  Exception getConnectError();

  String getConnectErrorMessage(Exception e);

  boolean isStarted();

  boolean isActive();

  boolean isPassiveUninitialized();

  boolean isPassiveStandby();

  long getStartTime();

  long getActivateTime();

  TCServerInfoMBean getServerInfoBean();

  DSOMBean getDSOBean();

  IServer[] getClusterServers();

  void doShutdown();

  IClient[] getClients();

  void addClientConnectionListener(ClientConnectionListener listener);

  void removeClientConnectionListener(ClientConnectionListener listener);

  IBasicObject[] getRoots();

  void addRootCreationListener(RootCreationListener listener);

  void removeRootCreationListener(RootCreationListener listener);

  Map getServerStatistics();

  Statistic[] getDSOStatistics(String[] names);

  Map<IClient, CountStatistic> getAllPendingTransactionsCount() ;
  
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

  int getLiveObjectCount();
  
  void addDBBackupListener(DBBackupListener listener);
  
  void removeDBBackupListener(DBBackupListener listener);
  
  void backupDB() throws IOException;

  void backupDB(String path) throws IOException;
  
  boolean isDBBackupRunning();

  String getDefaultDBBackupPath();

  boolean isDBBackupEnabled();

  String getDBHome();
  
  void disconnect();
}
