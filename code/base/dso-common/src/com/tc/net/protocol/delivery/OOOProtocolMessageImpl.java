/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkMessage;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolException;
import com.tc.net.protocol.delivery.OOOProtocolMessageHeader.ProtocolMessageHeaderFactory;
import com.tc.util.Assert;

class OOOProtocolMessageImpl extends AbstractTCNetworkMessage implements OOOProtocolMessage {

  /**
   * Create a header-only message (no payload). Useful for ack and ack request messages.
   */
  private OOOProtocolMessageImpl(OOOProtocolMessageHeader header) {
    super(header);
  }

  /**
   * Create a message with the given payload from the network. Useful for propogating messages up the network stack.
   */
  private OOOProtocolMessageImpl(OOOProtocolMessageHeader header, TCByteBuffer[] payload) {
    super(header, payload);
  }

  /**
   * Create a message with the given TCNetworkMessage payload. Useful for propogating messages down the network stack.
   */
  private OOOProtocolMessageImpl(OOOProtocolMessageHeader header, TCNetworkMessage msgPayload) {
    super(header, msgPayload);
  }

  private OOOProtocolMessageHeader getOOOPHeader() {
    return (OOOProtocolMessageHeader) getHeader();
  }

  public long getAckSequence() {
    return getOOOPHeader().getSequence();
  }

  public long getSent() {
    return getOOOPHeader().getSequence();
  }

  public boolean isAckRequest() {
    return getOOOPHeader().isAckRequest();
  }

  public boolean isSend() {
    return getOOOPHeader().isSend();
  }

  public boolean isAck() {
    return getOOOPHeader().isAck();
  }

  public boolean isGoodbye() {
    return getOOOPHeader().isGoodbye();
  }

  public void doRecycleOnWrite() {
    // we are disabling this because on ooo layer knows when it's safe to recycle the message
  }

  public void reallyDoRecycleOnWrite() {
    getOOOPHeader().recycle();
    AbstractTCNetworkMessage messagePayLoad = (AbstractTCNetworkMessage) getMessagePayload();
    if(messagePayLoad != null) {
      messagePayLoad.doRecycleOnWrite();
    }
  }

  public static class ProtocolMessageParserImpl implements OOOProtocolMessageParser {
    private final ProtocolMessageHeaderFactory headerFactory;
    private final OOOProtocolMessageFactory    messageFactory;

    public ProtocolMessageParserImpl(ProtocolMessageHeaderFactory headerFactory,
                                     OOOProtocolMessageFactory messageFactory) {
      this.headerFactory = headerFactory;
      this.messageFactory = messageFactory;
    }

    public OOOProtocolMessage parseMessage(TCByteBuffer[] data) throws TCProtocolException {
      int hdrLength = OOOProtocolMessageHeader.HEADER_LENGTH;
      if (hdrLength > data[0].limit()) { throw new TCProtocolException("header not contained in first buffer: "
                                                                       + hdrLength + " > " + data[0].limit()); }

      OOOProtocolMessageHeader header = headerFactory.createNewHeader(data[0]);
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

  public static class ProtocolMessageFactoryImpl implements OOOProtocolMessageFactory {

    private final ProtocolMessageHeaderFactory headerFactory;

    public ProtocolMessageFactoryImpl(ProtocolMessageHeaderFactory headerFactory) {
      this.headerFactory = headerFactory;
    }

    public OOOProtocolMessage createNewAckRequestMessage() {
      return new OOOProtocolMessageImpl(headerFactory.createNewAckRequest());
    }

    public OOOProtocolMessage createNewAckMessage(long sequence) {
      return new OOOProtocolMessageImpl(headerFactory.createNewAck(sequence));
    }

    public OOOProtocolMessage createNewSendMessage(long sequence, TCNetworkMessage payload) {
      return new OOOProtocolMessageImpl(headerFactory.createNewSend(sequence), payload);
    }

    public OOOProtocolMessage createNewMessage(OOOProtocolMessageHeader header, TCByteBuffer[] data) {
      return new OOOProtocolMessageImpl(header, data);
    }

    public OOOProtocolMessage createNewGoodbyeMessage() {
      return new OOOProtocolMessageImpl(headerFactory.createNewGoodbye());
    }
  }

}