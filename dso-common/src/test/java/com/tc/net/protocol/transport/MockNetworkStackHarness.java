/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkStackHarness;

public class MockNetworkStackHarness implements NetworkStackHarness {
  public boolean wasAttachNewConnectionCalled = false;
  public boolean wasFinalizeStackCalled       = false;

  public MessageTransport attachNewConnection(TCConnection connection) {
    this.wasAttachNewConnectionCalled = true;
    return null;
  }

  public void finalizeStack() {
    this.wasFinalizeStackCalled = true;
  }

}
