/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.NodeNameProvider;

public class ServerNameProvider implements NodeNameProvider {
  private final String nodeName;

  public ServerNameProvider(String serverNodeId) {
    this.nodeName = serverNodeId;
  }

  public String getNodeName() {
    return this.nodeName;
  }

}
