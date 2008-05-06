/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;

public class MockChannel implements Channel {

  private boolean isOpen = true;

  public final synchronized boolean isOpen() {
    return isOpen;
  }

  public final synchronized void close() {
    isOpen = false;
  }

  protected final synchronized void checkOpen() throws IOException {
    if (!isOpen()) { throw new ClosedChannelException(); }
  }
}
