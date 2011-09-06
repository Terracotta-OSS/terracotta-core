/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;

/**
 * A generic protocol adaptor (only useful for testing)
 * 
 * @author teck
 */
public class GenericProtocolAdaptor extends AbstractTCProtocolAdaptor {
  private final static TCLogger           logger = TCLogging.getLogger(GenericProtocolAdaptor.class);
  private final GenericNetworkMessageSink sink;

  public GenericProtocolAdaptor(GenericNetworkMessageSink sink) {
    super(logger);
    this.sink = sink;
  }

  protected TCNetworkMessage createMessage(TCConnection conn, TCNetworkHeader hdr, TCByteBuffer[] data) {
    GenericNetworkMessage rv = new GenericNetworkMessage(conn, hdr, data);
    return rv;
  }

  protected AbstractTCNetworkHeader getNewProtocolHeader() {
    return new GenericNetworkHeader();
  }

  protected int computeDataLength(TCNetworkHeader hdr) {
    return ((GenericNetworkHeader) hdr).getMessageDataLength();
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    GenericNetworkMessage msg = (GenericNetworkMessage) processIncomingData(source, data, length);

    if (msg != null) {
      init();
      sink.putMessage(msg);
    }

    return;
  }
}