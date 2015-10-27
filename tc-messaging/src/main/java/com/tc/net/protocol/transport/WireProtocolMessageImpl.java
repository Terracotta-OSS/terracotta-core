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
  
  @Override
  public void doRecycleOnWrite() {
    getWireProtocolHeader().recycle();
    AbstractTCNetworkMessage messagePayLoad = (AbstractTCNetworkMessage) getMessagePayload();
    if(messagePayLoad != null) {
      messagePayLoad.doRecycleOnWrite();
    }
  }

  @Override
  public short getMessageProtocol() {
    return ((WireProtocolHeader) getHeader()).getProtocol();
  }

  @Override
  public WireProtocolHeader getWireProtocolHeader() {
    return ((WireProtocolHeader) getHeader());
  }

  @Override
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
