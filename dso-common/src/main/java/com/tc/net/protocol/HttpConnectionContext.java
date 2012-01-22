/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;

import java.net.Socket;

public class HttpConnectionContext implements EventContext {

  private final TCByteBuffer buffer;
  private final Socket       socket;

  public HttpConnectionContext(Socket socket, TCByteBuffer buffer) {
    this.socket = socket;
    this.buffer = buffer;
  }

  public TCByteBuffer getBuffer() {
    return buffer;
  }

  public Socket getSocket() {
    return socket;
  }

}
