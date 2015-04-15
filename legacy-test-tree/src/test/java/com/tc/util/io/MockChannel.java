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
package com.tc.util.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;

public class MockChannel implements Channel {

  private boolean isOpen = true;

  @Override
  public final synchronized boolean isOpen() {
    return isOpen;
  }

  @Override
  public final synchronized void close() {
    isOpen = false;
  }

  protected final synchronized void checkOpen() throws IOException {
    if (!isOpen()) { throw new ClosedChannelException(); }
  }
}
