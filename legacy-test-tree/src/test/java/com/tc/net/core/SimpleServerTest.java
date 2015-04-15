/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.EchoSink;
import com.tc.net.proxy.TCPProxy;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleServerTest extends TCTestCase {
  private final boolean         useProxy  = false;
  private TCPProxy              proxy     = null;
  private int                   proxyPort = -1;
  private TCConnectionManager   connMgr;
  private SimpleServer          server;
  private final AtomicReference<Throwable> error     = new AtomicReference<Throwable>(null);

  protected void setUp(int serverThreadCount) throws Exception {
    connMgr = new TCConnectionManagerImpl();
    server = new SimpleServer(new EchoSink(true, new EchoSink.ErrorListener() {
      @Override
      public void error(Throwable t) {
        setError(t);
      }
    }), 0 /* PORT */, serverThreadCount);
    server.start();

    if (useProxy) {
      int serverPort = server.getServerAddr().getPort();
      proxyPort = serverPort + 1;
      proxy = new TCPProxy(proxyPort, server.getServerAddr().getAddress(), serverPort, 0, true, new File(System
          .getProperty("java.io.tmpdir")));
      proxy.start();
    }
  }

  @Override
  protected void setUp() throws Exception {
    //
  }

  private void setError(Throwable t) {
    t.printStackTrace();
    error.set(t);
  }

  @Override
  protected void tearDown() throws Exception {
    if (error.get() != null) {
      fail(error.get().toString());
    }
    if (proxy != null) {
      proxy.stop();
    }
    connMgr.shutdown();
    server.stop();
  }

  public void testLargeMessages() throws Exception {
    setUp(0);
    System.out.println("LARGE MESSAGES");
    runMultiClient(15, 5, 256, 2, 100, 150);
  }

  public void testSmallMessages() throws Exception {
    setUp(0);
    // these messages only take one buffer
    System.out.println("SMALLEST MESSAGES");
    runMultiClient(250, 20, 0, 100, 3, 5);
  }

  public void testKindaSmallMessages() throws Exception {
    setUp(0);
    // these messages span at least two byte buffers
    System.out.println("KINDA SMALL MESSAGES");
    runMultiClient(75, 10, 1, 100, 3, 7);
  }

  private void runMultiClient(int numClients, int maxConcurrent, final int dataSize, final int numToSend,
                              final int minDelay, final int maxDelay) throws Exception {
    runMultiClient(numClients, maxConcurrent, dataSize, true, numToSend, minDelay, maxDelay);
  }

  private void runMultiClient(int numClients, int maxConcurrent, final int dataSize, boolean addExtra,
                              final int numToSend, final int minDelay, final int maxDelay) throws Exception {
    long start = System.currentTimeMillis();

    try {
      final int numConcurrent = Math.min(maxConcurrent, numClients);
      ExecutorService pool = Executors.newFixedThreadPool(numConcurrent);

      final TCSocketAddress addr;
      if (proxy != null) {
        addr = new TCSocketAddress(proxyPort);
      } else {
        addr = new TCSocketAddress(server.getServerAddr().getPort());
      }
      List<ClientTask> clientTasks = new ArrayList<ClientTask>();
      for (int i = 0; i < numClients; i++) {
        clientTasks.add(new ClientTask(i, new VerifierClient(connMgr, addr, dataSize, addExtra, numToSend, minDelay,
                                                          maxDelay)));
      }

      pool.invokeAll(clientTasks);
      pool.shutdown();
    } finally {
      System.err.println("Took " + (System.currentTimeMillis() - start) + " millis for test: " + getName());
    }
  }

  private class ClientTask implements Callable<Void> {
    private final VerifierClient client;
    private final int            num;

    ClientTask(int num, VerifierClient client) {
      this.num = num;
      this.client = client;
    }

    @Override
    public Void call() throws Exception {
      try {
        client.run();
      } catch (Throwable t) {
        setError(t);
      } finally {
        System.err.println(System.currentTimeMillis() + ": client " + num + " finished");
      }
      return null;
    }

  }

}
