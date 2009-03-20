/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.stats.DSOClientMBean;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient implements NotificationListener {
  private final ConnectionContext     cc;
  private final ObjectName            beanName;
  private final DSOClientMBean        delegate;
  private final long                  clientID;
  private final String                remoteAddress;
  private String                      host;
  private Integer                     port;
  protected PropertyChangeSupport     changeHelper;

  private boolean                     isListeningForTunneledBeans;
  private L1InfoMBean                 l1InfoBean;
  private InstrumentationLoggingMBean instrumentationLoggingBean;
  private RuntimeLoggingMBean         runtimeLoggingBean;
  private RuntimeOutputOptionsMBean   runtimeOutputOptionsBean;

  public DSOClient(ConnectionContext cc, ObjectName beanName) {
    this.cc = cc;
    this.beanName = beanName;
    this.delegate = MBeanServerInvocationProxy.newMBeanProxy(cc.mbsc, beanName, DSOClientMBean.class, true);
    clientID = delegate.getClientID().toLong();
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
    runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, delegate
        .getRuntimeLoggingBeanName(), RuntimeLoggingMBean.class, true);
    runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
        .newProxyInstance(cc.mbsc, delegate.getRuntimeOutputOptionsBeanName(), RuntimeOutputOptionsMBean.class, true);
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans) return;
    try {
      cc.addNotificationListener(beanName, this);
    } catch (Exception e) {
      throw new RuntimeException("Adding listener to DSOClientMBean", e);
    }
    isListeningForTunneledBeans = true;
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
      fireTunneledBeansRegistered();
    }
  }

  private void fireTunneledBeansRegistered() {
    PropertyChangeEvent pce = new PropertyChangeEvent(this, DSOClientMBean.TUNNELED_BEANS_REGISTERED, null, null);
    changeHelper.firePropertyChange(pce);
  }

  public ObjectName getObjectName() {
    return beanName;
  }

  public boolean isTunneledBeansRegistered() {
    return delegate.isTunneledBeansRegistered();
  }

  public long getClientID() {
    return clientID;
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

  @Override
  public String toString() {
    return getRemoteAddress();
  }

  public Number[] getStatistics(String[] names) {
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

  public void killClient() {
    delegate.killClient();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.removePropertyChangeListener(listener);
  }
}
