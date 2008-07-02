/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import org.apache.commons.httpclient.auth.AuthScope;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.ServerHelper;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.dso.ClientsHelper;
import com.tc.admin.dso.DSOHelper;
import com.tc.config.schema.L2Info;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.stats.DSOClassInfo;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.DSOMBean;
import com.tc.stats.DSORootMBean;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;
import com.tc.util.ProductInfo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.naming.CommunicationException;
import javax.naming.ServiceUnavailableException;
import javax.swing.event.EventListenerList;

public class Server implements IServer, NotificationListener, ManagedObjectFacadeProvider {
  protected ServerConnectionManager       m_connectManager;
  protected boolean                       m_connected;
  protected boolean                       m_ready;
  protected List<DSOClient>               m_clients;
  private ClientChangeListener            m_clientChangeListener;
  protected PropertyChangeSupport         m_propertyChangeSupport;
  protected EventListenerList             m_listenerList;
  protected List<DSOClient>               m_pendingClients;
  protected Exception                     m_connectException;
  protected TCServerInfoMBean             m_serverInfoBean;
  protected DSOMBean                      m_dsoBean;
  protected ObjectManagementMonitorMBean  m_objectManagementMonitorBean;
  protected ServerDBBackupMBean           m_serverDBBackupBean;
  protected ProductInfo                   m_productInfo;
  protected List<IBasicObject>            m_roots;
  protected Map<ObjectName, IBasicObject> m_rootMap;
  protected LogListener                   m_logListener;

  private static final ObjectName         SERVER_INFO_OBJECT_NAME = L2MBeanNames.TC_SERVER_INFO;
  private static final ObjectName         DSO_BEAN_OBJECT_NAME    = L2MBeanNames.DSO;

  public Server() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  public Server(final String host, final int jmxPort, final boolean autoConnect) {
    init();
    ConnectionManagerListener cml = new ConnectionManagerListener();
    m_connectManager = new ServerConnectionManager(host, jmxPort, autoConnect, cml);
    if (autoConnect) {
      refreshCachedCredentials();
    }
  }

  public Server(final L2Info l2Info) {
    init();
    ConnectionManagerListener cml = new ConnectionManagerListener();
    m_connectManager = new ServerConnectionManager(l2Info, true, cml);
  }

  public String getName() {
    return getConnectionManager().getName();
  }

  private void init() {
    m_propertyChangeSupport = new PropertyChangeSupport(this);
    m_listenerList = new EventListenerList();
    m_pendingClients = new ArrayList<DSOClient>();
    m_ready = false;
  }

  public L2Info getL2Info() {
    return getConnectionManager().getL2Info();
  }

  public String getConnectionStatusString() {
    return getConnectionManager().getStatusString();
  }

  private class ConnectionManagerListener implements ConnectionListener {
    public void handleConnection() {
      setConnected(m_connectManager != null && m_connectManager.isConnected());
    }

    public void handleException() {
      if (m_connectManager != null) {
        setConnectError(m_connectManager.getConnectionException());
      }
    }
  }

