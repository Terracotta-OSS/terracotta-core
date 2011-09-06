/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.AbstractTCNetworkMessage;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;

import java.util.ArrayList;
import java.util.Iterator;

public class WireProtocolGroupMessageImpl extends AbstractTCNetworkMessage implements WireProtocolGroupMessage {

  private final TCConnection                sourceConnection;
  private final ArrayList<TCNetworkMessage> messagePayloads;

  public static WireProtocolGroupMessageImpl wrapMessages(final ArrayList<TCNetworkMessage> msgPayloads,
                                                          TCConnection source) {
    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_MSGGROUP);
    header.setMessageCount(msgPayloads.size());

    int totalByteBuffers = 0;
    for (int i = 0; i < msgPayloads.size(); i++) {
      totalByteBuffers += msgPayloads.get(i).getEntireMessageData().length;
    }

    TCByteBuffer[] msgs = new TCByteBuffer[msgPayloads.size() + totalByteBuffers];
    int i = 0;
    int copyPos = 0;
    while (i < msgPayloads.size()) {
      TCByteBuffer tcb = TCByteBufferFactory.getInstance(false, (Integer.SIZE + Short.SIZE) / 8);
      tcb.putInt(msgPayloads.get(i).getTotalLength());
      tcb.putShort(WireProtocolHeader.getProtocolForMessageClass(msgPayloads.get(i)));
      tcb.flip();

      msgs[copyPos++] = tcb;
      // referring to the original payload buffers
      for (int j = 0; j < msgPayloads.get(i).getEntireMessageData().length; j++) {
        msgs[copyPos++] = msgPayloads.get(i).getEntireMessageData()[j];
      }
      i++;
    }

    return new WireProtocolGroupMessageImpl(source, header, msgs, msgPayloads);
  }

  // used by the reader
  protected WireProtocolGroupMessageImpl(final TCConnection source, final TCNetworkHeader header,
                                         final TCByteBuffer[] messagePayloadByteBuffers) {
    this(source, header, messagePayloadByteBuffers, null);
  }

  // used by the writer
  protected WireProtocolGroupMessageImpl(final TCConnection source, final TCNetworkHeader header,
                                         final TCByteBuffer[] messagePayloadByteBuffers,
                                         final ArrayList<TCNetworkMessage> messagePayloads) {
    super(header, messagePayloadByteBuffers);
    this.sourceConnection = source;
    this.messagePayloads = (messagePayloads != null ? messagePayloads
        : getMessagesFromByteBuffers(messagePayloadByteBuffers));
    recordLength();
  }

  private ArrayList<TCNetworkMessage> getMessagesFromByteBuffers(final TCByteBuffer[] messagePayloadByteBuffers) {
    ArrayList<TCNetworkMessage> messages = new ArrayList<TCNetworkMessage>();

    // XXX: should do without copying stuffs around by passing views to upper layers.
    // Recycle is little tricky though.
    byte[] fullMsgsBytes;
    TCByteBuffer[] msgs = messagePayloadByteBuffers;

    if (msgs.length > 1) {
      fullMsgsBytes = new byte[((WireProtocolHeader) getHeader()).getTotalPacketLength()
                               - ((WireProtocolHeader) getHeader()).getHeaderByteLength()];
      int copyPos = 0;
      for (int i = 0; i < msgs.length; i++) {
        System.arraycopy(msgs[i].array(), 0, fullMsgsBytes, copyPos, msgs[i].limit());
        copyPos += msgs[i].limit();
      }
    } else {
      fullMsgsBytes = msgs[0].array();
    }

    TCByteBuffer b = TCByteBufferFactory.wrap(fullMsgsBytes);
    for (int i = 0; i < getTotalMessageCount(); i++) {
      int msgLen = b.getInt();
      short msgProto = b.getShort();

      // XXX: we are giving out 4K BB for a smaller msgs too; Though it is recycled, can do some opti., here
      TCByteBuffer[] bufs = TCByteBufferFactory.getFixedSizedInstancesForLength(false, msgLen);
      for (TCByteBuffer buf : bufs) {
        b.get(buf.array(), 0, buf.limit());
      }

      WireProtocolHeader hdr;
      hdr = (WireProtocolHeader) ((WireProtocolHeader) getHeader()).clone();
      hdr.setTotalPacketLength(hdr.getHeaderByteLength() + msgLen);
      hdr.setProtocol(msgProto);
      hdr.setMessageCount(1);
      hdr.computeChecksum();
      WireProtocolMessage msg = new WireProtocolMessageImpl(this.sourceConnection, hdr, bufs);
      messages.add(msg);
    }
    fullMsgsBytes = null;
    for (TCByteBuffer buf : msgs) {
      buf.recycle();
    }
    return messages;
  }

  public Iterator<TCNetworkMessage> getMessageIterator() {
    return this.messagePayloads.iterator();
  }

  public int getTotalMessageCount() {
    return ((WireProtocolHeader) getHeader()).getMessageCount();
  }

  public TCConnection getSource() {
    return this.sourceConnection;
  }

  public WireProtocolHeader getWireProtocolHeader() {
    return ((WireProtocolHeader) getHeader());
  }

  public short getMessageProtocol() {
    return ((WireProtocolHeader) getHeader()).getProtocol();
  }

  protected void recordLength() {
    TCNetworkMessage msgPayload = getMessagePayload();
    // if the payload is null, then we need to record our own length as the packet length. Otherwise, we need to add the
    // our header length + the length of the our payload message length.
    int packetLength = msgPayload == null ? getTotalLength() : getHeader().getHeaderByteLength()
                                                               + msgPayload.getTotalLength();

    ((WireProtocolHeader) getHeader()).setTotalPacketLength(packetLength);
  }

  @Override
  public void doRecycleOnWrite() {
    getWireProtocolHeader().recycle();
    // recycle individual messages
    for (TCNetworkMessage networkMessage : messagePayloads) {
      ((AbstractTCNetworkMessage) networkMessage).doRecycleOnWrite();
    }
  }

}
