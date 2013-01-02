/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCListenerEventListener;

import java.net.InetAddress;

public class TestTCListener implements TCListener {

  @Override
  public void addEventListener(TCListenerEventListener lsnr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InetAddress getBindAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getBindPort() {
    return 0;
  }

  @Override
  public TCSocketAddress getBindSocketAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isStopped() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEventListener(TCListenerEventListener lsnr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stop() {
    return;
  }

  @Override
  public void stop(long timeout) {
    return;
  }

}
