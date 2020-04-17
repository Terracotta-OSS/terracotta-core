/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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
