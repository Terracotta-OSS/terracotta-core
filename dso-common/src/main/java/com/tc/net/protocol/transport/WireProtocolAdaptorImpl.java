/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.AbstractTCNetworkHeader;
import com.tc.net.protocol.AbstractTCProtocolAdaptor;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.Assert;

import java.util.Iterator;

/**
 * Connection adaptor to parse wire protocol messages
 * 
 * @author teck
 */
public class WireProtocolAdaptorImpl extends AbstractTCProtocolAdaptor implements WireProtocolAdaptor {
  private static final TCLogger         logger = TCLogging.getLogger(WireProtocolAdaptorImpl.class);
  private final WireProtocolMessageSink sink;

  protected WireProtocolAdaptorImpl(WireProtocolMessageSink sink) {
    super(logger);
    this.sink = sink;
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    final WireProtocolMessage msg = (WireProtocolMessage) this.processIncomingData(source, data, length);

    if (msg != null) {
      init();
      if (logger.isDebugEnabled()) {
        logger.debug("\nRECEIVE\n" + msg.toString());
      }
      if (msg.getWireProtocolHeader().isMessagesGrouped()) {
        WireProtocolGroupMessage wpmg = (WireProtocolGroupMessage) msg;
        int msgCount = wpmg.getWireProtocolHeader().getMessageCount();
        Assert.eval(msgCount > 1);

        for (Iterator i = wpmg.getMessageIterator(); i.hasNext();) {
          WireProtocolMessage wpm = (WireProtocolMessage) i.next();
          sink.putMessage(wpm);
        }
        msg.getWireProtocolHeader().recycle();
        // Individual messages are recycled on top layers only.
      } else {
        sink.putMessage(msg);
      }
    }
    return;
  }

  protected AbstractTCNetworkHeader getNewProtocolHeader() {
    return new WireProtocolHeader();
  }

  protected int computeDataLength(TCNetworkHeader header) {
    WireProtocolHeader wph = (WireProtocolHeader) header;
    return wph.getTotalPacketLength() - wph.getHeaderByteLength();
  }

  protected TCNetworkMessage createMessage(TCConnection source, TCNetworkHeader hdr, TCByteBuffer[] data)
      throws TCProtocolException {
    if (data == null) { throw new TCProtocolException("Wire protocol messages must have a payload"); }
    WireProtocolHeader wph = (WireProtocolHeader) hdr;
    final WireProtocolMessage rv;

    if (wph.isHandshakeOrHealthCheckMessage()) {
      rv = new TransportMessageImpl(source, wph, data);
    } else {
      if (wph.getMessageCount() == 1) {
        rv = new WireProtocolMessageImpl(source, wph, data);
      } else {
        rv = new WireProtocolGroupMessageImpl(source, wph, data);
      }
    }

    return rv;
  }
}