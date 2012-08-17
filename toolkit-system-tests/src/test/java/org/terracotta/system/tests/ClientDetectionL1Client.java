package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

public class ClientDetectionL1Client extends ClientBase {
  static final int               NUM_OF_CLIENTS = 3;
  private final ToolkitBarrier clientDetectionBarrier;

  public ClientDetectionL1Client(String[] args) {
    super(args);
    this.clientDetectionBarrier = getClusteringToolkit().getBarrier("clientDetectionBarrier", NUM_OF_CLIENTS);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    System.out.println("@@@@@@@ I'm online.... id = " + getClientID());
    this.clientDetectionBarrier.await();
    Thread.sleep(10 * 1000);
  }
}