/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.base;

import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;

public abstract class ClientBase extends AbstractClientBase {
  private ToolkitBarrier    barrier;
  protected ToolkitInternal clusteringToolkit;

  public ClientBase(String args[]) {
    super(args);
    if (args[0].indexOf('@') != -1) {
      System.setProperty("tc.ssl.disableHostnameVerifier", "true");
      System.setProperty("tc.ssl.trustAllCerts", "true");
    }
  }

  @Override
  protected void doTest() throws Throwable {
    test(getClusteringToolkit());
  }

  protected synchronized Toolkit getClusteringToolkit() {
    if (clusteringToolkit == null) {
      clusteringToolkit = createToolkit();
    }
    return clusteringToolkit;
  }

  protected ToolkitInternal createToolkit() {
    try {
      return (ToolkitInternal) ToolkitFactory.createToolkit(getTerracottaTypeSubType() + getTerracottaUrl());
    } catch (ToolkitInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected ToolkitInternal createProxyToolkit(boolean rejoinEnabled) {
    try {
      Properties properties = new Properties();
      properties.setProperty("tcConfigSnippet", getTestControlMbean().getTsaProxyTcConfig());
      properties.put("rejoin", Boolean.toString(rejoinEnabled));
      return (ToolkitInternal) ToolkitFactory.createToolkit(getTerracottaTypeSubType()
                                                                + getTestControlMbean().getTsaProxyTerracottaUrl(),
                                                            properties);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected String getTerracottaTypeSubType() {
    return "toolkit:terracotta://";
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

  public void waitForAllCurrentTransactionsToComplete(Toolkit toolkit) {
    ((ToolkitInternal) toolkit).waitUntilAllTransactionsComplete();
  }

  public String getClientUUID(Toolkit toolkit) {
    return ((ToolkitInternal) toolkit).getClientUUID();
  }
}
