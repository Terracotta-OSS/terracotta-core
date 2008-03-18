/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.statistics.Statistic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient {
  private ConnectionContext       cc;
  private ObjectName              bean;
  private DSOClientMBean          delegate;
  private L1InfoMBean             l1InfoBean;
  private long                    channelID;
  private String                  remoteAddress;
  private String                  host;
  private Integer                 port;
  protected PropertyChangeSupport changeHelper;

  public DSOClient(ConnectionContext cc, ObjectName bean) {
    this.cc = cc;
    this.bean = bean;
    this.delegate = (DSOClientMBean) MBeanServerInvocationProxy.newProxyInstance(cc.mbsc, bean, DSOClientMBean.class,
                                                                                 true);
    channelID = delegate.getChannelID().toLong();
    remoteAddress = delegate.getRemoteAddress();
    changeHelper = new PropertyChangeSupport(this);
  }

  public ObjectName getObjectName() {
    return bean;
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
      port = new Integer(-1);

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
      cc.invoke(bean, "refresh", new Object[] {}, new String[] {});
      changeHelper.firePropertyChange(new PropertyChangeEvent(this, null, null, null));
    } catch (Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public String toString() {
    return getRemoteAddress();
  }

  public Statistic[] getStatistics(String[] names) {
    return delegate.getStatistics(names);
  }

  void addNotificationListener(ObjectName on, NotificationListener listener) throws Exception {
    cc.addNotificationListener(on, listener);
  }

  private void addRegistrationListener(NotificationListener listener) throws Exception {
    ObjectName mbsd = cc.queryName("JMImplementation:type=MBeanServerDelegate");
    if (mbsd != null) {
      try {
        cc.removeNotificationListener(mbsd, listener);
      } catch (Exception e) {/**/
      }
      addNotificationListener(mbsd, listener);
    }
  }

  private ObjectName getTunneledBeanName(ObjectName on) {
    try {
      String name = on.getCanonicalName() + ",clients=Clients,node=" + getRemoteAddress().replace(':', '/');
      return new ObjectName(name);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException("Creating ObjectName", mone);
    }
  }

  public ObjectName getL1InfoObjectName() {
    return getTunneledBeanName(L1MBeanNames.L1INFO_PUBLIC);
  }

  public L1InfoMBean getL1InfoMBean() throws Exception {
    return getL1InfoMBean(null);
  }

  public L1InfoMBean getL1InfoMBean(NotificationListener listener) throws Exception {
    if (l1InfoBean != null) return l1InfoBean;

    ObjectName o = getL1InfoObjectName();
    if (cc.mbsc.isRegistered(o)) {
      l1InfoBean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(cc.mbsc, o, L1InfoMBean.class, true);
    } else if (listener != null) {
      addRegistrationListener(listener);
    }
    return l1InfoBean;
  }

  public ObjectName getInstrumentationLoggingObjectName() {
    return getTunneledBeanName(L1MBeanNames.INSTRUMENTATION_LOGGING_PUBLIC);
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingMBean() throws Exception {
    return getInstrumentationLoggingMBean(null);
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingMBean(NotificationListener listener) throws Exception {
    ObjectName o = getInstrumentationLoggingObjectName();
    if (cc.mbsc.isRegistered(o)) {
      InstrumentationLoggingMBean instrumentationLoggingBean = (InstrumentationLoggingMBean) MBeanServerInvocationHandler
          .newProxyInstance(cc.mbsc, o, InstrumentationLoggingMBean.class, false);
      return instrumentationLoggingBean;
    } else if (listener != null) {
      addRegistrationListener(listener);
    }
    return null;
  }

  public ObjectName getRuntimeLoggingObjectName() {
    return getTunneledBeanName(L1MBeanNames.RUNTIME_LOGGING_PUBLIC);
  }

  public RuntimeLoggingMBean getRuntimeLoggingMBean() throws Exception {
    return getRuntimeLoggingMBean(null);
  }

  public RuntimeLoggingMBean getRuntimeLoggingMBean(NotificationListener listener) throws Exception {
    ObjectName o = getRuntimeLoggingObjectName();
    if (cc.mbsc.isRegistered(o)) {
      RuntimeLoggingMBean runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler
          .newProxyInstance(cc.mbsc, o, RuntimeLoggingMBean.class, false);
      return runtimeLoggingBean;
    } else if (listener != null) {
      addRegistrationListener(listener);
    }
    return null;
  }

  public ObjectName getRuntimeOutputOptionsObjectName() {
    return getTunneledBeanName(L1MBeanNames.RUNTIME_OUTPUT_OPTIONS_PUBLIC);
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsMBean() throws Exception {
    return getRuntimeOutputOptionsMBean(null);
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsMBean(NotificationListener listener) throws Exception {
    ObjectName o = getRuntimeOutputOptionsObjectName();
    if (cc.mbsc.isRegistered(o)) {
      RuntimeOutputOptionsMBean runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
          .newProxyInstance(cc.mbsc, o, RuntimeOutputOptionsMBean.class, false);
      return runtimeOutputOptionsBean;
    } else if (listener != null) {
      addRegistrationListener(listener);
    }
    return null;
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
