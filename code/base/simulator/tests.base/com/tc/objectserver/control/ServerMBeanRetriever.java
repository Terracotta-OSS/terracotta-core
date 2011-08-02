/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.control;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.CallableWaiter;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class ServerMBeanRetriever {
  private final String host;
  private final int    jmxPort;
  private DSOMBean     dsoMBean;

  public ServerMBeanRetriever(final String host, final int jmxPort) {
    this.host = host;
    this.jmxPort = jmxPort;
  }

  public L2DumperMBean getL2DumperMBean() throws Exception {
    return getL2DumperMBean(host, jmxPort);
  }

  public DSOMBean getDSOMBean() throws Exception {
    if (dsoMBean != null) {
      try {
        dsoMBean.getLiveObjectCount();
        return dsoMBean;
      } catch (Exception e) {
        // the dsoMBean is dead, re-obtain it.
      }
    }
    dsoMBean = getDSOMBean(host, jmxPort);
    return dsoMBean;
  }

  public int getJmxPort() {
    return jmxPort;
  }

  public String getHost() {
    return host;
  }

  public static DSOMBean getDSOMBean(final String host, final int jmxPort) throws Exception {
    final AtomicReference<DSOMBean> dsoMBeanRef = new AtomicReference<DSOMBean>();
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        try {
          JMXConnector jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          DSOMBean dsoMBean = MBeanServerInvocationProxy.newMBeanProxy(msc, L2MBeanNames.DSO, DSOMBean.class, false);
          dsoMBean.getLiveObjectCount();
          dsoMBeanRef.set(dsoMBean);
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    });
    return dsoMBeanRef.get();
  }

  public static L2DumperMBean getL2DumperMBean(final String host, final int jmxPort) throws Exception {
    // Not going to cache this and check liveness because it's not used super frequently and it doesn't have an easy
    // non-destructive check method.
    final AtomicReference<L2DumperMBean> l2DumperRef = new AtomicReference<L2DumperMBean>();
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        try {
          JMXConnector jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          l2DumperRef.set(MBeanServerInvocationProxy
              .newMBeanProxy(msc, L2MBeanNames.DUMPER, L2DumperMBean.class, false));
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    });
    return l2DumperRef.get();
  }
}
