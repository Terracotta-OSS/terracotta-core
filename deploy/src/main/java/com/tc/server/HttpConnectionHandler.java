/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.HttpConnectionContext;

import java.net.Socket;

public class HttpConnectionHandler extends AbstractEventHandler {

  private static final TCLogger     logger = TCLogging.getLogger(HttpConnectionContext.class);

  private final TerracottaConnector terracottaConnector;

  public HttpConnectionHandler(TerracottaConnector terracottaConnector) {
    this.terracottaConnector = terracottaConnector;
    //
  }

  public void handleEvent(EventContext context) {
    HttpConnectionContext connContext = (HttpConnectionContext) context;

    Socket s = connContext.getSocket();
    TCByteBuffer buffer = connContext.getBuffer();
    byte[] data = new byte[buffer.limit()];
    buffer.get(data);
    try {
      terracottaConnector.handleSocketFromDSO(s, data);
    } catch (Exception e) {
      logger.error(e);
    }
  }

}