  private void connectionEstablished() {
    try {
      ObjectName mbsd = getConnectionContext().queryName("JMImplementation:type=MBeanServerDelegate");
      getConnectionContext().addNotificationListener(mbsd, this);
      boolean isDsoBeanRegistered = isMBeanRegistered(DSO_BEAN_OBJECT_NAME);
      if (isDsoBeanRegistered) {
        getConnectionContext().addNotificationListener(DSO_BEAN_OBJECT_NAME, this);
      }
      boolean isServerInfoBeanRegistered = isMBeanRegistered(SERVER_INFO_OBJECT_NAME);
      boolean isDBBackupBeanRegistered = isMBeanRegistered(L2MBeanNames.SERVER_DB_BACKUP);
      if(isDBBackupBeanRegistered) {
        getServerDBBackupBean();
      }
      if (!isReady() && isDsoBeanRegistered && isServerInfoBeanRegistered && isDBBackupBeanRegistered) {
        setReady(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setConnected(boolean connected) {
    boolean oldConnected = m_connected;
    m_connected = connected;
    firePropertyChange(PROP_CONNECTED, !connected, connected);
    if (oldConnected == false) {
      connectionEstablished();
    }
    if (!connected) {
      handleDisconnect();
    }
  }

  private void removeAllClients() {
    if (m_clients == null) return;
    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      fireClientDisconnected(client);
    }
    m_clients.clear();
    m_clients = null;
  }

  public boolean isConnected() {
    return m_connected;
  }

  private void setConnectError(Exception e) {
    Exception oldConnectError = m_connectException;
    m_connectException = e;
    firePropertyChange(PROP_CONNECT_ERROR, oldConnectError, e);
  }

  public boolean hasConnectError() {
    return m_connectException != null;
  }

  public Exception getConnectError() {
    return m_connectException;
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
    AdminClientContext acc = AdminClient.getContext();
    String msg = null;
    Throwable cause = ExceptionHelper.getRootCause(e);

    if (cause instanceof ServiceUnavailableException) {
      String tmpl = acc.getMessage("service.unavailable");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else if (cause instanceof ConnectException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else if (cause instanceof UnknownHostException || cause instanceof java.rmi.UnknownHostException) {
      String tmpl = acc.getMessage("unknown.host");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cc.host };
      msg = form.format(args);
    } else if (cause instanceof CommunicationException) {
      String tmpl = acc.getMessage("cannot.connect.to");
      MessageFormat form = new MessageFormat(tmpl);
      Object[] args = new Object[] { cc };
      msg = form.format(args);
    } else {
      msg = cause != null ? cause.getMessage() : e.getMessage();
    }

    return "<html>" + msg + "</html>";
  }

  ServerConnectionManager getConnectionManager() {
    return m_connectManager;
  }

  public String[] getConnectionCredentials() {
    return getConnectionManager().getCredentials();
  }

  public Map<String, Object> getConnectionEnvironment() {
    return getConnectionManager().getConnectionEnvironment();
  }

  public ConnectionContext getConnectionContext() {
    return getConnectionManager().getConnectionContext();
  }

  public void setHost(String host) {
    getConnectionManager().setHostname(host);
  }

  public String getHost() {
    return getConnectionManager().getHostname();
  }

  public void setPort(int port) {
    getConnectionManager().setJMXPortNumber(port);
  }

  public int getPort() {
    return getConnectionManager().getJMXPortNumber();
  }

  public Integer getDSOListenPort() {
    return getServerInfoBean().getDSOListenPort();
  }

  public String getStatsExportServletURI() {
    Integer dsoPort = getDSOListenPort();
    Object[] args = new Object[] { getHost(), dsoPort.toString() };
    return MessageFormat.format("http://{0}:{1}/statistics-gatherer/retrieveStatistics", args);
  }

  public String getStatsExportServletURI(String sessionId) {
    Integer dsoPort = getDSOListenPort();
    Object[] args = new Object[] { getHost(), dsoPort.toString(), sessionId };
    return MessageFormat.format("http://{0}:{1}/stats-export?session={2}", args);
  }

  AuthScope getAuthScope() throws Exception {
    return new AuthScope(getHost(), getDSOListenPort());
  }

  // boolean isConnected() {
  // return m_connectManager != null && m_connectManager.isConnected();
  // }

  public boolean isStarted() {
    return getConnectionManager().isStarted();
  }

  public boolean isActive() {
    return getConnectionManager().isActive();
  }

  public boolean isPassiveUninitialized() {
    return getConnectionManager().isPassiveUninitialized();
  }

  public boolean isPassiveStandby() {
    return getConnectionManager().isPassiveStandby();
  }

  public void doShutdown() {
    getServerInfoBean().shutdown();
  }

  public boolean isAutoConnect() {
    return getConnectionManager().isAutoConnect();
  }

  public void setAutoConnect(boolean autoConnect) {
    if (autoConnect) {
      refreshCachedCredentials();
    }
    getConnectionManager().setAutoConnect(autoConnect);
  }

  public void refreshCachedCredentials() {
    String[] creds = ServerConnectionManager.getCachedCredentials(getConnectionManager());
    if (creds != null) {
      setConnectionCredentials(creds);
    }
  }

  public void setConnectionCredentials(String[] creds) {
    getConnectionManager().setCredentials(creds);
  }

  public JMXConnector getJMXConnector() {
    return getConnectionManager().getJmxConnector();
  }

  public void setJMXConnector(JMXConnector jmxc) throws IOException {
    getConnectionManager().setJMXConnector(jmxc);
  }

  public TCServerInfoMBean getServerInfoBean() {
    if (m_serverInfoBean != null) return m_serverInfoBean;
    m_serverInfoBean = ServerHelper.getHelper().getServerInfoBean(getConnectionContext());
    return m_serverInfoBean;
  }

  public DSOMBean getDSOBean() {
    if (m_dsoBean != null) return m_dsoBean;
    m_dsoBean = DSOHelper.getHelper().getDSOBean(getConnectionContext());
    return m_dsoBean;
  }

  public ObjectManagementMonitorMBean getObjectManagementMonitorBean() {
    if (m_objectManagementMonitorBean != null) return m_objectManagementMonitorBean;
    m_objectManagementMonitorBean = DSOHelper.getHelper().getObjectManagementMonitorBean(getConnectionContext());
    return m_objectManagementMonitorBean;
  }

  public ProductInfo getProductInfo() {
    if (m_productInfo != null) return m_productInfo;

    TCServerInfoMBean serverInfo = getServerInfoBean();
    String version = serverInfo.getVersion();
    String buildID = serverInfo.getBuildID();
    String license = serverInfo.getDescriptionOfCapabilities();
    String copyright = serverInfo.getCopyright();

    return m_productInfo = new ProductInfo(version, buildID, license, copyright);
  }

  public String getProductVersion() {
    return getProductInfo().version();
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
    return getServerInfoBean().getStartTime();
  }

  public long getActivateTime() {
    return getServerInfoBean().getActivateTime();
  }

  public CountStatistic getTransactionRate() {
    return getDSOBean().getTransactionRate();
  }

  public StatisticData[] getCpuUsage() {
    return getServerInfoBean().getCpuUsage();
  }

  public String[] getCpuStatNames() {
    return getServerInfoBean().getCpuStatNames();
  }

  public Map getServerStatistics() {
    return getServerInfoBean().getStatistics();
  }

  public Statistic[] getDSOStatistics(String[] names) {
    return getDSOBean().getStatistics(names);
  }

  public Map getPrimaryStatistics() {
    Map result = getServerStatistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }

  public Map<IClient, CountStatistic> getAllPendingTransactionsCount() {
    Map<ObjectName, CountStatistic> map = getDSOBean().getAllPendingTransactionsCount();
    Map<IClient, CountStatistic> result = new HashMap<IClient, CountStatistic>();
    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }

  public Map<IClient, Integer> getClientLiveObjectCount() {
    Map<ObjectName, Integer> map = getDSOBean().getClientLiveObjectCount();
    Map<IClient, Integer> result = new HashMap<IClient, Integer>();
    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }
  
  public void addClientConnectionListener(ClientConnectionListener listener) {
    m_listenerList.add(ClientConnectionListener.class, listener);
  }

  public void removeClientConnectionListener(ClientConnectionListener listener) {
    m_listenerList.remove(ClientConnectionListener.class, listener);
  }

  protected void fireClientConnected(DSOClient client) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ClientConnectionListener.class) {
        ((ClientConnectionListener) listeners[i + 1]).clientConnected(client);
      }
    }
  }

  protected void fireClientDisconnected(DSOClient client) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ClientConnectionListener.class) {
        ((ClientConnectionListener) listeners[i + 1]).clientDisconnected(client);
      }
    }
  }

  protected void setReady(boolean ready) {
    boolean oldReady = m_ready;
    m_ready = ready;
    firePropertyChange(PROP_READY, oldReady, ready);
  }

  public boolean isReady() {
    return m_ready;
  }

  private void beanRegistered(ObjectName beanName) {
    if (beanName.equals(DSO_BEAN_OBJECT_NAME)) {
      try {
        getConnectionContext().addNotificationListener(DSO_BEAN_OBJECT_NAME, this);
        if (isMBeanRegistered(SERVER_INFO_OBJECT_NAME)) {
          setReady(true);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (beanName.equals(SERVER_INFO_OBJECT_NAME)) {
      if (isMBeanRegistered(DSO_BEAN_OBJECT_NAME)) {
        setReady(true);
      }
    }
  }

  private boolean isMBeanRegistered(ObjectName beanName) {
    try {
      return getConnectionContext().isRegistered(beanName);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean haveClient(ObjectName objectName) {
    if (m_clients == null) return false;
    for (DSOClient client : m_clients) {
      if (client.getObjectName().equals(objectName)) { return true; }
    }
    return false;
  }

  private class ClientChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      final String prop = evt.getPropertyName();
      if (DSOClientMBean.TUNNELED_BEANS_REGISTERED.equals(prop)) {
        DSOClient client = (DSOClient) evt.getSource();
        m_clients.add(client);
        fireClientConnected(client);
        client.removePropertyChangeListener(this);
      }
    }
  }

  private ClientChangeListener getClientChangeListener() {
    if (m_clientChangeListener == null) {
      m_clientChangeListener = new ClientChangeListener();
    }
    return m_clientChangeListener;
  }

  private void clientNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type)) {
      ObjectName clientObjectName = (ObjectName) notification.getSource();
      if (haveClient(clientObjectName)) return;
      DSOClient client = new DSOClient(getConnectionContext(), clientObjectName);
      if (m_clients == null) m_clients = new ArrayList<DSOClient>();
      if (client.isTunneledBeansRegistered()) {
        m_clients.add(client);
        fireClientConnected(client);
      } else {
        client.addPropertyChangeListener(getClientChangeListener());
        m_pendingClients.add(client);
      }
    } else if (DSOMBean.CLIENT_DETACHED.equals(type)) {
      ObjectName beanName = (ObjectName) notification.getSource();
      Iterator<DSOClient> iter = m_clients.iterator();
      while (iter.hasNext()) {
        DSOClient client = iter.next();
        if (client.getObjectName().equals(beanName)) {
          fireClientDisconnected(client);
          iter.remove();
          return;
        }
      }
    }
  }

  public void addRootCreationListener(RootCreationListener listener) {
    m_listenerList.add(RootCreationListener.class, listener);
  }

  public void removeRootCreationListener(RootCreationListener listener) {
    m_listenerList.remove(RootCreationListener.class, listener);
  }

  protected void fireRootCreated(IBasicObject root) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == RootCreationListener.class) {
        ((RootCreationListener) listeners[i + 1]).rootCreated(root);
      }
    }
  }

  private boolean haveRoot(ObjectName objectName) {
    return m_rootMap.get(objectName) != null;
  }

  private void rootAdded(Notification notification, Object handback) {
    ObjectName objectName = (ObjectName) notification.getSource();
    if (haveRoot(objectName)) return;
    IBasicObject root = addRoot(objectName);
    fireRootCreated(root);
  }

  private ManagedObjectFacade safeLookupFacade(DSORootMBean rootBean) {
    try {
      return rootBean.lookupFacade(ConnectionContext.DSO_SMALL_BATCH_SIZE);
    } catch (NoSuchObjectException nsoe) {
      return null;
    }
  }

  private IBasicObject addRoot(ObjectName rootBeanName) {
    ConnectionContext cc = getConnectionContext();
    DSORootMBean rootBean = (DSORootMBean) MBeanServerInvocationProxy.newProxyInstance(cc.mbsc, rootBeanName,
                                                                                       DSORootMBean.class, false);
    String fieldName = rootBean.getRootName();
    ManagedObjectFacade facade = safeLookupFacade(rootBean);
    if (facade != null) {
      String type = facade.getClassName();
      IBasicObject root = new BasicTcObject(this, fieldName, facade, type, null);
      m_rootMap.put(rootBeanName, root);
      m_roots.add(root);
      return root;
    }
    return null;
  }

  public IBasicObject[] getRoots() {
    if (m_roots != null) { return m_roots.toArray(new IBasicObject[0]); }

    m_roots = new ArrayList<IBasicObject>();
    m_rootMap = new HashMap<ObjectName, IBasicObject>();
    ObjectName[] rootNames = getDSOBean().getRoots();
    if (rootNames != null) {
      for (ObjectName rootBeanName : rootNames) {
        addRoot(rootBeanName);
      }
    }

    return m_roots.toArray(new IBasicObject[0]);
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        beanRegistered(mbsn.getMBeanName());
      }
    } else if (DSOMBean.CLIENT_ATTACHED.equals(type) || DSOMBean.CLIENT_DETACHED.equals(type)) {
      clientNotification(notification, handback);
    } else if (DSOMBean.ROOT_ADDED.equals(type)) {
      rootAdded(notification, handback);
    } else if (DSOMBean.GC_COMPLETED.equals(type)) {
      fireGarbageCollected((GCStats) notification.getSource());
    }
  }

  public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    m_propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    m_propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    m_propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
  }

  public String getCanonicalHostName() {
    return getConnectionManager().safeGetHostName();
  }

  public String getHostAddress() {
    return getConnectionManager().safeGetHostAddress();
  }

  public StatisticsLocalGathererMBean getStatisticsGathererMBean() {
    ConnectionContext cc = getConnectionContext();
    return (StatisticsLocalGathererMBean) MBeanServerInvocationProxy
        .newProxyInstance(cc.mbsc, StatisticsMBeanNames.STATISTICS_GATHERER, StatisticsLocalGathererMBean.class, true);
  }

  public Server[] getClusterServers() {
    TCServerInfoMBean serverInfo = getServerInfoBean();
    L2Info[] l2Infos = serverInfo.getL2Info();
    Server[] result = new Server[l2Infos.length];
    for (int i = 0; i < l2Infos.length; i++) {
      result[i] = new Server(l2Infos[i]);
    }
    return result;
  }

  public DSOClient[] getClients() {
    if (m_clients != null) { return m_clients.toArray(new DSOClient[0]); }

    m_clients = new ArrayList<DSOClient>();
    for (DSOClient client : ClientsHelper.getHelper().getClients(getConnectionContext())) {
      m_clients.add(client);
    }

    if(m_clients.size() == 0) {
      System.err.println(getConnectionStatusString());
    }
    
    return m_clients.toArray(new DSOClient[0]);
  }

  protected void resetBeanProxies() {
    m_serverInfoBean = null;
    m_dsoBean = null;
    m_objectManagementMonitorBean = null;
    m_serverDBBackupBean  = null;
    m_productInfo = null;
  }

  public void disconnect() {
    getConnectionManager().disconnect();
  }

  void handleDisconnect() {
    setReady(false);
    removeAllClients();
    resetBeanProxies();
  }

  public String takeThreadDump(long moment) {
    return getServerInfoBean().takeThreadDump(moment);
  }

  private void testAddLogListener() {
    if (m_logListener == null && m_listenerList.getListenerCount(ServerLogListener.class) > 0) {
      try {
        ConnectionContext cc = getConnectionContext();
        cc.addNotificationListener(L2MBeanNames.LOGGER, m_logListener = new LogListener());
      } catch (Exception e) {
        /* connection has probably dropped */
      }
    }
  }

  public void addServerLogListener(ServerLogListener listener) {
    testAddLogListener();
    m_listenerList.add(ServerLogListener.class, listener);
  }

  private void testRemoveLogListener() {
    if (m_logListener != null && m_listenerList.getListenerCount(ServerLogListener.class) == 0) {
      try {
        ConnectionContext cc = getConnectionContext();
        cc.removeNotificationListener(L2MBeanNames.LOGGER, m_logListener);
        m_logListener = null;
      } catch (Exception e) {
        /* connection has probably dropped */
      }
    }
  }

  public void removeServerLogListener(ServerLogListener listener) {
    m_listenerList.remove(ServerLogListener.class, listener);
    testRemoveLogListener();
  }

  private void fireMessageLogged(String logMsg) {
    Object[] listeners = m_listenerList.getListenerList();
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

  public ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException {
    return getDSOBean().lookupFacade(objectID, limit);
  }

  public DSOClassInfo[] getClassInfo() {
    return getDSOBean().getClassInfo();
  }

  public GCStats[] getGCStats() {
    return getDSOBean().getGarbageCollectorStats();
  }

  public void addDGCListener(DGCListener listener) {
    m_listenerList.add(DGCListener.class, listener);
  }

  public void removeDGCListener(DGCListener listener) {
    m_listenerList.remove(DGCListener.class, listener);
  }

  private void fireGarbageCollected(GCStats gcStats) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DGCListener.class) {
        ((DGCListener) listeners[i + 1]).garbageCollected(gcStats);
      }
    }
  }

  public void runGC() {
    getObjectManagementMonitorBean().runGC();
  }

  public int getLiveObjectCount() {
    return getDSOBean().getLiveObjectCount();
  }

  public boolean isResident(ObjectID oid) {
    return true;
  }

  public ServerDBBackupMBean getServerDBBackupBean() {
    if (m_serverDBBackupBean != null) return m_serverDBBackupBean;
    ConnectionContext cc = getConnectionContext();
    m_serverDBBackupBean = (ServerDBBackupMBean) MBeanServerInvocationProxy
        .newProxyInstance(cc.mbsc, L2MBeanNames.SERVER_DB_BACKUP, ServerDBBackupMBean.class, true);
    if (m_serverDBBackupBean != null && m_serverDBBackupBean.isBackupEnabled()) {
      try {
        cc.addNotificationListener(L2MBeanNames.SERVER_DB_BACKUP, new ServerDBBackupListener());
      } catch (Exception e) {
        /**/
      }
    }
    return m_serverDBBackupBean;
  }

  private class ServerDBBackupListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      String type = notification.getType();
      
      if(ServerDBBackupMBean.BACKUP_STARTED.equals(type)) {
        fireDBBackupStarted();
      } else if(ServerDBBackupMBean.BACKUP_COMPLETED.equals(type)) {
        fireDBBackupCompleted();
      } else if(ServerDBBackupMBean.PERCENTAGE_COPIED.equals(type)) {
        String message = notification.getMessage();
        String value = message.substring(0, message.indexOf(' '));
        int percentCopied = Integer.parseInt(value);
        fireDBBackupProgress(percentCopied);
      } else if(ServerDBBackupMBean.BACKUP_FAILED.equals(type)) {
        fireDBBackupFailed(notification.getMessage());
      }
    }
  }

  public void addDBBackupListener(DBBackupListener listener) {
    m_listenerList.add(DBBackupListener.class, listener);
  }
  
  public void removeDBBackupListener(DBBackupListener listener) {
    m_listenerList.remove(DBBackupListener.class, listener);
  }

  private void fireDBBackupStarted() {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupStarted();
      }
    }
  }
  
  private void fireDBBackupCompleted() {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupCompleted();
      }
    }
  }
  
  private void fireDBBackupFailed(String message) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupFailed(message);
      }
    }
  }
  
  private void fireDBBackupProgress(int percentCopied) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBBackupListener.class) {
        ((DBBackupListener) listeners[i + 1]).backupProgress(percentCopied);
      }
    }
  }
  
  public void backupDB() throws IOException {
    getServerDBBackupBean().runBackUp();
  }

  public void backupDB(String path) throws IOException {
    getServerDBBackupBean().runBackUp(path);
  }

  public boolean isDBBackupRunning() {
    return getServerDBBackupBean().isBackUpRunning();
  }

  public String getDefaultDBBackupPath() {
    return getServerDBBackupBean().getDefaultPathForBackup();
  }

  public boolean isDBBackupEnabled() {
    return getServerDBBackupBean().isBackupEnabled();
  }

  public String getDBHome() {
    return getServerDBBackupBean().getDbHome();
  }

  public String toString() {
    return m_connectManager != null ? m_connectManager.toString() : "tornDown";
  }

  public void tearDown() {
    m_clients.clear();
    m_clients = null;
    m_roots.clear();
    m_roots = null;
    m_pendingClients.clear();
    m_pendingClients = null;
    m_clientChangeListener = null;
    m_propertyChangeSupport = null;
    m_connectManager.tearDown();
    m_connectManager = null;
    m_connectException = null;
    m_serverInfoBean = null;
    m_dsoBean = null;
    m_objectManagementMonitorBean = null;
    m_productInfo = null;
  }
}
