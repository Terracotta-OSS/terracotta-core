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
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.stats.DSOClassInfo;
import com.tc.stats.DSOMBean;
import com.tc.stats.DSORootMBean;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.event.EventListenerList;

public class Server extends BaseClusterNode implements IServer, NotificationListener, ManagedObjectFacadeProvider,
    PropertyChangeListener {
  protected IClusterModel                 clusterModel;
  protected IServerGroup                  serverGroup;
  protected final ServerConnectionManager connectManager;
  protected String                        displayLabel;
  protected boolean                       connected;
  protected Set<ObjectName>               readySet;
  protected boolean                       ready;
  protected List<DSOClient>               clients;
  protected Map<ObjectName, DSOClient>    clientMap;
  private ClientChangeListener            clientChangeListener;
  protected EventListenerList             listenerList;
  protected List<DSOClient>               pendingClients;
  protected Exception                     connectException;
  protected TCServerInfoMBean             serverInfoBean;
  protected L2DumperMBean                 serverDumperBean;
  protected DSOMBean                      dsoBean;
  protected ObjectManagementMonitorMBean  objectManagementMonitorBean;
  protected boolean                       serverDBBackupSupported;
  protected ServerDBBackupMBean           serverDBBackupBean;
  protected ProductVersion                productInfo;
  protected List<IBasicObject>            roots;
  protected Map<ObjectName, IBasicObject> rootMap;
  protected LogListener                   logListener;
  protected String                        name;
  protected long                          startTime;
  protected long                          activateTime;
  protected String                        persistenceMode;
  protected String                        failoverMode;
  protected Integer                       jmxPort;
  protected Integer                       dsoListenPort;
  protected Integer                       dsoGroupPort;

  protected LockStatisticsMonitorMBean    lockProfilerBean;
  protected boolean                       lockProfilingSupported;
  protected int                           lockProfilerTraceDepth;
  protected Boolean                       lockProfilingEnabled;

  private StatisticsLocalGathererMBean    clusterStatsBean;
  private boolean                         clusterStatsSupported;

  private static final PolledAttribute    PA_CPU_USAGE                       = new PolledAttribute(
                                                                                                   L2MBeanNames.TC_SERVER_INFO,
                                                                                                   POLLED_ATTR_CPU_USAGE);
  private static final PolledAttribute    PA_USED_MEMORY                     = new PolledAttribute(
                                                                                                   L2MBeanNames.TC_SERVER_INFO,
                                                                                                   POLLED_ATTR_USED_MEMORY);
  private static final PolledAttribute    PA_MAX_MEMORY                      = new PolledAttribute(
                                                                                                   L2MBeanNames.TC_SERVER_INFO,
                                                                                                   POLLED_ATTR_MAX_MEMORY);
  private static final PolledAttribute    PA_OBJECT_FLUSH_RATE               = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OBJECT_FLUSH_RATE);
  private static final PolledAttribute    PA_OBJECT_FAULT_RATE               = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OBJECT_FAULT_RATE);
  private static final PolledAttribute    PA_TRANSACTION_RATE                = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_TRANSACTION_RATE);
  private static final PolledAttribute    PA_CACHE_MISS_RATE                 = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_CACHE_MISS_RATE);
  private static final PolledAttribute    PA_ONHEAP_FAULT_RATE               = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_ONHEAP_FAULT_RATE);
  private static final PolledAttribute    PA_ONHEAP_FLUSH_RATE               = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_ONHEAP_FLUSH_RATE);
  private static final PolledAttribute    PA_OFFHEAP_FAULT_RATE              = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OFFHEAP_FAULT_RATE);
  private static final PolledAttribute    PA_OFFHEAP_FLUSH_RATE              = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OFFHEAP_FLUSH_RATE);
  private static final PolledAttribute    PA_LIVE_OBJECT_COUNT               = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_LIVE_OBJECT_COUNT);
  private static final PolledAttribute    PA_LOCK_RECALL_RATE                = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_LOCK_RECALL_RATE);
  private static final PolledAttribute    PA_BROADCAST_RATE                  = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_BROADCAST_RATE);
  private static final PolledAttribute    PA_TRANSACTION_SIZE_RATE           = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_TRANSACTION_SIZE_RATE);
  private static final PolledAttribute    PA_PENDING_TRANSACTIONS_COUNT      = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_PENDING_TRANSACTIONS_COUNT);
  private static final PolledAttribute    PA_CACHED_OBJECT_COUNT             = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_CACHED_OBJECT_COUNT);
  private static final PolledAttribute    PA_OFFHEAP_OBJECT_CACHED_COUNT     = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OFFHEAP_OBJECT_CACHED_COUNT);
  private static final PolledAttribute    PA_POLLED_ATTR_OFFHEAP_MAX_MEMORY  = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OFFHEAP_MAX_MEMORY);
  private static final PolledAttribute    PA_POLLED_ATTR_OFFHEAP_USED_MEMORY = new PolledAttribute(L2MBeanNames.DSO,
                                                                                                   POLLED_ATTR_OFFHEAP_USED_MEMORY);

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
    if (serverGroup != null) {
      serverGroup.addPropertyChangeListener(this);
    }
    connectManager.setAutoConnect(true);
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public IServerGroup getServerGroup() {
    return serverGroup;
  }

  public synchronized String getName() {
    if (name == null) {
      ServerConnectionManager scm = getConnectionManager();
      name = scm != null ? scm.getName() : "";
    }
    return name;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IServerGroup.PROP_ACTIVE_SERVER.equals(prop)) {
      if (isGroupLeader() && isReady()) {
        try {
          setupFromDSOBean();
        } catch (Exception e) {
          /**/
        }
      }
    }
  }

  protected void init() {
    name = null;
    startTime = activateTime = -1;
    displayLabel = connectManager.toString();
    listenerList = new EventListenerList();
    logListener = new LogListener();
    pendingClients = new ArrayList<DSOClient>();
    clients = new ArrayList<DSOClient>();
    clientMap = new ConcurrentHashMap<ObjectName, DSOClient>();
    clientChangeListener = new ClientChangeListener();
    roots = new ArrayList<IBasicObject>();
    rootMap = new ConcurrentHashMap<ObjectName, IBasicObject>();
    readySet = Collections.synchronizedSet(new HashSet<ObjectName>());
    initReadySet();
    initPolledAttributes();
    ready = false;
  }

  protected synchronized Set<ObjectName> getReadySet() {
    return readySet;
  }

  protected synchronized void initReadySet() {
    Set<ObjectName> theReadySet = getReadySet();
    if (theReadySet != null) {
      theReadySet.add(L2MBeanNames.TC_SERVER_INFO);
      theReadySet.add(L2MBeanNames.DSO);
      theReadySet.add(L2MBeanNames.OBJECT_MANAGEMENT);
      theReadySet.add(L2MBeanNames.LOGGER);
      theReadySet.add(L2MBeanNames.LOCK_STATISTICS);
      theReadySet.add(StatisticsMBeanNames.STATISTICS_GATHERER);
    }
  }

  private void initPolledAttributes() {
    registerPolledAttribute(PA_CPU_USAGE);
    registerPolledAttribute(PA_USED_MEMORY);
    registerPolledAttribute(PA_MAX_MEMORY);
    registerPolledAttribute(PA_OBJECT_FLUSH_RATE);
    registerPolledAttribute(PA_OBJECT_FAULT_RATE);
    registerPolledAttribute(PA_TRANSACTION_RATE);
    registerPolledAttribute(PA_CACHE_MISS_RATE);
    registerPolledAttribute(PA_ONHEAP_FAULT_RATE);
    registerPolledAttribute(PA_ONHEAP_FLUSH_RATE);
    registerPolledAttribute(PA_OFFHEAP_FAULT_RATE);
    registerPolledAttribute(PA_OFFHEAP_FLUSH_RATE);
    registerPolledAttribute(PA_LIVE_OBJECT_COUNT);
    registerPolledAttribute(PA_LOCK_RECALL_RATE);
    registerPolledAttribute(PA_BROADCAST_RATE);
    registerPolledAttribute(PA_TRANSACTION_SIZE_RATE);
    registerPolledAttribute(PA_PENDING_TRANSACTIONS_COUNT);
    registerPolledAttribute(PA_CACHED_OBJECT_COUNT);
    registerPolledAttribute(PA_OFFHEAP_OBJECT_CACHED_COUNT);
    registerPolledAttribute(PA_POLLED_ATTR_OFFHEAP_MAX_MEMORY);
    registerPolledAttribute(PA_POLLED_ATTR_OFFHEAP_USED_MEMORY);
  }

  private synchronized void filterReadySet() {
    Set<ObjectName> theReadySet = getReadySet();
    if (theReadySet != null) {
      for (ObjectName beanName : theReadySet.toArray(new ObjectName[0])) {
        if (isMBeanRegistered(beanName)) {
          testInitRegisteredBean(theReadySet, beanName);
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

  private void setupFromDSOBean() throws Exception {
    DSOMBean theDsoBean = getDSOBean();

    if (theDsoBean == null) { return; }

    if (isGroupLeader()) {
      synchronized (Server.this) {
        clients.clear();
        clientMap.clear();
        for (ObjectName clientBeanName : theDsoBean.getClients()) {
          if (!haveClient(clientBeanName)) {
            addClient(clientBeanName);
          }
        }
        roots.clear();
        rootMap.clear();
        for (ObjectName rootBeanName : theDsoBean.getRoots()) {
          if (!haveRoot(rootBeanName)) {
            addRoot(rootBeanName);
          }
        }
      }

      for (DSOClient client : getClients()) {
        fireClientConnected(client);
      }

      for (IBasicObject root : getRoots()) {
        fireRootCreated(root);
      }
    }

    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      cc.addNotificationListener(L2MBeanNames.DSO, this);
    }
  }

  private void connectionEstablished() {
    Set<ObjectName> theReadySet = getReadySet();
    if (theReadySet == null) { return; }

    try {
      ObjectName mbsd = getConnectionContext().queryName("JMImplementation:type=MBeanServerDelegate");
      getConnectionContext().addNotificationListener(mbsd, this);
      testAddLogListener();
      filterReadySet();
    } catch (Exception e) {
      /* Connection probably dropped */
    }
  }

  protected void setConnected(boolean connected) {
    boolean oldConnected;
    synchronized (Server.this) {
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
    synchronized (Server.this) {
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
    String msg;

    MessageFormat form = new MessageFormat("Unable to connect to {0}");
    msg = form.format(new Object[] { cc });

    Throwable cause = ExceptionHelper.getRootCause(e);
    if (cause instanceof ServiceUnavailableException) {
      form = new MessageFormat("Service Unavailable: {0}");
      msg = form.format(new Object[] { cc });
    } else if (cause instanceof ConnectException) {
      form = new MessageFormat("Unable to connect to {0}");
      msg = form.format(new Object[] { cc });
    } else if (cause instanceof UnknownHostException || cause instanceof java.rmi.UnknownHostException) {
      form = new MessageFormat("Unknown host: {0}");
      msg = form.format(new Object[] { cc.host });
    } else if (cause instanceof CommunicationException) {
      form = new MessageFormat("Unable to connect to {0}");
      msg = form.format(new Object[] { cc });
    } else {
      String exceptionMessage = cause != null ? cause.getMessage() : e.getMessage();
      if (exceptionMessage != null) {
        msg = exceptionMessage;
      }
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
    if (scm == null) { throw new IllegalStateException("ServerConnectManager is null"); }
    if (!host.equals(scm.getHostname())) {
      scm.setHostname(host);
      displayLabel = getConnectionManager().toString();
      name = getConnectionManager().getName();
    }
  }

  public synchronized String getHost() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getHostname() : null;
  }

  public synchronized void setPort(int port) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectManager is null"); }
    if (port != scm.getJMXPortNumber()) {
      scm.setJMXPortNumber(port);
      jmxPort = Integer.valueOf(port);
      displayLabel = scm.toString();
    }
  }

  public synchronized int getPort() {
    if (jmxPort == null) {
      ServerConnectionManager scm = getConnectionManager();
      jmxPort = Integer.valueOf(scm != null ? scm.getJMXPortNumber() : -1);
    }
    return jmxPort.intValue();
  }

  public synchronized Integer getDSOListenPort() {
    if (dsoListenPort == null) {
      TCServerInfoMBean theServerInfoBean = getServerInfoBean();
      dsoListenPort = Integer.valueOf(theServerInfoBean != null ? theServerInfoBean.getDSOListenPort() : -1);
    }
    return dsoListenPort;
  }

  public synchronized Integer getDSOGroupPort() {
    if (dsoGroupPort == null) {
      TCServerInfoMBean theServerInfoBean = getServerInfoBean();
      dsoGroupPort = Integer.valueOf(theServerInfoBean != null ? theServerInfoBean.getDSOGroupPort() : -1);
    }
    return dsoGroupPort;
  }

  public synchronized String getPersistenceMode() {
    if (persistenceMode == null) {
      try {
        TCServerInfoMBean theServerInfoBean = getServerInfoBean();
        if (theServerInfoBean != null) {
          persistenceMode = theServerInfoBean.getPersistenceMode();
        } else {
          persistenceMode = "unknown";
        }
      } catch (UndeclaredThrowableException edte) {
        persistenceMode = "unknown";
      }
    }
    return persistenceMode;
  }

  public synchronized String getFailoverMode() {
    if (failoverMode == null) {
      try {
        TCServerInfoMBean theServerInfoBean = getServerInfoBean();
        if (theServerInfoBean != null) {
          failoverMode = theServerInfoBean.getFailoverMode();
        } else {
          failoverMode = "unknown";
        }
      } catch (UndeclaredThrowableException udte) {
        failoverMode = "unknown";
      }
    }
    return failoverMode;
  }

  public synchronized String getStatsExportServletURI() {
    Integer port = isActive() ? getDSOListenPort() : getDSOGroupPort();
    Object[] args = new Object[] { getHost(), port.toString() };
    return MessageFormat.format("http://{0}:{1}/statistics-gatherer/retrieveStatistics?format=zip", args);
  }

  public synchronized String getStatsExportServletURI(String sessionId) {
    Integer port = isActive() ? getDSOListenPort() : getDSOGroupPort();
    Object[] args = new Object[] { getHost(), port.toString(), sessionId };
    return MessageFormat.format("http://{0}:{1}/stats-export?session={2}", args);
  }

  synchronized AuthScope getAuthScope() throws Exception {
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
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    if (theServerInfoBean != null) {
      theServerInfoBean.shutdown();
    }
  }

  public synchronized boolean isAutoConnect() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null && scm.isAutoConnect();
  }

  public synchronized void setAutoConnect(boolean autoConnect) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectionManager is null"); }
    if (autoConnect) {
      refreshCachedCredentials();
    }
    scm.setAutoConnect(autoConnect);
  }

  public synchronized void refreshCachedCredentials() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectionManager is null"); }
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);
    if (creds != null) {
      setConnectionCredentials(creds);
    }
  }

  public synchronized void setConnectionCredentials(String[] creds) {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectionManager is null"); }
    scm.setCredentials(creds);
  }

  public synchronized void clearConnectionCredentials() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectionManager is null"); }
    scm.clearCredentials();
  }

  public JMXConnector getJMXConnector() {
    ServerConnectionManager scm = getConnectionManager();
    return scm != null ? scm.getJmxConnector() : null;
  }

  public void setJMXConnector(JMXConnector jmxc) throws IOException {
    ServerConnectionManager scm = getConnectionManager();
    if (scm == null) { throw new IllegalStateException("ServerConnectionManager is null"); }
    scm.setJMXConnector(jmxc);
  }

  public synchronized <T> T getMBeanProxy(ObjectName on, Class<T> mbeanType) {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) { return MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, on, mbeanType, false); }
    return null;
  }

  private void safeRemoveNotificationListener(ObjectName on, NotificationListener listener) {
    try {
      ConnectionContext cc = getConnectionContext();
      if (cc.mbsc != null) {
        cc.mbsc.removeNotificationListener(on, listener, null, null);
      }
    } catch (Exception e) {
      /**/
    }
  }

  public boolean addNotificationListener(ObjectName on, NotificationListener listener) throws IOException,
      InstanceNotFoundException {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      safeRemoveNotificationListener(on, listener);
      cc.mbsc.addNotificationListener(on, listener, null, null);
      return true;
    }
    return false;
  }

  public boolean removeNotificationListener(ObjectName on, NotificationListener listener) throws IOException,
      InstanceNotFoundException, ListenerNotFoundException {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      cc.mbsc.removeNotificationListener(on, listener, null, null);
      return true;
    }
    return false;
  }

  public Set<ObjectName> queryNames(ObjectName on, QueryExp query) throws IOException {
    ConnectionContext cc = getConnectionContext();
    if (cc != null && cc.mbsc != null) { return cc.mbsc.queryNames(on, query); }
    return Collections.emptySet();
  }

  protected synchronized TCServerInfoMBean getServerInfoBean() {
    if (serverInfoBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        if (cc.mbsc == null) { return null; }
        serverInfoBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                                  TCServerInfoMBean.class, false);
      }
    }
    return serverInfoBean;
  }

  protected synchronized L2DumperMBean getServerDumperBean() {
    if (serverDumperBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        if (cc.mbsc == null) { return null; }
        serverDumperBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.DUMPER, L2DumperMBean.class,
                                                                    false);
      }
    }
    return serverDumperBean;
  }

  protected synchronized DSOMBean getDSOBean() {
    if (dsoBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        if (cc.mbsc == null) { return null; }
        dsoBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
      }
    }
    return dsoBean;
  }

  private synchronized ObjectManagementMonitorMBean getObjectManagementMonitorBean() {
    if (objectManagementMonitorBean == null) {
      ConnectionContext cc = getConnectionContext();
      if (cc != null) {
        if (cc.mbsc == null) { return null; }
        objectManagementMonitorBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.OBJECT_MANAGEMENT,
                                                                               ObjectManagementMonitorMBean.class,
                                                                               false);
      }
    }
    return objectManagementMonitorBean;
  }

  public synchronized IProductVersion getProductInfo() {
    if (productInfo == null) {
      DSOMBean theDsoBean = getDSOBean();
      if (theDsoBean == null) { return null; }

      Map<ObjectName, Set<String>> requestMap = new HashMap<ObjectName, Set<String>>();
      String[] attributes = { "Version", "MavenArtifactsVersion", "Patched", "PatchLevel", "PatchVersion", "BuildID",
          "DescriptionOfCapabilities", "Copyright" };
      requestMap.put(L2MBeanNames.TC_SERVER_INFO, new HashSet(Arrays.asList(attributes)));
      Map<ObjectName, Map<String, Object>> resultMap = theDsoBean.getAttributeMap(requestMap, Integer.MAX_VALUE,
                                                                                  TimeUnit.SECONDS);
      Map<String, Object> results = resultMap.get(L2MBeanNames.TC_SERVER_INFO);
      String version = ProductInfo.UNKNOWN_VALUE;
      String mavenArtifactsVersion = ProductInfo.UNKNOWN_VALUE;
      String patchLevel = ProductInfo.UNKNOWN_VALUE;
      String patchVersion = ProductInfo.UNKNOWN_VALUE;
      String buildID = ProductInfo.UNKNOWN_VALUE;
      String capabilities = ProductInfo.UNKNOWN_VALUE;
      String copyright = ProductInfo.UNKNOWN_VALUE;
      Object value;
      if ((value = results.get("Version")) != null) {
        version = (String) value;
      }
      if ((value = results.get("MavenArtifactsVersion")) != null) {
        mavenArtifactsVersion = (String) value;
      }
      boolean isPatched = false;
      if ((value = results.get("Patched")) != null) {
        isPatched = (Boolean) value;
      }
      if ((value = results.get("PatchLevel")) != null) {
        patchLevel = isPatched ? (String) value : null;
      }
      if ((value = results.get("PatchVersion")) != null) {
        patchVersion = (String) value;
      }
      if ((value = results.get("BuildID")) != null) {
        buildID = (String) value;
      }
      if ((value = results.get("DescriptionOfCapabilities")) != null) {
        capabilities = (String) value;
      }
      if ((value = results.get("Copyright")) != null) {
        copyright = (String) value;
      }
      productInfo = new ProductVersion(version, mavenArtifactsVersion, patchLevel, patchVersion, buildID, capabilities,
                                       copyright);
    }
    return productInfo;
  }

  public String getProductVersion() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.version() : "";
  }

  public String getProductPatchLevel() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.patchLevel() : "";
  }

  public String getProductPatchVersion() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.patchVersion() : "";
  }

  public String getProductBuildID() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.buildID() : "";
  }

  public String getProductLicense() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.license() : "";
  }

  public String getProductCopyright() {
    IProductVersion pi = getProductInfo();
    return pi != null ? pi.copyright() : "";
  }

  public String getEnvironment() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getEnvironment() : "";
  }

  public String getConfig() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getConfig() : "";
  }

  public long getStartTime() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    if (startTime == -1) {
      startTime = theServerInfoBean != null ? theServerInfoBean.getStartTime() : 0;
    }
    return startTime;
  }

  public long getActivateTime() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    if (activateTime == -1) {
      activateTime = theServerInfoBean != null ? theServerInfoBean.getActivateTime() : 0;
    }
    return activateTime;
  }

  public synchronized long getTransactionRate() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getTransactionRate() : 0;
  }

  private static final StatisticData[] EMPTY_STATDATA_ARRAY = {};

  public synchronized StatisticData[] getCpuUsage() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getCpuUsage() : EMPTY_STATDATA_ARRAY;
  }

  private static final String[] EMPTY_CPU_ARRAY = {};

  public String[] getCpuStatNames() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();

    if (theServerInfoBean != null && isReady()) {
      return theServerInfoBean.getCpuStatNames();
    } else {
      return EMPTY_CPU_ARRAY;
    }
  }

  public synchronized Map getServerStatistics() {
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    return theServerInfoBean != null ? theServerInfoBean.getStatistics() : Collections.emptyMap();
  }

  public synchronized Number[] getDSOStatistics(String[] names) {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getStatistics(names) : new Number[names.length];
  }

  public synchronized Map getPrimaryStatistics() {
    Map result = getServerStatistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }

  public Map<IClient, Long> getAllPendingTransactionsCount() {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean == null) { return Collections.emptyMap(); }

    Map<ObjectName, Long> map = theDsoBean.getAllPendingTransactionsCount();
    Map<IClient, Long> result = new HashMap<IClient, Long>();
    Iterator<DSOClient> clientIter = clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Integer> getClientLiveObjectCount() {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean == null) { return Collections.emptyMap(); }

    Map<ObjectName, Integer> map = theDsoBean.getClientLiveObjectCount();
    Map<IClient, Integer> result = new HashMap<IClient, Integer>();
    Iterator<DSOClient> clientIter = clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Map<String, Object>> getPrimaryClientStatistics() {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean == null) { return Collections.emptyMap(); }

    Map<ObjectName, Map> map = theDsoBean.getPrimaryClientStatistics();
    Map<IClient, Map<String, Object>> result = new HashMap<IClient, Map<String, Object>>();
    for (DSOClient client : getClients()) {
      result.put(client, map.get(client.getBeanName()));
    }
    return result;
  }

  public Map<IClient, Long> getClientTransactionRates() {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean == null) { return Collections.emptyMap(); }

    Map<ObjectName, Long> map = theDsoBean.getClientTransactionRates();
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
    synchronized (Server.this) {
      oldReady = isReady();
      this.ready = ready;
    }
    if (ready && oldReady != ready) {
      try {
        setupFromDSOBean();
      } catch (Exception e) {
        /**/
      }
    }
    firePropertyChange(PROP_READY, oldReady, ready);
  }

  public synchronized boolean isReady() {
    return ready;
  }

  private void beanRegistered(ObjectName beanName) {
    Set<ObjectName> theReadySet = getReadySet();
    if (theReadySet == null) { return; }

    testInitRegisteredBean(theReadySet, beanName);
  }

  protected void testInitRegisteredBean(Set<ObjectName> theReadySet, ObjectName beanName) {

    if (theReadySet.contains(beanName)) {
      if (beanName.equals(L2MBeanNames.DSO)) {
        try {
          setupFromDSOBean();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else if (beanName.equals(StatisticsMBeanNames.STATISTICS_GATHERER)) {
        initClusterStatsBean();
      } else if (beanName.equals(L2MBeanNames.LOCK_STATISTICS)) {
        initLockProfilerBean();
      } else if (beanName.equals(L2MBeanNames.TC_SERVER_INFO)) {
        initServerInfoBean();
      }

      synchronized (this) {
        theReadySet.remove(beanName);
      }
      setReady(theReadySet.isEmpty());
    }
  }

  private void initServerInfoBean() {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        cc.addNotificationListener(L2MBeanNames.TC_SERVER_INFO, this);
      } catch (Exception e) {
        /**/
      }
    }
  }

  private synchronized boolean isMBeanRegistered(ObjectName beanName) {
    try {
      ConnectionContext cc = getConnectionContext();
      return cc != null && cc.isRegistered(beanName);
    } catch (Exception e) {
      return false;
    }
  }

  private synchronized boolean haveClient(ObjectName clientObjectName) {
    return clientMap.containsKey(clientObjectName);
  }

  private class ClientChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      final String prop = evt.getPropertyName();
      if (IClusterModelElement.PROP_READY.equals(prop)) {
        DSOClient client = (DSOClient) evt.getSource();
        boolean wasAdded = false;
        synchronized (Server.this) {
          if (client.isReady()) {
            client.removePropertyChangeListener(this);
            if (!clients.contains(client)) {
              clients.add(client);
            }
            wasAdded = true;
          }
        }
        if (wasAdded) {
          fireClientConnected(client);
        }
      }
    }
  }

  protected synchronized DSOClient addClient(ObjectName clientBeanName) {
    assertGroupLeader();

    DSOClient client = new DSOClient(getConnectionContext(), clientBeanName, clusterModel);
    initializeDSOClient(client, clientBeanName);
    return client;
  }

  protected void initializeDSOClient(DSOClient client, ObjectName clientBeanName) {
    client.testSetupTunneledBeans();
    client.testSetupTunneledBeans();
    client.addPropertyChangeListener(clientChangeListener);
    clients.add(client);
    // Don't notify the client's existence until it's ready
    if (client.isReady()) {
      client.removePropertyChangeListener(clientChangeListener);
    } else {
      pendingClients.add(client);
    }
    clientMap.put(clientBeanName, client);
  }

  private synchronized DSOClient removeClient(ObjectName clientBeanName) {
    DSOClient client = clientMap.remove(clientBeanName);
    if (client != null) {
      clients.remove(client);
      return client;
    }
    return null;
  }

  private void clientNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type)) {
      ObjectName clientObjectName = (ObjectName) notification.getSource();
      DSOClient client = null;
      synchronized (Server.this) {
        if (isGroupLeader() && !haveClient(clientObjectName)) {
          client = addClient(clientObjectName);
        }
      }
      if (client != null && client.isReady()) {
        fireClientConnected(client);
      }
    } else if (DSOMBean.CLIENT_DETACHED.equals(type)) {
      ObjectName clientObjectName = (ObjectName) notification.getSource();
      DSOClient client = null;
      synchronized (Server.this) {
        if (isGroupLeader() && haveClient(clientObjectName)) {
          client = removeClient(clientObjectName);
        }
      }
      if (client != null) {
        fireClientDisconnected(client);
      }
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

  private synchronized boolean haveRoot(ObjectName objectName) {
    return rootMap.containsKey(objectName);
  }

  private void rootAdded(Notification notification, Object handback) {
    assertActiveCoordinator();

    ObjectName objectName = (ObjectName) notification.getSource();
    IBasicObject newRoot = null;
    synchronized (Server.this) {
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
      return clusterModel.lookupFacade(rootBean.getObjectID(), ConnectionContext.DSO_SMALL_BATCH_SIZE);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void assertActiveCoordinator() {
    boolean activeCoordinator = isActiveCoordinator();
    if (!activeCoordinator) {
      Thread.dumpStack();
    }
    Assert.assertTrue(activeCoordinator);
  }

  public boolean isActiveCoordinator() {
    IServerGroup group = getServerGroup();
    if (group != null && group.isCoordinator()) { return this.equals(group.getActiveServer()); }
    return false;
  }

  protected void assertGroupLeader() {
    boolean isGroupLeader = isGroupLeader();
    if (!isGroupLeader) {
      Thread.dumpStack();
    }
    Assert.assertTrue(isGroupLeader);
  }

  public boolean isGroupLeader() {
    IServerGroup group = getServerGroup();
    if (group != null) { return this.equals(group.getActiveServer()); }
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
      if (isGroupLeader()) {
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
    } else if ("jmx.attribute.change".equals(type)) {
      AttributeChangeNotification acn = (AttributeChangeNotification) notification;
      PropertyChangeEvent pce = new PropertyChangeEvent(this, acn.getAttributeName(), acn.getOldValue(),
                                                        acn.getNewValue());
      propertyChangeSupport.firePropertyChange(pce);
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

  protected static final IServer[] EMPTY_SERVER_ARRAY = {};

  public IServer[] getClusterServers() {
    IServer[] result = EMPTY_SERVER_ARRAY;
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    if (theServerInfoBean != null) {
      L2Info[] l2Infos = theServerInfoBean.getL2Info();
      result = new Server[l2Infos.length];
      for (int i = 0; i < l2Infos.length; i++) {
        result[i] = new Server(getClusterModel(), getServerGroup(), l2Infos[i]);
      }
    }
    return result;
  }

  private static final ServerGroup[] EMPTY_SERVER_GROUP_ARRAY = {};

  public ServerGroup[] getClusterServerGroups() {
    ServerGroup[] result = EMPTY_SERVER_GROUP_ARRAY;
    TCServerInfoMBean theServerInfoBean = getServerInfoBean();
    if (theServerInfoBean != null) {
      ServerGroupInfo[] serverGroupInfos = theServerInfoBean.getServerGroupInfo();
      result = new ServerGroup[serverGroupInfos.length];
      for (int i = 0; i < serverGroupInfos.length; i++) {
        result[i] = new ServerGroup(getClusterModel(), serverGroupInfos[i]);
      }
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
    jmxPort = dsoListenPort = dsoGroupPort = null;
  }

  public synchronized void disconnect() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm != null) {
      scm.disconnect();
    }
  }

  public synchronized void splitbrain() {
    ServerConnectionManager scm = getConnectionManager();
    if (scm != null) {
      setConnectError(new RuntimeException("split-brain"));
      scm.setConnected(false);
    }
  }

  private void removeAllClients() {
    DSOClient[] theClients = getClients();
    synchronized (Server.this) {
      clients.clear();
      clientMap.clear();
    }
    for (DSOClient client : theClients) {
      fireClientDisconnected(client);
    }
  }

  private synchronized void reset() {
    if (roots == null) { return; }
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
    if (theServerInfoBean == null) { return "not connected"; }
    byte[] zippedByte = theServerInfoBean.takeCompressedThreadDump(moment);
    if (zippedByte == null) { return MESSAGE_ON_EXCEPTION; }
    ZipInputStream zIn = new ZipInputStream(new ByteArrayInputStream(zippedByte));
    return decompress(zIn);
  }

  public synchronized String takeClusterDump() {
    L2DumperMBean theServerDumperBean = getServerDumperBean();
    if (theServerDumperBean == null) { return "not connected"; }
    theServerDumperBean.doServerDump();
    return "server dump taken";
  }

  public synchronized void addServerLogListener(ServerLogListener listener) {
    listenerList.remove(ServerLogListener.class, listener);
    listenerList.add(ServerLogListener.class, listener);
    testAddLogListener();
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

  private synchronized void testAddLogListener() {
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

  public synchronized void removeServerLogListener(ServerLogListener listener) {
    listenerList.remove(ServerLogListener.class, listener);
    testRemoveLogListener();
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

  public Object safeGetFieldValue(ManagedObjectFacade mof, String fieldName) {
    String className = mof.getClassName();
    try {
      if (!mof.isList() && !mof.isMap() && !mof.isSet() && fieldName.indexOf('.') == -1) {
        fieldName = className + "." + fieldName;
      }
      Object o = mof.getFieldValue(fieldName);
      if (o instanceof ObjectID) {
        return clusterModel.lookupFacade((ObjectID) o, Integer.MAX_VALUE);
      } else {
        return o;
      }
    } catch (Exception e) {
      System.err.println(className + ": " + Arrays.asList(mof.getFields()));
      e.printStackTrace();
      return null;
    }
  }

  public ManagedObjectFacade CDM_translate(ManagedObjectFacade mof, int limit) {
    ManagedObjectFacade mapFacade = (ManagedObjectFacade) safeGetFieldValue(mof, "map");
    if (mapFacade == null) { return mof; }
    mapFacade = (ManagedObjectFacade) safeGetFieldValue(mapFacade, "storeList");
    if (mapFacade == null) { return mof; }
    List<MapEntryFacade> list = new ArrayList<MapEntryFacade>();
    int trueSize = 0;
    boolean haveLimit = false;
    for (String fieldName : mapFacade.getFields()) {
      ManagedObjectFacade field = (ManagedObjectFacade) safeGetFieldValue(mapFacade, fieldName);
      if (field != null) {
        String[] fields = field.getFields();
        if (fields != null && fields.length > 0) {
          trueSize += fields.length;
          if (!haveLimit) {
            for (String field2 : fields) {
              MapEntryFacade mapEntryFacade = (MapEntryFacade) safeGetFieldValue(field, field2);
              if (mapEntryFacade != null) {
                list.add(mapEntryFacade);
                if (list.size() >= limit) {
                  haveLimit = true;
                }
              }
            }
          }
        }
      }
    }
    MapEntryFacade[] mefa = list.toArray(new MapEntryFacade[0]);
    return LogicalManagedObjectFacade.createMapInstance(mof.getObjectId(), mof.getClassName(), mefa, trueSize);
  }

  public ManagedObjectFacade ClusteredStore_translate(ManagedObjectFacade mof, int limit) {
    ManagedObjectFacade mapFacade = (ManagedObjectFacade) safeGetFieldValue(mof, "backend");
    if (mapFacade == null) { return mof; }
    mapFacade = (ManagedObjectFacade) safeGetFieldValue(mapFacade,
                                                        "org.terracotta.cache.impl.DistributedCacheImpl.data");
    if (mapFacade == null) { return mof; }
    List<MapEntryFacade> list = new ArrayList<MapEntryFacade>();
    String[] fields = mapFacade.getFields();
    int trueSize = mapFacade.getTrueObjectSize();
    for (String fieldName : fields) {
      MapEntryFacade mapEntryFacade = (MapEntryFacade) safeGetFieldValue(mapFacade, fieldName);
      if (mapEntryFacade != null) {
        list.add(mapEntryFacade);
        if (list.size() >= limit) {
          break;
        }
      }
    }
    MapEntryFacade[] mefa = list.toArray(new MapEntryFacade[0]);
    return LogicalManagedObjectFacade.createMapInstance(mof.getObjectId(), mof.getClassName(), mefa, trueSize);
  }

  public ManagedObjectFacade CDS_translate(ManagedObjectFacade mof, int limit) {
    ManagedObjectFacade mapFacade = (ManagedObjectFacade) safeGetFieldValue(mof, "map");
    if (mapFacade == null) { return mof; }
    int trueSize = mapFacade.getTrueObjectSize();
    List<Object> list = new ArrayList<Object>();
    for (String fieldName : mapFacade.getFields()) {
      MapEntryFacade field = (MapEntryFacade) safeGetFieldValue(mapFacade, fieldName);
      if (field != null) {
        list.add(field.getKey());
      }
    }
    Object[] mofa = list.toArray(new Object[0]);
    return LogicalManagedObjectFacade.createSetInstance(mof.getObjectId(), mof.getClassName(), mofa, trueSize);
  }

  private static boolean showRaw = false;

  public ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException {
    DSOMBean theDsoBean = getDSOBean();
    ManagedObjectFacade mof = theDsoBean != null ? theDsoBean.lookupFacade(objectID, limit) : null;
    if (!showRaw) {
      if (mof != null) {
        if ("org.terracotta.collections.ConcurrentDistributedMap".equals(mof.getClassName())) {
          mof = CDM_translate(mof, limit);
        } else if ("org.terracotta.modules.ehcache.store.ClusteredStore".equals(mof.getClassName())) {
          mof = ClusteredStore_translate(mof, limit);
        } else if ("org.terracotta.collections.ConcurrentDistributedSet".equals(mof.getClassName())) {
          mof = CDS_translate(mof, limit);
        }
      }
    }
    return mof;
  }

  private static final DSOClassInfo[] EMPTY_CLASSINFO_ARRAY = {};

  public synchronized DSOClassInfo[] getClassInfo() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getClassInfo() : EMPTY_CLASSINFO_ARRAY;
  }

  private static final GCStats[] EMPTY_GCSTATS_ARRAY = {};

  public synchronized GCStats[] getGCStats() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getGarbageCollectorStats() : EMPTY_GCSTATS_ARRAY;
  }

  public synchronized List<TerracottaOperatorEvent> getOperatorEvents() {
    DSOMBean theDsoBean = getDSOBean();
    return theDsoBean != null ? theDsoBean.getOperatorEvents() : new ArrayList<TerracottaOperatorEvent>();
  }

  public void addDGCListener(DGCListener listener) {
    listenerList.add(DGCListener.class, listener);
  }

  public void addTerracottaOperatorEventsListener(TerracottaOperatorEventsListener listener) {
    listenerList.add(TerracottaOperatorEventsListener.class, listener);
  }

  public void removeDGCListener(DGCListener listener) {
    listenerList.remove(DGCListener.class, listener);
  }

  public void removeTerracottaOperatorEventsListener(TerracottaOperatorEventsListener listener) {
    listenerList.remove(TerracottaOperatorEventsListener.class, listener);
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

  public synchronized void initLockProfilerBean() {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        lockProfilerBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, L2MBeanNames.LOCK_STATISTICS,
                                                                    LockStatisticsMonitorMBean.class, true);
        cc.addNotificationListener(L2MBeanNames.LOCK_STATISTICS, new LockStatsNotificationListener());
        lockProfilingSupported = true;
      } catch (Exception e) {
        lockProfilingSupported = false;
      }
    }
  }

  public synchronized LockStatisticsMonitorMBean getLockProfilerBean() {
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
        boolean oldLockStatsEnabled = lockProfilingEnabled != null ? lockProfilingEnabled.booleanValue() : false;
        lockProfilingEnabled = null;
        firePropertyChange(PROP_LOCK_STATS_ENABLED, oldLockStatsEnabled, isLockProfilingEnabled());
      }
    }
  }

  public synchronized boolean isLockProfilingSupported() {
    return lockProfilingSupported;
  }

  public int getLockProfilerTraceDepth() {
    if (lockProfilerTraceDepth != -1) { return lockProfilerTraceDepth; }
    LockStatisticsMonitorMBean theLockProfilerBean = getLockProfilerBean();
    if (theLockProfilerBean != null) { return lockProfilerTraceDepth = theLockProfilerBean.getTraceDepth(); }
    return -1;
  }

  public void setLockProfilerTraceDepth(int traceDepth) {
    LockStatisticsMonitorMBean theLockProfilerBean = getLockProfilerBean();
    if (theLockProfilerBean != null && traceDepth != lockProfilerTraceDepth) {
      theLockProfilerBean.setLockStatisticsConfig(traceDepth, 1);
    }
  }

  public boolean isLockProfilingEnabled() {
    if (lockProfilingEnabled != null) { return lockProfilingEnabled.booleanValue(); }
    LockStatisticsMonitorMBean theLockProfilerBean = getLockProfilerBean();
    if (theLockProfilerBean != null) {
      lockProfilingEnabled = Boolean.valueOf(theLockProfilerBean.isLockStatisticsEnabled());
      return lockProfilingEnabled;
    }
    return false;
  }

  public void setLockProfilingEnabled(boolean lockStatsEnabled) {
    LockStatisticsMonitorMBean theLockProfilerBean = getLockProfilerBean();
    if (theLockProfilerBean != null) {
      theLockProfilerBean.setLockStatisticsEnabled(lockStatsEnabled);
    }
  }

  public Collection<LockSpec> getLockSpecs() {
    LockStatisticsMonitorMBean theLockProfilerBean = getLockProfilerBean();
    if (theLockProfilerBean != null) { return theLockProfilerBean.getLockSpecs(); }
    return Collections.emptySet();
  }

  private synchronized void initClusterStatsBean() {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      try {
        clusterStatsBean = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, StatisticsMBeanNames.STATISTICS_GATHERER,
                                                                    StatisticsLocalGathererMBean.class, true);
        if ((clusterStatsSupported = clusterStatsBean.isActive()) == true) {
          cc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATHERER, new ClusterStatsNotificationListener());
        }
      } catch (Exception e) {
        clusterStatsSupported = false;
      }
    }
  }

  public synchronized StatisticsLocalGathererMBean getClusterStatsBean() {
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

  public synchronized boolean isClusterStatsSupported() {
    return clusterStatsSupported;
  }

  /*
   * Listener will receive connected message.
   */
  public void startupClusterStats() {
    StatisticsLocalGathererMBean theClusterStatsBean = getClusterStatsBean();
    if (theClusterStatsBean != null) {
      theClusterStatsBean.startup();
    } else {
      throw new RuntimeException("startupClusterStats: ClusterStatsBean not initialized.");
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
      theClusterStatsBean.setSessionParam(StatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL,
                                          Long.valueOf(samplePeriodMillis));
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
    return getName();
  }

  @Override
  public synchronized void tearDown() {
    if (serverGroup != null) {
      serverGroup.removePropertyChangeListener(this);
    }

    super.tearDown();

    clients.clear();
    clients = null;
    clientMap.clear();
    clientMap = null;
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
    serverGroup = null;
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
    sb.append(" ready=");
    sb.append(isReady());
    sb.append(" isConnected=");
    sb.append(isConnected());
    sb.append(" autoConnect=");
    sb.append(isAutoConnect());
    return sb.toString();
  }

  public void setAttribute(ObjectName on, String attrName, Object attrValue) throws Exception {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) {
      cc.setAttribute(on, attrName, attrValue);
    }
  }

  public void setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue) throws Exception {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean != null && isReady()) {
      theDsoBean.setAttribute(onSet, attrName, attrValue);
    }
  }

  public Object getAttribute(ObjectName on, String attrName) throws Exception {
    ConnectionContext cc = getConnectionContext();
    if (cc != null) { return cc.getAttribute(on, attrName); }
    return null;
  }

  public Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                              TimeUnit unit) {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean != null && isReady()) { return theDsoBean.getAttributeMap(attributeMap, timeout, unit); }
    return Collections.emptyMap();
  }

  public Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap) {
    return getAttributeMap(attributeMap, Long.MAX_VALUE, TimeUnit.SECONDS);
  }

  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit) {
    return invoke(onSet, operation, timeout, unit, new Object[0], new String[0]);
  }

  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation) {
    return invoke(onSet, operation, Long.MAX_VALUE, TimeUnit.SECONDS, new Object[0], new String[0]);
  }

  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit,
                                        Object[] args, String[] sigs) {
    DSOMBean theDsoBean = getDSOBean();
    if (theDsoBean != null && isReady()) { return theDsoBean.invoke(onSet, operation, timeout, unit, args, sigs); }
    return Collections.emptyMap();
  }

  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, Object[] args, String[] sigs) {
    return invoke(onSet, operation, Long.MAX_VALUE, TimeUnit.SECONDS, args, sigs);
  }

  public void gc() {
    getServerInfoBean().gc();
  }

  public boolean isVerboseGC() {
    return getServerInfoBean().isVerboseGC();
  }

  public void setVerboseGC(boolean verboseGC) {
    getServerInfoBean().setVerboseGC(verboseGC);
  }

}
