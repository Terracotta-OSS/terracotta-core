/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.GenericNetworkMessage;
import com.tc.net.protocol.GenericNetworkMessageSink;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.util.HexDump;

public class Verifier implements GenericNetworkMessageSink {
  private final int clientNum;
  private int       counter  = 0;
  private int       sequence = 0;

  public Verifier(int clientNum) {
    this.clientNum = clientNum;
  }

  public int getClientNum() {
    return this.clientNum;
  }

  public void putMessage(GenericNetworkMessage msg) {
    final int cn = msg.getClientNum();
    if (cn != clientNum) {
      headerError("unexpected client number " + cn + ", expecting " + clientNum, msg);
    }

    final int seq = msg.getSequence();
    if (seq != sequence) {
      headerError("unexpected sequence number " + seq + ", expecting sequence " + sequence, msg);
    }
    sequence++;

    verifyMessage(msg);
  }

  private void verifyMessage(GenericNetworkMessage msg) {
    TCByteBuffer data[] = msg.getPayload();
    for (int i = 0; i < data.length; i++) {
      TCByteBuffer buf = data[i].duplicate();

      while (buf.hasRemaining()) {
        final int num = buf.getInt();
        if (num != clientNum) {
          dataError("unexpected client number " + num + ", expecting " + clientNum, buf, buf.position() - 4, i,
                    data.length);
        }

        final int cnt = buf.getInt();
        if (cnt != counter) {
          dataError("unexpected counter value " + cnt + ", expecting " + counter, buf, buf.position() - 4, i,
                    data.length);
        }
        counter++;
      }
    }
  }

  private void dataError(String error, TCByteBuffer buf, int position, int numBuf, int numBufs) {
    error += "\n";
    error += "Message " + sequence + ", Buffer " + (numBuf + 1) + " of " + numBufs + " at position 0x"
             + Integer.toHexString(position).toUpperCase();
    error += " " + HexDump.dump(buf.array(), buf.arrayOffset(), buf.limit());
    throw new RuntimeException(error);
  }

  private void headerError(String errorMsg, GenericNetworkMessage msg) {
    TCNetworkHeader hdr = msg.getHeader();
    TCByteBuffer hdrData = hdr.getDataBuffer();
    throw new RuntimeException(errorMsg + "\n"
                               + HexDump.dump(hdrData.array(), hdrData.arrayOffset(), hdr.getHeaderByteLength()));
  }
}