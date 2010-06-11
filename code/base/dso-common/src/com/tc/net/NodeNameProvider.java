/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface NodeNameProvider {
  public static final NodeNameProvider DEFAULT_NODE_NAME_PROVIDER = new NodeNameProvider() {
                                                                    public String getNodeName() {
                                                                      try {
                                                                        return InetAddress.getLocalHost().getHostName();
                                                                      } catch (UnknownHostException e) {
                                                                        throw new RuntimeException(e);
                                                                      }
                                                                    }
                                                                  };

  String getNodeName();
}
