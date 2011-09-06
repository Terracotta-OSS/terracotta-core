/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCListenerEventListener;

import java.net.InetAddress;

public class TestTCListener implements TCListener {

  public void addEventListener(TCListenerEventListener lsnr) {
    throw new UnsupportedOperationException();
  }

  public InetAddress getBindAddress() {
    throw new UnsupportedOperationException();
  }

  public int getBindPort() {
    return 0;
  }

  public TCSocketAddress getBindSocketAddress() {
    throw new UnsupportedOperationException();
  }

  public boolean isStopped() {
    throw new UnsupportedOperationException();
  }

  public void removeEventListener(TCListenerEventListener lsnr) {
    throw new UnsupportedOperationException();
  }

  public void stop() {
    return;
  }

  public void stop(long timeout) {
    return;
  }

}
