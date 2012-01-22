/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.management.remote.JMXConnector;

public interface IClusterModel extends IClusterModelElement, ManagedObjectFacadeProvider, ILiveObjectCountProvider {
  static final String PROP_ACTIVE_COORDINATOR = "activeCoordinator";
  static final String PROP_CONNECTED          = "connected";
  static final String PROP_CONNECT_ERROR      = "connectError";
  static final String PROP_AUTO_CONNECT       = "autoConnect";

  static enum PollScope {
    ALL_SERVERS, ACTIVE_SERVERS, CLIENTS
  }

  void setName(String name);

  String getName();

  void setHost(String host);

  String getHost();

  void setPort(int port);

  int getPort();

  String[] getConnectionCredentials();

  Map<String, Object> getConnectionEnvironment();

  JMXConnector getJMXConnector();

  void setJMXConnector(JMXConnector jmxc) throws IOException;

  void setConnectionCredentials(String[] creds);

  void clearConnectionCredentials();

  void refreshCachedCredentials();

  boolean hasConnectError();

  Exception getConnectError();

  String getConnectErrorMessage(Exception e);

  boolean isConnected();

  boolean isReady();

  void addServerStateListener(ServerStateListener listener);

  void removeServerStateListener(ServerStateListener listener);

  void connect();

  void disconnect();

  boolean isAutoConnect();

  void setAutoConnect(boolean autoConnect);

  IServer getActiveCoordinator();

  IServerGroup[] getServerGroups();

  IBasicObject[] getRoots();

  void addRootCreationListener(RootCreationListener listener);

  void removeRootCreationListener(RootCreationListener listener);

  IClient[] getClients();

  boolean isResidentOnClient(IClient client, ObjectID oid);

  int getLiveObjectCount();

  Map<IServer, Map<String, Object>> getPrimaryServerStatistics();

  Map<IClient, Map<String, Object>> getPrimaryClientStatistics();

  Map<IClient, Long> getClientTransactionRates();

  Map<IClusterNode, Future<String>> takeThreadDump();

  Future<String> takeThreadDump(IClusterNode node);

  Map<IClusterNode, Future<String>> takeClusterDump();

  Future<String> takeClusterDump(IClusterNode node);

  void addPolledAttributeListener(PollScope scope, Set<String> attributes, PolledAttributeListener listener);

  void addPolledAttributeListener(PollScope scope, String attribute, PolledAttributeListener listener);

  void removePolledAttributeListener(PollScope scope, Set<String> attributes, PolledAttributeListener listener);

  void removePolledAttributeListener(PollScope scope, String attribute, PolledAttributeListener listener);

  String dump();

  void setPollPeriod(int seconds);

  void setPollTimeout(int seconds);

  void tearDown();
}
