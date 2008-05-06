/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.AbstractTCNetworkMessage;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;

/**
 * Wire protocol message. All network communications in the TC world are conducted with wire protocol messages. Wire
 * protocol is a lot like TCP/IP in the sense that is meant to carry other "application" level protocols on top/within
 * it
 * 
 * @author teck
 */
public class WireProtocolMessageImpl extends AbstractTCNetworkMessage implements WireProtocolMessage {
  private final TCConnection sourceConnection;

  /**
   * Wrap the given network message with a wire protocol message instance. The header for the returned instance will
   * have it's total length
   * 
   * @param msgPayload the network message to wrap
   * @return a new wire protocol message instance that contains the given message as it's payload.
   */
  public static WireProtocolMessage wrapMessage(TCNetworkMessage msgPayload, TCConnection source) {
    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.getProtocolForMessageClass(msgPayload));

    // seal the message if necessary
    if (!msgPayload.isSealed()) {
      msgPayload.seal();
    }

    WireProtocolMessage rv = new WireProtocolMessageImpl(source, header, msgPayload);
    return rv;
  }

  protected WireProtocolMessageImpl(TCConnection source, TCNetworkHeader header, TCByteBuffer[] data) {
    super(header, data);
    recordLength();
    this.sourceConnection = source;
  }

  private WireProtocolMessageImpl(TCConnection source, TCNetworkHeader header, TCNetworkMessage subMessage) {
    super(header, subMessage);
    recordLength();
    this.sourceConnection = source;
  }
  
  public void doRecycleOnWrite() {
    getWireProtocolHeader().recycle();
    AbstractTCNetworkMessage messagePayLoad = (AbstractTCNetworkMessage) getMessagePayload();
    if(messagePayLoad != null) {
      messagePayLoad.doRecycleOnWrite();
    }
  }

  public short getMessageProtocol() {
    return ((WireProtocolHeader) getHeader()).getProtocol();
  }

  public WireProtocolHeader getWireProtocolHeader() {
    return ((WireProtocolHeader) getHeader());
  }

  public TCConnection getSource() {
    return sourceConnection;
  }

  protected void recordLength() {
    TCNetworkMessage msgPayload = getMessagePayload();
    // if the payload is null, then we need to record our own length as the packet length. Otherwise, we need to add the
    // our header length + the length of the our payload message length.
    int packetLength = msgPayload == null ? getTotalLength() : getHeader().getHeaderByteLength()
                                                               + msgPayload.getTotalLength();

    ((WireProtocolHeader) getHeader()).setTotalPacketLength(packetLength);
  }

}