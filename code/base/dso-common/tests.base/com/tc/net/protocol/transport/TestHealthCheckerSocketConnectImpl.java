/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;

public class TestHealthCheckerSocketConnectImpl extends HealthCheckerSocketConnectImpl {

  public TestHealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn, String remoteNodeDesc,
                                            TCLogger logger, int timeoutInterval) {
    super(peerNode, conn, remoteNodeDesc, logger, timeoutInterval);
  }

  public synchronized void closeEvent(TCConnectionEvent event) {
    //
  }

  public synchronized void connectEvent(TCConnectionEvent event) {
    // ignore the connect events
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

}
