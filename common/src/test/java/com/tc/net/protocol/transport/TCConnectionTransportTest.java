/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCNetworkMessage;
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
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import java.net.InetSocketAddress;
import java.util.Objects;

import org.terracotta.utilities.test.net.PortManager;

/**
 * To Test Message Packup happening at the comms writer.
 */
public class TCConnectionTransportTest extends TestCase {
  private TCConnectionManager connMgr;
  private PortManager.PortRef portRef;
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
      @Override
      public TCProtocolAdaptor getInstance() {
        return new WireProtocolAdaptorImpl(new ServerWPMGSink());
      }
    };

    portRef = PortManager.getInstance().reservePort();
    server = connMgr.createListener(new InetSocketAddress(portRef.port()), factory);
  }

  @Override
  protected void tearDown() throws Exception {
    connMgr.shutdown();
    server.stop();
    portRef.close();
  }

  Random r = new Random();

  public void testBasic() throws TCTimeoutException, IOException, InterruptedException, BrokenBarrierException {
    final TCConnection clientConn = connMgr.createConnection(new WireProtocolAdaptorImpl(new ClientWPMGSink()));
    clientConn.connect(new InetSocketAddress(server.getBindSocketAddress().getPort()), 3000);
    final CyclicBarrier startBarrier = new CyclicBarrier(2);
    final CyclicBarrier endBarrier = new CyclicBarrier(2);
    
    server.addEventListener(new TCListenerEventListener() {
      @Override
      public void closeEvent(TCListenerEvent event) {
        System.out.println(event);
      }
      
    });
       

    Thread checker = new Thread(new Runnable() {
      @Override
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
                  rcvdMessages2TotalLength.wait(1000);
                  System.out.println(rcvdMessages2TotalLength + " " + sentMessagesTotalLength);
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
    for (int i = 0; i < 1000; i++) {

      int bufCount = r.nextInt(15 + 1);
      fullySent.set(false);

      startBarrier.await();
      startBarrier.reset();

      TCNetworkMessage message = getMessages(bufCount);
      message.addCompleteCallback(()->{
        sentMessagesTotalLength.addAndGet(message.getTotalLength());

        synchronized (fullySent) {
          System.out.println("XXX total msgs sent " + sentMessagesTotalLength);
          fullySent.set(true);
          fullySent.notify();
        }
      });
      clientConn.putMessage(message);

      endBarrier.await();
      endBarrier.reset();
      System.out.println("XXX Completed Round " + i + "\n\n");
    }
    long endTime = System.currentTimeMillis();
    System.out.println("XXX SuccesS. Took " + (endTime - startTime) / 1000 + " seconds");
  }

  SequenceGenerator seq = new SequenceGenerator(1);

  private TCNetworkMessage getMessages(int bufcount) {
    MessageMonitor monitor = new NullMessageMonitor();
    ArrayList<TCByteBuffer> bufs = new ArrayList<TCByteBuffer>();
    int len = 0;
    while (bufcount > 0) {
      len = r.nextInt(500 + 1);

      TCByteBuffer sourceBuffer = TCByteBufferFactory.wrap(getContent(len).getBytes());
      Assert.assertEquals(len, sourceBuffer.remaining());

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
          break;
        default:
          throw new AssertionError();
      }

      bufs.add(sourceBuffer);
      bufcount--;
    }
    TCNetworkMessage message = getDSOMessage(monitor, TCReferenceSupport.createGCReference(bufs));
    return message;
  }

  private String getContent(int length) {
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

  private TCNetworkMessage getDSOMessage(MessageMonitor monitor, TCReference bufs) {
    TCActionNetworkMessage nmsg = new MyMessage(monitor, seq.getNextSequence(), bufs).getNetworkMessage();
    return nmsg;
  }

  class MyMessage extends DSOMessageBase {

    private static final byte SEQUENCE = 1;
    private static final byte DATA     = 2;
    private long              seqID;
    private TCReference    data;

    @SuppressWarnings("resource")
    MyMessage(MessageMonitor monitor, long nextSequence, TCReference bufs) {
      super(new SessionID(0), monitor, new TCByteBufferOutputStream(), null, TCMessageType.PING_MESSAGE);
      initialize(nextSequence, bufs);
    }

    private void initialize(long nextSequence, TCReference bufs) {
      Objects.requireNonNull(bufs);
      this.seqID = nextSequence;
      this.data = bufs;
    }
    
    private TCActionNetworkMessage getNetworkMessage() {
      return convertToNetworkMessage();
    }

    @Override
    public TCReference getDataBuffers() {
      dehydrateValues();
      TCByteBufferOutputStream out = getOutputStream();
      final TCReference refdata = out.accessBuffers();

      return refdata;
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
          int len = getInputStream().available();
          TCByteBufferOutputStream out = new TCByteBufferOutputStream();
          while (getInputStream().available() > 0) {
            int c = getInputStream().read();
            if (c >= 0) {
              out.write(c);
            }
          }
          out.close();
          data = out.accessBuffers();
          return true;
        default:
          throw new AssertionError("unknown type in message");
      }
    }
  }

  class ClientWPMGSink implements WireProtocolMessageSink {
    @Override
    public void putMessage(WireProtocolMessage message) {

      rcvdMessagesTotalLength.addAndGet(message.getDataLength());
      System.out.println("XXX Client rcvd msgs " + rcvdMessagesTotalLength);

    }

  }

  class ServerWPMGSink implements WireProtocolMessageSink {
    // private volatile boolean senderStarted = false;
    // private volatile TCConnection serverconn;

    @Override
    public void putMessage(WireProtocolMessage message) {

      synchronized (rcvdMessages2TotalLength) {
        rcvdMessages2TotalLength.addAndGet(message.getDataLength());
        rcvdMessages2TotalLength.notify();
      }

      System.out.println("XXX Server rcvd msgs " + rcvdMessages2TotalLength);

    }
  }
}
