/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import org.apache.commons.httpclient.auth.AuthScope;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.stats.DSOClassInfo;
import com.tc.stats.DSOMBean;
import com.tc.stats.DSORootMBean;
import com.tc.util.ProductInfo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.event.EventListenerList;

public class Server extends BaseClusterNode implements IServer, NotificationListener, ManagedObjectFacadeProvider {
  protected IClusterModel                 clusterModel;
  protected IServerGroup                  serverGroup;
  protected final ServerConnectionManager connectManager;
  protected String                        displayLabel;
  protected boolean                       connected;
  protected Set<ObjectName>               readySet;
  protected boolean                       ready;
  protected List<DSOClient>               clients;
  private ClientChangeListener            clientChangeListener;
  protected EventListenerList             listenerList;
  protected List<DSOClient>               pendingClients;
  protected Exception                     connectException;
  protected TCServerInfoMBean             serverInfoBean;
  protected DSOMBean                      dsoBean;
  protected ObjectManagementMonitorMBean  objectManagementMonitorBean;
  protected boolean                       serverDBBackupSupported;
  protected ServerDBBackupMBean           serverDBBackupBean;
  protected ProductVersion                productInfo;
  protected List<IBasicObject>            roots;
  protected Map<ObjectName, IBasicObject> rootMap;
  protected LogListener                   logListener;
  protected long                          startTime;
  protected long                          activateTime;
  protected String                        persistenceMode;
  protected String                        failoverMode;
  protected LockStatisticsMonitorMBean    lockProfilerBean;
  protected boolean                       lockProfilingSupported;
  protected int                           lockProfilerTraceDepth;
  protected Boolean                       lockProfilingEnabled;

  private StatisticsLocalGathererMBean    clusterStatsBean;
  private boolean                         clusterStatsSupported;

