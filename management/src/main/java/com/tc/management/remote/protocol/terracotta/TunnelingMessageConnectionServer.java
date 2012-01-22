/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
