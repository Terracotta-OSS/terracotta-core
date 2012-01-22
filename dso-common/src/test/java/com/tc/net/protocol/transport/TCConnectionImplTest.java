/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;
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

/**
 * To Test Message Packup happening at the comms writer.
 */
public class TCConnectionImplTest extends TestCase {
  private TCConnectionManager connMgr;
  private TCListener          server;
  private final AtomicLong    sentMessagesTotalLength  = new AtomicLong(0);
  private final AtomicLong    rcvdMessagesTotalLength  = new AtomicLong(0);

  // private final AtomicLong sentMessages2 = new AtomicLong(0);
  private final AtomicLong    rcvdMessages2TotalLength = new AtomicLong(0);

  private final AtomicBoolean fullySent                = new AtomicBoolean(false);

  @Override
  protected void setUp() throws Exception {
    connMgr = new TCConnectionManagerImpl();

    ProtocolAdaptorFactory factory = new ProtocolAdaptorFactory() {
      public TCProtocolAdaptor getInstance() {
        return new WireProtocolAdaptorImpl(new ServerWPMGSink());
      }
    };

    server = connMgr.createListener(new TCSocketAddress(5678), factory);
  }

  @Override
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
            synchronized (rcvdMessages2TotalLength) {
              while (rcvdMessages2TotalLength.get() != sentMessagesTotalLength.get()) {
                try {
                  rcvdMessages2TotalLength.wait();
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
    for (int i = 0; i < 100; i++) {

      int bufCount = r.nextInt(15 + 1);
      fullySent.set(false);

      startBarrier.await();
      startBarrier.reset();

      TCNetworkMessage message = getMessages(bufCount);
      clientConn.putMessage(message);

      sentMessagesTotalLength.addAndGet(message.getTotalLength());

      synchronized (fullySent) {
        System.out.println("XXX total msgs sent " + sentMessagesTotalLength);
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

  SequenceGenerator seq = new SequenceGenerator(1);

  private TCNetworkMessage getMessages(int bufCunt) {
    MessageMonitor monitor = new NullMessageMonitor();
    ArrayList<TCByteBuffer> bufs = new ArrayList<TCByteBuffer>();
    int len = 0;
    long totLen = 0;
    while (bufCunt > 0) {
      len = r.nextInt(500 + 1);

      TCByteBuffer sourceBuffer = TCByteBufferFactory.wrap(getContent(len).getBytes());

      switch (len % 3) {
        case 0:
          break;
        case 1:
          int pos = r.nextInt(len);
          if (pos < 0 || pos > sourceBuffer.limit()) pos = 0;
          sourceBuffer.position(pos);
          sourceBuffer = sourceBuffer.slice();
          break;
        case 2:
          if (sourceBuffer.hasRemaining()) sourceBuffer.get();
          if (sourceBuffer.remaining() > TCMessageHeader.HEADER_LENGTH) sourceBuffer
              .get(new byte[TCMessageHeader.HEADER_LENGTH]);
          sourceBuffer = sourceBuffer.duplicate();
      }

      bufs.add(sourceBuffer);
      bufCunt--;
      totLen += len;
    }
    TCNetworkMessage message = getDSOMessage(monitor, bufs.toArray(new TCByteBuffer[] {}), totLen);
    return message;
  }

  private String getContent(final int length) {
    StringBuffer buf = new StringBuffer();
    String str = new String("abcde12345abcde12345abcde");

    int remaining = length;
    while (remaining > 0) {
      int copyLen = (remaining > str.length()) ? str.length() : remaining;
      buf.append(str.substring(0, copyLen));
      remaining -= copyLen;
    }
    return buf.toString();
  }

  private TCNetworkMessage getDSOMessage(final MessageMonitor monitor, TCByteBuffer[] bufs, long totLen) {
    TCNetworkMessage msg = new MyMessage(monitor);
    ((MyMessage) msg).initialize(seq.getNextSequence(), bufs, totLen);
    msg.seal();
    return msg;
  }

  class MyMessage extends DSOMessageBase {

    private static final byte SEQUENCE = 1;
    private static final byte DATA     = 2;
    private long              seqID;
    private TCByteBuffer[]    data;
    private long              totLen;

    MyMessage(MessageMonitor monitor) {
      super(new SessionID(0), monitor, new TCByteBufferOutputStream(), null, TCMessageType.BENCH_MESSAGE);
    }

    public void initialize(long nextSequence, TCByteBuffer[] bufs, long totLe) {
      this.seqID = nextSequence;
      this.data = bufs;
      this.totLen = totLe;
      dehydrateValues();
      final TCByteBuffer[] nvData = getOutputStream().toArray();
      Assert.eval(nvData.length > 0);
      nvData[0].putInt(0, 2);
      this.setPayload(nvData);
    }

    public long getTotLen() {
      return totLen;
    }

    @Override
    protected void dehydrateValues() {
      putNVPair(SEQUENCE, seqID);
      putNVPair(DATA, data);
      data = null;
    }

    @Override
    protected boolean hydrateValue(byte name) throws IOException {
      switch (name) {
        case SEQUENCE:
          this.seqID = getLongValue();
          return true;
        case DATA:
          this.data = getInputStream().toArray();
          return true;
        default:
          throw new AssertionError("unknown type in message");
      }
    }
  }

  class ClientWPMGSink implements WireProtocolMessageSink {
    public void putMessage(WireProtocolMessage message) {

      rcvdMessagesTotalLength.addAndGet(message.getDataLength());
      message.recycle();
      System.out.println("XXX Client rcvd msgs " + rcvdMessagesTotalLength);

    }

  }

  class ServerWPMGSink implements WireProtocolMessageSink {
    // private volatile boolean senderStarted = false;
    // private volatile TCConnection serverconn;

    public void putMessage(WireProtocolMessage message) {

      message.recycle();
      synchronized (rcvdMessages2TotalLength) {
        rcvdMessages2TotalLength.addAndGet(message.getDataLength());
        rcvdMessages2TotalLength.notify();
      }

      System.out.println("XXX Server rcvd msgs " + rcvdMessages2TotalLength);

    }
  }
}
