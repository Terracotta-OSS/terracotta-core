/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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