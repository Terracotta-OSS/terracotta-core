/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.cluster.Cluster;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.PauseListener;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.objectserver.control.ServerControl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.PortChooser;

import java.net.InetAddress;
import java.util.ArrayList;

public class DeadClientCrashedServerReconnectTest extends BaseDSOTestCase {

  private TestPauseListener pauseListener;

  public DeadClientCrashedServerReconnectTest() {
    this.disableAllUntil("3001-01-01");
  }

  public void setUp() {
    pauseListener = new TestPauseListener();
  }

  public void testClientsStayDeadAcrossRestarts() throws Exception {
    final boolean isCrashy = true;
    PortChooser portChooser = new PortChooser();
    RestartTestHelper helper = new RestartTestHelper(isCrashy,
                                                     new RestartTestEnvironment(this.getTempDirectory(), portChooser,
                                                                                RestartTestEnvironment.DEV_MODE),
                                                                                new ArrayList());
    ServerControl server = helper.getServerControl();
    long startTimeout = 20 * 60 * 1000;
    server.start(startTimeout);

    int proxyListenPort = portChooser.chooseRandomPort();
    InetAddress destHost = InetAddress.getByName("localhost");
    int destPort = server.getDsoPort();
    long delay = 0;
    boolean logData = true;
    TCPProxy proxy = new TCPProxy(proxyListenPort, destHost, destPort, delay, logData, getTempDirectory());
    proxy.toggleDebug();
    proxy.start();

    makeClientUsePort(proxyListenPort);

    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();
    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelper(manager);

    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);

    DistributedObjectClient client = new DistributedObjectClient(configHelper,
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(), components, NullManager
                                                                     .getInstance(), new Cluster());
    client.setPauseListener(pauseListener);
    client.start();

    // wait until client handshake is complete...
    pauseListener.waitUntilUnpaused();

    // disconnect the client from the server... this should make the server kill the client from the server
    // perspective...
    proxy.stop();

    // Now crash the server
    server.crash();

    // start the server back up
    proxy.start();
    server.start(startTimeout);

    // XXX: This is kind of dumb... there should be a way to find out from the server if the client has actually
    // tried to connect but was not allowed to.
    Thread.sleep(10 * 1000);
    assertTrue(pauseListener.isPaused());
  }

  private static final class TestPauseListener implements PauseListener {

    private boolean paused = true;

    public void waitUntilPaused() throws InterruptedException {
      waitUntilCondition(true);
    }

    public void waitUntilUnpaused() throws InterruptedException {
      waitUntilCondition(false);
    }

    public boolean isPaused() {
      synchronized (this) {
        return paused;
      }
    }

    private void waitUntilCondition(boolean b) throws InterruptedException {
      synchronized (this) {
        while (b != paused) {
          wait();
        }
      }
    }

    public void notifyPause() {
      synchronized (this) {
        paused = true;
        notifyAll();
      }
    }

    public void notifyUnpause() {
      synchronized (this) {
        paused = false;
        notifyAll();
      }
    }

  }

}
