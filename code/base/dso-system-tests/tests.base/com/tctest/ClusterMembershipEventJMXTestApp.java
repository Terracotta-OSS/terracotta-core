/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.ExceptionHelperImpl;
import com.tc.exception.RuntimeExceptionHelper;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.exposed.TerracottaClusterMBean;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

public class ClusterMembershipEventJMXTestApp extends AbstractTransparentApp {
  private MBeanServer server         = null;
  private ObjectName  clusterBean    = null;
  private final List  clusterBeanBag = new ArrayList();

  public ClusterMembershipEventJMXTestApp(final String appId, final ApplicationConfig config,
                                          final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterMembershipEventJMXTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(testClass + "$*");
  }

  public void run() {
    try {
      locateClusterBean();
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    TerracottaClusterMBean cluster = (TerracottaClusterMBean) MBeanServerInvocationHandler
        .newProxyInstance(server, clusterBean, TerracottaClusterMBean.class, true);
    ExceptionHelperImpl helper = new ExceptionHelperImpl();
    helper.addHelper(new RuntimeExceptionHelper());

    try {
      cluster.getNodeId();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(helper.getUltimateCause(e) instanceof UnsupportedOperationException);
    }

    try {
      cluster.getNodesInCluster();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(helper.getUltimateCause(e) instanceof UnsupportedOperationException);
    }

    try {
      cluster.isConnected();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(helper.getUltimateCause(e) instanceof UnsupportedOperationException);
    }

    NotificationListener listener = new NotificationListener() {
      public void handleNotification(final Notification notification, final Object handback) {
        /**/
      }
    };

    try {
      server.addNotificationListener(clusterBean, listener, null, clusterBean);
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  private void locateClusterBean() throws Exception {
    List servers = MBeanServerFactory.findMBeanServer(null);
    if (servers.size() == 0) { throw new RuntimeException("No mean server found!"); }

    clusterBean = TerracottaManagement.addNodeInfo(L1MBeanNames.CLUSTER_BEAN_PUBLIC, new UUID(ManagerUtil.getUUID()));
    for (int i = 0; i < servers.size(); i++) {
      MBeanServer mbeanServer = (MBeanServer) servers.get(i);
      if (mbeanServer.isRegistered(clusterBean)) {
        server = mbeanServer;
        break;
      }
    }

    Assert.assertNotNull(server);

    // Fairly certain L1 beans are registered early. The assertion above also indicates
    // the bean must already be registered. Finally, if all this were necessary, such logic would
    // need to be run against each MBeanServer found, not just the one that we already determined
    // contains the bean.

    if (false) {
      ObjectName delegateName = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");

      // listener for newly registered MBeans;
      // only clusterBean will ever be notified for due to the filter below
      NotificationListener listener = new NotificationListener() {
        public void handleNotification(final Notification notification, final Object handback) {
          synchronized (clusterBeanBag) {
            clusterBeanBag.add(handback);
            clusterBeanBag.notifyAll();
          }
        }
      };

      // filter to let only clusterBean passed through
      NotificationFilter filter = new NotificationFilter() {
        public boolean isNotificationEnabled(final Notification notification) {
          if (notification.getType().equals("JMX.mbean.registered")
              && ((MBeanServerNotification) notification).getMBeanName().equals(clusterBean)) return true;
          return false;
        }
      };

      // add our listener for clusterBean's registration
      server.addNotificationListener(delegateName, listener, filter, clusterBean);

      // because of race condition, clusterBean might already have registered
      // before we registered the listener
      Set allObjectNames = server.queryNames(null, null);

      if (!allObjectNames.contains(clusterBean)) {
        synchronized (clusterBeanBag) {
          while (clusterBeanBag.isEmpty()) {
            clusterBeanBag.wait();
          }
        }
      }

      // clusterBean is now registered, no need to listen for it
      server.removeNotificationListener(delegateName, listener);
    }
  }

  public static class L1Client {
    public static void main(final String args[]) {
      // nothing to do
    }
  }
}
