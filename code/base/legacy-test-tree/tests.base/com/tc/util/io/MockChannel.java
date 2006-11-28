/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
