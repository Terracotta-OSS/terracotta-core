/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
    final boolean msgDone = this.processIncomingData(source, data, length);

    if (msgDone) {
      try {
        WireProtocolMessage msg = getWireProtocolMessage();
        // TODO: validate the src/dest IP and port in header against the connection it came in on

        if (logger.isDebugEnabled()) {
          logger.debug("\nRECEIVE\n" + msg.toString());
        }

        sink.putMessage(msg);
      } finally {
        init();
      }
    }

    return;
  }

  protected AbstractTCNetworkHeader getNewProtocolHeader() {
    return new WireProtocolHeader();
  }

  private WireProtocolMessage getWireProtocolMessage() {
    WireProtocolMessage rv = (WireProtocolMessage) collectMessage();
    return rv;
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

    if (wph.isTransportHandshakeMessage()) {
      rv = new TransportHandshakeMessageImpl(source, wph, data);
    } else {
      rv = new WireProtocolMessageImpl(source, wph, data);
    }

    return rv;
  }
}