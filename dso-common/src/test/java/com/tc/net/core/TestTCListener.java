/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
