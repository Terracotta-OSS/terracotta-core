/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.exception.ExceptionHelperImpl;
import com.tc.exception.RuntimeExceptionHelper;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.exposed.TerracottaClusterMBean;
import com.tc.util.UUID;

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

import junit.framework.Assert;

public class ClusterMembershipEventJMXTestClient extends ClientBase {
  private MBeanServer server         = null;
  private ObjectName  clusterBean    = null;
  private final List  clusterBeanBag = new ArrayList();

  public ClusterMembershipEventJMXTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new ClusterMembershipEventJMXTestClient(args).run();
  }

  @Override
  public void test(Toolkit toolkit) throws Throwable {
    locateClusterBean(toolkit);
    runTest();
  }

  private void runTest() throws Throwable {
    TerracottaClusterMBean cluster = MBeanServerInvocationHandler.newProxyInstance(server, clusterBean,
                                                                                   TerracottaClusterMBean.class, true);
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
      @Override
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

  private void locateClusterBean(Toolkit toolkit) throws Exception {
    List servers = MBeanServerFactory.findMBeanServer(null);
    if (servers.size() == 0) { throw new RuntimeException("No mean server found!"); }

    clusterBean = TerracottaManagement.addNodeInfo(L1MBeanNames.CLUSTER_BEAN_PUBLIC, new UUID(getClientUUID(toolkit)));
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
        @Override
        public void handleNotification(final Notification notification, final Object handback) {
          synchronized (clusterBeanBag) {
            clusterBeanBag.add(handback);
            clusterBeanBag.notifyAll();
          }
        }
      };

      // filter to let only clusterBean passed through
      NotificationFilter filter = new NotificationFilter() {
        @Override
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

}
