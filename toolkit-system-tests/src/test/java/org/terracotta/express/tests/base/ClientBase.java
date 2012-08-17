/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.base;

import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import java.util.concurrent.BrokenBarrierException;

public abstract class ClientBase extends AbstractClientBase {
  private static final String TEST_LIST_NAME                                             = "testList@testFramework";
  private static final String MANAGER_UTIL_CLASS_NAME                                    = "com.tc.object.bytecode.ManagerUtil";
  private static final String MANAGER_UTIL_GETUUID_METHOD                                = "getUUID";
  private static final String MANAGER_UTIL_WAITFORALLCURRENTTRANSACTIONTOCOMPLETE_METHOD = "waitForAllCurrentTransactionsToComplete";
  private static final String MANAGER_UTIL_GETCLIENTID_METHOD                            = "getClientID";
  private ToolkitBarrier      barrier;
  private Toolkit             clusteringToolkit;

  public ClientBase(String args[]) {
    super(args);
    if (args[0].indexOf('@') != -1) {
      System.setProperty("tc.ssl.disableHostnameVerifier", "true");
      System.setProperty("tc.ssl.trustAllCerts", "true");
    }
  }

  @Override
  protected final void doTest() throws Throwable {
    test(getClusteringToolkit());
  }

  protected synchronized Toolkit getClusteringToolkit() {
    if (clusteringToolkit == null) {
      clusteringToolkit = createToolkit();
    }
    return clusteringToolkit;
  }

  private Toolkit createToolkit() {
    try {
      return ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl());
    } catch (ToolkitInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract void test(Toolkit toolkit) throws Throwable;

  @Override
  protected void pass() {
    System.out.println("[PASS: " + getClass().getName() + "]");
  }

  protected synchronized final ToolkitBarrier getBarrierForAllClients() {
    if (barrier == null) {
      barrier = getClusteringToolkit().getBarrier("barrier with all clients", getParticipantCount());
    }
    return barrier;
  }

  protected final int waitForAllClients() throws InterruptedException, BrokenBarrierException {
    return getBarrierForAllClients().await();
  }

  public String getUUID() {
    try {
      ClassLoader cl = getClusteringToolkit().getList(TEST_LIST_NAME, null).getClass().getClassLoader();
      Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
      return (String) managerUtil.getMethod(MANAGER_UTIL_GETUUID_METHOD).invoke(null);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  // work around for ManagerUtil.waitForAllCurrentTransactionsToComplete()
  public void waitForAllCurrentTransactionsToComplete() {
    try {
      ClassLoader cl = getClusteringToolkit().getList(TEST_LIST_NAME, null).getClass().getClassLoader();
      Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
      managerUtil.getMethod(MANAGER_UTIL_WAITFORALLCURRENTTRANSACTIONTOCOMPLETE_METHOD).invoke(null);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  // work around for ManagerUtil.getClientID
  public String getClientID() {
    try {
      ClassLoader cl = getClusteringToolkit().getList(TEST_LIST_NAME, null).getClass().getClassLoader();
      Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
      return (String) managerUtil.getMethod(MANAGER_UTIL_GETCLIENTID_METHOD).invoke(null);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static final void debug(String fmt, Object... args) {
    System.out.println("[T E S T] " + String.format(fmt, args));
  }
}
