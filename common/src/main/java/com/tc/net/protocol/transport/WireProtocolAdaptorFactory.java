/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolAdaptor;

public interface WireProtocolAdaptorFactory {
  public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink);
}
