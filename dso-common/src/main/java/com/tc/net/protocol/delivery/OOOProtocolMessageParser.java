/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.Assert;

/**
 * Parses incoming network data into ProtocolMessages
 */
class OOOProtocolMessageParser {
  private final OOOProtocolMessageFactory messageFactory;

  public OOOProtocolMessageParser(OOOProtocolMessageFactory messageFactory) {
    this.messageFactory = messageFactory;
  }

  public OOOProtocolMessage parseMessage(TCByteBuffer[] data) throws TCProtocolException {
    int hdrLength = OOOProtocolMessageHeader.HEADER_LENGTH;
    if (hdrLength > data[0].limit()) { throw new TCProtocolException("header not contained in first buffer: "
                                                                     + hdrLength + " > " + data[0].limit()); }

    OOOProtocolMessageHeader header = new OOOProtocolMessageHeader(data[0].duplicate()
        .limit(OOOProtocolMessageHeader.HEADER_LENGTH));
    header.validate();

    TCByteBuffer msgData[];
    if (header.getHeaderByteLength() < data[0].limit()) {
      msgData = new TCByteBuffer[data.length];
      System.arraycopy(data, 0, msgData, 0, msgData.length);

      TCByteBuffer firstPayloadBuffer = msgData[0].duplicate();
      firstPayloadBuffer.position(header.getHeaderByteLength());
      msgData[0] = firstPayloadBuffer.slice();
    } else {
      Assert.eval(data.length >= 1);
      msgData = new TCByteBuffer[data.length - 1];
      System.arraycopy(data, 1, msgData, 0, msgData.length);
    }

    return messageFactory.createNewMessage(header, msgData);
  }

}
