/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;

/**
 * This is not a very intersting interface. It's here to allow testing of the once and only once network stack harness
 * with mock objects. The stack harness needs to treat the OOOP network layer as both a network layer and a transport
 * listener, hence this interface which combines the two.
 */
public interface OnceAndOnlyOnceProtocolNetworkLayer extends NetworkLayer, MessageTransport, MessageTransportListener {
  void start();
  
  void pause();

  void resume();

  void startRestoringConnection();

  void connectionRestoreFailed();

  boolean isClosed();
}
