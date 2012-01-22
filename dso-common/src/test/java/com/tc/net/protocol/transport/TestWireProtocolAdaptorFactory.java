/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public class TestWireProtocolAdaptorFactory implements WireProtocolAdaptorFactory {
  public final NoExceptionLinkedQueue newWireProtocolAdaptorCalls = new NoExceptionLinkedQueue();

  public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink) {
    WireProtocolAdaptor rv = new WireProtocolAdaptorImpl(sink);
    newWireProtocolAdaptorCalls.put(sink);
    return rv;
  }

}
