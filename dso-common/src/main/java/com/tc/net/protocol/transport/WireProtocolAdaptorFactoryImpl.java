/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.async.api.Sink;
import com.tc.net.protocol.ProtocolSwitch;
import com.tc.net.protocol.TCProtocolAdaptor;

public class WireProtocolAdaptorFactoryImpl implements WireProtocolAdaptorFactory {

  private final Sink httpSink;

  // This version is for the server and will use the HTTP protocol switcher thingy
  public WireProtocolAdaptorFactoryImpl(Sink httpSink) {
    this.httpSink = httpSink;
  }

  public WireProtocolAdaptorFactoryImpl() {
    this(null);
  }

  public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink) {
    if (httpSink != null) { return new ProtocolSwitch(new WireProtocolAdaptorImpl(sink), httpSink); }
    return new WireProtocolAdaptorImpl(sink);
  }
}
