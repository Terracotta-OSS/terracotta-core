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
package com.tc.management.remote.protocol.terracotta;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.MessageConnection;
import javax.management.remote.generic.MessageConnectionServer;

public final class TunnelingMessageConnectionServer implements MessageConnectionServer {

  public static final String    TUNNELING_HANDLER = TunnelingMessageConnectionServer.class.getName()
                                                    + ".tunnelingHandler";

  private final JMXServiceURL   address;
  private TunnelingEventHandler handler;

  TunnelingMessageConnectionServer(final JMXServiceURL address) {
    this.address = address;
  }

  public MessageConnection accept() throws IOException {
    TunnelingEventHandler h;
    synchronized (this) {
      if (handler == null) throw new IOException("Not yet started");
      h = handler;
    }
    return h.accept();
  }

  public JMXServiceURL getAddress() {
    return address;
  }

  public synchronized void start(final Map environment) throws IOException {
    handler = (TunnelingEventHandler) environment.get(TUNNELING_HANDLER);
    if (handler == null) { throw new IOException("Tunneling event handler must be defined in the start environment"); }
  }

  public synchronized void stop() {
    handler.stopAccept();
    handler = null;
  }

}
