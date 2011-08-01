/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.exception.TCNotRunningException;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.CallableWaiter;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public abstract class ServerControlBase implements ServerControl {

  private final int    adminPort;
  private final String host;
  private final int    dsoPort;

  private DSOMBean     dsoMBean;

  public ServerControlBase(String host, int dsoPort, int adminPort) {
    this.host = host;
    this.dsoPort = dsoPort;
    this.adminPort = adminPort;
  }

  public boolean isRunning() {
    Socket socket = null;
    try {
      socket = new Socket(host, adminPort);
      if (!socket.isConnected()) throw new AssertionError();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  public int getAdminPort() {
    return adminPort;
  }

  public int getDsoPort() {
    return dsoPort;
  }

  protected String getHost() {
    return host;
  }

  public L2DumperMBean getL2DumperMBean() throws Exception {
    // Not going to cache this and check liveness because it's not used super frequently and it doesn't have an easy
    // non-destructive check method.
    if (!isRunning()) { throw new TCNotRunningException("Server is not up."); }
    final AtomicReference<L2DumperMBean> l2DumperRef = new AtomicReference<L2DumperMBean>();
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        try {
          JMXConnector jmxConnector = JMXUtils.getJMXConnector(getHost(), getAdminPort());
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

  public DSOMBean getDSOMBean() throws Exception {
    if (!isRunning()) { throw new TCNotRunningException("Server is not up."); }
    if (dsoMBean != null) {
      try {
        dsoMBean.getLiveObjectCount();
        return dsoMBean;
      } catch (Exception e) {
        // the dsoMBean is dead, re-obtain it.
      }
    }
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        try {
          JMXConnector jmxConnector = JMXUtils.getJMXConnector(getHost(), getAdminPort());
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          dsoMBean = MBeanServerInvocationProxy.newMBeanProxy(msc, L2MBeanNames.DSO, DSOMBean.class, false);
          dsoMBean.getLiveObjectCount();
          return true;
        } catch (Exception e) {
          return false;
        }
      }
    });
    return dsoMBean;
  }
}
