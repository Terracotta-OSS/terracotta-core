/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.GenericNetworkMessage;
import com.tc.net.protocol.GenericNetworkMessageSink;
import com.tc.net.protocol.GenericProtocolAdaptor;

public class SimpleClient {
  private final int                 numMsgs;
  private final TCConnectionManager connMgr;
  private final TCSocketAddress     addr;
  private final int                 dataSize;
  private final SynchronizedLong    msgs = new SynchronizedLong(0);
  private final long                sleepFor;

  public SimpleClient(TCConnectionManager connMgr, TCSocketAddress addr, int numMsgs, int dataSize, long sleepFor) {
    this.connMgr = connMgr;
    this.addr = addr;
    this.numMsgs = numMsgs;
    this.dataSize = dataSize;
    this.sleepFor = sleepFor;
  }

  public void run() throws Exception {
    final GenericNetworkMessageSink recvSink = new GenericNetworkMessageSink() {
      public void putMessage(GenericNetworkMessage msg) {
        final long recv = msgs.increment();
        if ((recv % 1000) == 0) {
          System.out.println("Processed " + (recv * msg.getTotalLength()) + " bytes...");
        }
      }
    };

    final TCConnection conn = connMgr.createConnection(new GenericProtocolAdaptor(recvSink));
    conn.connect(addr, 3000);

    for (int i = 0; (numMsgs < 0) || (i < numMsgs); i++) {
      TCByteBuffer data[] = TCByteBufferFactory.getFixedSizedInstancesForLength(false, dataSize);
      final GenericNetworkMessage msg = new GenericNetworkMessage(conn, data);
      msg.setSentCallback(new Runnable() {
        public void run() {
          msg.setSent();
        }
      });

      conn.putMessage(msg);

      if (sleepFor < 0) {
        msg.waitUntilSent();
      } else {
        Thread.sleep(sleepFor);
      }
    }

    Thread.sleep(5000);
    conn.close(3000);
  }

  public static void main(String args[]) throws Throwable {
    try {
      TCConnectionManager connMgr = new TCConnectionManagerImpl();
      SimpleClient client = new SimpleClient(connMgr, new TCSocketAddress(args[0], Integer.parseInt(args[1])), Integer
          .parseInt(args[3]), Integer.parseInt(args[2]), Integer.parseInt(args[4]));
      client.run();
    } catch (Throwable t) {
      System.err.println("usage: " + SimpleClient.class.getName()
                         + " <host> <port> <msgSize> <numMsgs, -1 for unlimited> <delay, -1 for single fire>\n\n");
      throw t;
    }
  }
}