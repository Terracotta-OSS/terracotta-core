/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;

public class BasicTsaProxyTest extends AbstractToolkitTestBase {

  public BasicTsaProxyTest(TestConfig testConfig) {
    super(testConfig, Client.class);
    testConfig.getL2Config().setProxyTsaPorts(true);
    testConfig.getL2Config().setManualProxycontrol(true);
    testConfig.getClientConfig().addExtraClientJvmArg("-Dskip.validation.for.proxy.tests=true");
  }

  public static class Client extends ClientBase {

    public Client(String[] args) {
      super(args);
    }

    @Override
    protected ToolkitInternal createToolkit() {
      return createProxyToolkit(true);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      System.out.println("Terracotta URL : " + getTerracottaUrl());
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

    }

  }

}
