/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.util.SequenceGenerator;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

public class WireProtocolGroupMessageImplTest extends TestCase {
  private TCConnectionManager connMgr;
  private TCListener          server;
  private final AtomicLong    sentMessages  = new AtomicLong(0);
  private final AtomicLong    rcvdMessages  = new AtomicLong(0);

  // private final AtomicLong sentMessages2 = new AtomicLong(0);
  private final AtomicLong    rcvdMessages2 = new AtomicLong(0);

  private final AtomicBoolean fullySent     = new AtomicBoolean(false);

  protected void setUp() throws Exception {
    connMgr = new TCConnectionManagerImpl();

    ProtocolAdaptorFactory factory = new ProtocolAdaptorFactory() {
      public TCProtocolAdaptor getInstance() {
        return new WireProtocolAdaptorImpl(new ServerWPMGSink());
      }
    };

    server = connMgr.createListener(new TCSocketAddress(5678), factory);
  }

  protected void tearDown() throws Exception {
    connMgr.shutdown();
    server.stop();
  }

  Random r = new Random();

  public void testBasic() throws TCTimeoutException, IOException, InterruptedException, BrokenBarrierException {
    final TCConnection clientConn = connMgr.createConnection(new WireProtocolAdaptorImpl(new ClientWPMGSink()));
    clientConn.connect(new TCSocketAddress(server.getBindPort()), 3000);

    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);

    Thread checker = new Thread(new Runnable() {
      public void run() {
        while (true) {

          try {
            startBarrier.await();

            System.out.println("XXX Waiting for Client to send all msgs ");
            synchronized (fullySent) {
              while (!fullySent.get()) {
                try {
                  fullySent.wait();
                } catch (InterruptedException e) {
                  System.out.println("fullySent: " + e);
                }
              }
            }

            System.out.println("XXX Waiting for server to rcv all msgs");
            synchronized (rcvdMessages2) {
              while (rcvdMessages2.get() != sentMessages.get()) {
                try {
                  rcvdMessages2.wait();
                } catch (InterruptedException e) {
                  System.out.println("rcvdMessages2: " + e);
                }
              }
            }

            endBarrier.await();
            
          } catch (BrokenBarrierException ie) {
            System.out.println("XXX Thread " + ie);
          } catch (InterruptedException e) {
            System.out.println("XXX Thread " + e);
          }
        }
      }
    });

    checker.start();
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 25; i++) {

      int count = 10000;
      fullySent.set(false);
      
      startBarrier.await();
      startBarrier.reset();

      ArrayList<TCNetworkMessage> messages = getMessages(count);
      for (TCNetworkMessage msg : messages) {
        clientConn.putMessage(msg);
      }

      sentMessages.addAndGet(count);

      synchronized (fullySent) {
        System.out.println("XXX total msgs sent " + sentMessages);
        fullySent.set(true);
        fullySent.notify();
      }

      endBarrier.await();
      endBarrier.reset();
      System.out.println("XXX Completed Round " + i + "\n\n");
    }
    long endTime = System.currentTimeMillis();
    System.out.println("XXX SuccesS. Took " + (endTime - startTime) / 1000 + " seconds");
  }

  private ArrayList<TCNetworkMessage> getMessages(final int count) {
    MessageMonitor monitor = new NullMessageMonitor();
    SequenceGenerator seq = new SequenceGenerator(1);
    TransportMessageFactoryImpl msgFactory = new TransportMessageFactoryImpl();
    ArrayList<TCNetworkMessage> messages = new ArrayList<TCNetworkMessage>();
    for (int i = 0; i < count; i++) {
      r.setSeed(System.currentTimeMillis());
      int value = r.nextInt(10);
      switch (value) {
        case 0:
        case 1:
          messages.add(msgFactory.createSyn(new ConnectionID(1), new MockTCConnection(), (short) 1, 1));
          break;
        default:
          messages.add(getDSOMessage(monitor, seq));
          break;
      }

    }
    return messages;
  }

  private TCNetworkMessage getDSOMessage(final MessageMonitor monitor, final SequenceGenerator seq) {
    TCNetworkMessage msg = new PingMessage(monitor);
    ((PingMessage) msg).initialize(seq);
    msg.seal();
    return msg;
  }

  class ClientWPMGSink implements WireProtocolMessageSink {
    public void putMessage(WireProtocolMessage message) {

      rcvdMessages.incrementAndGet();
      message.recycle();
      if (rcvdMessages.get() % 5000 == 0) {
        System.out.println("XXX Client rcvd msgs " + rcvdMessages);
      }

    }

  }

  class ServerWPMGSink implements WireProtocolMessageSink {
    // private volatile boolean senderStarted = false;
    // private volatile TCConnection serverconn;

    public void putMessage(WireProtocolMessage message) {

      message.recycle();
      synchronized (rcvdMessages2) {
        rcvdMessages2.incrementAndGet();
        rcvdMessages2.notify();
      }

      if (rcvdMessages2.get() % 5000 == 0) {
        System.out.println("XXX Server rcvd msgs " + rcvdMessages2);

        // if (!senderStarted) {
        // senderStarted = true;
        // serverconn = message.getSource();
        //
        // Thread t = new Thread(new Runnable() {
        // public void run() {
        // while (true) {
        // r.setSeed(System.currentTimeMillis());
        // int count = r.nextInt(1000);
        // ArrayList<TCNetworkMessage> messages = getMessages(count);
        // for (TCNetworkMessage msg : messages)
        // serverconn.putMessage(msg);
        // sentMessages2.addAndGet(count);
        // ThreadUtil.reallySleep(100);
        // }
        // }
        // });
        // t.start();
        // }

      }
    }
  }
}
