/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import org.apache.commons.io.CopyUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.core.Verifier;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Silly little sink that just echoes any messages it receives back to the client
 * 
 * @author teck
 */
public class EchoSink implements GenericNetworkMessageSink, TCConnectionEventListener {

  private final Map states = new HashMap();

  public interface ErrorListener {
    void error(Throwable t);
  }

  private final ErrorListener        listener;
  private final boolean              verify;

  private static final ErrorListener defaultListener = new ErrorListener() {
                                                       public void error(Throwable t) {
                                                         t.printStackTrace();
                                                       }
                                                     };

  public EchoSink() {
    this(false);
  }

  public EchoSink(boolean verify) {
    this(verify, defaultListener);
  }

  public EchoSink(boolean verify, ErrorListener listener) {
    this.verify = verify;
    this.listener = listener;
  }

  public void putMessage(GenericNetworkMessage msg) {
    try {
      putMessage0(msg);
    } catch (Throwable t) {
      listener.error(t);
    }
  }

  public void putMessage0(GenericNetworkMessage msg) throws IOException {
    final TCConnection source = msg.getSource();

    if (verify) {
      verifyIncomingMessage(source, msg);
    }

    // copy the message and send it right back to the client
    TCByteBuffer[] recvData = msg.getPayload();
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    TCByteBufferInputStream in = new TCByteBufferInputStream(recvData);

    final int bytesCopied = CopyUtils.copy(in, out);
    Assert.assertEquals(bytesCopied, msg.getDataLength());

    GenericNetworkMessage send = new GenericNetworkMessage(source, out.toArray());
    Assert.assertEquals(msg.getDataLength(), send.getDataLength());
    send.setSequence(msg.getSequence());
    send.setClientNum(msg.getClientNum());

    if (verify) {
      compareData(msg.getEntireMessageData(), send.getEntireMessageData());
    }

    source.putMessage(send);
  }

  static void compareData(TCByteBuffer[] in, TCByteBuffer[] out) {
    TCByteBufferInputStream ins = new TCByteBufferInputStream(in);
    TCByteBufferInputStream outs = new TCByteBufferInputStream(out);

    final int numBytes = ins.available();
    if (numBytes != outs.available()) { throw new RuntimeException("different data lengths: " + numBytes + " vs "
                                                                   + outs.available()); }

    for (int i = 0; i < numBytes; i++) {
      final int inByte = ins.read();
      final int outByte = outs.read();

      if ((inByte == -1) || (outByte == -1)) { throw new RuntimeException("premature EOF in stream"); }

      if (inByte != outByte) { throw new RuntimeException("different byte " + inByte + " != " + outByte); }
    }
  }

  private void verifyIncomingMessage(TCConnection source, GenericNetworkMessage msg) {
    final Verifier verifier;
    synchronized (states) {
      if (!states.containsKey(source)) {
        states.put(source, new Verifier(msg.getClientNum()));
      }
      verifier = (Verifier) states.get(source);
      source.addListener(this);
    }

    verifier.putMessage(msg);
  }

  public void connectEvent(TCConnectionEvent event) {
    //    
  }

  public void closeEvent(TCConnectionEvent event) {
    synchronized (states) {
      states.remove(event.getSource());
    }
  }

  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //    
  }

  public void endOfFileEvent(TCConnectionEvent event) {
    //    
  }
}

