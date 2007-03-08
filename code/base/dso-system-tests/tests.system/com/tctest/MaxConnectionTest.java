/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.cluster.Cluster;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.server.TCServerImpl;
import com.tc.util.concurrent.ThreadUtil;

/**
 * Test to make sure that the maximum number of connected DSO clients may be limited. This isn't a transparency test,
 * but it is a system test which is why it's in the com.tctest package. It could, perhaps, be moved somewhere more
 * appropriate.
 */
public class MaxConnectionTest extends BaseDSOTestCase {

  private TCServerImpl server;

  private DistributedObjectClient newClient() throws Exception {
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();
    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelper(manager);

    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);
    return new DistributedObjectClient(configHelper, new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectClient.class))), new MockClassProvider(), components, NullManager.getInstance(),
                                       new Cluster());
  }

  public void testsMaxConnectionLimitAndClientDisconnectAccounting() throws Exception {

    ConnectionPolicy connectionPolicy = new ConnectionPolicyImpl(2);
    TestTVSConfigurationSetupManagerFactory factory = createDistributedConfigFactory();
    L2TVSConfigurationSetupManager l2Manager = factory.createL2TVSConfigurationSetupManager(null);
    server = new TCServerImpl(l2Manager, new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectServer.class))), connectionPolicy);
    server.start();

    makeClientUsePort(server.getDSOListenPort());

    DistributedObjectClient client1 = newClient();

    client1.start();
    newClient().start();

    final boolean[] done = new boolean[] { false };

    new Thread() {
      public void run() {
        try {
          newClient().start();
          if (!done[0]) fail("Expected a MaxConnectionsExceededException");
          else System.out.println("proceeded now that someone was killed");
        } catch (Exception e) {
          throw new AssertionError("oops");
        }
      }
    }.start();// with the maximum number of clients connected, make sure that a new client can't connect.

    // disconnect one of the clients
    LinkedQueue stopQueue = new LinkedQueue();
    long timeout = 5 * 1000;
    ThreadUtil.reallySleep(3000);
    done[0] = true;
    new Thread(new ClientStopper(stopQueue, client1)).start();
    if (stopQueue.poll(timeout) == null) {
      fail("Client failed to stop within timeout: " + timeout + " ms.");
    }

  }

  private static final class ClientStopper implements Runnable {
    private final LinkedQueue             queue;
    private final DistributedObjectClient myClient;

    private ClientStopper(LinkedQueue queue, DistributedObjectClient client) {
      this.queue = queue;
      this.myClient = client;
    }

    public void run() {
      myClient.stop();
      try {
        queue.put(new Object());
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }
}
