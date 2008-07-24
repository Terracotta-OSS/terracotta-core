/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.object.ObjectID;
import com.tc.statistics.StatisticData;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient implements IClient, NotificationListener {
  private ConnectionContext           cc;
  private ObjectName                  beanName;
  private DSOClientMBean              delegate;
  private long                        channelID;
  private String                      remoteAddress;
  private String                      host;
  private Integer                     port;
  protected PropertyChangeSupport     changeHelper;

  private boolean                     ready;
  private boolean                     isListeningForTunneledBeans;
  private L1InfoMBean                 l1InfoBean;
  private InstrumentationLoggingMBean instrumentationLoggingBean;
  private RuntimeLoggingMBean         runtimeLoggingBean;
  private RuntimeOutputOptionsMBean   runtimeOutputOptionsBean;

  public DSOClient(ConnectionContext cc, ObjectName beanName) {
    this.cc = cc;
    this.beanName = beanName;
    this.delegate = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, beanName, DSOClientMBean.class, true);
    channelID = delegate.getChannelID().toLong();
    remoteAddress = delegate.getRemoteAddress();
    changeHelper = new PropertyChangeSupport(this);

    testSetupTunneledBeans();
  }

  private void testSetupTunneledBeans() {
    if (delegate.isTunneledBeansRegistered()) {
      setupTunneledBeans();
    } else {
      startListeningForTunneledBeans();
    }
  }

  private void setupTunneledBeans() {
    l1InfoBean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, delegate.getL1InfoBeanName(),
                                                                             L1InfoMBean.class, false);
    instrumentationLoggingBean = (InstrumentationLoggingMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, delegate.getInstrumentationLoggingBeanName(), InstrumentationLoggingMBean.class,
                          true);
    addMBeanNotificationListener(delegate.getInstrumentationLoggingBeanName(), this, "InstrumentationLoggingMBean");

    runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, delegate
        .getRuntimeLoggingBeanName(), RuntimeLoggingMBean.class, true);
    addMBeanNotificationListener(delegate.getRuntimeLoggingBeanName(), this, "RuntimeLoggingMBean");

    runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, delegate.getRuntimeOutputOptionsBeanName(), RuntimeOutputOptionsMBean.class, true);
    addMBeanNotificationListener(delegate.getRuntimeOutputOptionsBeanName(), this, "RuntimeOutputOptionsMBean");

    fireTunneledBeansRegistered();
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans) return;
    addMBeanNotificationListener(beanName, this, "DSOClientMBean");
    isListeningForTunneledBeans = true;
  }

  private void addMBeanNotificationListener(ObjectName objectName, NotificationListener listener, String beanType) {
    try {
      cc.addNotificationListener(objectName, listener);
    } catch (Exception e) {
      throw new RuntimeException("Adding listener to " + beanType, e);
    }
  }

  private void stopListeningForTunneledBeans() {
    if (!isListeningForTunneledBeans) return;
    try {
      cc.removeNotificationListener(beanName, this);
    } catch (Exception e) {
      throw new RuntimeException("Removing listener from DSOClientMBean", e);
    }
    isListeningForTunneledBeans = false;
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (DSOClientMBean.TUNNELED_BEANS_REGISTERED.equals(type)) {
      setupTunneledBeans();
      stopListeningForTunneledBeans();
    } else if (type.startsWith("tc.logging.")) {
      Boolean newValue = Boolean.valueOf(notification.getMessage());
      Boolean oldValue = Boolean.valueOf(!newValue.booleanValue());
      PropertyChangeEvent pce = new PropertyChangeEvent(this, type, newValue, oldValue);
      changeHelper.firePropertyChange(pce);
    }
  }

  private void fireTunneledBeansRegistered() {
    PropertyChangeEvent pce = new PropertyChangeEvent(this, DSOClientMBean.TUNNELED_BEANS_REGISTERED, null, null);
    changeHelper.firePropertyChange(pce);
    setReady(true);
  }

  private void setReady(boolean ready) {
    boolean oldValue;
    
    synchronized (this) {
      oldValue = this.ready;
      this.ready = ready;
    }

    changeHelper.firePropertyChange(PROP_READY, oldValue, ready);
  }

  public synchronized boolean isReady() {
    return ready;
  }

  public ObjectName getObjectName() {
    return beanName;
  }

  public boolean isTunneledBeansRegistered() {
    return delegate.isTunneledBeansRegistered();
  }

  public long getChannelID() {
    return channelID;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public String getHost() {
    if (host == null) {
      host = "unknown";

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(':') != -1) {
        host = addr.substring(0, addr.lastIndexOf(':'));
      }
    }

    return host;
  }

  public int getPort() {
    if (port == null) {
      port = Integer.valueOf(-1);

      String addr = getRemoteAddress();
      if (addr != null && addr.indexOf(":") != -1) {
        try {
          port = new Integer(addr.substring(addr.lastIndexOf(':') + 1));
        } catch (Exception e) {/**/
        }
      }
    }

    return port.intValue();
  }

  public void refresh() {
    try {
      cc.invoke(beanName, "refresh", new Object[] {}, new String[] {});
      changeHelper.firePropertyChange(new PropertyChangeEvent(this, null, null, null));
    } catch (Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public String toString() {
    return getRemoteAddress();
  }

  public Statistic[] getDSOStatistics(String[] names) {
    return delegate.getStatistics(names);
  }

  public void addNotificationListener(NotificationListener listener) throws Exception {
    cc.addNotificationListener(beanName, listener);
  }

  public void addNotificationListener(ObjectName on, NotificationListener listener) throws Exception {
    cc.addNotificationListener(on, listener);
  }

  public ObjectName getL1InfoObjectName() {
    return delegate.getL1InfoBeanName();
  }

  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  public String[] getCpuStatNames() {
    return getL1InfoBean().getCpuStatNames();
  }

  public StatisticData[] getCpuUsage() {
    return getL1InfoBean().getCpuUsage();
  }

  public CountStatistic getTransactionRate() {
    return delegate.getTransactionRate();
  }

  public ObjectName getInstrumentationLoggingObjectName() {
    return delegate.getInstrumentationLoggingBeanName();
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingBean() {
    return instrumentationLoggingBean;
  }

  public ObjectName getRuntimeLoggingObjectName() {
    return delegate.getRuntimeLoggingBeanName();
  }

  public RuntimeLoggingMBean getRuntimeLoggingBean() {
    return runtimeLoggingBean;
  }

  public ObjectName getRuntimeOutputOptionsObjectName() {
    return delegate.getRuntimeOutputOptionsBeanName();
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean() {
    return runtimeOutputOptionsBean;
  }

  public String takeThreadDump(long requestMillis) {
    return l1InfoBean != null ? l1InfoBean.takeThreadDump(requestMillis) : null;
  }

  public int getLiveObjectCount() {
    return delegate.getLiveObjectCount();
  }

  public boolean isResident(ObjectID oid) {
    return delegate.isResident(oid);
  }

  public void killClient() {
    delegate.killClient();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.removePropertyChangeListener(listener);
  }

  public String getConfig() {
    return getL1InfoBean().getConfig();
  }

  public String getEnvironment() {
    return getL1InfoBean().getEnvironment();
  }

  public Map getL1Statistics() {
    return getL1InfoBean().getStatistics();
  }

  /**
   * Cpu usage, Memory usage, Transaction rate.
   * 
   * @see IClusterModel.getPrimaryClientStatistics
   * @see IClusterModel.getPrimaryServerStatistics
   */
  public Map getPrimaryStatistics() {
    Map result = getL1Statistics();
    result.put("TransactionRate", getTransactionRate());
    return result;
  }
}
