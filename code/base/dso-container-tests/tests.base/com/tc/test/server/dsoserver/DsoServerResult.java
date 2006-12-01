/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.dsoserver;

import com.tc.test.server.Server;
import com.tc.test.server.ServerResult;

/**
 * Values returned by the DSO server startup process.
 */
public final class DsoServerResult implements ServerResult {

  private final int    port;
  private final Server ref;

  public DsoServerResult(int port, Server ref) {
    this.port = port;
    this.ref = ref;
  }

  public int dsoPort() {
    return port;
  }

  public int serverPort() {
    return port;
  }

  public Server ref() {
    return ref;
  }
}
