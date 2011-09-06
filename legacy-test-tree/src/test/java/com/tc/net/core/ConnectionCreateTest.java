/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

import java.util.Random;

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
      public TCProtocolAdaptor getInstance() {
        return new NullProtocolAdaptor();
      }
    });

    addr = lsnr.getBindSocketAddress();

    final int numClients = 100;
    final int numThreads = 5;
    final Object STOP = new Object();
    final Object work = new Object();
    final SynchronizedInt failures = new SynchronizedInt(0);
    final LinkedQueue queue = new LinkedQueue();

    class ConnectTask implements Runnable {
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
            failures.increment();
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

    for (int i = 0; i < threads.length; i++) {
      queue.put(STOP);
    }

    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }

    int errors = failures.get();
    assertTrue("Failure count = " + errors, errors == 0);
  }

}