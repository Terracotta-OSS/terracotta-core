/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterEvent.Type;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;

import java.util.Properties;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class BasicTsaProxyTest extends AbstractToolkitTestBase {

  public BasicTsaProxyTest(TestConfig testConfig) {
    super(testConfig, ProxyClient.class, SimpleClient.class);
    TestBaseUtil.enabledL1ProxyConnection(testConfig);
  }

  public static class SimpleClient extends ClientBase {
    private volatile boolean operationsDisconnected = false;

    public SimpleClient(String[] args) {
      super(args);
    }

    @Override
    protected ToolkitInternal createToolkit() {
      try {
        Properties properties = new Properties();
        properties.put("rejoin", Boolean.toString(true));
        return (ToolkitInternal) ToolkitFactory.createToolkit(getTerracottaTypeSubType()
                                                              + getTestControlMbean().getTerracottaUrl(), properties);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      final ClusterInfo clusterInfo = toolkit.getClusterInfo();
      clusterInfo.addClusterListener(new ClusterListener() {

        @Override
        public void onClusterEvent(ClusterEvent event) {
          if (event.getType().equals(Type.NODE_LEFT)) {
            if (clusterInfo.getCurrentNode().equals(event.getNode())) {
              operationsDisconnected = true;
            }
          }
        }
      });
      getBarrierForAllClients().await(); // wait until other client starts.
      getBarrierForAllClients().await(); // wait until second client finishes.
      Assert.assertFalse(operationsDisconnected);
    }
  }

  public static class ProxyClient extends ClientBase {

    public ProxyClient(String[] args) {
      super(args);
    }

    @Override
    protected ToolkitInternal createToolkit() {
      return createProxyToolkit(true);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      getBarrierForAllClients().await();
      final ClusterInfo clusterInfo = toolkit.getClusterInfo();
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return clusterInfo.areOperationsEnabled();
        }
      });
      getTestControlMbean().stopTsaProxy(0);
      WaitUtil.waitUntilCallableReturnsFalse(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return clusterInfo.areOperationsEnabled();
        }
      });
      getTestControlMbean().startTsaProxy(0);
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return clusterInfo.areOperationsEnabled();
        }
      });
      getBarrierForAllClients().await();

    }

  }

}
