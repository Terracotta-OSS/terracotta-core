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
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCNetworkMessageImpl;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class WireProtocolGroupMessageImpl extends TCNetworkMessageImpl implements WireProtocolGroupMessage {

  private final TCConnection                sourceConnection;
  private final List<TCNetworkMessage> messagePayloads;

  public static WireProtocolGroupMessageImpl wrapMessages(List<TCNetworkMessage> msgPayloads,
                                                          TCConnection source) {
    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_MSGGROUP);

    return new WireProtocolGroupMessageImpl(source, header, msgPayloads);
  }

  // used by the reader
  protected WireProtocolGroupMessageImpl(TCConnection source, WireProtocolHeader header,
                                         TCByteBuffer[] messagePayloadByteBuffers) {
    super(header, messagePayloadByteBuffers);
    this.sourceConnection = source;
    messagePayloads = getMessagesFromByteBuffers();
  }
  // used by the writer
  protected WireProtocolGroupMessageImpl(TCConnection source, WireProtocolHeader header,
                                         List<TCNetworkMessage> messagePayloads) {
    super(header, ()->generatePayload(messagePayloads));
    this.sourceConnection = source;
    this.messagePayloads = messagePayloads;
  }

  @Override
  public boolean commit() {
    // package bytes with super.load
    if (super.commit() && iterate(TCNetworkMessage::commit) && super.load()) {
      getWireProtocolHeader().setMessageCount(messagePayloads.size());
      getWireProtocolHeader().finalizeHeader(getTotalLength());
      return getWireProtocolHeader().getMessageCount() > 0;
    } else {
      return false;
    }
  }

  @Override
  public boolean load() {
    // load will happen at commit time
    return true;
  }

  private boolean iterate(Predicate<TCNetworkMessage> check) {
    Iterator<TCNetworkMessage> msg = getMessageIterator();
    while (msg.hasNext()) {
      if (!check.test(msg.next())) {
        msg.remove();
      }
    }
    return !messagePayloads.isEmpty();
  }
  
  private static TCByteBuffer[] generatePayload(List<TCNetworkMessage> messagePayloads) {
    List<TCByteBuffer> msgs = new ArrayList<>(messagePayloads.size() * 2);
    Iterator<TCNetworkMessage> msgI = messagePayloads.iterator();
    while (msgI.hasNext()) {
      TCNetworkMessage msg = msgI.next();
      if (msg.commit()) {
        TCByteBuffer tcb = TCByteBufferFactory.getInstance((Integer.SIZE + Short.SIZE) / 8);
        tcb.putInt(msg.getTotalLength());
        tcb.putShort(WireProtocolHeader.getProtocolForMessageClass(msg));
        tcb.flip();

        msgs.add(tcb);
        // referring to the original payload buffers
        msgs.addAll(Arrays.asList(msg.getEntireMessageData()));
      } else {
        msg.complete();
        msgI.remove();
      }
    }
    return msgs.toArray(new TCByteBuffer[msgs.size()]);
  }
  
  private List<TCNetworkMessage> getMessagesFromByteBuffers() {
    ArrayList<TCNetworkMessage> messages = new ArrayList<>();

    TCByteBufferInputStream msgs = new TCByteBufferInputStream(getPayload());

    for (int i = 0; i < getWireProtocolHeader().getMessageCount(); i++) {
      try {
        int msgLen = msgs.readInt();
        short msgProto = msgs.readShort();
        
        WireProtocolHeader hdr;
        hdr = (WireProtocolHeader) getWireProtocolHeader().clone();
        hdr.setTotalPacketLength(hdr.getHeaderByteLength() + msgLen);
        hdr.setProtocol(msgProto);
        hdr.setMessageCount(1);
        hdr.computeChecksum();
        WireProtocolMessage msg = new WireProtocolMessageImpl(this.sourceConnection, hdr, new TCByteBuffer[] {msgs.read(msgLen)});
        messages.add(msg);
      } catch (IOException ioe) {
        
      }
    }
    return messages;
  }

  @Override
  public Iterator<TCNetworkMessage> getMessageIterator() {
    return this.messagePayloads.iterator();
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
    this.getMessageIterator().forEachRemaining(TCNetworkMessage::complete);
    super.complete();
  }

  @Override
  public boolean isCancelled() {
    return messagePayloads.stream().allMatch(TCNetworkMessage::isCancelled);
  }
}