  private static final PolledAttribute    PA_CPU_USAGE                  = new PolledAttribute(
                                                                                              L2MBeanNames.TC_SERVER_INFO,
                                                                                              POLLED_ATTR_CPU_USAGE);
  private static final PolledAttribute    PA_USED_MEMORY                = new PolledAttribute(
                                                                                              L2MBeanNames.TC_SERVER_INFO,
                                                                                              POLLED_ATTR_USED_MEMORY);
  private static final PolledAttribute    PA_MAX_MEMORY                 = new PolledAttribute(
                                                                                              L2MBeanNames.TC_SERVER_INFO,
                                                                                              POLLED_ATTR_MAX_MEMORY);
  private static final PolledAttribute    PA_OBJECT_FLUSH_RATE          = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_OBJECT_FLUSH_RATE);
  private static final PolledAttribute    PA_OBJECT_FAULT_RATE          = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_OBJECT_FAULT_RATE);
  private static final PolledAttribute    PA_TRANSACTION_RATE           = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_TRANSACTION_RATE);
  private static final PolledAttribute    PA_CACHE_MISS_RATE            = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_CACHE_MISS_RATE);
  private static final PolledAttribute    PA_LIVE_OBJECT_COUNT          = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_LIVE_OBJECT_COUNT);
  private static final PolledAttribute    PA_LOCK_RECALL_RATE           = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_LOCK_RECALL_RATE);
  private static final PolledAttribute    PA_BROADCAST_RATE             = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_BROADCAST_RATE);
  private static final PolledAttribute    PA_TRANSACTION_SIZE_RATE      = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_TRANSACTION_SIZE_RATE);
  private static final PolledAttribute    PA_PENDING_TRANSACTIONS_COUNT = new PolledAttribute(L2MBeanNames.DSO,
                                                                                              POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);

  public Server(IClusterModel clusterModel) {
    this(clusterModel, ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT,
         ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  public Server(IClusterModel clusterModel, String host, int jmxPort, boolean autoConnect) {
    this.clusterModel = clusterModel;
    ConnectionManagerListener cml = new ConnectionManagerListener();
    connectManager = new ServerConnectionManager(host, jmxPort, false, cml);
    if (autoConnect) {
      refreshCachedCredentials();
    }
    init();
    connectManager.setAutoConnect(autoConnect);
  }

  public Server(IClusterModel clusterModel, IServerGroup serverGroup, L2Info l2Info) {
    super();
    this.clusterModel = clusterModel;
    this.serverGroup = serverGroup;
    ConnectionManagerListener cml = new ConnectionManagerListener();
    connectManager = new ServerConnectionManager(l2Info, false, cml);
    init();
    connectManager.setAutoConnect(true);
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public IServerGroup getServerGroup() {
    return serverGroup;
  }

  public synchronized String getName() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getName() : "not connected";
  }

  private void init() {
    startTime = activateTime = -1;
    displayLabel = connectManager.toString();
    listenerList = new EventListenerList();
    logListener = new LogListener();
    pendingClients = new CopyOnWriteArrayList<DSOClient>();
    clients = new CopyOnWriteArrayList<DSOClient>();
    clientChangeListener = new ClientChangeListener();
    roots = new CopyOnWriteArrayList<IBasicObject>();
    rootMap = new ConcurrentHashMap<ObjectName, IBasicObject>();
    readySet = Collections.synchronizedSet(new HashSet<ObjectName>());
    initReadySet();
    initPolledAttributes();
    ready = false;
  }

  private void initReadySet() {
    readySet.add(L2MBeanNames.TC_SERVER_INFO);
    readySet.add(L2MBeanNames.DSO);
    readySet.add(L2MBeanNames.SERVER_DB_BACKUP);
    readySet.add(L2MBeanNames.OBJECT_MANAGEMENT);
    readySet.add(L2MBeanNames.LOGGER);
    readySet.add(L2MBeanNames.LOCK_STATISTICS);
    readySet.add(StatisticsMBeanNames.STATISTICS_GATHERER);
  }

  private void initPolledAttributes() {
    registerPolledAttribute(PA_CPU_USAGE);
    registerPolledAttribute(PA_USED_MEMORY);
    registerPolledAttribute(PA_MAX_MEMORY);
    registerPolledAttribute(PA_OBJECT_FLUSH_RATE);
    registerPolledAttribute(PA_OBJECT_FAULT_RATE);
    registerPolledAttribute(PA_TRANSACTION_RATE);
    registerPolledAttribute(PA_CACHE_MISS_RATE);
    registerPolledAttribute(PA_LIVE_OBJECT_COUNT);
    registerPolledAttribute(PA_LOCK_RECALL_RATE);
    registerPolledAttribute(PA_BROADCAST_RATE);
    registerPolledAttribute(PA_TRANSACTION_SIZE_RATE);
    registerPolledAttribute(PA_PENDING_TRANSACTIONS_COUNT);
  }

  private void filterReadySet() {
    synchronized (readySet) {
      Iterator<ObjectName> iter = readySet.iterator();
      while (iter.hasNext()) {
        ObjectName beanName = iter.next();
        if (isMBeanRegistered(beanName)) {
          iter.remove();
        }
      }
    }
  }

  public synchronized L2Info getL2Info() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getL2Info() : null;
  }

  public String getConnectionStatusString() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getStatusString() : "not connected";
  }

  private class ConnectionManagerListener implements ConnectionListener {
    public void handleConnection() {
      ServerConnectionManager scm = getConnectionManager();
      if (scm != null) {
        boolean isConnected = scm.isConnected();
        setConnected(isConnected);
        if (isConnected) {
          clearConnectError();
        }
      }
    }

    public void handleException() {
      ServerConnectionManager scm = getConnectionManager();
      if (scm != null) {
        setConnectError(scm.getConnectionException());
      }
    }
  }

  private synchronized void setupFromDSOBean() throws Exception {
    if (isActiveCoordinator()) {
      for (ObjectName clientBeanName : getDSOBean().getClients()) {
        if (!haveClient(clientBeanName)) {
          addClient(clientBeanName);
        }
      }
      for (ObjectName rootBeanName : getDSOBean().getRoots()) {
        if (!haveRoot(rootBeanName)) {
          addRoot(rootBeanName);
        }
      }
    }

    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      cc.addNotificationListener(L2MBeanNames.DSO, this);
    }
  }

  private void connectionEstablished() {
    if (readySet == null) return;
    try {
      ObjectName mbsd = getConnectionContext().queryName("JMImplementation:type=MBeanServerDelegate");
      getConnectionContext().addNotificationListener(mbsd, this);
      testAddLogListener();
      filterReadySet();
      if (!readySet.contains(L2MBeanNames.DSO)) {
        setupFromDSOBean();
      }
      /*
       * We don't want to have knowledge of if/how/when MBeans are handled by the server but for backward compatibility
       * reasons, if the SERVER_DB_BACKUP bean isn't registered by this time, we can remove it from readySet and deal
       * with it not existing. The set of MBeans already registered at connection time are defined by
       * L2Management.registerMBeans.
       */
      if (!readySet.contains(L2MBeanNames.SERVER_DB_BACKUP)) {
        serverDBBackupSupported = true;
        getServerDBBackupBean();
      } else {
        readySet.remove(L2MBeanNames.SERVER_DB_BACKUP);
        serverDBBackupSupported = false;
      }
      if (!readySet.contains(L2MBeanNames.LOCK_STATISTICS)) {
        lockProfilingSupported = true;
        getLockProfilerBean();
      } else {
        readySet.remove(L2MBeanNames.LOCK_STATISTICS);
        lockProfilingSupported = false;
      }
      if (!readySet.contains(StatisticsMBeanNames.STATISTICS_GATHERER)) {
        clusterStatsSupported = true;
        getClusterStatsBean();
      } else {
        readySet.remove(StatisticsMBeanNames.STATISTICS_GATHERER);
        clusterStatsSupported = false;
      }
      setReady(readySet.isEmpty());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void setConnected(boolean connected) {
    boolean oldConnected;
    synchronized (this) {
      if (readySet == null) { return; }
      oldConnected = isConnected();
      this.connected = connected;
    }
    firePropertyChange(PROP_CONNECTED, !connected, connected);
    if (connected == true && oldConnected == false) {
      connectionEstablished();
    }
    if (oldConnected == true && connected == false) {
      setReady(false);
      handleDisconnect();
    }
  }

  public synchronized boolean isConnected() {
    return connected;
  }

  private void setConnectError(Exception e) {
    Exception oldConnectError;
    synchronized (this) {
      oldConnectError = connectException;
      connectException = e;
    }
    firePropertyChange(PROP_CONNECT_ERROR, oldConnectError, e);
  }

  private void clearConnectError() {
    setConnectError(null);
  }

  public synchronized boolean hasConnectError() {
    return connectException != null;
  }

  public synchronized Exception getConnectError() {
    return connectException;
  }

  public String getConnectErrorMessage() {
    return getConnectErrorMessage(getConnectError(), this);
  }

  public String getConnectErrorMessage(Exception e) {
    return getConnectErrorMessage(e, this);
  }

  public static String getConnectErrorMessage(Exception e, Server server) {
    return getConnectErrorMessage(e, server.getConnectionManager().getConnectionContext());
  }

  public static String getConnectErrorMessage(Exception e, ConnectionContext cc) {
    String msg = null;
    Throwable cause = ExceptionHelper.getRootCause(e);

    if (cause instanceof ServiceUnavailableException) {
      MessageFormat form = new MessageFormat("Service Unavailable: {0}");
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else if (cause instanceof ConnectException) {
      MessageFormat form = new MessageFormat("Unable to connect to {0}");
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else if (cause instanceof UnknownHostException || cause instanceof java.rmi.UnknownHostException) {
      MessageFormat form = new MessageFormat("Unknown host: {0}");
      Object[] args = new Object[] { cc.host };
      msg = form.format(args);
    } else if (cause instanceof CommunicationException) {
      MessageFormat form = new MessageFormat("Unable to connect to {0}");
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else {
      msg = cause != null ? cause.getMessage() : e.getMessage();
    }

    return "<html>" + msg + "</html>";
  }

  synchronized ServerConnectionManager getConnectionManager() {
    return connectManager;
  }

  public String[] getConnectionCredentials() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getCredentials() : null;
  }

  public Map<String, Object> getConnectionEnvironment() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getConnectionEnvironment() : null;
  }

  public synchronized ConnectionContext getConnectionContext() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getConnectionContext() : null;
  }

  public synchronized void setHost(String host) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectManager is null");
    if (!host.equals(scm.getHostname())) {
      scm.setHostname(host);
      displayLabel = getConnectionManager().toString();
    }
  }

  public synchronized String getHost() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getHostname() : null;
  }

  public synchronized void setPort(int port) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectManager is null");
    if (port != scm.getJMXPortNumber()) {
      scm.setJMXPortNumber(port);
      displayLabel = getConnectionManager().toString();
    }
  }

  public synchronized int getPort() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getJMXPortNumber() : -1;
  }

  public synchronized Integer getDSOListenPort() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getDSOListenPort() : Integer.valueOf(-1);
  }

  public synchronized Integer getDSOGroupPort() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getDSOGroupPort() : Integer.valueOf(-1);
  }

  public String getPersistenceMode() {
    if (persistenceMode == null) {
      try {
        persistenceMode = getServerInfoBean().getPersistenceMode();
      } catch (UndeclaredThrowableException edte) {
        persistenceMode = "unknown";
      }
    }
    return persistenceMode;
  }

  public String getFailoverMode() {
    if (failoverMode == null) {
      try {
        failoverMode = getServerInfoBean().getFailoverMode();
      } catch (UndeclaredThrowableException udte) {
        failoverMode = "unknown";
      }
    }
    return failoverMode;
  }

  public String getStatsExportServletURI() {
    Integer port = isActive() ? getDSOListenPort() : getDSOGroupPort();
    Object[] args = new Object[] { getHost(), port.toString() };
    return MessageFormat.format("http://{0}:{1}/statistics-gatherer/retrieveStatistics?format=zip", args);
  }

  public String getStatsExportServletURI(String sessionId) {
    Integer port = isActive() ? getDSOListenPort() : getDSOGroupPort();
    Object[] args = new Object[] { getHost(), port.toString(), sessionId };
    return MessageFormat.format("http://{0}:{1}/stats-export?session={2}", args);
  }

  AuthScope getAuthScope() throws Exception {
    Integer port = isActive() ? getDSOListenPort() : getDSOGroupPort();
    return new AuthScope(getHost(), port);
  }

  public synchronized boolean isStarted() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isStarted();
  }

  public synchronized boolean isActive() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isActive();
  }

  public synchronized boolean testIsActive() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.testIsActive();
  }

  public synchronized boolean isPassiveUninitialized() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isPassiveUninitialized();
  }

  public synchronized boolean isPassiveStandby() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isPassiveStandby();
  }

  public void doShutdown() {
    getServerInfoBean().shutdown();
  }

  public synchronized boolean isAutoConnect() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isAutoConnect();
  }

  public synchronized void setAutoConnect(boolean autoConnect) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectionManager is null");
    if (autoConnect) {
      refreshCachedCredentials();
    }
    scm.setAutoConnect(autoConnect);
  }

  public synchronized void refreshCachedCredentials() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectionManager is null");
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);
    if (creds != null) {
      setConnectionCredentials(creds);
    }
  }

  public synchronized void setConnectionCredentials(String[] creds) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectionManager is null");
    scm.setCredentials(creds);
  }

  public JMXConnector getJMXConnector() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getJmxConnector() : null;
  }

  public void setJMXConnector(JMXConnector jmxc) throws IOException {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) throw new IllegalStateException("ServerConnectionManager is null");
    scm.setJMXConnector(jmxc);
  }

  protected synchronized TCServerInfoMBean getServerInfoBean() {
    if (serverInfoBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        serverInfoBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                                  TCServerInfoMBean.class, false);
      }
    }
    return serverInfoBean;
  }

  protected synchronized DSOMBean getDSOBean() {
    if (dsoBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        dsoBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
      }
    }
    return dsoBean;
  }

  private synchronized ObjectManagementMonitorMBean getObjectManagementMonitorBean() {
    if (objectManagementMonitorBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        objectManagementMonitorBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT,
                                                                               ObjectManagementMonitorMBean.class,
                                                                               false);
      }
    }
    return objectManagementMonitorBean;
  }

  public synchronized IProductVersion getProductInfo() {
    if (productInfo == null) {
      ConnectionContext cc = getConnectionContext();
      String[] attributes = { "Version", "Patched", "PatchLevel", "PatchVersion", "BuildID",
          "DescriptionOfCapabilities", "Copyright" };
      String version = ProductInfo.UNKNOWN_VALUE;
      String patchLevel = ProductInfo.UNKNOWN_VALUE;
      String patchVersion = ProductInfo.UNKNOWN_VALUE;
      String buildID = ProductInfo.UNKNOWN_VALUE;
      String capabilities = ProductInfo.UNKNOWN_VALUE;
      String copyright = ProductInfo.UNKNOWN_VALUE;
      try {
        AttributeList attrList = cc.mbsc.getAttributes(L2MBeanNames.TC_SERVER_INFO, attributes);
        if (attrList.get(0) != null) {
          version = (String) ((Attribute) attrList.get(0)).getValue();
        }
        boolean isPatched = false;
        if (attrList.get(1) != null) {
          isPatched = (Boolean) ((Attribute) attrList.get(1)).getValue();
        }
        if (attrList.get(2) != null) {
          patchLevel = isPatched ? (String) ((Attribute) attrList.get(2)).getValue() : null;
        }
        if (attrList.get(3) != null) {
          patchVersion = (String) ((Attribute) attrList.get(3)).getValue();
        }
        if (attrList.get(4) != null) {
          buildID = (String) ((Attribute) attrList.get(4)).getValue();
        }
        if (attrList.get(5) != null) {
          capabilities = (String) ((Attribute) attrList.get(5)).getValue();
        }
        if (attrList.get(6) != null) {
          copyright = (String) ((Attribute) attrList.get(6)).getValue();
        }
      } catch (Exception e) {
        System.err.println(e);
      }
      productInfo = new ProductVersion(version, patchLevel, patchVersion, buildID, capabilities, copyright);
    }
    return productInfo;
  }

  public String getProductVersion() {
    return getProductInfo().version();
  }

  public String getProductPatchLevel() {
    return getProductInfo().patchLevel();
  }

  public String getProductPatchVersion() {
    return getProductInfo().patchVersion();
  }

  public String getProductBuildID() {
    return getProductInfo().buildID();
  }

  public String getProductLicense() {
    return getProductInfo().license();
  }

  public String getProductCopyright() {
    return getProductInfo().copyright();
  }

  public String getEnvironment() {
    return getServerInfoBean().getEnvironment();
  }

  public String getConfig() {
    return getServerInfoBean().getConfig();
  }

  public long getStartTime() {
    if (startTime == -1) {
      startTime = getServerInfoBean().getStartTime();
    }
    return startTime;
  }

  public long getActivateTime() {
    if (activateTime == -1) {
      activateTime = getServerInfoBean().getActivateTime();
    }
    return activateTime;
  }

  public synchronized long getTransactionRate() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getTransactionRate() : null;
  }

  public synchronized StatisticData[] getCpuUsage() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getCpuUsage() : null;
  }

  public String[] getCpuStatNames() {
    if (isReady()) {
      return getServerInfoBean().getCpuStatNames();
    } else {
      return new String[0];
    }
  }

  public synchronized Map getServerStatistics() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getStatistics() : null;
  }

  public synchronized Number[] getDSOStatistics(String[] names) {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getStatistics(names) : null;
  }

  public synchronized Map getPrimaryStatistics() {
    Map result = getServerStatistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }

  public Map<IClient, Long> getAllPendingTransactionsCount() {
    Map<ObjectName, Long> map = getDSOBean().getAllPendingTransactionsCount();
    Map<IClient, Long> result = new HashMap<IClient, Long>();
    Iterator<DSOClient> clientIter = clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Integer> getClientLiveObjectCount() {
    Map<ObjectName, Integer> map = getDSOBean().getClientLiveObjectCount();
    Map<IClient, Integer> result = new HashMap<IClient, Integer>();
    Iterator<DSOClient> clientIter = clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Map<String, Object>> getPrimaryClientStatistics() {
    Map<ObjectName, Map> map = getDSOBean().getPrimaryClientStatistics();
    Map<IClient, Map<String, Object>> result = new HashMap<IClient, Map<String, Object>>();
    for (DSOClient client : getClients()) {
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Long> getClientTransactionRates() {
    Map<ObjectName, Long> map = getDSOBean().getClientTransactionRates();
    Map<IClient, Long> result = new HashMap<IClient, Long>();
    for (DSOClient client : getClients()) {
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public void addClientConnectionListener(ClientConnectionListener listener) {
    listenerList.remove(ClientConnectionListener.class, listener);
    listenerList.add(ClientConnectionListener.class, listener);
  }

  public void removeClientConnectionListener(ClientConnectionListener listener) {
    listenerList.remove(ClientConnectionListener.class, listener);
  }

  protected void fireClientConnected(DSOClient client) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ClientConnectionListener.class) {
        ((ClientConnectionListener) listeners[i + 1]).clientConnected(client);
      }
    }
  }

  protected void fireClientDisconnected(DSOClient client) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ClientConnectionListener.class) {
        ((ClientConnectionListener) listeners[i + 1]).clientDisconnected(client);
      }
    }
  }

  protected void setReady(boolean ready) {
    boolean oldReady;
    synchronized (this) {
      oldReady = isReady();
      this.ready = ready;
    }
    firePropertyChange(PROP_READY, oldReady, ready);
  }

  public synchronized boolean isReady() {
    return ready;
  }

  private void beanRegistered(ObjectName beanName) {
    if (beanName.equals(L2MBeanNames.DSO)) {
      try {
        setupFromDSOBean();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    readySet.remove(beanName);
    setReady(readySet.isEmpty());
  }

  private synchronized boolean isMBeanRegistered(ObjectName beanName) {
    try {
      ConnectionContext cc = getConnectionContext();
      return cc != null && cc.isRegistered(beanName);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean haveClient(ObjectName objectName) {
    synchronized (clients) {
      for (DSOClient client : clients) {
        if (client.getBeanName().equals(objectName)) { return true; }
      }
    }
    return false;
  }

  private class ClientChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      final String prop = evt.getPropertyName();
      if (IClusterModelElement.PROP_READY.equals(prop)) {
        DSOClient client = (DSOClient) evt.getSource();
        synchronized (CLIENT_ADD_LOCK) {
          if (client.isReady() && !clients.contains(client)) {
            clients.add(client);
            fireClientConnected(client);
            client.removePropertyChangeListener(this);
          }
        }
      }
    }
  }

  private DSOClient addClient(ObjectName clientBeanName) {
    assertActiveCoordinator();
    DSOClient client = new DSOClient(getConnectionContext(), clientBeanName, clusterModel);
    if (client.isReady()) {
      clients.add(client);
      fireClientConnected(client);
    } else {
      pendingClients.add(client);
      client.addPropertyChangeListener(clientChangeListener);
    }
    return client;
  }

  private void removeClient(ObjectName clientBeanName) {
    DSOClient target = null;
    Iterator<DSOClient> iter = clients.iterator();
    while (iter.hasNext()) {
      DSOClient client = iter.next();
      if (client.getBeanName().equals(clientBeanName)) {
        target = client;
        clients.remove(client);
        break;
      }
    }
    if (target != null) {
      fireClientDisconnected(target);
    }
  }

  private final Object CLIENT_ADD_LOCK = new Object();

  private void clientNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type)) {
      ObjectName clientObjectName = (ObjectName) notification.getSource();
      synchronized (CLIENT_ADD_LOCK) {
        if (isActiveCoordinator() && !haveClient(clientObjectName)) {
          addClient(clientObjectName);
        }
      }
    } else if (DSOMBean.CLIENT_DETACHED.equals(type)) {
      removeClient((ObjectName) notification.getSource());
    }
  }

  public void addRootCreationListener(RootCreationListener listener) {
    listenerList.add(RootCreationListener.class, listener);
  }

  public void removeRootCreationListener(RootCreationListener listener) {
    listenerList.remove(RootCreationListener.class, listener);
  }

  protected void fireRootCreated(IBasicObject root) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == RootCreationListener.class) {
        ((RootCreationListener) listeners[i + 1]).rootCreated(root);
      }
    }
  }

  private boolean haveRoot(ObjectName objectName) {
    return rootMap.containsKey(objectName);
  }

  private void rootAdded(Notification notification, Object handback) {
    assertActiveCoordinator();

    ObjectName objectName = (ObjectName) notification.getSource();
    IBasicObject newRoot = null;
    synchronized (this) {
      if (!haveRoot(objectName)) {
        newRoot = addRoot(objectName);
      }
    }
    if (newRoot != null) {
      fireRootCreated(newRoot);
    }
  }

  private ManagedObjectFacade safeLookupFacade(DSORootMBean rootBean) {
    try {
      return rootBean.lookupFacade(ConnectionContext.DSO_SMALL_BATCH_SIZE);
    } catch (Exception e) {
      return null;
    }
  }

  private void assertActiveCoordinator() {
    boolean isActiveCoord = isActiveCoordinator();
    if (!isActiveCoord) {
      Thread.dumpStack();
    }
    assert isActiveCoord;
  }

  public boolean isActiveCoordinator() {
    IServerGroup group = getServerGroup();
    if (group != null) {
      IServer activeCoord = group.getActiveServer();
      return this.equals(activeCoord);
    }
    return false;
  }

  private synchronized IBasicObject addRoot(ObjectName rootBeanName) {
    assertActiveCoordinator();
    ConnectionContext cc = getConnectionContext();
    DSORootMBean rootBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, rootBeanName, DSORootMBean.class, false);
    String fieldName = rootBean.getRootName();
    ManagedObjectFacade facade = safeLookupFacade(rootBean);
    if (facade != null) {
      String type = facade.getClassName();
      IBasicObject root = new BasicTcObject(getClusterModel(), fieldName, facade, type, null);
      rootMap.put(rootBeanName, root);
      roots.add(root);
      return root;
    }
    return null;
  }

  public synchronized IBasicObject[] getRoots() {
    return roots.toArray(new IBasicObject[roots.size()]);
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        beanRegistered(mbsn.getMBeanName());
      }
    } else if (DSOMBean.CLIENT_ATTACHED.equals(type) || DSOMBean.CLIENT_DETACHED.equals(type)) {
      if (isActiveCoordinator()) {
        clientNotification(notification, handback);
      }
    } else if (DSOMBean.ROOT_ADDED.equals(type)) {
      if (isActiveCoordinator()) {
        rootAdded(notification, handback);
      }
    } else if (DSOMBean.GC_STATUS_UPDATE.equals(type)) {
      if (isActiveCoordinator()) {
        fireStatusUpdated((GCStats) notification.getSource());
      }
    }
  }

  public synchronized String getCanonicalHostName() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.safeGetHostName() : "not connected";
  }

  public synchronized String getHostAddress() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.safeGetHostAddress() : "not connected";
  }

  public StatisticsLocalGathererMBean getStatisticsGathererMBean() {
    ConnectionContext cc = getConnectionContext();
    return MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, StatisticsMBeanNames.STATISTICS_GATHERER,
                                                    StatisticsLocalGathererMBean.class, true);
  }

  public IServer[] getClusterServers() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    L2Info[] l2Infos = serverInfo.getL2Info();
    Server[] result = new Server[l2Infos.length];
    for (int i = 0; i < l2Infos.length; i++) {
      result[i] = new Server(getClusterModel(), getServerGroup(), l2Infos[i]);
    }
    return result;
  }

  public IServerGroup[] getClusterServerGroups() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    ServerGroupInfo[] serverGroupInfos = serverInfo.getServerGroupInfo();
    ServerGroup[] result = new ServerGroup[serverGroupInfos.length];
    for (int i = 0; i < serverGroupInfos.length; i++) {
      result[i] = new ServerGroup(getClusterModel(), serverGroupInfos[i]);
    }
    return result;
  }

  public synchronized DSOClient[] getClients() {
    return clients.toArray(new DSOClient[clients.size()]);
  }

  protected synchronized void resetBeanProxies() {
    serverInfoBean = null;
    dsoBean = null;
    objectManagementMonitorBean = null;
    serverDBBackupBean = null;
    lockProfilerBean = null;
    clusterStatsBean = null;
    productInfo = null;
  }

  public synchronized void disconnect() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm != null) {
      scm.disconnect();
    }
  }

  private void removeAllClients() {
    DSOClient[] theClients = getClients();
    for (DSOClient client : theClients) {
      clients.remove(client);
      fireClientDisconnected(client);
    }
  }

  synchronized void reset() {
    if (roots == null) return;
    startTime = activateTime = -1;
    connected = ready = false;
    initReadySet();
    roots.clear();
    rootMap.clear();
    removeAllClients();
    resetBeanProxies();
  }

  void handleDisconnect() {
    reset();
  }

  public synchronized String takeThreadDump(long moment) {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.takeThreadDump(moment) : "not connected";
  }

  public void addServerLogListener(ServerLogListener listener) {
    synchronized (listenerList) {
      listenerList.remove(ServerLogListener.class, listener);
      listenerList.add(ServerLogListener.class, listener);
      testAddLogListener();
    }
  }

  private void safeRemoveLogListener() {
    try {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        cc.removeNotificationListener(L2MBeanNames.LOGGER, logListener);
      }
    } catch (Exception e) {
      /**/
    }
  }

  private void testAddLogListener() {
    if (listenerList.getListenerCount(ServerLogListener.class) > 0) {
      try {
        ConnectionContext cc = getConnectionContext();
        if (cc != null) {
          safeRemoveLogListener();
          cc.addNotificationListener(L2MBeanNames.LOGGER, logListener);
        }
      } catch (Exception e) {
        /* connection has probably dropped */
      }
    }
  }

  public void removeServerLogListener(ServerLogListener listener) {
    synchronized (listenerList) {
      listenerList.remove(ServerLogListener.class, listener);
      testRemoveLogListener();
    }
  }

  private synchronized void testRemoveLogListener() {
    if (listenerList.getListenerCount(ServerLogListener.class) == 0) {
      try {
        ConnectionContext cc = getConnectionContext();
        if (cc != null) {
          cc.removeNotificationListener(L2MBeanNames.LOGGER, logListener);
        }
      } catch (Exception e) {
        /* connection has probably dropped */
      }
    }
  }

  private void fireMessageLogged(String logMsg) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ServerLogListener.class) {
        ((ServerLogListener) listeners[i + 1]).messageLogged(logMsg);
      }
    }
  }

  class LogListener implements NotificationListener {
    public void handleNotification(Notification notice, Object handback) {
      fireMessageLogged(notice.getMessage());
    }
  }

  public synchronized ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.lookupFacade(objectID, limit) : null;
  }

  public synchronized DSOClassInfo[] getClassInfo() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getClassInfo() : null;
  }

  public synchronized GCStats[] getGCStats() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getGarbageCollectorStats() : null;
  }

  public void addDGCListener(DGCListener listener) {
    listenerList.add(DGCListener.class, listener);
  }

  public void removeDGCListener(DGCListener listener) {
    listenerList.remove(DGCListener.class, listener);
  }

  private void fireStatusUpdated(GCStats gcStats) {
    assertActiveCoordinator();

    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DGCListener.class) {
        ((DGCListener) listeners[i + 1]).statusUpdate(gcStats);
      }
    }
  }

  public synchronized void runGC() {
    assertActiveCoordinator();

    ObjectManagementMonitorMBean oomb = getObjectManagementMonitorBean();
    if (oomb != null) {
      oomb.runGC();
    }
  }

  public synchronized int getLiveObjectCount() {
    try {
      DSOMBean theDsoBean = getDSOBean();
      return theDsoBean != null ? theDsoBean.getLiveObjectCount() : -1;
    } catch (UndeclaredThrowableException ute) {
      return -1;
    }
  }

  public boolean isResidentOnClient(IClient client, ObjectID oid) {
    try {
      DSOMBean theDsoBean = getDSOBean();
      return theDsoBean != null ? theDsoBean.isResident(client.getClientID(), oid) : false;
    } catch (UndeclaredThrowableException ute) {
      return false;
    }
  }

  public synchronized ServerDBBackupMBean getServerDBBackupBean() {
    if (!serverDBBackupSupported) return null;
    if (serverDBBackupBean != null) return serverDBBackupBean;
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        serverDBBackupBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.SERVER_DB_BACKUP,
                                                                      ServerDBBackupMBean.class, true);
        if (serverDBBackupBean != null && serverDBBackupBean.isBackupEnabled()) {
          cc.addNotificationListener(L2MBeanNames.SERVER_DB_BACKUP, new ServerDBBackupListener());
        }
      } catch (Exception e) {
        /* Connection probably dropped */
      }
    }
    return serverDBBackupBean;
  }

  private class ServerDBBackupListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      String type = notification.getType();

      if (ServerDBBackupMBean.BACKUP_STARTED.equals(type)) {
        fireDBBackupStarted();
      } else if (ServerDBBackupMBean.BACKUP_COMPLETED.equals(type)) {
        fireDBBackupCompleted();
      } else if (ServerDBBackupMBean.PERCENTAGE_COPIED.equals(type)) {
        String message = notification.getMessage();
        String value = message.substring(0, message.indexOf(' '));
        int percentCopied = Integer.parseInt(value);
        fireDBBackupProgress(percentCopied);
      } else if (ServerDBBackupMBean.BACKUP_FAILED.equals(type)) {
        fireDBBackupFailed(notification.getMessage());
      }
    }
  }

  public void addDBBackupListener(DBBackupListener listener) {
    removeDBBackupListener(listener);
    listenerList.add(DBBackupListener.class, listener);
  }

  public void removeDBBackupListener(DBBackupListener listener) {
    listenerList.remove(DBBackupListener.class, listener);
  }

  private void fireDBBackupStarted() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupStarted();
      }
    }
  }

  private void fireDBBackupCompleted() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupCompleted();
      }
    }
  }

  private void fireDBBackupFailed(String message) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupFailed(message);
      }
    }
  }

  private void fireDBBackupProgress(int percentCopied) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupProgress(percentCopied);
      }
    }
  }

  public synchronized boolean isDBBackupSupported() {
    return serverDBBackupSupported;
  }

  private void ensureDBBackupEnabled() {
    if (!isDBBackupSupported()) { throw new IllegalStateException("DBBackup not supported"); }
  }

  public synchronized void backupDB() throws IOException {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      backupBean.runBackUp();
    }
  }

  public synchronized void backupDB(String path) throws IOException {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      backupBean.runBackUp(path);
    }
  }

  public synchronized boolean isDBBackupRunning() {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      return backupBean.isBackUpRunning();
    }
    return false;
  }

  public synchronized String getDefaultDBBackupPath() {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      return backupBean.getDefaultPathForBackup();
    }
    return "not connected";
  }

  public synchronized boolean isDBBackupEnabled() {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      return backupBean.isBackupEnabled();
    }
    return false;
  }

  public synchronized String getDBHome() {
    ServerDBBackupMBean backupBean = getServerDBBackupBean();
    if (backupBean != null) {
      ensureDBBackupEnabled();
      return backupBean.getDbHome();
    }
    return "not connected";
  }

  public synchronized LockStatisticsMonitorMBean getLockProfilerBean() {
    if (lockProfilerBean != null) return lockProfilerBean;
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        lockProfilerBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.LOCK_STATISTICS,
                                                                    LockStatisticsMonitorMBean.class, true);
        cc.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, new LockStatsNotificationListener());
      } catch (Exception e) {
        lockProfilingSupported = false;
      }
    }
    return lockProfilerBean;
  }

  private class LockStatsNotificationListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      String type = notification.getType();
      if (type.equals(LockStatisticsMonitorMBean.TRACE_DEPTH)) {
        int oldTraceDepth = lockProfilerTraceDepth;
        lockProfilerTraceDepth = -1;
        firePropertyChange(PROP_LOCK_STATS_TRACE_DEPTH, oldTraceDepth, getLockProfilerTraceDepth());
      } else if (type.equals(LockStatisticsMonitorMBean.TRACES_ENABLED)) {
        boolean oldLockStatsEnabled = lockProfilingEnabled.booleanValue();
        lockProfilingEnabled = null;
        firePropertyChange(PROP_LOCK_STATS_ENABLED, oldLockStatsEnabled, isLockProfilingEnabled());
      }
    }
  }

  public boolean isLockProfilingSupported() {
    return lockProfilingSupported;
  }

  public int getLockProfilerTraceDepth() {
    if (lockProfilerTraceDepth != -1) return lockProfilerTraceDepth;
    if (lockProfilerBean != null) { return lockProfilerTraceDepth = lockProfilerBean.getTraceDepth(); }
    return -1;
  }

  public void setLockProfilerTraceDepth(int traceDepth) {
    if (lockProfilerBean != null && traceDepth != lockProfilerTraceDepth) {
      lockProfilerBean.setLockStatisticsConfig(traceDepth, 1);
    }
  }

  public boolean isLockProfilingEnabled() {
    if (lockProfilingEnabled != null) return lockProfilingEnabled.booleanValue();
    if (lockProfilerBean != null) { return lockProfilingEnabled = Boolean.valueOf(lockProfilerBean
        .isLockStatisticsEnabled()); }
    return false;
  }

  public void setLockProfilingEnabled(boolean lockStatsEnabled) {
    if (lockProfilerBean != null) {
      lockProfilerBean.setLockStatisticsEnabled(lockStatsEnabled);
    }
  }

  public Collection<LockSpec> getLockSpecs() {
    if (lockProfilerBean != null) { return lockProfilerBean.getLockSpecs(); }
    return Collections.emptySet();
  }

  public synchronized StatisticsLocalGathererMBean getClusterStatsBean() {
    if (clusterStatsBean != null) return clusterStatsBean;
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        clusterStatsBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, StatisticsMBeanNames.STATISTICS_GATHERER,
                                                                    StatisticsLocalGathererMBean.class, true);
        clusterStatsSupported = clusterStatsBean.isActive();
        if (clusterStatsSupported) {
          cc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATHERER, new ClusterStatsNotificationListener());
        }
      } catch (Exception e) {
        clusterStatsSupported = false;
      }
    }
    return clusterStatsBean;
  }

  public void addClusterStatsListener(IClusterStatsListener listener) {
    removeClusterStatsListener(listener);
    listenerList.add(IClusterStatsListener.class, listener);
  }

  public void removeClusterStatsListener(IClusterStatsListener listener) {
    listenerList.remove(IClusterStatsListener.class, listener);
  }

  private void fireClusterStatsConnected() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).connected();
      }
    }
  }

  private void fireClusterStatsSessionCreated(String sessionId) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).sessionCreated(sessionId);
      }
    }
  }

  private void fireClusterStatsSessionStarted(String sessionId) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).sessionStarted(sessionId);
      }
    }
  }

  private void fireClusterStatsSessionStopped(String sessionId) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).sessionStopped(sessionId);
      }
    }
  }

  private void fireClusterStatsSessionCleared(String sessionId) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).sessionCleared(sessionId);
      }
    }
  }

  private void fireClusterStatsAllSessionsCleared() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).allSessionsCleared();
      }
    }
  }

  private void fireClusterStatsDisconnected() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).disconnected();
      }
    }
  }

  private void fireClusterStatsReinitialized() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == IClusterStatsListener.class) {
        ((IClusterStatsListener) listeners[i + 1]).reinitialized();
      }
    }
  }

  private class ClusterStatsNotificationListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      String type = notification.getType();
      Object userData = notification.getUserData();

      if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_STARTEDUP_TYPE)) {
        fireClusterStatsConnected();
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE)) {
        fireClusterStatsSessionCreated((String) userData);
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE)) {
        fireClusterStatsSessionStarted((String) userData);
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE)) {
        fireClusterStatsSessionStopped((String) userData);
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE)) {
        fireClusterStatsSessionCleared((String) userData);
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE)) {
        fireClusterStatsAllSessionsCleared();
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_SHUTDOWN_TYPE)) {
        fireClusterStatsDisconnected();
      } else if (type.equals(StatisticsLocalGathererMBean.STATISTICS_LOCALGATHERER_REINITIALIZED_TYPE)) {
        fireClusterStatsReinitialized();
      }
    }
  }

  public boolean isClusterStatsSupported() {
    return clusterStatsSupported;
  }

  /*
   * Listener will receive connected message.
   */
  public void startupClusterStats() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.startup();
    }
  }

  public String[] getSupportedClusterStats() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) { return theClusterStatsBean.getSupportedStatistics(); }
    return new String[0];
  }

  public void clearAllClusterStats() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.clearAllStatistics();
    }
  }

  public void clearClusterStatsSession(String sessionId) {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.clearStatistics(sessionId);
    }
  }

  public void startClusterStatsSession(String sessionId, String[] statsToRecord, long samplePeriodMillis) {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.createSession(sessionId);
      theClusterStatsBean.setSessionParam(StatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL, Long
          .valueOf(samplePeriodMillis));
      theClusterStatsBean.enableStatistics(statsToRecord);
      theClusterStatsBean.startCapturing();
    }
  }

  public void endCurrentClusterStatsSession() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.stopCapturing();
      theClusterStatsBean.closeSession();
    }
  }

  public void captureClusterStat(String sraName) {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.captureStatistic(sraName);
    }
  }

  public String[] getAllClusterStatsSessions() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) { return theClusterStatsBean.getAvailableSessionIds(); }
    return new String[0];
  }

  public boolean isActiveClusterStatsSession() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) { return theClusterStatsBean.isCapturing(); }
    return false;
  }

  public String getActiveClusterStatsSession() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) { return theClusterStatsBean.getActiveSessionId(); }
    return "";
  }

  @Override
  public String toString() {
    return displayLabel;
  }

  @Override
  public synchronized void tearDown() {
    super.tearDown();

    clients.clear();
    clients = null;
    readySet.clear();
    readySet = null;
    roots.clear();
    roots = null;
    rootMap.clear();
    rootMap = null;
    pendingClients.clear();
    pendingClients = null;
    clientChangeListener = null;
    connectManager.tearDown();
    connectException = null;
    serverInfoBean = null;
    dsoBean = null;
    objectManagementMonitorBean = null;
    lockProfilerBean = null;
    clusterStatsBean = null;
    productInfo = null;
    logListener = null;
  }

  public void setFaultDebug(boolean faultDebug) {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) {
      serverInfo.setFaultDebug(faultDebug);
    }
  }

  public boolean getFaultDebug() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getFaultDebug(); }
    return false;
  }

  public boolean getFlushDebug() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getFlushDebug(); }
    return false;
  }

  public void setFlushDebug(boolean flushDebug) {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) {
      serverInfo.setFlushDebug(flushDebug);
    }
  }

  public boolean getRequestDebug() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getRequestDebug(); }
    return false;
  }

  public void setRequestDebug(boolean requestDebug) {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) {
      serverInfo.setRequestDebug(requestDebug);
    }
  }

  public boolean getBroadcastDebug() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getBroadcastDebug(); }
    return false;
  }

  public void setBroadcastDebug(boolean broadcastDebug) {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) {
      serverInfo.setBroadcastDebug(broadcastDebug);
    }
  }

  public boolean getCommitDebug() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getCommitDebug(); }
    return false;
  }

  public void setCommitDebug(boolean commitDebug) {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) {
      serverInfo.setCommitDebug(commitDebug);
    }
  }

  public int getGarbageCollectionInterval() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.getGarbageCollectionInterval(); }
    return -1;
  }

  public boolean isGarbageCollectionEnabled() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    if (serverInfo != null) { return serverInfo.isGarbageCollectionEnabled(); }
    return false;
  }

  public String dump() {
    StringBuilder sb = new StringBuilder();
    sb.append(toString());
    return sb.toString();
  }

  public Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                              TimeUnit unit) {
    if (isReady()) { return getDSOBean().getAttributeMap(attributeMap, timeout, unit); }
    return Collections.emptyMap();
  }
}
