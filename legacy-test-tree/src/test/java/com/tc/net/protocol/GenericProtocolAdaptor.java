/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  protected TCNetworkMessage createMessage(TCConnection conn, TCNetworkHeader hdr, TCByteBuffer[] data) {
    GenericNetworkMessage rv = new GenericNetworkMessage(conn, hdr, data);
    return rv;
  }

  @Override
  protected AbstractTCNetworkHeader getNewProtocolHeader() {
    return new GenericNetworkHeader();
  }

  @Override
  protected int computeDataLength(TCNetworkHeader hdr) {
    return ((GenericNetworkHeader) hdr).getMessageDataLength();
  }

  @Override
  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    GenericNetworkMessage msg = (GenericNetworkMessage) processIncomingData(source, data, length);

    if (msg != null) {
      init();
      sink.putMessage(msg);
    }

    return;
  }
}