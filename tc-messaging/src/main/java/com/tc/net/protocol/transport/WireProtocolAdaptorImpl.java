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

  @Override
  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    final WireProtocolMessage msg;
    try {
      msg = (WireProtocolMessage) this.processIncomingData(source, data, length);
    } catch (TCProtocolException e) {
      init();
      throw e;
    }
    if (msg != null) {
      init();
      if (logger.isDebugEnabled()) {
        logger.debug("\nRECEIVE\n" + msg.toString());
      }
      if (msg.getWireProtocolHeader().isMessagesGrouped()) {
        WireProtocolGroupMessage wpmg = (WireProtocolGroupMessage) msg;
        int msgCount = wpmg.getWireProtocolHeader().getMessageCount();
        Assert.eval(msgCount > 1);

        for (Iterator<TCNetworkMessage> i = wpmg.getMessageIterator(); i.hasNext();) {
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

  @Override
  protected AbstractTCNetworkHeader getNewProtocolHeader() {
    return new WireProtocolHeader();
  }

  @Override
  protected int computeDataLength(TCNetworkHeader header) {
    WireProtocolHeader wph = (WireProtocolHeader) header;
    return wph.getTotalPacketLength() - wph.getHeaderByteLength();
  }

  @Override
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
