/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;

import java.util.Timer;

/**
 * This is not a very interesting interface. It's here to allow testing of the once and only once network stack harness
 * with mock objects. The stack harness needs to treat the OOOP network layer as both a network layer and a transport
 * listener, hence this interface which combines the two.
 */
public interface OnceAndOnlyOnceProtocolNetworkLayer extends NetworkLayer, MessageTransport, MessageTransportListener {

  void startRestoringConnection();

  void connectionRestoreFailed();

  Timer getRestoreConnectTimer();

  boolean isClosed();

}
