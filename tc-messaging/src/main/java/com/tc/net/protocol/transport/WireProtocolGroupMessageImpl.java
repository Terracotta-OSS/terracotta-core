/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

/**
 * This class models the payload portion of a TC wire protocol message. Not Thread Safe!!
 * 
 * <pre>
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                       WireProtocolHeader                      | 
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                       Message 1 Length                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |       Message 1 Protocol      |       Message 1 data        ...
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                       Message 2 Length                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |       Message 2 Protocol      |       Message 2 data        ...
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        .                               .                               .   
 *        .                               .                               . 
 *        .                               .                               .
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |                       Message n Length                        |
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *        |       Message n Protocol      |       Message n data        ...        
 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * </pre>
 */

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCNetworkMessageImpl;
import com.tc.net.protocol.TCProtocolException;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WireProtocolGroupMessageImpl extends TCNetworkMessageImpl implements WireProtocolGroupMessage {

  private final TCConnection                sourceConnection;
  private final List<TCActionNetworkMessage> messagePayloads;

  public static WireProtocolGroupMessageImpl wrapMessages(List<TCActionNetworkMessage> msgPayloads,
                                                          TCConnection source) {
    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_MSGGROUP);

    return new WireProtocolGroupMessageImpl(source, header, msgPayloads);
  }

  // used by the reader
  protected WireProtocolGroupMessageImpl(TCConnection source, WireProtocolHeader header,
                                         TCReference messagePayloadByteBuffers) {
    super(header);
    setPayload(messagePayloadByteBuffers);
    this.sourceConnection = source;
    messagePayloads = null;
  }
  // used by the writer
  protected WireProtocolGroupMessageImpl(TCConnection source, WireProtocolHeader header,
                                         List<TCActionNetworkMessage> messagePayloads) {
    super(header);
    this.sourceConnection = source;
    this.messagePayloads = messagePayloads;
  }

  @Override
  public boolean prepareToSend() {
    setPayload(generatePayload());
    getWireProtocolHeader().setMessageCount(messagePayloads.size());
    getWireProtocolHeader().finalizeHeader(getTotalLength());
    return getWireProtocolHeader().getMessageCount() > 0;
  }
  
  private TCReference generatePayload() {
    List<TCReference> msgs = new ArrayList<>(messagePayloads.size() * 2);
    Iterator<TCActionNetworkMessage> msgI = messagePayloads.iterator();
    while (msgI.hasNext()) {
      TCActionNetworkMessage msg = msgI.next();
      if (msg.commit()) {
        TCByteBuffer tcb = TCByteBufferFactory.getInstance((Integer.SIZE + Short.SIZE) / 8);
        tcb.putInt(msg.getTotalLength());
        tcb.putShort(WireProtocolHeader.getProtocolForMessageClass(msg));
        tcb.flip();

        TCReference header = TCReferenceSupport.createGCReference(tcb);
        msgs.add(header);
        // not nescessary because its just GC but do for completness 
        msg.addCompleteCallback(header::close); 
        // referring to the original payload buffers
        msgs.add(msg.getEntireMessageData());
      } else {
        msg.complete();
        msgI.remove();
      }
    }
    return TCReferenceSupport.createAggregateReference(msgs);
  }
  
  private List<TCNetworkMessage> getMessagesFromByteBuffers() throws IOException {
    ArrayList<TCNetworkMessage> messages = new ArrayList<>();

    TCReference src = getPayload();

    try (TCByteBufferInputStream msgs = new TCByteBufferInputStream(src)) {
      for (int i = 0; i < getWireProtocolHeader().getMessageCount(); i++) {
        int msgLen = msgs.readInt();
        short msgProto = msgs.readShort();

        WireProtocolHeader hdr;
        hdr = (WireProtocolHeader) getWireProtocolHeader().clone();
        hdr.setTotalPacketLength(hdr.getHeaderByteLength() + msgLen);
        hdr.setProtocol(msgProto);
        hdr.setMessageCount(1);
        hdr.computeChecksum();
        WireProtocolMessage msg = new WireProtocolMessageImpl(this.sourceConnection, hdr, msgs.readReference(msgLen));
        messages.add(msg);
      }
    }

    return messages;
  }

  @Override
  public Iterator<TCNetworkMessage> getMessageIterator() throws TCProtocolException {
    try {
      return getMessagesFromByteBuffers().iterator();
    } catch (IOException ioe) {
      throw new TCProtocolException(ioe);
    }
  }

  @Override
  public int getTotalMessageCount() {
    return ((WireProtocolHeader) getHeader()).getMessageCount();
  }

  @Override
  public TCConnection getSource() {
    return this.sourceConnection;
  }

  @Override
  public WireProtocolHeader getWireProtocolHeader() {
    return ((WireProtocolHeader) getHeader());
  }

  @Override
  public short getMessageProtocol() {
    return ((WireProtocolHeader) getHeader()).getProtocol();
  }

  @Override
  public void complete() {
    if (this.messagePayloads != null) {
      this.messagePayloads.forEach(TCNetworkMessage::complete);
    }
    super.complete();
  }

  @Override
  public boolean isValid() {
    return !this.messagePayloads.stream().allMatch(TCActionNetworkMessage::isCancelled);
  }
}
