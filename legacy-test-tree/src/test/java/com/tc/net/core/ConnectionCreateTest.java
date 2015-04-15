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
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class ConnectionCreateTest extends TestCase {

  public void testConnectionCreate() throws Exception {
    final Random random = new Random();
    final TCConnectionManager clientConnMgr;
    final TCConnectionManager serverConnMgr;
    final TCSocketAddress addr;
    clientConnMgr = new TCConnectionManagerImpl();
    serverConnMgr = new TCConnectionManagerImpl();

    TCListener lsnr = serverConnMgr.createListener(new TCSocketAddress(0), new ProtocolAdaptorFactory() {
      @Override
      public TCProtocolAdaptor getInstance() {
        return new NullProtocolAdaptor();
      }
    });

    addr = lsnr.getBindSocketAddress();

    final int numClients = 100;
    final int numThreads = 5;
    final Object STOP = new Object();
    final Object work = new Object();
    final AtomicInteger failures = new AtomicInteger(0);
    final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

    class ConnectTask implements Runnable {
      @Override
      public void run() {
        while (true) {
          try {
            Object o = queue.take();
            if (o == STOP) { return; }
            TCConnection conn = clientConnMgr.createConnection(new NullProtocolAdaptor());
            conn.connect(addr, 3000);
            Thread.sleep(random.nextInt(100));
            conn.close(3000);
            return;
          } catch (Throwable t) {
            t.printStackTrace();
            failures.incrementAndGet();
          }

        }
      }
    }

    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(new ConnectTask(), "Connect thread " + i);
      threads[i].start();
    }

    for (int i = 0; i < numClients; i++) {
      queue.put(work);
    }

    for (Thread thread : threads) {
      queue.put(STOP);
    }

    for (Thread thread : threads) {
      thread.join();
    }

    int errors = failures.get();
    assertTrue("Failure count = " + errors, errors == 0);
  }

}