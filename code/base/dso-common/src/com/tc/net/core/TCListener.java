/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.util.TCTimeoutException;

import java.net.InetAddress;

/**
 * A handle to a network listening port
 * 
 * @author teck
 */
public interface TCListener {
  public void stop();

  public void stop(long timeout) throws TCTimeoutException;

  public int getBindPort();

  public InetAddress getBindAddress();

  public TCSocketAddress getBindSocketAddress();

  public void addEventListener(TCListenerEventListener lsnr);

  public void removeEventListener(TCListenerEventListener lsnr);

  public boolean isStopped();
}