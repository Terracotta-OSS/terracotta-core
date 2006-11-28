/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolAdaptor;

public interface WireProtocolAdaptorFactory {
  public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink);
}
