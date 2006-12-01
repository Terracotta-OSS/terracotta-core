/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.test.server.Server;
import com.tc.test.server.ServerResult;

/**
 * Data Object returned by {@link AbstractAppServer.start()}.
 */
public final class AppServerResult implements ServerResult {

  private int    serverPort;
  private Server ref;

  public AppServerResult(int serverPort, Server ref) {
    this.serverPort = serverPort;
    this.ref = ref;
  }

  public int serverPort() {
    return serverPort;
  }

  public Server ref() {
    return ref;
  }
}
