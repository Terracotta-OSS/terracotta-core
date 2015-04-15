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

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.GenericNetworkMessage;
import com.tc.net.protocol.GenericNetworkMessageSink;
import com.tc.net.protocol.GenericProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author teck
 */
public class VerifierClient implements Runnable {
  private static final int          TIMEOUT       = 60000 * 2;
  private static int                clientCounter = 0;

  private final TCConnectionManager connMgr;
  private final TCSocketAddress     addr;
  private final int                 clientNum;
  private final int                 dataSize;
  private final boolean             addExtra;
  private final int                 numToSend;
  private final int                 maxDelay;
  private final int                 minDelay;
  private final AtomicReference<Throwable> error         = new AtomicReference<Throwable>(null);
  private final Random              random;
  private final Verifier            verifier;
  private final Verifier            sendVerifier;
  private int                       sendSequence  = 0;
  private int                       sendCounter   = 0;
  private int                       numToRecv;

  public VerifierClient(TCConnectionManager connMgr, TCSocketAddress addr, int dataSize, boolean addExtra,
                        int numToSend, int minDelay, int maxDelay) {

    if ((maxDelay < 0) || (minDelay < 0)) {
      // make formatter sane
      throw new IllegalArgumentException("delay values must be greater than or equal to zero");
    }

    if (maxDelay < minDelay) { throw new IllegalArgumentException("max cannot be less than min"); }

    this.clientNum = getNextClientNum();
    this.connMgr = connMgr;
    this.addr = addr;
    this.dataSize = dataSize;
    this.addExtra = addExtra;
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.numToSend = numToSend;
    this.random = new Random();
    this.verifier = new Verifier(this.clientNum);
    this.sendVerifier = new Verifier(this.clientNum);
    this.numToRecv = numToSend;
  }

  private static synchronized int getNextClientNum() {
    return ++clientCounter;
  }

  private void delay() {
    final int range = minDelay - maxDelay;
    if (range > 0) {
      final long sleepFor = minDelay + random.nextInt(maxDelay - minDelay);
      ThreadUtil.reallySleep(sleepFor);
    }
  }

  private class Sink implements GenericNetworkMessageSink {
    @Override
    public void putMessage(GenericNetworkMessage msg) {
      try {
        verifier.putMessage(msg);
      } catch (Throwable t) {
        setError(t);
      } finally {
        msgRecv();
      }
    }
  }

  private void msgRecv() {
    synchronized (this) {
      numToRecv--;
      notify();
    }
  }

  private void setError(Throwable t) {
    t.printStackTrace();
    error.set(t);
  }

  @Override
  public void run() {
    try {
      run0();
    } catch (Throwable t) {
      setError(t);
    } finally {
      checkForError();
    }
  }

  public void run0() throws Throwable {
    final HashMap sentCallbacks = new HashMap();
    final TCConnection conn = connMgr.createConnection(new GenericProtocolAdaptor(new Sink()));
    conn.connect(addr, TIMEOUT);

    for (int i = 0; i < numToSend; i++) {
      checkForError();
      delay();

      final GenericNetworkMessage msg = makeNextMessage(conn);
      sendVerifier.putMessage(msg);

      synchronized (sentCallbacks) {
        Object o = sentCallbacks.put(msg, new SetOnceFlag());
        Assert.eval("There is a msg already in map; old = " + o + "; new = " + msg, (o == null));
      }

      msg.setSentCallback(new Runnable() {
        @Override
        public void run() {
          synchronized (sentCallbacks) {
            ((SetOnceFlag) sentCallbacks.get(msg)).set();
            sentCallbacks.notify();
          }
        }
      });

      conn.putMessage(msg);
    }

    checkForError();

    synchronized (this) {
      while (numToRecv > 0) {
        wait();
      }
    }

    checkForError();

    conn.close(TIMEOUT);

    // make sure that the sent callback was called once and only once for each message
    synchronized (sentCallbacks) {
      for (final Iterator iter = sentCallbacks.values().iterator(); iter.hasNext();) {
        SetOnceFlag sent = (SetOnceFlag) iter.next();
        int count = 0;
        while (!sent.isSet()) {
          count++;
          System.out.println("XXX waiting for sent callback to be set " + iter);
          if (count % 36 == 0) {
            System.out.println("thread dump :" + ThreadDumpUtil.getThreadDump());
            Assert.eval("XXX One of the sentCallback not set for long time", false);
          }
          sentCallbacks.wait();
        }
        iter.remove();
      }
    }

    checkForError();
  }

  private GenericNetworkMessage makeNextMessage(TCConnection conn) {
    // must use a multiple of 8 for the data in this message. Data is <id><counter><id><counter>....where id and
    // counter are both 4 byte ints
    int extra = 8 + (8 * random.nextInt(13));
    TCByteBuffer data[] = TCByteBufferFactory.getFixedSizedInstancesForLength(false, 4096
                                                                                     * dataSize
                                                                                     + (this.addExtra == true ? extra
                                                                                         : 0));

    if (this.dataSize == 0 && this.addExtra) {
      Assert.assertEquals(1, data.length);
    }

    for (TCByteBuffer buf : data) {
      Assert.eval((buf.limit() % 8) == 0);

      while (buf.hasRemaining()) {
        buf.putInt(clientNum);
        buf.putInt(sendCounter++);
      }

      buf.flip();
    }

    GenericNetworkMessage msg = new GenericNetworkMessage(conn, data);
    msg.setSequence(sendSequence++);
    msg.setClientNum(this.clientNum);
    return msg;
  }

  private void checkForError() {
    final Throwable t = error.get();
    if (t != null) { throw new RuntimeException(t); }
  }
}
